@file:OptIn(ExperimentalMaterial3Api::class)

package com.coder.videocrawler.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coder.videocrawler.model.VideoPlatform
import com.coder.videocrawler.service.DownloadState
import com.coder.videocrawler.service.YtDlpManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VideoCrawler") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // 安装引导
            if (state.showInstallGuide) {
                InstallGuideCard(
                    needsTermux = state.ytDlpStatus?.needsTermux == true,
                    needsYtDlp = state.ytDlpStatus?.needsYtDlp == true,
                    onInstallYtDlp = { viewModel.installYtDlp() }
                )
                Spacer(Modifier.height(16.dp))
            }

            // URL 输入栏
            OutlinedTextField(
                value = state.url,
                onValueChange = viewModel::onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("粘贴视频链接...") },
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = { viewModel.fetchInfo(context) },
                        enabled = state.url.isNotBlank()
                                && state.downloadState.status != DownloadState.Status.FETCHING_INFO
                                && state.ytDlpStatus?.success == true
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "解析")
                    }
                }
            )

            Spacer(Modifier.height(8.dp))

            // 平台快速选择
            PlatformChips(
                selected = state.selectedPlatform,
                onSelect = viewModel::onPlatformSelect
            )

            Spacer(Modifier.height(16.dp))

            // 状态提示
            when (state.downloadState.status) {
                DownloadState.Status.FETCHING_INFO -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text("正在解析视频信息...", style = MaterialTheme.typography.bodyMedium)
                }

                DownloadState.Status.READY -> {
                    VideoInfoCard(state.downloadState.videoInfo!!, viewModel)
                }

                DownloadState.Status.DOWNLOADING -> {
                    DownloadProgressCard(state.downloadState)
                }

                DownloadState.Status.DONE -> {
                    DoneCard(viewModel)
                }

                DownloadState.Status.ERROR -> {
                    ErrorCard(state.downloadState.error ?: "未知错误", viewModel)
                }

                DownloadState.Status.IDLE -> {
                    if (!state.showInstallGuide) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "支持 YouTube / B站 / 抖音 / 快手 / 小红书 / 西瓜视频",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "粘贴链接即可解析下载",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 安装引导卡片 */
@Composable
private fun InstallGuideCard(
    needsTermux: Boolean,
    needsYtDlp: Boolean,
    onInstallYtDlp: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "需要安装运行环境",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            if (needsTermux) {
                Text(
                    "本 App 依赖 Termux 提供的 yt-dlp 来解析和下载视频。",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://f-droid.org/repo/com.termux_118.apk")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("下载安装 Termux")
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "安装 Termux 后重新打开 App，将自动引导安装 yt-dlp",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (needsYtDlp) {
                Text(
                    "Termux 已安装，但尚未安装 yt-dlp 插件。",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onInstallYtDlp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Build, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("一键安装 yt-dlp")
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "预计耗时 1-2 分钟，请保持 App 在前台",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlatformChips(selected: VideoPlatform, onSelect: (VideoPlatform) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VideoPlatform.entries.filter { it != VideoPlatform.UNKNOWN }.forEach { platform ->
            FilterChip(
                selected = selected == platform,
                onClick = { onSelect(platform) },
                label = { Text(platform.displayName, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}

@Composable
private fun VideoInfoCard(info: com.coder.videocrawler.model.VideoInfo, viewModel: HomeViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(info.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("UP主: ${info.uploader}", style = MaterialTheme.typography.bodySmall)
                val minutes = info.duration / 60
                val seconds = info.duration % 60
                Text(
                    "时长: ${minutes}:${seconds.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(12.dp))

            // 格式选择
            Text("选择画质:", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))

            val videoFormats = info.formats
                .filter { it.vcodec != "none" && it.resolution.isNotBlank() }
                .distinctBy { it.resolution }
                .take(8)

            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                items(videoFormats) { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(format.displayLabel, style = MaterialTheme.typography.bodySmall)
                        Button(
                            onClick = { viewModel.download(info, format.formatId) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("下载", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 仅下载音频
            OutlinedButton(
                onClick = { viewModel.downloadAudio(info) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("仅下载音频 (最佳音质)")
            }
        }
    }
}

@Composable
private fun DownloadProgressCard(state: DownloadState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("下载中...", style = MaterialTheme.typography.titleSmall)
            if (state.progress.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(state.progress, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DoneCard(viewModel: HomeViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("下载完成", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("文件保存在 Download 文件夹", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { viewModel.reset() }) {
                Text("继续下载")
            }
        }
    }
}

@Composable
private fun ErrorCard(error: String, viewModel: HomeViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("出错了", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { viewModel.reset() }) {
                Text("重试")
            }
        }
    }
}
