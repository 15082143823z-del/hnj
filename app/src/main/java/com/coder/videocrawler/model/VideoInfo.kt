package com.coder.videocrawler.model

import com.google.gson.annotations.SerializedName

/**
 * yt-dlp --dump-json 返回的视频信息
 */
data class VideoInfo(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val thumbnail: String = "",
    val duration: Long = 0,
    @SerializedName("webpage_url")
    val webpageUrl: String = "",
    val uploader: String = "",
    @SerializedName("upload_date")
    val uploadDate: String = "",
    val formats: List<FormatInfo> = emptyList(),
    val platform: VideoPlatform = VideoPlatform.UNKNOWN
)

data class FormatInfo(
    @SerializedName("format_id")
    val formatId: String = "",
    val ext: String = "",
    val resolution: String = "",
    val fps: Float = 0f,
    val filesize: Long = 0L,
    @SerializedName("filesize_approx")
    val filesizeApprox: Long = 0L,
    @SerializedName("tbr")
    val bitrate: Float = 0f,
    @SerializedName("vcodec")
    val vcodec: String = "",
    @SerializedName("acodec")
    val acodec: String = "",
    @SerializedName("format_note")
    val formatNote: String = ""
) {
    /** 人类可读的文件大小 */
    val sizeText: String
        get() {
            val size = if (filesize > 0) filesize else filesizeApprox
            return when {
                size <= 0 -> "未知"
                size < 1024 * 1024 -> "${size / 1024}KB"
                size < 1024 * 1024 * 1024 -> "%.1fMB".format(size / (1024.0 * 1024))
                else -> "%.1fGB".format(size / (1024.0 * 1024 * 1024))
            }
        }

    /** 显示标签：分辨率 + 编码 + 大小 */
    val displayLabel: String
        get() = buildString {
            if (resolution.isNotBlank() && resolution != "audio only") {
                append(resolution)
            }
            if (ext.isNotBlank()) {
                if (isNotEmpty()) append(" ")
                append(ext.uppercase())
            }
            if (formatNote.isNotBlank()) {
                append(" ($formatNote)")
            }
            if (sizeText != "未知") {
                append(" - $sizeText")
            }
        }
}

enum class VideoPlatform(val displayName: String, val domains: List<String>) {
    YOUTUBE("YouTube", listOf("youtube.com", "youtu.be")),
    BILIBILI("B站", listOf("bilibili.com", "b23.tv")),
    DOUYIN("抖音", listOf("douyin.com", "tiktok.com")),
    KUAISHOU("快手", listOf("kuaishou.com")),
    XIAOHONGSHU("小红书", listOf("xiaohongshu.com", "xhslink.com")),
    WEIBO("微博", listOf("weibo.com")),
    XIGUA("西瓜视频", listOf("ixigua.com")),
    UNKNOWN("其他", emptyList());

    companion object {
        fun fromUrl(url: String): VideoPlatform =
            entries.firstOrNull { platform ->
                platform.domains.any { url.contains(it, ignoreCase = true) }
            } ?: UNKNOWN
    }
}
