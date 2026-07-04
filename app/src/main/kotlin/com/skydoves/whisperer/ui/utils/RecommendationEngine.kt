package com.skydoves.whisperer.ui.utils

import com.skydoves.whisperer.core.model.ClothingItem
import com.skydoves.whisperer.core.model.Outfit


object RecommendationEngine {

  fun generateRecommendations(temp: Double, items: List<ClothingItem>): List<Outfit> {
    val wardrobe = items.filter { it.type.isNotBlank() && !it.type.equals("Outfit", ignoreCase = true) }
    if (wardrobe.isEmpty()) return emptyList()

    val insulationPref = when {
      temp >= 22.0 -> "Light"
      temp >= 15.0 -> "Medium"
      else -> "Warm"
    }
    val weatherDesc = "Best for ${temp.toInt()}°C"

    fun List<ClothingItem>.ofType(t: String) = filter { it.type.equals(t, ignoreCase = true) }
    fun List<ClothingItem>.byWeather() = sortedByDescending { it.insulation == insulationPref }

    val tops = wardrobe.ofType("Top").byWeather()
    val bottoms = wardrobe.ofType("Bottom").byWeather()
    val shoes = wardrobe.ofType("Shoes")
    val outerwear = wardrobe.ofType("Outerwear")
    val fullBody = wardrobe.ofType("Full Body").byWeather()
    val firstShoes = shoes.firstOrNull()

    val outfits = mutableListOf<Outfit>()
    fun add(title: String, reason: String, core: List<ClothingItem>, vararg extras: ClothingItem?) {
      if (core.isEmpty()) return
      val pieces = core + extras.filterNotNull()
      val fit = pieces.count { it.insulation == insulationPref }
      val pct = 60 + 40 * fit / pieces.size
      outfits.add(
        Outfit(
          title = title,
          score = "$pct%",
          items = pieces.joinToString(", ") { it.alias },
          reason = reason
        )
      )
    }

    // Everyday look
    if (tops.isNotEmpty() && bottoms.isNotEmpty()) {
      add("Everyday look", weatherDesc, listOf(tops[0], bottoms[0]), firstShoes)
    } else if (fullBody.isNotEmpty()) {
      add("Everyday look", weatherDesc, listOf(fullBody[0]), firstShoes)
    }

    // A dressier / alternative combination if the wardrobe allows it
    val smartTop = tops.firstOrNull {
      it.style.contains("Smart", true) || it.style.contains("Elegant", true) || it.style.contains("Formal", true)
    }
    val smartBottom = bottoms.firstOrNull {
      it.style.contains("Smart", true) || it.style.contains("Formal", true)
    }
    when {
      smartTop != null && smartBottom != null ->
        add("Smart look", "Dressier pick", listOf(smartTop, smartBottom), shoes.getOrNull(1) ?: firstShoes)
      tops.size >= 2 && bottoms.size >= 2 ->
        add("Alternative look", "Another combination", listOf(tops[1], bottoms[1]), firstShoes)
      fullBody.size >= 2 ->
        add("Alternative look", "Another combination", listOf(fullBody[1]), firstShoes)
    }

    // Layered look — only when there is a real core to layer over
    val core = when {
      tops.isNotEmpty() && bottoms.isNotEmpty() -> listOf(tops[0], bottoms[0])
      fullBody.isNotEmpty() -> listOf(fullBody[0])
      else -> emptyList()
    }
    if (outerwear.isNotEmpty() && core.isNotEmpty()) {
      add("Layered look", "Extra layer for wind or cold", listOf(outerwear[0]) + core, firstShoes)
    }

    return outfits.distinctBy { it.items }.take(3)
  }
}
