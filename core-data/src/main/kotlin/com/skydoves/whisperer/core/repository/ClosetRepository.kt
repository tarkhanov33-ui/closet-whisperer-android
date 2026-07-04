package com.skydoves.whisperer.core.repository

import com.skydoves.whisperer.core.model.ClothingItem
import com.skydoves.whisperer.core.model.Outfit
import com.skydoves.whisperer.core.model.User
import com.skydoves.whisperer.core.model.UserProfile
import com.skydoves.whisperer.core.model.Weather
import kotlinx.coroutines.flow.Flow

interface ClosetRepository {
  fun initSession(email: String, password: String, systemId: String = "ambient_invisible_intelligence")
  fun login(email: String, password: String): Flow<User>
  fun register(email: String, name: String, password: String, gender: String, dateOfBirth: String): Flow<User>
  fun fetchItems(): Flow<List<ClothingItem>>
  fun createItem(item: ClothingItem, imageBase64: String? = null): Flow<ClothingItem>
  fun analyzeImage(imageBase64: String): Flow<Map<String, Any>>

  /** Real current weather for the given coordinates (open-meteo). */
  fun fetchWeather(lat: Double, lon: Double): Flow<Weather>
  fun deleteItem(systemID: String, objectId: String): Flow<Boolean>
  fun updateItem(item: ClothingItem): Flow<Boolean>

  /** Marks every item in a worn outfit as Dirty on the backend (real laundry update). */
  fun wearOutfit(itemIds: List<String>): Flow<Boolean>

  /** Generates an outfit on the backend (POST /outfits — LLM stylist / weighted scoring). */
  fun generateOutfit(): Flow<Outfit>

  /** Rates a backend outfit LIKE (saves it to favorites; boosts its colors/styles). */
  fun likeOutfit(outfitId: String): Flow<Boolean>

  /**
   * Rejects a backend outfit: rates it DISLIKE (so the backend learns to avoid its
   * colors/styles for the next generation) and logs the free-text comment.
   */
  fun rejectOutfit(outfitId: String, outfitTitle: String, items: String, comment: String): Flow<Boolean>
  fun fetchProfile(): Flow<UserProfile>
  fun updateBiometrics(height: Double, weight: Double, proportions: String, skinTone: String, hairColor: String): Flow<UserProfile>
  fun updatePreferences(stylePreference: String, thermalSensitivity: String): Flow<UserProfile>

  /** Updates the signed-in user's account details (username / date of birth) on the backend. */
  fun updateUser(username: String, gender: String, dateOfBirth: String): Flow<Boolean>
  fun fetchOutfitHistory(): Flow<List<Outfit>>
}
