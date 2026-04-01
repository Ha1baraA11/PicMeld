package com.zetazero.photogrid

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.ceil

data class MainUiState(
    val selectedUris: List<Uri> = emptyList(),
    val totalCount: Int = 0,
    val expectedOutputCount: Int = 0,
    val isProcessing: Boolean = false,
    val progressMessage: String = "",
    val progressValue: Int = 0,
    val generationError: String? = null,
    val isSuccess: Boolean = false
)

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    fun addUris(newUris: List<Uri>) {
        val currentList = _uiState.value.selectedUris.toMutableList()
        for (uri in newUris) {
            // 安全限制总上限 200 张且不重复
            if (!currentList.contains(uri) && currentList.size < 200) {
                currentList.add(uri)
            }
        }
        updateUriState(currentList)
    }

    fun removeUri(uri: Uri) {
        val currentList = _uiState.value.selectedUris.toMutableList()
        currentList.remove(uri)
        updateUriState(currentList)
    }

    private fun updateUriState(newList: List<Uri>) {
        _uiState.value = _uiState.value.copy(
            selectedUris = newList,
            totalCount = newList.size,
            expectedOutputCount = ceil(newList.size / 4.0).toInt()
        )
    }

    fun updateProgress(current: Int, total: Int) {
        _uiState.value = _uiState.value.copy(
            progressValue = (current * 100 / total).coerceIn(0, 100),
            progressMessage = "已处理: $current / $total 张拼接图"
        )
    }

    fun setProcessing(isProcessing: Boolean) {
        _uiState.value = _uiState.value.copy(
            isProcessing = isProcessing,
            isSuccess = false,
            generationError = null
        )
    }

    fun setError(msg: String) {
        _uiState.value = _uiState.value.copy(
            isProcessing = false,
            generationError = msg
        )
    }

    fun setSuccess() {
        // 出于项目需求，完成时自动清空所选列表
        _uiState.value = MainUiState(isSuccess = true)
    }

    fun resetSuccessState() {
        _uiState.value = _uiState.value.copy(isSuccess = false)
    }
}
