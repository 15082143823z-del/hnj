package com.coder.videocrawler.service

import android.content.Context
import android.os.Environment
import com.coder.videocrawler.model.VideoInfo
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class DownloadState(
    val status: Status = Status.IDLE,
    val progress: String = "",
    val videoInfo: VideoInfo? = null,
    val error: String? = null
) {
    enum class Status { IDLE, FETCHING_INFO, READY, DOWNLOADING, DONE, ERROR }
}

class DownloadManager(appContext: Context) {

    private val gson = Gson()
    private val context = appContext.applicationContext  // 存 ApplicationContext，防泄漏
    private val _state = MutableStateFlow(DownloadState())
    val state: StateFlow<DownloadState> = _state.asStateFlow()

    private val downloadDir = Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_DOWNLOADS
    )

    /** 上次下载的文件路径（用于精准扫描） */
    private var lastOutputFile: File? = null

    /** 获取视频信息（不下载） */
    suspend fun fetchVideoInfo(url: String): Result<VideoInfo> {
        _state.value = _state.value.copy(status = DownloadState.Status.FETCHING_INFO)

        return YtDlpManager.execute("--dump-json", "--no-playlist", url).map { raw ->
            // yt-dlp 可能输出多行（warnings + JSON），取第一个有效 JSON 行
            val jsonLine = raw.lines()
                .firstOrNull { it.trimStart().startsWith("{") && it.trimEnd().endsWith("}") }
                ?: throw RuntimeException("未找到有效 JSON 输出")

            val info = gson.fromJson(jsonLine, VideoInfo::class.java)
            _state.value = _state.value.copy(
                status = DownloadState.Status.READY,
                videoInfo = info
            )
            info
        }.onFailure { e ->
            _state.value = _state.value.copy(
                status = DownloadState.Status.ERROR,
                error = e.message ?: "解析失败"
            )
        }
    }

    /** 下载视频 */
    suspend fun download(info: VideoInfo, formatId: String = "best") {
        _state.value = _state.value.copy(status = DownloadState.Status.DOWNLOADING, progress = "准备中...")

        val safeTitle = info.title
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
            .take(100)

        val outputTemplate = File(downloadDir, "$safeTitle.%(ext)s").absolutePath

        YtDlpManager.executeWithProgress(
            "-f", formatId,
            "-o", outputTemplate,
            "--newline",
            "--print", "after_move:filepath",
            info.webpageUrl
        ) { line ->
            val progress = parseProgress(line)
            if (progress != null) {
                _state.value = _state.value.copy(progress = progress)
            }
        }.onSuccess { fullOutput ->
            // 提取实际输出文件路径（--print after_move:filepath 会输出路径）
            val actualPath = fullOutput.lines()
                .firstOrNull { it.startsWith("/") && File(it).exists() }

            _state.value = _state.value.copy(
                status = DownloadState.Status.DONE,
                progress = "下载完成"
            )

            // 只扫描新下载的文件
            if (actualPath != null) {
                scanFile(File(actualPath))
            }
        }.onFailure { e ->
            _state.value = _state.value.copy(
                status = DownloadState.Status.ERROR,
                error = e.message ?: "下载失败"
            )
        }
    }

    /** 下载音频（最佳音质） */
    suspend fun downloadAudio(info: VideoInfo) {
        download(info, "bestaudio/best")
    }

    /** 重置状态 */
    fun reset() {
        _state.value = DownloadState()
        lastOutputFile = null
    }

    private fun parseProgress(line: String): String? {
        // yt-dlp 进度格式: [download]  12.3% of ~50.00MiB at  1.2MiB/s ETA 00:30
        val regex = Regex("""\[download\]\s+(\d+\.?\d*%.+)""")
        return regex.find(line)?.groupValues?.get(1)
    }

    /** 只扫描指定文件 */
    private fun scanFile(file: File) {
        if (!file.exists()) return
        android.media.MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            null,
            null
        )
    }
}
