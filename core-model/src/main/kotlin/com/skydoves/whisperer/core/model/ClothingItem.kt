package com.skydoves.whisperer.core.model

import com.squareup.moshi.Json

data class ObjectId(
    val objectId: String,
    @Json(name = "systemID") val systemId: String = "ambient_invisible_intelligence"
) {
    val systemID: String get() = systemId
}

data class CreatedBy(
    val userId: UserId
)

data class Location(
    val lat: Double = 32.0853,
    val lng: Double = 34.7818
)

data class ClothingItem(
    val id: ObjectId? = null,
    val ownerProfileId: String? = null,
    val category: String = "",
    val subCategory: String = "",
    val color: String = "",
    val status: String = "Clean",
    val styleTag: String = "",
    val thermalInsulation: Int = 1,
    val active: Boolean = true,
    val imageUrl: String = "",
    // Backend now stores the photo as base64 (imageBase64 column) and returns it here.
    val imageBase64: String? = null
) {
    val type: String get() = category
    val alias: String get() = subCategory
    val style: String get() = styleTag
    val insulation: String
        get() = when (thermalInsulation) {
            2    -> "Medium"
            3    -> "Warm"
            else -> "Light"
        }
    val image: String get() = imageUrl
}
