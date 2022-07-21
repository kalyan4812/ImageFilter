package com.example.workmanagerdemo

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import androidx.work.*
import com.bumptech.glide.Glide
import com.example.workmanagerdemo.utils.WorkerKeys
import com.example.workmanagerdemo.workmanger.DownloadWorker
import com.example.workmanagerdemo.workmanger.ImageFilterWorker
import dagger.hilt.android.AndroidEntryPoint
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var workManager: WorkManager


    private lateinit var imageuri: MutableLiveData<Uri>

    private lateinit var color_filter_request: OneTimeWorkRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        imageuri = MutableLiveData()


        val download_request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            ).build()

        color_filter_request = OneTimeWorkRequestBuilder<ImageFilterWorker>().build()


        workManager.getWorkInfoByIdLiveData(download_request.id).observe(this) {
            textview.text = when (it?.state) {
                WorkInfo.State.RUNNING -> "Downloading..."
                WorkInfo.State.SUCCEEDED -> "SUCCESSS.."
                WorkInfo.State.FAILED -> "FAILED...."
                else -> "Please wait...."
            }
            val uri = it.outputData?.getString(WorkerKeys.IMAGE_URI)?.toUri()
            imageuri.postValue(uri)

        }
        workManager.getWorkInfoByIdLiveData(color_filter_request.id).observe(this) {
            textview2.text = when (it?.state) {
                WorkInfo.State.RUNNING -> "Filtering..."
                WorkInfo.State.SUCCEEDED -> "Filter SUCCESSS.."
                WorkInfo.State.FAILED -> "Filter FAILED...."
                else -> "Please wait...."
            }
            val uri = it.outputData?.getString(WorkerKeys.FILTER_IMAGE_URI)?.toUri()
            imageuri.postValue(uri)

        }



        btn_load.setOnClickListener {
//            workManager.beginUniqueWork("download", ExistingWorkPolicy.KEEP, download_request)
//                .then(color_filter_request).enqueue()
            workManager.beginWith(download_request).then(color_filter_request).enqueue()
        }

        imageuri.observe(this) {
            if (it != null) {

                Glide.with(applicationContext)
                    .load(it) // Uri of the picture
                    .into(imageView);
            }
        }


    }

}