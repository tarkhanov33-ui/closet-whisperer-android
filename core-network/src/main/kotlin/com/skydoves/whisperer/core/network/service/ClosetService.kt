package com.skydoves.whisperer.core.network.service

import com.skydoves.whisperer.core.model.ClothingItem
import com.skydoves.whisperer.core.model.User
import com.skydoves.whisperer.core.model.UserProfile
import com.skydoves.whisperer.core.model.OutfitResponse
import com.skydoves.whisperer.core.model.BackendWeather
import com.skydoves.sandwich.ApiResponse
import retrofit2.http.*


interface ClosetService {

    

    @POST("users")
    suspend fun registerUser(
        @Body user: User
    ): ApiResponse<User>

    @GET("users/login/{systemId}/{userEmail}")
    suspend fun loginUser(
        @Path("systemId") systemId: String,
        @Path("userEmail") userEmail: String,
        @Query("password") password: String
    ): ApiResponse<User>

    @PUT("users/{systemId}/{userEmail}")
    suspend fun updateUser(
        @Path("systemId") systemId: String,
        @Path("userEmail") userEmail: String,
        @Query("password") password: String,
        @Body user: User
    ): ApiResponse<Unit>

    

    
    @GET("profiles")
    suspend fun getProfile(
        @Query("userSystemID") userSystemID: String,
        @Query("userEmail") userEmail: String,
        @Query("userPassword") userPassword: String
    ): ApiResponse<UserProfile>

    @PUT("profiles/{userId}/biometrics")
    suspend fun updateBiometrics(
        @Path("userId") userId: String,
        @Query("userSystemID") userSystemID: String,
        @Query("userEmail") userEmail: String,
        @Query("userPassword") userPassword: String,
        @Body profile: UserProfile
    ): ApiResponse<UserProfile>

    @PUT("profiles/{userId}/preferences")
    suspend fun updatePreferences(
        @Path("userId") userId: String,
        @Query("userSystemID") userSystemID: String,
        @Query("userEmail") userEmail: String,
        @Query("userPassword") userPassword: String,
        @Body profile: UserProfile
    ): ApiResponse<UserProfile>

    

    
    @GET("items")
    suspend fun getItems(
        @Query("ownerProfileId") ownerProfileId: String,
        @Query("userSystemID") userSystemID: String,
        @Query("userEmail") userEmail: String,
        @Query("userPassword") userPassword: String
    ): ApiResponse<List<ClothingItem>>

    
    @POST("items")
    suspend fun createItem(
        @Query("userSystemID") userSystemID: String,
        @Query("userEmail") userEmail: String,
        @Query("userPassword") userPassword: String,
        @Query("imageUrl") imageUrl: String? = null,
        @Query("imageBase64") imageBase64: String? = null,
        @Body item: ClothingItem
    ): ApiResponse<ClothingItem>

    @POST("items/analyze")
    suspend fun analyzeImage(
        @Query("userSystemID") userSystemID: String,
        @Query("userEmail") userEmail: String,
        @Query("userPassword") userPassword: String,
        @Body body: Map<String, String>
    ): ApiResponse<Map<String, Any>>

    
    @PUT("items/{itemId}/details")
    suspend fun updateItemDetails(
        @Path("itemId") itemId: String,
        @Query("userSystemID") userSystemID: String,
        @Query("userEmail") userEmail: String,
        @Query("userPassword") userPassword: String,
        @Body item: ClothingItem
    ): ApiResponse<ClothingItem>

    
    @PUT("items/{itemId}/status")
    suspend fun changeItemStatus(
        @Path("itemId") itemId: String,
        @Query("userSystemID") userSystemID: String,
        @Query("userEmail") userEmail: String,
        @Query("userPassword") userPassword: String,
        @Query("status") status: String
    ): ApiResponse<ClothingItem>

    
    @DELETE("items/{itemId}")
    suspend fun deleteItem(
        @Path("itemId") itemId: String,
        @Query("userSystemID") userSystemID: String,
        @Query("userEmail") userEmail: String,
        @Query("userPassword") userPassword: String
    ): ApiResponse<Unit>

    

    @POST("commands")
    suspend fun invokeCommand(
        @Query("userPassword") userPassword: String,
        @Body command: Any
    ): ApiResponse<List<Any>>

    @GET("weather")
    suspend fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("userSystemID") userSystemID: String,
        @Query("userEmail") userEmail: String,
        @Query("userPassword") userPassword: String
    ): ApiResponse<BackendWeather>

    @POST("outfits")
    suspend fun generateOutfit(
        @Query("userId") userId: String,
        @Query("userSystemID") userSystemID: String,
        @Query("userEmail") userEmail: String,
        @Query("userPassword") userPassword: String,
        @Body hints: Map<String, String>
    ): ApiResponse<OutfitResponse>

    @PUT("outfits/{outfitId}/rate")
    suspend fun rateOutfit(
        @Path("outfitId") outfitId: String,
        @Query("score") score: Int,
        @Query("userSystemID") userSystemID: String,
        @Query("userEmail") userEmail: String,
        @Query("userPassword") userPassword: String
    ): ApiResponse<OutfitResponse>

    @GET("outfits/history")
    suspend fun getOutfitHistory(
        @Query("userId") userId: String,
        @Query("userSystemID") userSystemID: String,
        @Query("userEmail") userEmail: String,
        @Query("userPassword") userPassword: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100
    ): ApiResponse<List<OutfitResponse>>
}
