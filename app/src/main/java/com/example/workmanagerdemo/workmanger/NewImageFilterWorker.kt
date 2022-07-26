package com.example.workmanagerdemo.workmanger

import BitMapProccessor
import android.content.Context
import android.graphics.*
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.workmanagerdemo.utils.WorkerKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class NewImageFilterWorker(
    private val context: Context,
    private val workerParameters: WorkerParameters
) :
    CoroutineWorker(context, workerParameters) {
    @RequiresApi(Build.VERSION_CODES.Q)
    override suspend fun doWork(): Result {
        setProgressAsync(Data.Builder().putInt("progress", 0).build())
        val data = inputData
        val filterKey = data.getString("filter_key")
        val imageUri = data.getString("imageUri")
        val image_file =
            imageUri?.toUri()?.toFile()

        setProgressAsync(Data.Builder().putInt("progress", 20).build())
        println(image_file)
        println(image_file?.absolutePath)
        return image_file?.let { file ->
            val bitmap = BitmapFactory.decodeFile(file.path)
            var filtered_bitmap = bitmap.copy(bitmap.config, true)
            setProgressAsync(Data.Builder().putInt("progress", 50).build())
            withContext(Dispatchers.IO) {
                filtered_bitmap = callFilterFunUsingKey(filterKey!!, filtered_bitmap)
                setProgressAsync(Data.Builder().putInt("progress", 80).build())
                val result_file = File(context.cacheDir, "filter_workmanager.jpg")
                val outputstream = FileOutputStream(result_file)
                val success = filtered_bitmap.compress(
                    Bitmap.CompressFormat.JPEG, 90, outputstream
                )
                if (success) {
                    setProgressAsync(Data.Builder().putInt("progress", 100).build())
                    Result.success(
                        workDataOf(
                            WorkerKeys.FILTER_IMAGE_URI to result_file.toUri().toString(),
                            WorkerKeys.NEW_IMAGE_URI to result_file.absolutePath
                        )
                    )

                } else {
                    Result.failure()
                }
            }
        } ?: Result.failure()
    }


    fun callFilterFunUsingKey(filter: String, bitmap: Bitmap): Bitmap {
        return when (filter) {
            "Rotate" -> {
                BitMapProccessor.rotate(bitmap, 180f)
            }
            "Flip" -> {
                BitMapProccessor.flip(bitmap, false, true)
            }
            "Emoboss" -> {
                BitMapProccessor.emboss(bitmap)
            }
            "Cfilter" -> {
                BitMapProccessor.cfilter(bitmap, 100.toDouble(), 100.toDouble(), 100.toDouble())
            }
            "Gaussian" -> {
                BitMapProccessor.gaussian(bitmap)
            }
            "Cdepth" -> {
                BitMapProccessor.cdepth(bitmap, 32)
            }
            "Sharpen" -> {
                BitMapProccessor.sharpen(bitmap)
            }
            "Noise" -> {
                BitMapProccessor.noise(bitmap)
            }
            "Brightness" -> {
                BitMapProccessor.brightness(bitmap, 255)
            }
            "Sepia" -> {
                BitMapProccessor.sepia(bitmap)
            }
            "Gamma" -> {
                BitMapProccessor.gamma(bitmap, 45.toDouble(), 45.toDouble(), 45.toDouble())
            }
            "Contrast" -> {
                BitMapProccessor.contrast(bitmap, 100.toDouble())
            }
            "Saturation" -> {
                BitMapProccessor.saturation(bitmap, 100)
            }
            "Grayscale" -> {
                BitMapProccessor.grayscale(bitmap)
            }
            "Vignette" -> {
                BitMapProccessor.vignette(bitmap)
            }
            "Hue" -> {
                BitMapProccessor.hue(bitmap, 0f)
            }

            "Invert" -> {
                BitMapProccessor.invert(bitmap)
            }
            "Boost" -> {
                BitMapProccessor.boost(bitmap, 2, 150f)
            }
            "Sketch" -> {
                BitMapProccessor.sketch(bitmap)
            }
            else -> {
                bitmap
            }


        }
    }
}