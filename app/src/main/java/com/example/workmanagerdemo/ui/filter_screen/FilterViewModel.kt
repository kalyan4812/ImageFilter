package com.example.workmanagerdemo.ui.filter_screen

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.example.workmanagerdemo.repository.FilterRepository
import com.example.workmanagerdemo.utils.UIEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class FilterViewModel @Inject constructor(private val filterRepository: FilterRepository) :
    ViewModel() {

    private val _uievents = Channel<UIEvent>()
    val ui_events = _uievents.receiveAsFlow()

    fun onFilterOption(item: String, file: File) {
        filterRepository.getFilterImage(item, file)
    }

    fun liveImageData()
            : LiveData<WorkInfo> {
        return filterRepository.liveImageSate()
    }

    fun downloadFile(uri: String?) {
        val bool = filterRepository.createFileInExternalStorage(uri)
        if (bool) {
            sendUiEvent(UIEvent.showToast("Image saved successfully."))
        } else {
            sendUiEvent(UIEvent.showToast("Failed to save the image."))
        }
    }

    private fun sendUiEvent(event: UIEvent) {
        viewModelScope.launch {
            _uievents.send(event)
        }
    }


}