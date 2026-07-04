package com.skydoves.whisperer.core.di

import com.skydoves.whisperer.core.repository.ClosetRepository
import com.skydoves.whisperer.core.repository.ClosetRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface DataModule {

  @Binds
  @Singleton
  fun bindsClosetRepository(closetRepositoryImpl: ClosetRepositoryImpl): ClosetRepository
}
