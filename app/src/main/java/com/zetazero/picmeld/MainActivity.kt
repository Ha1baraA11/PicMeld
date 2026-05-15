package com.zetazero.picmeld

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.zetazero.picmeld.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var photoAdapter: PhotoAdapter

    // Photo Picker: 在裸机或纯虚拟机系统极易闪退。更换为经典传统多选的兼容写法：
    private val pickMultipleMediaCompat = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uris = mutableListOf<Uri>()
            val data = result.data
            if (data?.clipData != null) {
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    uris.add(data.clipData!!.getItemAt(i).uri)
                }
            } else if (data?.data != null) {
                uris.add(data.data!!)
            }
            if (uris.isNotEmpty()) {
                viewModel.addUris(uris)
            }
        }
    }

    // Android 8.0/9.0 存储权限的注册检测拦截器
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startGeneration()
        } else {
            Toast.makeText(this, "需要存储权限才可以保存拼图到相册啊！", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        observeUiState()
    }

    private fun setupRecyclerView() {
        // 配置当点击每个项时，进行移除动作，推入 ViewModel 处理
        photoAdapter = PhotoAdapter { uri ->
            viewModel.removeUri(uri)
        }
        // 关键修复：必须设置 LayoutManager，否则视图只会收集数据，不知道怎么排列，因此显示空白
        binding.rvPhotos.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 3)
        binding.rvPhotos.adapter = photoAdapter
    }

    private fun setupListeners() {
        binding.btnSelect.setOnClickListener {
            // 使用传统的 ACTION_GET_CONTENT 完全兼容任何设备并抓取异常，防闪退
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            try {
                pickMultipleMediaCompat.launch(Intent.createChooser(intent, "请选择多张图片"))
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "错误：当前设备未安装任何能够响应图片的图库或文件管理器！", Toast.LENGTH_LONG).show()
            }
        }

        binding.btnGenerate.setOnClickListener {
            // Android 10 (Q) 及以上无需额外磁盘读写权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startGeneration()
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                    startGeneration()
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
    }

    private fun startGeneration() {
        val uris = viewModel.uiState.value.selectedUris
        if (uris.isEmpty()) return

        viewModel.setProcessing(true)
        lifecycleScope.launch {
            val result = ImageProcessor.generateGrids(this@MainActivity, uris) { current, total ->
                viewModel.updateProgress(current, total)
            }

            if (result.isSuccess) {
                viewModel.setSuccess()
            } else {
                viewModel.setError(result.exceptionOrNull()?.message ?: "拼接操作出现了未知错误")
            }
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // 更新数字展现
                    binding.tvSummary.text = "已选: ${state.totalCount} 张 | 预计拼接: ${state.expectedOutputCount} 张"

                    // 更新网格图片
                    photoAdapter.submitList(state.selectedUris)
                    
                    // 当在“工作中”时锁定所有底部按钮，避免误触
                    binding.btnGenerate.isEnabled = state.totalCount > 0 && !state.isProcessing
                    binding.btnSelect.isEnabled = !state.isProcessing

                    // 进度条与遮罩控制
                    if (state.isProcessing) {
                        binding.overlayProgress.visibility = View.VISIBLE
                        binding.tvProgressMsg.text = state.progressMessage
                        binding.progressBar.isIndeterminate = state.progressValue == 0
                        binding.progressBar.progress = state.progressValue
                    } else {
                        binding.overlayProgress.visibility = View.GONE
                    }

                    // 成功处理并已还原至初始化
                    if (state.isSuccess) {
                        Toast.makeText(this@MainActivity, "拼图已成功存入系统相册！请退出查看~", Toast.LENGTH_LONG).show()
                        viewModel.resetSuccessState()
                    }

                    // 有报错弹Toast
                    if (state.generationError != null) {
                        Toast.makeText(this@MainActivity, "发生致命错误: ${state.generationError}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}