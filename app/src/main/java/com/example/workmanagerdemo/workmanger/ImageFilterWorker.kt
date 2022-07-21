package com.example.workmanagerdemo.workmanger

import android.content.Context
import android.graphics.*
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.workmanagerdemo.utils.WorkerKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream


class ImageFilterWorker(
    private val context: Context,
    private val workerParameters: WorkerParameters
) :
    CoroutineWorker(context, workerParameters) {
    @RequiresApi(Build.VERSION_CODES.Q)
    override suspend fun doWork(): Result {
        val image_file =
            workerParameters.inputData.getString(WorkerKeys.IMAGE_URI)?.toUri()?.toFile()

        delay(500L)

        return image_file?.let { file ->
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            var filtered_bitmap = bitmap.copy(bitmap.config, true)
            val paint = Paint()
            paint.colorFilter = LightingColorFilter(0xFFDD000, 5)

            val canvas = Canvas(filtered_bitmap)
            canvas.drawBitmap(filtered_bitmap, 0f, 0f, paint)

            withContext(Dispatchers.IO) {

                filtered_bitmap = callFilterFunUsingKey("Invert", filtered_bitmap)
                val result_file = File(context.cacheDir, "filter_workmanager.jpg")
                val outputstream = FileOutputStream(result_file)
                val success = filtered_bitmap.compress(
                    Bitmap.CompressFormat.JPEG, 90, outputstream
                )
                if (success) {
                    Result.success(
                        workDataOf(
                            WorkerKeys.FILTER_IMAGE_URI to result_file.toUri().toString()
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