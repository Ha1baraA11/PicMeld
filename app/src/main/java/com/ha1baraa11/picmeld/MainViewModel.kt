package com.ha1baraa11.picmeld

import android.graphics.Color
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
    val isSuccess: Boolean = false,
    val selectedLayout: LayoutConfig = LayoutConfig.LAYOUT_2X2,
    val bgColor: Int = Color.WHITE,
    val gapPx: Int = 0
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
        val groupSize = _uiState.value.selectedLayout.groupSize
        _uiState.value = _uiState.value.copy(
            selectedUris = newList,
            totalCount = newList.size,
            expectedOutputCount = ceil(newList.size / groupSize.toDouble()).toInt()
        )
    }

    fun setLayout(layout: LayoutConfig) {
        val current = _uiState.value
        _uiState.value = current.copy(
            selectedLayout = layout,
            expectedOutputCount = ceil(current.totalCount / layout.groupSize.toDouble()).toInt()
        )
    }

    fun reorderUris(newOrder: List<Uri>) {
        _uiState.value = _uiState.value.copy(selectedUris = newOrder)
    }

    fun setBgColor(color: Int) {
        _uiState.value = _uiState.value.copy(bgColor = color)
    }

    fun setGapPx(gap: Int) {
        _uiState.value = _uiState.value.copy(gapPx = gap)
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
