package com.coder.videocrawler.ui.screen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coder.videocrawler.model.VideoInfo
import com.coder.videocrawler.model.VideoPlatform
import com.coder.videocrawler.service.DownloadManager
import com.coder.videocrawler.service.DownloadState
import com.coder.videocrawler.service.YtDlpManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val url: String = "",
    val selectedPlatform: VideoPlatform = VideoPlatform.UNKNOWN,
    val downloadState: DownloadState = DownloadState(),
    val ytDlpStatus: YtDlpManager.InitResult? = null,
    val ytDlpVersion: String = "",
    val showInstallGuide: Boolean = false
)

class HomeViewModel : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var downloadManager: DownloadManager? = null
    private var collectJob: Job? = null
    private var appContext: Context? = null

    /** 初始化 yt-dlp（需要传入 ApplicationContext） */
    fun init(context: Context) {
        appContext = context.applicationContext
        if (_state.value.ytDlpStatus == null) {
            viewModelScope.launch {
                val result = YtDlpManager.init(context.applicationContext)
                _state.value = _state.value.copy(ytDlpStatus = result)

                if (result.success) {
                    _state.value = _state.value.copy(
                        ytDlpVersion = YtDlpManager.version()
                    )
                } else if (result.needsTermux || result.needsYtDlp) {
                    _state.value = _state.value.copy(showInstallGuide = true)
                }
            }
        }
    }

    /** 安装 yt-dlp 到 Termux */
    fun installYtDlp() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                downloadState = DownloadState(
                    status = DownloadState.Status.FETCHING_INFO
                )
            )
            YtDlpManager.installYtDlpInTermux()
                .onSuccess {
                    _state.value = _state.value.copy(
                        ytDlpStatus = YtDlpManager.InitResult(true, "yt-dlp 安装完成"),
                        ytDlpVersion = YtDlpManager.version(),
                        showInstallGuide = false,
                        downloadState = DownloadState()
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        downloadState = DownloadState(
                            status = DownloadState.Status.ERROR,
                            error = "安装失败: ${it.message}"
                        )
                    )
                }
        }
    }

    fun onUrlChange(url: String) {
        _state.value = _state.value.copy(
            url = url,
            selectedPlatform = VideoPlatform.fromUrl(url)
        )
    }

    fun onPlatformSelect(platform: VideoPlatform) {
        _state.value = _state.value.copy(selectedPlatform = platform)
    }

    fun fetchInfo(context: Context) {
        val url = _state.value.url.trim()
        if (url.isBlank()) return

        // 取消旧的 collector，避免协程泄漏
        collectJob?.cancel()

        downloadManager = DownloadManager(context.applicationContext)
        collectJob = viewModelScope.launch {
            downloadManager!!.state.collect { ds ->
                _state.value = _state.value.copy(downloadState = ds)
            }
        }
        viewModelScope.launch {
            downloadManager!!.fetchVideoInfo(url)
        }
    }

    fun download(info: VideoInfo, formatId: String) {
        downloadManager?.let { dm ->
            viewModelScope.launch { dm.download(info, formatId) }
        }
    }

    fun downloadAudio(info: VideoInfo) {
        downloadManager?.let { dm ->
            viewModelScope.launch { dm.downloadAudio(info) }
        }
    }

    fun reset() {
        downloadManager?.reset()
        collectJob?.cancel()
        downloadManager = null
        _state.value = _state.value.copy(
            url = "",
            downloadState = DownloadState()
        )
    }
}
