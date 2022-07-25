package com.example.workmanagerdemo.di.modules

import android.content.Context
import androidx.work.WorkManager
import com.example.workmanagerdemo.FilterRepository
import com.example.workmanagerdemo.remote_data.FileApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    //provide ApiSource
    @Singleton
    @Provides
    fun getApiSource(retrofit: Retrofit): FileApi {
        return retrofit.create(FileApi::class.java)
    }

    @Singleton
    @Provides
    fun getWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Singleton
    @Provides
    fun provideRepository(workManager: WorkManager): FilterRepository {
        return FilterRepository(workManager)
    }
}