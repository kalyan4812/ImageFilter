package com.example.workmanagerdemo.ui.filter_screen

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.R
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkInfo
import com.example.workmanagerdemo.R.string
import com.example.workmanagerdemo.databinding.ActivityFilterBinding
import com.example.workmanagerdemo.utils.Constants
import com.example.workmanagerdemo.utils.TryHandler.exceptionalCode
import com.example.workmanagerdemo.utils.TryHandler.ifError
import com.example.workmanagerdemo.utils.UIEvent
import com.example.workmanagerdemo.utils.URIPathHelper
import com.example.workmanagerdemo.utils.WorkerKeys
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_filter.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import java.io.File


@AndroidEntryPoint
class FilterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFilterBinding

    private lateinit var imageUri: MutableLiveData<Uri>
    private lateinit var originalImage: Uri

    private lateinit var values: List<String>

    private val viewModel: FilterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFilterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        imageUri = MutableLiveData()
        binding.textview.text = getString(string.Please_pick_an_image)
        setUpSpinner()
        setUpListeners()
        setUpObservers()

    }

    private fun askPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, WRITE_EXTERNAL_STORAGE)) {
            Snackbar.make(
                binding.root,
                "App needs access to storage",
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(
                    "Enable"
                ) {
                    requestLauncher.launch(
                        arrayOf(
                            READ_EXTERNAL_STORAGE,
                            WRITE_EXTERNAL_STORAGE
                        )
                    )
                }.setActionTextColor(getResources().getColor(android.R.color.holo_red_light))
                .show()

        } else {
            requestLauncher.launch(arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE))
        }
    }

    private fun setUpSpinner() {
        values = Constants.SPINNER_VALUES
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
        collectLifecycleAwareChannelFlow(viewModel.ui_events) { event ->
            when (event) {

                is UIEvent.showToast -> {
                    showToast(event.messsage)
                }
                else -> Unit
            }
        }

    }

    fun <T> collectLifecycleAwareChannelFlow(
        flow: Flow<T>,
        collect: FlowCollector<T>
    ) {
        lifecycleScope.launch {
            lifecycleScope.launchWhenStarted {
                flow.collect(collect)
            }
        }
    }

    private fun setUpListeners() {
        binding.btnPick.setOnClickListener {
            if (hadPermission()) {
                binding.spinnerFilter.visibility = View.GONE
                binding.textview2.visibility = View.GONE
                binding.Download.visibility = View.GONE
                binding.textview.visibility = View.VISIBLE
                spinner_filter.setSelection(0)
                openPicker()
            } else {
                askPermission()
            }
        }
        binding.spinnerFilter.setOnItemSelectedListener(object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                if (pos == 0) {
                    return
                } else if (pos == 1) {
                    binding.imageView.setImageURI(originalImage)
                    binding.Download.visibility = View.GONE
                    binding.textview2.visibility = View.GONE
                    return
                }
                binding.textview2.visibility = View.VISIBLE
                binding.progressBar.visibility=View.VISIBLE
                binding.progressPercentage.visibility=View.VISIBLE
                binding.imageView.setImageURI(originalImage)
                viewModel.onFilterOption(parent?.selectedItem?.toString()!!, file)
                observeData()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
        })
        binding.Download.setOnClickListener {
            imageUri?.let {
                viewModel.downloadFile(filtered_image)
            }
        }

    }

    private fun hadPermission(): Boolean {
        return (ActivityCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
            this,
            WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
                )
    }

    private var filtered_image: String? = null

    fun observeData() {
        viewModel.liveImageData().observe(this) {
            val uri = it.outputData.getString(WorkerKeys.FILTER_IMAGE_URI)?.toUri()
            val path = it.outputData.getString(WorkerKeys.NEW_IMAGE_URI)
            val progress: Int = it.getProgress().getInt("progress", -1)

            textview2.text = when (it?.state) {
                WorkInfo.State.RUNNING -> "Applying filter..."
                WorkInfo.State.SUCCEEDED -> {
                    binding.progressBar.visibility=View.GONE
                    binding.progressPercentage.visibility=View.GONE
                    binding.Download.visibility = View.VISIBLE
                    filtered_image = path
                    "Filter SUCCESSS.."
                }
                WorkInfo.State.FAILED -> "Filter FAILED...."
                else -> "Please wait...."
            }
            binding.progressBar.progress=progress
            if(progress>=0){
                binding.progressPercentage.text="$progress %"
            }

            uri?.let {
                imageUri.postValue(uri)
            }
        }
    }

    private fun openPicker() {
        val intent = Intent()
        intent.action = Intent.ACTION_PICK
        intent.type = "image/*"
        resultLauncher.launch(intent)
    }

    private lateinit var file: File
    private val resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == RESULT_OK) {
            exceptionalCode {
                binding.textview.text = getString(string.attach_image)
                originalImage = data?.data!!
                file = File(URIPathHelper.getPath(this,originalImage))
                if (file.exists()) {
                    println("file created..........")
                }
                imageUri.postValue(originalImage)
            }.ifError {
                showToast("error occured")
                binding.textview.text = getString(string.failed_to_attach_image)
            }
        } else {
            binding.textview.text = "Failed to attach image"
            showToast("Failed to pick the image")
        }
    }
    private val requestLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (granted[READ_EXTERNAL_STORAGE] == true
                && granted[WRITE_EXTERNAL_STORAGE] == true
            ) {
                showToast("Permission Granted")
                spinner_filter.setSelection(0)
                openPicker()
            } else {
                showToast("Permission Denied")
            }
        }

    private fun showToast(msg: String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
    }
}