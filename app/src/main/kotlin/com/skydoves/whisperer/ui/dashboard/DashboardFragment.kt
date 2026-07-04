package com.skydoves.whisperer.ui.dashboard

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.skydoves.whisperer.R
import com.skydoves.whisperer.core.model.ClothingItem
import com.skydoves.whisperer.core.model.HistoryItem
import com.skydoves.whisperer.core.model.Outfit
import com.skydoves.whisperer.core.repository.ClosetRepository
import com.skydoves.whisperer.ui.adapter.ClothingAdapter
import com.skydoves.whisperer.ui.adapter.HistoryAdapter
import com.skydoves.whisperer.ui.main.MainActivity
import com.skydoves.whisperer.ui.utils.LocationProvider
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Dashboard: greeting, live weather, recommended pieces and "Lately worn".
 * The outfit suggestion is generated on the backend on demand (button) and can
 * be rejected with a comment; rejecting rates it DISLIKE on the backend so the
 * next generation avoids it.
 */
@AndroidEntryPoint
class DashboardFragment : Fragment() {

  @Inject
  lateinit var closetRepository: ClosetRepository

  private lateinit var heroOutfitTitle: TextView
  private lateinit var heroOutfitItems: TextView
  private lateinit var heroOutfitScore: TextView
  private lateinit var heroOutfitReason: TextView

  private lateinit var weatherTempText: TextView
  private lateinit var weatherLocationText: TextView
  private lateinit var weatherConditionText: TextView
  private lateinit var weatherFeelsLikeText: TextView
  private lateinit var weatherHumidityText: TextView
  private lateinit var weatherWindText: TextView

  private lateinit var recommendedAdapter: ClothingAdapter
  private lateinit var historyAdapter: HistoryAdapter
  private var historySection: View? = null

  private lateinit var heroCard: View
  private lateinit var generateButton: Button
  private lateinit var actionsContainer: View
  private lateinit var wearButton: Button
  private lateinit var rejectButton: Button

  private var currentWardrobe: List<ClothingItem> = emptyList()
  private var currentTemp = 20.0
  private var currentLook: Outfit? = null

  private val locationPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { loadDashboardData() }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

    heroOutfitTitle = view.findViewById(R.id.heroOutfitTitle)
    heroOutfitItems = view.findViewById(R.id.heroOutfitItems)
    heroOutfitScore = view.findViewById(R.id.heroOutfitScore)
    heroOutfitReason = view.findViewById(R.id.heroOutfitReason)

    weatherTempText = view.findViewById(R.id.weatherTempText)
    weatherLocationText = view.findViewById(R.id.weatherLocationText)
    weatherConditionText = view.findViewById(R.id.weatherConditionText)
    weatherFeelsLikeText = view.findViewById(R.id.weatherFeelsLikeText)
    weatherHumidityText = view.findViewById(R.id.weatherHumidityText)
    weatherWindText = view.findViewById(R.id.weatherWindText)

    historySection = view.findViewById(R.id.dashboardHistorySection)
    heroCard = view.findViewById(R.id.outfitHeroCard)
    generateButton = view.findViewById(R.id.dashboardGenerateButton)
    actionsContainer = view.findViewById(R.id.dashboardActionsContainer)
    wearButton = view.findViewById(R.id.dashboardWearButton)
    rejectButton = view.findViewById(R.id.dashboardRejectButton)

    generateButton.setOnClickListener { generateLook() }
    rejectButton.setOnClickListener { showRejectDialog() }
    wearButton.setOnClickListener { wearLook() }

    val greetingName = view.findViewById<TextView>(R.id.dashboardGreetingName)
    val userName = activity?.intent?.getStringExtra("user_name").orEmpty()
    greetingName.text = userName.substringBefore(" ")

    val recommendedRecyclerView = view.findViewById<RecyclerView>(R.id.dashboardRecommendedRecyclerView)
    recommendedRecyclerView.layoutManager = GridLayoutManager(context, 2)
    recommendedAdapter = ClothingAdapter(onItemClick = { item ->
      Toast.makeText(context, "Item: ${item.alias} (${item.type})", Toast.LENGTH_SHORT).show()
    })
    recommendedRecyclerView.adapter = recommendedAdapter

    val historyRecyclerView = view.findViewById<RecyclerView>(R.id.dashboardHistoryRecyclerView)
    historyRecyclerView.layoutManager = LinearLayoutManager(context)
    historyAdapter = HistoryAdapter()
    historyRecyclerView.adapter = historyAdapter

    view.findViewById<View>(R.id.dashboardSeeAllButton).setOnClickListener {
      (activity as? MainActivity)?.selectTab(R.id.navigation_closet)
    }

    if (LocationProvider.hasPermission(requireContext())) {
      loadDashboardData()
    } else {
      locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    return view
  }

  private fun loadDashboardData() {
    lifecycleScope.launch {
      val place = withContext(Dispatchers.IO) { LocationProvider.current(requireContext()) }
      val lat = place?.lat ?: LocationProvider.DEFAULT_LAT
      val lon = place?.lon ?: LocationProvider.DEFAULT_LON
      combine(
        closetRepository.fetchItems(),
        closetRepository.fetchWeather(lat, lon),
        closetRepository.fetchOutfitHistory()
      ) { items, weather, history -> Triple(items, weather, history) }
        .catch { err ->
          Toast.makeText(context, err.message ?: "Could not reach the server", Toast.LENGTH_LONG).show()
        }
        .collect { (items, weather, history) ->
          val cityName = place?.city.orEmpty()
          weatherLocationText.text = cityName
          weatherLocationText.visibility = if (cityName.isBlank()) View.GONE else View.VISIBLE

          currentWardrobe = items.filter { it.type.isNotBlank() && !it.type.equals("Outfit", ignoreCase = true) }
          currentTemp = weather.temp

          recommendedAdapter.submitList(currentWardrobe.filter { it.status.equals("Clean", ignoreCase = true) }.take(4))

          val worn = currentWardrobe
            .filter { it.status.equals("Dirty", ignoreCase = true) }
            .map { HistoryItem(day = "Worn", outfit = it.alias, score = "", weather = "${it.color} · ${it.style}") }
          historyAdapter.submitList(worn)
          historySection?.visibility = if (worn.isEmpty()) View.GONE else View.VISIBLE

          weatherTempText.text = "${weather.temp.toInt()}°C"
          weatherConditionText.text = weather.condition
          weatherFeelsLikeText.text = "${weather.feelsLike.toInt()}°C"
          weatherHumidityText.text = "${weather.humidity.toInt()}%"
          weatherWindText.text = "${weather.windSpeed.toInt()} km/h"

          if (history.isNotEmpty()) {
            val approvedLook = history.first()
            currentLook = approvedLook
            showApprovedLook(approvedLook)
          } else {
            if (currentLook == null) {
              hideLook()
            }
          }
        }
    }
  }


  private fun generateLook() {
    generateButton.isEnabled = false
    generateButton.text = "Generating…"
    lifecycleScope.launch {
      closetRepository.generateOutfit()
        .catch { err ->
          generateButton.isEnabled = true
          generateButton.text = "Generate today's look"
          hideLook()
          Toast.makeText(context, err.message ?: "Could not generate a look", Toast.LENGTH_LONG).show()
        }
        .collect { look ->
          generateButton.isEnabled = true
          generateButton.text = "Regenerate"
          currentLook = look
          showLook(look)
        }
    }
  }

  private fun showLook(look: Outfit) {
    heroOutfitTitle.text = look.title
    heroOutfitItems.text = look.items
    heroOutfitScore.text = "★ Stylist"
    heroOutfitScore.visibility = View.VISIBLE
    heroOutfitReason.text = look.reason

    heroCard.visibility = View.VISIBLE
    actionsContainer.visibility = View.VISIBLE
    generateButton.text = "Regenerate today's look"
  }

  private fun showApprovedLook(look: Outfit) {
    heroOutfitTitle.text = look.title
    heroOutfitItems.text = look.items
    heroOutfitScore.text = "★ Stylist"
    heroOutfitScore.visibility = View.VISIBLE
    heroOutfitReason.text = look.reason

    heroCard.visibility = View.VISIBLE
    actionsContainer.visibility = View.GONE
    generateButton.text = "Regenerate today's look"
  }

  private fun hideLook() {
    heroCard.visibility = View.GONE
    actionsContainer.visibility = View.GONE
    generateButton.text = "Generate today's look"
  }

  private fun wearLook() {
    val look = currentLook ?: return
    val id = look.id?.objectId ?: return
    val names = look.items.split(",").map { it.trim().lowercase() }
    val ids = currentWardrobe.filter { it.alias.lowercase() in names }.mapNotNull { it.id?.objectId }
    if (ids.isEmpty()) {
      Toast.makeText(context, "Could not match these items in your closet", Toast.LENGTH_SHORT).show()
      return
    }
    lifecycleScope.launch {
      closetRepository.likeOutfit(id)
        .catch { /* ignore liking failure if wear succeeds */ }
        .collect {
          closetRepository.wearOutfit(ids)
            .catch { err -> Toast.makeText(context, err.message ?: "Server unavailable", Toast.LENGTH_LONG).show() }
            .collect {
              Toast.makeText(context, "Look approved and items marked as dirty!", Toast.LENGTH_SHORT).show()
              loadDashboardData()
            }
        }
    }
  }

  private fun showRejectDialog() {
    val look = currentLook ?: return
    val context = requireContext()
    val builder = AlertDialog.Builder(context, R.style.CustomAlertDialogTheme)
    val input = EditText(builder.context).apply {
      hint = "Why doesn't this work for you? (optional)"
      minLines = 2
      setTextColor(ContextCompat.getColor(context, R.color.ink_primary))
      setHintTextColor(ContextCompat.getColor(context, R.color.ink_muted))
    }
    builder
      .setTitle("Reject this look")
      .setView(input)
      .setPositiveButton("Send & regenerate") { d, _ ->
        val comment = input.text.toString().trim()
        lifecycleScope.launch {
          closetRepository.rejectOutfit(look.id?.objectId ?: "", look.title, look.items, comment)
            .catch { err -> Toast.makeText(context, err.message ?: "Could not send feedback", Toast.LENGTH_LONG).show() }
            .collect {
              Toast.makeText(context, "Thanks — building a different look.", Toast.LENGTH_SHORT).show()
              generateLook()
            }
        }
        d.dismiss()
      }
      .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
      .show()
  }
}
