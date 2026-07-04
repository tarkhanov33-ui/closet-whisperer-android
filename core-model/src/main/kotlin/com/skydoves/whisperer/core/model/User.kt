package com.skydoves.whisperer.core.model

import com.squareup.moshi.Json

data class UserId(
    val email: String,
    @Json(name = "systemID") val systemId: String = "ambient_invisible_intelligence"
)

data class User(
    val userId: UserId,
    val role: String = "END_USER",
    val username: String,
    val avatar: String,
    val password: String? = null,
    val gender: String = "",
    val dateOfBirth: String = ""
)

data class BodyParameters(
    val height: Double? = null,
    val weight: Double? = null,
    val proportions: String? = null,
    val skinTone: String? = null,
    val hairColor: String? = null
)

data class UserProfile(
    @Json(name = "id") val id: ObjectId? = null,
    val ownerEmail: String = "",
    val bodyParameters: BodyParameters? = null,
    val thermalSensitivity: String? = null,
    val stylePreference: String? = null,
    val profileImage: String? = null
)
