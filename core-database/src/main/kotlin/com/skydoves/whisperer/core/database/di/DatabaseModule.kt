package com.skydoves.whisperer.core.database.di

import android.app.Application
import androidx.room.Room
import com.skydoves.whisperer.core.database.ClosetDatabase
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

  @Provides
  @Singleton
  fun provideMoshi(): Moshi {
    return Moshi.Builder()
      .addLast(KotlinJsonAdapterFactory())
      .build()
  }

  @Provides
  @Singleton
  fun provideAppDatabase(
    application: Application,
  ): ClosetDatabase {
    return Room
      .databaseBuilder(application, ClosetDatabase::class.java, "Closet.db")
      .fallbackToDestructiveMigration()
      .build()
  }
}
