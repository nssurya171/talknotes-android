package com.example.talknotes.di

import com.example.talknotes.BuildConfig
import com.example.talknotes.data.remote.api.SummaryApi
import com.example.talknotes.data.remote.api.TranscriptionApi
import com.example.talknotes.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
            redactHeader("Authorization")
        }
    }

    @Provides
    @Singleton
    @Named("openAiClient")
    fun provideOpenAiOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        require(BuildConfig.OPENAI_API_KEY.isNotBlank()) {
            "OPENAI_API_KEY is empty. Check local.properties and build.gradle.kts"
        }

        android.util.Log.d("TalkNotes", "OPENAI key prefix = ${BuildConfig.OPENAI_API_KEY.take(10)}")

        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("openRouterClient")
    fun provideOpenRouterOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${BuildConfig.OPENROUTER_API_KEY}")
                    .addHeader("HTTP-Referer", "https://talknotes.app")
                    .addHeader("X-Title", "TalkNotes")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("openAiRetrofit")
    fun provideOpenAiRetrofit(
        @Named("openAiClient") okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.OPENAI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("openRouterRetrofit")
    fun provideOpenRouterRetrofit(
        @Named("openRouterClient") okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.OPENROUTER_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTranscriptionApi(
        @Named("openAiRetrofit") retrofit: Retrofit
    ): TranscriptionApi {
        return retrofit.create(TranscriptionApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSummaryApi(
        @Named("openRouterRetrofit") retrofit: Retrofit
    ): SummaryApi {
        return retrofit.create(SummaryApi::class.java)
    }
}