package com.agyaccess.app.di

import com.agyaccess.app.data.remote.api.AgyApiService
import com.agyaccess.app.data.remote.network.SecureOkHttpProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpProvider: SecureOkHttpProvider,
        json: Json
    ): Retrofit {
        val contentType = "application/json; charset=utf-8".toMediaType()
        return Retrofit.Builder()
            // L'hôte réel est injecté et vérifié HTTPS à chaque requête par DynamicBaseUrlInterceptor
            .baseUrl("https://placeholder.invalid/")
            // Call.Factory délégué : applique immédiatement toute mise à jour de Certificate Pinning
            .callFactory { request -> okHttpProvider.getRestClient().newCall(request) }
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideAgyApiService(retrofit: Retrofit): AgyApiService =
        retrofit.create(AgyApiService::class.java)
}
