package com.example.workmanagerdemo.workmanger

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.workmanagerdemo.R
import com.example.workmanagerdemo.remote_data.FileApi
import com.example.workmanagerdemo.utils.Constants
import com.example.workmanagerdemo.utils.WorkerKeys
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.Exception
import javax.inject.Inject
import kotlin.random.Random


class DownloadWorker(private val context: Context, private val workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {


    lateinit var fileApi: FileApi

    override suspend fun doWork(): Result {
        fileApi = FileApi.instance
        showForegroundNotification()
        delay(500L)
        val response = fileApi.getFile()
        response.body()?.let { body ->
            return withContext(Dispatchers.IO) {
                val file = File(context.cacheDir, "workmanager.jpg")
                val outputstream = FileOutputStream(file)
                outputstream.use { stream ->
                    try {
                        stream.write(body.bytes())
                    } catch (e: Exception) {
                        return@withContext Result.failure(workDataOf(WorkerKeys.ERROR_MSG to e.localizedMessage))
                    }

                }

                Result.success(workDataOf(WorkerKeys.IMAGE_URI to file.toUri().toString()))
            }
        }
        if (!response.isSuccessful) {
            if (response.code().toString().startsWith("5")) {
                //server side error-so retry
                return Result.retry()
            }
            return Result.failure(workDataOf(WorkerKeys.ERROR_MSG to "Network error"))
        }

        return Result.failure(workDataOf(WorkerKeys.ERROR_MSG to "Unknown Error"))

    }

    private suspend fun showForegroundNotification() {
        setForeground(
            ForegroundInfo(
                Random.nextInt(), NotificationCompat.Builder(context, Constants.NOTIFIVATION_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentText("Downloading....")
                    .setContentTitle("Download in progress")
                    .build()
            )
        )

    }

}