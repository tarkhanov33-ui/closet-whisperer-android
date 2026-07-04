package com.skydoves.whisperer.core.network.di

import com.skydoves.whisperer.core.network.interceptor.HttpRequestInterceptor
import com.skydoves.whisperer.core.network.service.ClosetService
import com.skydoves.whisperer.core.network.service.WeatherService
import com.skydoves.sandwich.retrofit.adapters.ApiResponseCallAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {

  @Provides
  @Singleton
  fun provideOkHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
      .addInterceptor(HttpRequestInterceptor())
      .build()
  }

  @Provides
  @Singleton
  @Named("Closet")
  fun provideClosetRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
    return Retrofit.Builder()
      .client(okHttpClient)
      .baseUrl("http://10.0.2.2:8081/ambient_invisible_intelligence/")
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
      .build()
  }

  @Provides
  @Singleton
  @Named("Weather")
  fun provideWeatherRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
    return Retrofit.Builder()
      .client(okHttpClient)
      .baseUrl("https://api.open-meteo.com/")
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
      .build()
  }

  @Provides
  @Singleton
  fun provideClosetService(@Named("Closet") retrofit: Retrofit): ClosetService {
    return retrofit.create(ClosetService::class.java)
  }

  @Provides
  @Singleton
  fun provideWeatherService(@Named("Weather") retrofit: Retrofit): WeatherService {
    return retrofit.create(WeatherService::class.java)
  }
}
