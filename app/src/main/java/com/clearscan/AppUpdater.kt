package com.clearscan

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class UpdateSettings(val autoCheck: Boolean = true, val autoDownload: Boolean = true, val wifiOnly: Boolean = true)
data class UpdateInfo(val version: String, val notes: String, val downloadUrl: String, val fileName: String, val sizeBytes: Long, val sha256: String?)
data class UpdateDownloadState(val status: String = "idle", val downloaded: Long = 0, val total: Long = 0, val filePath: String? = null, val error: String? = null)

class GitHubUpdateRepository(private val context: Context) {
    suspend fun checkLatest(): UpdateInfo? = withContext(Dispatchers.IO) {
        val connection = (URL("https://api.github.com/repos/SuiYueMengHen/ClearScan/releases/latest").openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "ClearScan/${BuildConfig.VERSION_NAME}")
        }
        try {
            check(connection.responseCode in 200..299) { "GitHub returned HTTP ${connection.responseCode}" }
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val version = json.optString("tag_name").removePrefix("v")
            if (compareVersions(version, BuildConfig.VERSION_NAME.substringBefore('-')) <= 0) return@withContext null
            val assets = json.getJSONArray("assets")
            val candidates = (0 until assets.length()).map { assets.getJSONObject(it) }.filter { it.optString("name").endsWith(".apk", true) }
            val asset = candidates.firstOrNull { it.optString("name").contains("arm64", true) } ?: candidates.firstOrNull() ?: error("Release has no APK asset")
            UpdateInfo(
                version = version,
                notes = json.optString("body"),
                downloadUrl = asset.getString("browser_download_url"),
                fileName = asset.getString("name"),
                sizeBytes = asset.optLong("size"),
                sha256 = asset.optString("digest").removePrefix("sha256:").takeIf { it.length == 64 },
            )
        } finally {
            connection.disconnect()
        }
    }

    suspend fun download(info: UpdateInfo, onProgress: (Long, Long) -> Unit = { _, _ -> }): File = withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, "updates").apply { mkdirs() }
        val target = File(directory, "ClearScan-${info.version}-arm64-v8a.apk")
        val partial = File(directory, "${target.name}.part")
        var existing = partial.takeIf { it.exists() }?.length() ?: 0L
        val connection = (URL(info.downloadUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            if (existing > 0) setRequestProperty("Range", "bytes=$existing-")
        }
        try {
            val append = existing > 0 && connection.responseCode == HttpURLConnection.HTTP_PARTIAL
            if (!append) { existing = 0; partial.delete() }
            val total = if (connection.contentLengthLong > 0) existing + connection.contentLengthLong else info.sizeBytes
            connection.inputStream.use { input ->
                FileOutputStream(partial, append).use { output ->
                    val buffer = ByteArray(256 * 1024)
                    var downloaded = existing
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, total)
                    }
                }
            }
            if (info.sizeBytes > 0) check(partial.length() == info.sizeBytes) { "Downloaded APK size is incomplete" }
            info.sha256?.let { check(sha256(partial).equals(it, true)) { "APK checksum verification failed" } }
            validateApk(partial)
            if (target.exists()) target.delete()
            check(partial.renameTo(target)) { "Unable to finalize update APK" }
            target
        } finally {
            connection.disconnect()
        }
    }

    fun validateApk(file: File) {
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val archive = context.packageManager.getPackageArchiveInfo(file.absolutePath, flags) ?: error("Invalid APK")
        check(archive.packageName == context.packageName) { "Update package name does not match" }
        val installed = context.packageManager.getPackageInfo(context.packageName, flags)
        check(PackageInfoCompat.getLongVersionCode(archive) > PackageInfoCompat.getLongVersionCode(installed)) { "APK is not newer than the installed version" }
        val archiveSigners = if (Build.VERSION.SDK_INT >= 28) archive.signingInfo?.apkContentsSigners else @Suppress("DEPRECATION") archive.signatures
        val installedSigners = if (Build.VERSION.SDK_INT >= 28) installed.signingInfo?.apkContentsSigners else @Suppress("DEPRECATION") installed.signatures
        check(!archiveSigners.isNullOrEmpty() && !installedSigners.isNullOrEmpty() && archiveSigners.any { a -> installedSigners.any { b -> a.toByteArray().contentEquals(b.toByteArray()) } }) {
            "Update signature does not match the installed app"
        }
    }

    fun install(file: File) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    companion object {
        fun compareVersions(left: String, right: String): Int {
            val a = left.removePrefix("v").substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
            val b = right.removePrefix("v").substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
            repeat(maxOf(a.size, b.size)) { index ->
                val result = (a.getOrElse(index) { 0 }).compareTo(b.getOrElse(index) { 0 })
                if (result != 0) return result
            }
            return 0
        }

        private fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(128 * 1024)
                while (true) { val read = input.read(buffer); if (read < 0) break; digest.update(buffer, 0, read) }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        fun schedule(context: Context, enabled: Boolean, wifiOnly: Boolean) {
            val manager = WorkManager.getInstance(context)
            if (!enabled) { manager.cancelUniqueWork("clearscan-update-check"); return }
            val constraints = Constraints.Builder().setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED).build()
            val request = PeriodicWorkRequestBuilder<UpdateWorker>(24, TimeUnit.HOURS).setConstraints(constraints).build()
            manager.enqueueUniquePeriodicWork("clearscan-update-check", ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}

class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching {
        val prefs = applicationContext.getSharedPreferences("clearscan-settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("autoCheckUpdates", true)) return Result.success()
        val repo = GitHubUpdateRepository(applicationContext)
        val update = repo.checkLatest() ?: return Result.success()
        val file = if (prefs.getBoolean("autoDownloadUpdates", true)) repo.download(update) else null
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val chinese = prefs.getString("language", "English") == "中文"
        manager.createNotificationChannel(NotificationChannel("updates", if (chinese) "应用更新" else "App updates", NotificationManager.IMPORTANCE_DEFAULT))
        val notification = NotificationCompat.Builder(applicationContext, "updates")
            .setSmallIcon(com.clearscan.R.drawable.ic_launcher)
            .setContentTitle(if (chinese) "ClearScan ${update.version} 可用" else "ClearScan ${update.version} is available")
            .setContentText(if (file != null) (if (chinese) "更新已下载，请在设置中安装" else "Downloaded; open Settings to install") else (if (chinese) "打开设置下载更新" else "Open Settings to download"))
            .setAutoCancel(true).build()
        runCatching { manager.notify(103, notification) }
        Result.success()
    }.getOrElse { AppLogger.e("Update", "Background update check failed", it); Result.retry() }
}
