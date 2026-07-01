package com.coder.videocrawler.service

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * yt-dlp 运行环境管理
 *
 * 策略（按优先级）：
 * 1. Termux 已安装 → 直接用 Termux 里的 yt-dlp
 * 2. Termux 已安装但没装 yt-dlp → 提供一键安装
 * 3. 都没装 → 引导用户装 Termux
 */
object YtDlpManager {

    private const val TERMUX_PACKAGE = "com.termux"
    private const val TERMUX_YT_DLP = "/data/data/com.termux/files/usr/bin/yt-dlp"

    private var binaryPath: String = ""
    private var useTermux: Boolean = false
    private var isReady: Boolean = false
    private var appContext: Context? = null

    /** 初始化结果 */
    data class InitResult(
        val success: Boolean,
        val message: String,
        val needsTermux: Boolean = false,
        val needsYtDlp: Boolean = false
    )

    /** 初始化：检测可用的 yt-dlp */
    suspend fun init(context: Context): InitResult = withContext(Dispatchers.IO) {
        try {
            appContext = context.applicationContext
            // 策略 1: 检查 Termux
            if (isTermuxInstalled(context)) {
                val ytDlp = File(TERMUX_YT_DLP)
                if (ytDlp.exists() && ytDlp.canExecute()) {
                    binaryPath = TERMUX_YT_DLP
                    useTermux = false  // 直接路径，不需要 termux 包装
                    isReady = true
                    return@withContext InitResult(
                        success = true,
                        message = "Termux yt-dlp 就绪"
                    )
                } else {
                    // Termux 装了但没装 yt-dlp
                    return@withContext InitResult(
                        success = false,
                        message = "Termux 已安装，但未安装 yt-dlp",
                        needsYtDlp = true
                    )
                }
            }

            // 策略 2: 需要装 Termux
            return@withContext InitResult(
                success = false,
                message = "需要安装 Termux",
                needsTermux = true
            )
        } catch (e: Exception) {
            InitResult(success = false, message = "初始化异常: ${e.message}")
        }
    }

    /** 一键安装 yt-dlp (在 Termux 内执行 pkg install yt-dlp) */
    suspend fun installYtDlpInTermux(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cmd = listOf(
                "/data/data/com.termux/files/usr/bin/bash", "-c",
                "pkg update -y && pkg install -y yt-dlp && yt-dlp --version"
            )
            val process = ProcessBuilder(cmd)
                .directory(File("/data/data/com.termux/files/home"))
                .apply {
                    environment()["HOME"] = "/data/data/com.termux/files/home"
                    environment()["PATH"] = "/data/data/com.termux/files/usr/bin:/system/bin"
                    environment()["TMPDIR"] = "/data/data/com.termux/files/usr/tmp"
                }
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                binaryPath = TERMUX_YT_DLP
                isReady = true
                Result.success(output)
            } else {
                Result.failure(IOException("安装失败 (exit=$exitCode): $output"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 执行 yt-dlp 命令，返回完整 stdout */
    suspend fun execute(vararg args: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isReady) return@withContext Result.failure(IllegalStateException("yt-dlp 未就绪"))

        try {
            val cmd = if (useTermux) {
                listOf(
                    "/data/data/com.termux/files/usr/bin/bash", "-c",
                    "yt-dlp ${args.joinToString(" ")}"
                )
            } else {
                listOf(binaryPath, "--no-check-certificates") + args
            }

            val process = ProcessBuilder(cmd)
                .apply {
                    val termuxHome = "/data/data/com.termux/files/home"
                    if (File(termuxHome).exists()) {
                        directory(File(termuxHome))
                        environment()["HOME"] = termuxHome
                    }
                    environment()["TMPDIR"] = appContext?.cacheDir?.absolutePath ?: "/data/local/tmp"
                    environment()["PATH"] = "/data/data/com.termux/files/usr/bin:/system/bin"
                }
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                Result.failure(RuntimeException("yt-dlp exit=$exitCode\n$output"))
            } else {
                Result.success(output)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 执行 yt-dlp 并逐行回调（下载进度用）
     */
    suspend fun executeWithProgress(
        vararg args: String,
        onProgress: (line: String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isReady) return@withContext Result.failure(IllegalStateException("yt-dlp 未就绪"))

        try {
            val cmd = if (useTermux) {
                listOf(
                    "/data/data/com.termux/files/usr/bin/bash", "-c",
                    "yt-dlp ${args.joinToString(" ")}"
                )
            } else {
                listOf(binaryPath, "--no-check-certificates") + args
            }

            val process = ProcessBuilder(cmd)
                .apply {
                    val termuxHome = "/data/data/com.termux/files/home"
                    if (File(termuxHome).exists()) {
                        directory(File(termuxHome))
                        environment()["HOME"] = termuxHome
                    }
                    environment()["TMPDIR"] = appContext?.cacheDir?.absolutePath ?: "/data/local/tmp"
                    environment()["PATH"] = "/data/data/com.termux/files/usr/bin:/system/bin"
                }
                .redirectErrorStream(true)
                .start()

            val reader = process.inputStream.bufferedReader()
            val fullOutput = StringBuilder()

            reader.forEachLine { line ->
                fullOutput.appendLine(line)
                onProgress(line)
            }

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                Result.failure(RuntimeException("yt-dlp exit=$exitCode"))
            } else {
                Result.success(fullOutput.toString())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 获取版本号 */
    suspend fun version(): String = execute("--version").getOrDefault("未知")

    /** 是否就绪 */
    fun isReady(): Boolean = isReady

    /** 是否是 Termux 模式 */
    fun isTermuxMode(): Boolean = useTermux

    private fun isTermuxInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}
