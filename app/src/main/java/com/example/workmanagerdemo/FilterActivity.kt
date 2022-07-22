package com.example.workmanagerdemo

import android.R
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import androidx.work.*
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import com.example.workmanagerdemo.R.*
import com.example.workmanagerdemo.databinding.ActivityFilterBinding
import com.example.workmanagerdemo.utils.WorkerKeys
import com.example.workmanagerdemo.workmanger.NewImageFilterWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_filter.*
import kotlinx.android.synthetic.main.activity_filter.textview2
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject


@AndroidEntryPoint
class FilterActivity : AppCompatActivity() {


    @Inject
    lateinit var workManager: WorkManager

    private lateinit var binding: ActivityFilterBinding

    private lateinit var imageUri: MutableLiveData<Uri>
    private lateinit var originalImage: Uri

    private lateinit var values: List<String>

    private lateinit var colorFilterRequest: OneTimeWorkRequest
    private lateinit var colorFilterRequestBuilder: OneTimeWorkRequest.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFilterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        imageUri = MutableLiveData()
        binding.textview.text = getString(string.Please_pick_an_image)
        setUpWorkers()
        setUpSpinner()
        setUpListeners()
        setUpObservers()
    }

    private fun setUpWorkers() {
        colorFilterRequestBuilder = OneTimeWorkRequestBuilder<NewImageFilterWorker>()
    }

    private fun setUpSpinner() {
        values = listOf(
            "Apply Filter",
            "Rotate",
            "Flip",
            "Emoboss",
            "Cfilter",
            "Gaussian",
            "Cdepth",
            "Sharpen",
            "Noise",
            "Brightness",
            "Sepia",
            "Gamma",
            "Contrast",
            "Saturation",
            "Grayscale",
            "Vignette",
            "Hue",
            "Tint",
            "Invert",
            "Boost",
            "Sketch",
        )
        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
            this,
            R.layout.simple_spinner_dropdown_item,
            values
        )
        binding.spinnerFilter.adapter = adapter
    }

    private fun setUpObservers() {
        imageUri?.observe(this) {
            binding.imageView.setImageURI(imageUri.value)
            binding.textview.visibility = View.GONE
            spinner_filter.visibility = View.VISIBLE
        }

    }

    private fun setUpListeners() {
        binding.btnPick.setOnClickListener {
            spinner_filter.setSelection(0)
            openPicker()
        }
        binding.spinnerFilter.setOnItemSelectedListener(object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                if (pos == 0) {
                    return
                }
                binding.textview2.visibility = View.VISIBLE
                binding.imageView.setImageURI(originalImage)
                workManager.cancelAllWork()
                val data = Data.Builder().putString("filter_key", parent?.selectedItem?.toString())
                    .putString("imageUri", file.toUri().toString()).build()
                colorFilterRequest = colorFilterRequestBuilder.setInputData(data).build()
                workManager.enqueue(colorFilterRequest)
                observeData()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
        })
    }

    fun observeData() {
        workManager.getWorkInfoByIdLiveData(colorFilterRequest.id).observe(this) {
            textview2.text = when (it?.state) {
                WorkInfo.State.RUNNING -> "Applying filter..."
                WorkInfo.State.SUCCEEDED -> "Filter SUCCESSS.."
                WorkInfo.State.FAILED -> "Filter FAILED...."
                else -> "Please wait...."
            }
            val uri = it.outputData.getString(WorkerKeys.FILTER_IMAGE_URI)?.toUri()
            it?.let {
                imageUri.postValue(uri)
            }


        }

    }

    private fun openPicker() {
        val i = Intent()
        i.action = Intent.ACTION_PICK
        i.type = "image/*"
        startActivityForResult(i, 100)
    }

    private lateinit var file: File
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {

            100 -> {
                binding.textview.text = "Attaching Image...."
                originalImage = data?.data!!
                imageUri.postValue(originalImage)
                file = File(getPath(originalImage))
                if (file.exists()) {
                    println("file created..........")
                }
            }
        }
    }

    fun getPath(uri: Uri?): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri!!, projection, null, null, null) ?: return null
        val column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        val s = cursor.getString(column_index)
        cursor.close()
        return s
    }

}