package com.glm.aiapp.data.repository

import com.glm.aiapp.data.api.GlmApi
import com.glm.aiapp.data.api.StreamingChatClient
import com.glm.aiapp.data.db.AppDatabase
import com.glm.aiapp.data.prefs.SettingsStore
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import android.content.Context
import androidx.room.Room
import com.glm.aiapp.domain.repository.*
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        isLenient = true
    }

    @Provides @Singleton
    fun provideOkHttp(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Default Retrofit instance for non-streaming endpoints.
     * Base URL is a placeholder — repos inject the configured base URL per call.
     */
    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://open.bigmodel.cn/api/paas/v4/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides @Singleton
    fun provideGlmApi(retrofit: Retrofit): GlmApi = retrofit.create(GlmApi::class.java)

    @Provides @Singleton
    fun providePlatformClient(): com.glm.aiapp.data.api.PlatformClient = com.glm.aiapp.data.api.PlatformClient()

    @Provides @Singleton
    fun provideStreamingChatClient(client: OkHttpClient, json: Json) =
        StreamingChatClient(client, json)

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides @Singleton
    fun provideSettingsStore(@ApplicationContext context: Context) = SettingsStore(context)

    // ---- Repositories ----

    @Provides @Singleton
    fun provideChatRepository(
        db: AppDatabase, api: GlmApi, stream: StreamingChatClient,
        settings: SettingsRepository, json: Json
    ): ChatRepository = ChatRepositoryImpl(db, api, stream, settings, json)

    @Provides @Singleton
    fun provideVisionRepository(
        platform: com.glm.aiapp.data.api.PlatformClient,
        settings: SettingsRepository
    ): VisionRepository = VisionRepositoryImpl(platform, settings)

    @Provides @Singleton
    fun provideImageRepository(
        platform: com.glm.aiapp.data.api.PlatformClient,
        db: AppDatabase,
        settings: SettingsRepository
    ): ImageRepository = ImageRepositoryImpl(platform, db, settings)

    @Provides @Singleton
    fun provideVideoRepository(api: GlmApi, db: AppDatabase): VideoRepository =
        VideoRepositoryImpl(api, db)

    @Provides @Singleton
    fun provideSpeechRepository(api: GlmApi, db: AppDatabase): SpeechRepository =
        SpeechRepositoryImpl(api, db)

    @Provides @Singleton
    fun provideSearchRepository(
        platform: com.glm.aiapp.data.api.PlatformClient,
        settings: SettingsRepository
    ): SearchRepository = SearchRepositoryImpl(platform, settings)

    @Provides @Singleton
    fun providePageReaderRepository(
        platform: com.glm.aiapp.data.api.PlatformClient,
        settings: SettingsRepository
    ): PageReaderRepository = PageReaderRepositoryImpl(platform, settings)

    @Provides @Singleton
    fun provideFineTuneRepository(db: AppDatabase): FineTuneRepository =
        FineTuneRepositoryImpl(db)

    @Provides @Singleton
    fun provideSettingsRepository(store: SettingsStore): SettingsRepository =
        SettingsRepositoryImpl(store)
}
