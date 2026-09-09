package com.example.eyesbuddy.di

import android.content.Context
import com.example.eyesbuddy.domain.quotes.LocalQuoteRepository
import com.example.eyesbuddy.domain.quotes.QuoteRepository
import com.example.eyesbuddy.domain.motion.MotionInterpreter
import com.example.eyesbuddy.sensors.MotionSensorConsentStore
import com.example.eyesbuddy.weather.LocationPermissionGate
import com.example.eyesbuddy.weather.LocationSource
import com.example.eyesbuddy.weather.WeatherRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides small, replaceable application services for the presentation layer. */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideMotionInterpreter(): MotionInterpreter = MotionInterpreter()

    @Provides
    @Singleton
    fun provideMotionConsentStore(@ApplicationContext context: Context): MotionSensorConsentStore = MotionSensorConsentStore(context)

    @Provides
    @Singleton
    fun provideLocationPermissionGate(@ApplicationContext context: Context): LocationPermissionGate = LocationPermissionGate(context)

    @Provides
    @Singleton
    fun provideLocationSource(@ApplicationContext context: Context): LocationSource = LocationSource(context)

    @Provides
    @Singleton
    fun provideWeatherRepository(): WeatherRepository = WeatherRepository()

    @Provides
    @Singleton
    fun provideQuoteRepository(): QuoteRepository = LocalQuoteRepository()
}
