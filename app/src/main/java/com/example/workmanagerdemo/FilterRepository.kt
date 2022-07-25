package com.example.workmanagerdemo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.work.*
import com.example.workmanagerdemo.utils.TryHandler.exceptionalCode
import com.example.workmanagerdemo.workmanger.NewImageFilterWorker
import java.io.File
import java.io.FileOutputStream
import java.util.*


class FilterRepository(val workManager: WorkManager) {

    private var colorFilterRequest: OneTimeWorkRequest? = null
    private var colorFilterRequestBuilder: OneTimeWorkRequest.Builder


    init {
        colorFilterRequestBuilder = OneTimeWorkRequestBuilder<NewImageFilterWorker>()

    }

    fun getFilterImage(item: String, file: File) {
        workManager.cancelAllWork()
        val data = Data.Builder().putString("filter_key", item)
            .putString("imageUri", file.toUri().toString()).build()
        colorFilterRequest = colorFilterRequestBuilder.setInputData(data).build()
        workManager.enqueue(colorFilterRequest!!)
    }


    fun liveImageSate(): LiveData<WorkInfo> {
        return workManager.getWorkInfoByIdLiveData(colorFilterRequest?.id ?: UUID.randomUUID())
    }

    fun createFileInExternalStorage(filtered_image: String?): Boolean {
        var isSuccess = false
        exceptionalCode {
            val bitmap = BitmapFactory.decodeFile(filtered_image)
            var outStream: FileOutputStream? = null
            val sdCard = Environment.getExternalStorageDirectory()
            val dir = File(sdCard.absolutePath + "/FilterImages")
            dir.mkdirs()
            val fileName = String.format("%d.jpg", System.currentTimeMillis())
            val outFile = File(dir, fileName)
            outStream = FileOutputStream(outFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
            outStream.flush()
            outStream.close()
            isSuccess = true
        }
        return isSuccess
    }


}