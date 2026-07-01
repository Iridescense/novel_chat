package com.novelchat.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * GitHub Release API 返回的数据结构
 */
data class GitHubRelease(
    val tag_name: String = "",
    val body: String = "",
    val assets: List<GitHubAsset> = emptyList()
)

data class GitHubAsset(
    val name: String = "",
    val browser_download_url: String = ""
)

/**
 * 版本检查结果
 */
data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val releaseNotes: String,
    val downloadUrl: String
)

/**
 * 版本更新检测与安装
 */
object UpdateChecker {

    // 更新源 URL，后续换成自建服务器只需改这里
    private const val UPDATE_URL = "https://api.github.com/repos/你的用户名/novel_chat/releases/latest"
    private const val APK_FILE_NAME = "novelchat-update.apk"

    /**
     * 检查是否有新版本
     */
    suspend fun checkUpdate(currentVersion: String): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val conn = URL(UPDATE_URL).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val json = conn.inputStream.bufferedReader().readText()
            val release = Gson().fromJson(json, GitHubRelease::class.java)

            val latestVersion = release.tag_name.removePrefix("v")
            val apkUrl = release.assets.firstOrNull { it.name.endsWith(".apk") }
                ?.browser_download_url ?: ""

            val hasUpdate = compareVersions(latestVersion, currentVersion) > 0

            UpdateInfo(
                hasUpdate = hasUpdate,
                latestVersion = latestVersion,
                releaseNotes = release.body,
                downloadUrl = apkUrl
            )
        } catch (e: Exception) {
            e.printStackTrace()
            UpdateInfo(
                hasUpdate = false,
                latestVersion = currentVersion,
                releaseNotes = "",
                downloadUrl = ""
            )
        }
    }

    /**
     * 下载 APK 到应用私有目录
     */
    suspend fun downloadApk(context: Context, downloadUrl: String): File? = withContext(Dispatchers.IO) {
        try {
            val file = File(context.cacheDir, APK_FILE_NAME)
            if (file.exists()) file.delete()

            val conn = URL(downloadUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 30000
            conn.readTimeout = 30000
            conn.connect()

            val inputStream = conn.inputStream
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            outputStream.close()
            inputStream.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 安装 APK
     */
    fun installApk(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * 版本号比较，返回正数表示 v1 > v2
     */
    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(parts1.size, parts2.size)) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1 - p2
        }
        return 0
    }
}
