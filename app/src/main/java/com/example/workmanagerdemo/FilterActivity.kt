package com.example.workmanagerdemo

import android.Manifest.permission.*
import android.R
import android.R.attr.bitmap
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import androidx.work.*
import com.example.workmanagerdemo.R.*
import com.example.workmanagerdemo.databinding.ActivityFilterBinding
import com.example.workmanagerdemo.utils.WorkerKeys
import com.example.workmanagerdemo.workmanger.NewImageFilterWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_filter.*
import kotlinx.android.synthetic.main.activity_filter.textview2
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.*
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
        askPermission()
        setUpWorkers()
        setUpSpinner()
        setUpListeners()
        setUpObservers()
    }

    private fun askPermission() {
        if (ActivityCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {

        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE), 200
            )
        }


    }

    private fun setUpWorkers() {
        colorFilterRequestBuilder = OneTimeWorkRequestBuilder<NewImageFilterWorker>()
    }

    private fun setUpSpinner() {
        values = listOf(
            "Apply Filter",
            "original",
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
                } else if (pos == 1) {
                    binding.imageView.setImageURI(originalImage)
                    binding.Download.visibility = View.GONE
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
        binding.Download.setOnClickListener {
            imageUri?.let {
                createFileInExternalStorage()
            }
        }

    }

    private var filtered_image: String? = null
    private fun createFileInExternalStorage() {
        try {
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
            Toast.makeText(applicationContext, "Saved successfully...", Toast.LENGTH_SHORT).show()
        }
        catch (e:Exception){
            Toast.makeText(applicationContext, "Failed to save the image.", Toast.LENGTH_SHORT).show()
        }
    }

    fun observeData() {
        workManager.getWorkInfoByIdLiveData(colorFilterRequest.id).observe(this) {
            val uri = it.outputData.getString(WorkerKeys.FILTER_IMAGE_URI)?.toUri()
            val path = it.outputData.getString(WorkerKeys.NEW_IMAGE_URI)
            textview2.text = when (it?.state) {
                WorkInfo.State.RUNNING -> "Applying filter..."
                WorkInfo.State.SUCCEEDED -> {
                    binding.Download.visibility = View.VISIBLE
                    filtered_image = path
                    "Filter SUCCESSS.."
                }
                WorkInfo.State.FAILED -> "Filter FAILED...."
                else -> "Please wait...."
            }

            uri?.let {
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
            200 -> {
                if (resultCode == RESULT_OK) {
                    Toast.makeText(applicationContext, "Permission Granted", Toast.LENGTH_SHORT)
                        .show()
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