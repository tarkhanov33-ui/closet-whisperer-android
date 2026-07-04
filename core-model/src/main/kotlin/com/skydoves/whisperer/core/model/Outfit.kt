package com.skydoves.whisperer.core.model

import com.squareup.moshi.Json

data class Outfit(
    val id: ObjectId? = null,
    val title: String,
    val score: String,
    val items: String,
    val reason: String,
    val date: String = "Today"
)

/** Server shape returned by POST /outfits and PUT /outfits/{id}/rate. */
data class OutfitResponse(
    @Json(name = "id") val outfitId: ObjectId? = null,
    val ownerProfileId: String? = null,
    val items: List<OutfitItemBinding> = emptyList(),
    val userRating: String? = null
)

data class OutfitItemBinding(
    val itemId: String? = null,
    val role: String? = null
)
