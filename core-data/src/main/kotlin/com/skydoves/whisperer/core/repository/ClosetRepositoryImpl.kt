package com.skydoves.whisperer.core.repository

import com.skydoves.sandwich.ApiResponse
import com.skydoves.whisperer.core.network.Dispatcher
import com.skydoves.whisperer.core.network.WhispererAppDispatchers
import com.skydoves.whisperer.core.network.service.ClosetService
import com.skydoves.whisperer.core.network.service.WeatherService
import com.skydoves.sandwich.suspendOnFailure
import com.skydoves.sandwich.suspendOnSuccess
import com.skydoves.whisperer.core.model.BodyParameters
import com.skydoves.whisperer.core.model.ClothingItem
import com.skydoves.whisperer.core.model.Outfit
import com.skydoves.whisperer.core.model.User
import com.skydoves.whisperer.core.model.UserId
import com.skydoves.whisperer.core.model.UserProfile
import com.skydoves.whisperer.core.model.Weather
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.IOException
import javax.inject.Inject

class ClosetRepositoryImpl @Inject constructor(
  private val closetService: ClosetService,
  private val weatherService: WeatherService,
  @Dispatcher(WhispererAppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ClosetRepository {

  private var activeEmail: String = ""
  private var activePassword: String = ""
  private var activeSystemID: String = "ambient_invisible_intelligence"

  private var cachedProfileId: String = ""

  override fun initSession(email: String, password: String, systemId: String) {
    activeEmail = email
    activePassword = password
    activeSystemID = systemId
    cachedProfileId = ""
    itemsCache.clear()
  }

  private val itemsCache = mutableListOf<ClothingItem>()

  private fun ClothingItem.packedForServer(): ClothingItem =
    if (imageUrl.isNotBlank() && !styleTag.contains("###"))
      copy(styleTag = "$styleTag###$imageUrl")
    else this

  private fun ClothingItem.unpackedFromServer(): ClothingItem {
    if (!styleTag.contains("###")) return this
    val parts = styleTag.split("###", limit = 2)
    return copy(styleTag = parts[0], imageUrl = parts.getOrElse(1) { "" })
  }

  override fun login(email: String, password: String): Flow<User> = flow {
    activeEmail = email
    activePassword = password
    // Sign-out clears the session to "", so restore this deployment's system id
    // here — otherwise the login path segment is empty and the server 404s.
    activeSystemID = "ambient_invisible_intelligence"

    val response = closetService.loginUser(activeSystemID, email, password)
    response.suspendOnSuccess {
      activeSystemID = data.userId.systemId
      try {
        val companionEmail = email.replace("@", ".user@")
        val companionUser = User(
          userId = UserId(companionEmail),
          role = "END_USER",
          username = "${data.username} Companion",
          avatar = data.avatar,
          password = password,
          gender = data.gender,
          dateOfBirth = data.dateOfBirth
        )
        closetService.registerUser(companionUser)
      } catch (_: Exception) {}
      resolveProfileId()
      emit(data)
    }.suspendOnFailure {
      throw IOException(describeFailure("Login failed", this))
    }
  }.flowOn(ioDispatcher)

  override fun register(
    email: String,
    name: String,
    password: String,
    gender: String,
    dateOfBirth: String
  ): Flow<User> = flow {
    activeEmail = email
    activePassword = password

    val reqUser = User(
      userId = UserId(email),
      role = "END_USER",
      username = name,
      avatar = name,
      password = password,
      gender = gender,
      dateOfBirth = dateOfBirth
    )
    val response = closetService.registerUser(reqUser)
    response.suspendOnSuccess {
      activeSystemID = data.userId.systemId
      try {
        val companionEmail = email.replace("@", ".user@")
        val companionUser = reqUser.copy(
          userId = UserId(companionEmail),
          username = "$name Companion"
        )
        closetService.registerUser(companionUser)
      } catch (_: Exception) {}
      resolveProfileId()
      emit(data)
    }.suspendOnFailure {
      throw IOException(describeFailure("Registration failed", this))
    }
  }.flowOn(ioDispatcher)

  /** Turns a Sandwich failure into a human-readable reason (server rejected vs. unreachable). */
  private fun describeFailure(prefix: String, failure: ApiResponse.Failure<*>): String =
    when (failure) {
      is ApiResponse.Failure.Error ->
        "$prefix: the server rejected the request. This account may already exist (try logging in), " +
          "or the password must have 5+ characters, a digit and a symbol."
      is ApiResponse.Failure.Exception ->
        "$prefix: can't reach the server — make sure it's running on port 8081."
    }

  private suspend fun resolveProfileId() {
    if (activeEmail.isBlank()) return
    try {
      val resp = closetService.getProfile(activeSystemID, activeEmail, activePassword)
      resp.suspendOnSuccess {
        cachedProfileId = data.id?.objectId ?: ""
      }
    } catch (_: Exception) {}
  }

  override fun fetchItems(): Flow<List<ClothingItem>> = flow {
    if (cachedProfileId.isBlank()) resolveProfileId()

    val response = closetService.getItems(
      ownerProfileId = cachedProfileId,
      userSystemID = activeSystemID,
      userEmail = activeEmail,
      userPassword = activePassword
    )
    response.suspendOnSuccess {
      val items = data.map { it.unpackedFromServer() }
      itemsCache.clear()
      itemsCache.addAll(items)
      emit(items)
    }.suspendOnFailure {
      throw IOException("Could not load your closet — is the server running?")
    }
  }.flowOn(ioDispatcher)

  override fun createItem(item: ClothingItem, imageBase64: String?): Flow<ClothingItem> = flow {
    if (cachedProfileId.isBlank()) resolveProfileId()

    // Send the photo as base64 in the request BODY (not the query string) — a
    // full image is far too large for a URL parameter.
    val itemWithProfile = item.copy(ownerProfileId = cachedProfileId, imageBase64 = imageBase64)
    val response = closetService.createItem(
      userSystemID = activeSystemID,
      userEmail = activeEmail,
      userPassword = activePassword,
      imageUrl = null,
      imageBase64 = null,
      item = itemWithProfile.packedForServer()
    )
    response.suspendOnSuccess {
      val saved = data.unpackedFromServer()
      itemsCache.add(saved)
      emit(saved)
    }.suspendOnFailure {
      throw IOException("Could not save the item — is the server running?")
    }
  }.flowOn(ioDispatcher)

  override fun analyzeImage(imageBase64: String): Flow<Map<String, Any>> = flow {
    val body = mapOf("imageBase64" to imageBase64)
    val response = closetService.analyzeImage(
      userSystemID = activeSystemID,
      userEmail = activeEmail,
      userPassword = activePassword,
      body = body
    )
    response.suspendOnSuccess {
      emit(data)
    }.suspendOnFailure {
      emit(emptyMap())
    }
  }.flowOn(ioDispatcher)

  override fun updateItem(item: ClothingItem): Flow<Boolean> = flow {
    val itemId = item.id?.objectId ?: ""

    val detailsResp = closetService.updateItemDetails(
      itemId = itemId,
      userSystemID = activeSystemID,
      userEmail = activeEmail,
      userPassword = activePassword,
      item = item.packedForServer()
    )
    detailsResp.suspendOnFailure {
      throw IOException("Could not update the item — is the server running?")
    }

    if (item.status.isNotBlank()) {
      val backendStatus = if (item.status.equals("Clean", ignoreCase = true)) "CLEAN" else "DIRTY"
      val statusResp = closetService.changeItemStatus(
        itemId = itemId,
        userSystemID = activeSystemID,
        userEmail = activeEmail,
        userPassword = activePassword,
        status = backendStatus
      )
      statusResp.suspendOnFailure {
        throw IOException("Could not update the item status — is the server running?")
      }
    }
    val idx = itemsCache.indexOfFirst { it.id?.objectId == itemId }
    if (idx != -1) itemsCache[idx] = item
    emit(true)
  }.flowOn(ioDispatcher)

  override fun deleteItem(systemID: String, objectId: String): Flow<Boolean> = flow {
    val response = closetService.deleteItem(
      itemId = objectId,
      userSystemID = activeSystemID,
      userEmail = activeEmail,
      userPassword = activePassword
    )
    response.suspendOnSuccess {
      itemsCache.removeAll { it.id?.objectId == objectId }
      emit(true)
    }.suspendOnFailure {
      throw IOException("Could not delete the item — is the server running?")
    }
  }.flowOn(ioDispatcher)

  override fun fetchWeather(lat: Double, lon: Double): Flow<Weather> = flow {
    // Prefer the backend /weather endpoint; fall back to open-meteo when it returns
    // nothing (e.g. no OpenWeather key configured) so the widget always works.
    var emitted = false
    try {
      val backend = closetService.getWeather(lat, lon, activeSystemID, activeEmail, activePassword)
      backend.suspendOnSuccess {
        val t = data.main?.temperature
        if (t != null) {
          emit(
            Weather(
              location = data.name ?: "",
              temp = t,
              condition = data.weather.firstOrNull()?.name ?: "Clear",
              humidity = (data.main?.humidity ?: 0).toDouble(),
              feelsLike = data.main?.feelsLike ?: t,
              windSpeed = data.wind?.speed ?: 0.0
            )
          )
          emitted = true
        }
      }
    } catch (_: Exception) {}

    if (!emitted) {
      val response = weatherService.fetchCurrentWeather(latitude = lat, longitude = lon)
      response.suspendOnSuccess {
        val cur = data.current
        val condition = when (cur.weather_code) {
          0 -> "Clear"
          1, 2, 3 -> "Partly Cloudy"
          45, 48 -> "Foggy"
          51, 53, 55 -> "Drizzle"
          61, 63, 65 -> "Rainy"
          71, 73, 75 -> "Snowy"
          80, 81, 82 -> "Rain Showers"
          else -> "Clear"
        }
        emit(
          Weather(
            temp = cur.temperature_2m,
            condition = condition,
            humidity = cur.relative_humidity_2m,
            feelsLike = cur.apparent_temperature,
            windSpeed = cur.wind_speed_10m
          )
        )
      }.suspendOnFailure {
        throw IOException("Could not load the weather right now")
      }
    }
  }.flowOn(ioDispatcher)

  override fun wearOutfit(itemIds: List<String>): Flow<Boolean> = flow {
    // Wearing an outfit dirties its garments: mark each item DIRTY on the backend.
    for (id in itemIds) {
      val response = closetService.changeItemStatus(
        itemId = id,
        userSystemID = activeSystemID,
        userEmail = activeEmail,
        userPassword = activePassword,
        status = "DIRTY"
      )
      response.suspendOnFailure {
        throw IOException("Could not mark the outfit as worn — is the server running?")
      }
      val idx = itemsCache.indexOfFirst { it.id?.objectId == id }
      if (idx != -1) itemsCache[idx] = itemsCache[idx].copy(status = "Dirty")
    }
    emit(true)
  }.flowOn(ioDispatcher)

  override fun generateOutfit(): Flow<Outfit> = flow {
    if (cachedProfileId.isBlank()) resolveProfileId()
    // Ensure we can resolve item ids -> names for display.
    if (itemsCache.isEmpty()) {
      val itemsResp = closetService.getItems(cachedProfileId, activeSystemID, activeEmail, activePassword)
      itemsResp.suspendOnSuccess {
        itemsCache.clear()
        itemsCache.addAll(data.map { it.unpackedFromServer() })
      }
    }
    val response = closetService.generateOutfit(
      userId = cachedProfileId,
      userSystemID = activeSystemID,
      userEmail = activeEmail,
      userPassword = activePassword,
      hints = emptyMap()
    )
    response.suspendOnSuccess {
      val names = data.items.mapNotNull { binding ->
        itemsCache.firstOrNull { it.id?.objectId == binding.itemId }?.alias
      }
      if (names.isEmpty()) {
        throw IOException("Not enough clean items for a look — add a top and a bottom.")
      }
      emit(
        Outfit(
          id = data.outfitId,
          title = "Today's look",
          score = "",
          items = names.joinToString(", "),
          reason = "Styled for your wardrobe & weather"
        )
      )
    }.suspendOnFailure {
      throw IOException(describeFailure("Could not generate a look", this))
    }
  }.flowOn(ioDispatcher)

  override fun likeOutfit(outfitId: String): Flow<Boolean> = flow {
    val response = closetService.rateOutfit(
      outfitId = outfitId,
      score = 1,
      userSystemID = activeSystemID,
      userEmail = activeEmail,
      userPassword = activePassword
    )
    response.suspendOnSuccess {
      emit(true)
    }.suspendOnFailure {
      throw IOException(describeFailure("Could not save the look", this))
    }
  }.flowOn(ioDispatcher)

  override fun rejectOutfit(
    outfitId: String,
    outfitTitle: String,
    items: String,
    comment: String
  ): Flow<Boolean> = flow {
    // 1. Rate the outfit DISLIKE so the backend registers the rejection and the
    //    stylist avoids its colors/styles on the next generation.
    if (outfitId.isNotBlank()) {
      val rateResp = closetService.rateOutfit(
        outfitId = outfitId,
        score = -1,
        userSystemID = activeSystemID,
        userEmail = activeEmail,
        userPassword = activePassword
      )
      rateResp.suspendOnFailure {
        throw IOException(describeFailure("Could not send your feedback", this))
      }
    }
    // 2. Log the free-text comment as a command (best-effort).
    try {
      val payload = mapOf(
        "command" to "rejectOutfit",
        "targetObject" to mapOf(
          "id" to mapOf("objectId" to outfitId.ifBlank { "outfit_engine" }, "systemID" to activeSystemID)
        ),
        "invokedBy" to mapOf(
          "userId" to mapOf("email" to activeEmail, "systemID" to activeSystemID)
        ),
        "commandAttributes" to mapOf(
          "outfit" to outfitTitle,
          "items" to items,
          "comment" to comment,
          "rating" to "DISLIKE"
        )
      )
      closetService.invokeCommand(activePassword, payload)
    } catch (_: Exception) {}
    emit(true)
  }.flowOn(ioDispatcher)

  override fun fetchProfile(): Flow<UserProfile> = flow {
    val response = closetService.getProfile(activeSystemID, activeEmail, activePassword)
    response.suspendOnSuccess {
      cachedProfileId = data.id?.objectId ?: ""
      emit(data)
    }.suspendOnFailure {
      throw IOException("Failed to load profile")
    }
  }.flowOn(ioDispatcher)

  override fun updateBiometrics(
    height: Double,
    weight: Double,
    proportions: String,
    skinTone: String,
    hairColor: String
  ): Flow<UserProfile> = flow {
    if (cachedProfileId.isBlank()) resolveProfileId()
    val requestProfile = UserProfile(
      bodyParameters = BodyParameters(
        height = height,
        weight = weight,
        proportions = proportions,
        skinTone = skinTone,
        hairColor = hairColor
      )
    )
    val response = closetService.updateBiometrics(
      userId = cachedProfileId,
      userSystemID = activeSystemID,
      userEmail = activeEmail,
      userPassword = activePassword,
      profile = requestProfile
    )
    response.suspendOnSuccess {
      emit(data)
    }.suspendOnFailure {
      throw IOException("Failed to save body parameters")
    }
  }.flowOn(ioDispatcher)

  override fun updatePreferences(
    stylePreference: String,
    thermalSensitivity: String
  ): Flow<UserProfile> = flow {
    if (cachedProfileId.isBlank()) resolveProfileId()
    val requestProfile = UserProfile(
      stylePreference = stylePreference,
      thermalSensitivity = thermalSensitivity
    )
    val response = closetService.updatePreferences(
      userId = cachedProfileId,
      userSystemID = activeSystemID,
      userEmail = activeEmail,
      userPassword = activePassword,
      profile = requestProfile
    )
    response.suspendOnSuccess {
      emit(data)
    }.suspendOnFailure {
      throw IOException("Failed to save stylist settings")
    }
  }.flowOn(ioDispatcher)

  override fun updateUser(username: String, gender: String, dateOfBirth: String): Flow<Boolean> = flow {
    // password = null so it isn't changed; avatar "" so the backend leaves it as-is.
    val body = User(
      userId = UserId(activeEmail, activeSystemID),
      role = "END_USER",
      username = username,
      avatar = "",
      password = null,
      gender = gender,
      dateOfBirth = dateOfBirth
    )
    val response = closetService.updateUser(activeSystemID, activeEmail, activePassword, body)
    response.suspendOnSuccess {
      emit(true)
    }.suspendOnFailure {
      throw IOException(describeFailure("Could not save your details", this))
    }
  }.flowOn(ioDispatcher)

  override fun fetchOutfitHistory(): Flow<List<Outfit>> = flow {
    if (cachedProfileId.isBlank()) resolveProfileId()
    if (itemsCache.isEmpty()) {
      val itemsResp = closetService.getItems(cachedProfileId, activeSystemID, activeEmail, activePassword)
      itemsResp.suspendOnSuccess {
        itemsCache.clear()
        itemsCache.addAll(data.map { it.unpackedFromServer() })
      }
    }
    val response = closetService.getOutfitHistory(
      userId = cachedProfileId,
      userSystemID = activeSystemID,
      userEmail = activeEmail,
      userPassword = activePassword
    )
    response.suspendOnSuccess {
      val outfits = data.filter { it.userRating == "LIKE" }.map { resp ->
        val names = resp.items.mapNotNull { binding ->
          itemsCache.firstOrNull { it.id?.objectId == binding.itemId }?.alias
        }
        val itemsStr = names.joinToString(", ")
        Outfit(
          id = resp.outfitId,
          title = "Look: " + itemsStr,
          score = "",
          items = itemsStr,
          reason = "Styled for your wardrobe & weather",
          date = "Approved"
        )
      }
      emit(outfits)
    }.suspendOnFailure {
      throw IOException("Could not load outfits history")
    }
  }.flowOn(ioDispatcher)
}
