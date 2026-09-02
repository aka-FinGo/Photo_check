package com.fingo.photocheck.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {

    private const val GITHUB_LATEST_RELEASE_URL =
        "https://api.github.com/repos/aka-FinGo/Photo_check/releases/latest"

    /**
     * GitHub Releases API orqali eng so'nggi yangilanishni tekshirish
     */
    suspend fun checkForUpdate(context: Context): UpdateInfo? = checkForUpdates(context).getOrNull()

    suspend fun checkForUpdates(context: Context): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        val currentVersion = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.01"
        } catch (e: Exception) {
            "1.0.01"
        }

        // --- ATTEMPT 1: GitHub REST API with browser-grade User-Agent ---
        try {
            val url = URL(GITHUB_LATEST_RELEASE_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36 PhotoCheck/1.0")
                connectTimeout = 8000
                readTimeout = 10000
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(jsonString)

                val tagName = jsonObject.optString("tag_name", "").trim()
                val changelog = jsonObject.optString("body", "Yangi imkoniyatlar va yaxshilanishlar.").trim()
                val publishedAt = jsonObject.optString("published_at", "")

                var downloadUrl = ""
                var apkSize = 0L

                val assets = jsonObject.optJSONArray("assets")
                if (assets != null) {
                    var selectedAsset: JSONObject? = null
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.equals("PhotoCheck.apk", ignoreCase = true)) {
                            selectedAsset = asset
                            break
                        }
                    }
                    if (selectedAsset == null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if ((name.startsWith("PhotoCheck", ignoreCase = true) || name.contains("universal", ignoreCase = true)) && name.endsWith(".apk", ignoreCase = true)) {
                                selectedAsset = asset
                                break
                            }
                        }
                    }
                    if (selectedAsset == null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk", ignoreCase = true)) {
                                selectedAsset = asset
                                break
                            }
                        }
                    }
                    if (selectedAsset != null) {
                        downloadUrl = selectedAsset.optString("browser_download_url", "")
                        apkSize = selectedAsset.optLong("size", 0L)
                    }
                }

                val cleanLatest = tagName.removePrefix("v").removePrefix("V")
                val finalDownloadUrl = downloadUrl.ifBlank {
                    "https://github.com/aka-FinGo/Photo_check/releases/download/$tagName/PhotoCheck.apk"
                }
                val hasUpdate = UpdateInfo.isNewer(cleanLatest, currentVersion)

                return@withContext Result.success(
                    UpdateInfo(
                        latestVersion = cleanLatest.ifBlank { currentVersion },
                        currentVersion = currentVersion,
                        changelog = changelog,
                        downloadUrl = finalDownloadUrl,
                        apkSize = apkSize,
                        hasUpdate = hasUpdate,
                        publishedAt = publishedAt
                    )
                )
            }
        } catch (e: Exception) {
            // Proceed to Fallback
        }

        // --- ATTEMPT 2: Direct GitHub 302 Redirect Inspection (100% Rate-Limit Free) ---
        try {
            val directUrl = URL("https://github.com/aka-FinGo/Photo_check/releases/latest")
            val directConn = (directUrl.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")
                connectTimeout = 8000
                readTimeout = 8000
            }

            val loc = directConn.getHeaderField("Location")
            if (!loc.isNullOrBlank()) {
                val tag = loc.substringAfterLast("/")
                val cleanLatest = tag.removePrefix("v").removePrefix("V").trim()
                val downloadUrl = "https://github.com/aka-FinGo/Photo_check/releases/download/$tag/PhotoCheck.apk"
                val hasUpdate = UpdateInfo.isNewer(cleanLatest, currentVersion)

                return@withContext Result.success(
                    UpdateInfo(
                        latestVersion = cleanLatest,
                        currentVersion = currentVersion,
                        changelog = "Yangi reliz: v$cleanLatest. Dasturni yangilash tavsiya etiladi.",
                        downloadUrl = downloadUrl,
                        apkSize = 0L,
                        hasUpdate = hasUpdate,
                        publishedAt = ""
                    )
                )
            }
        } catch (e2: Exception) {
            return@withContext Result.failure(
                Exception("Internetga ulanishni tekshiring: ${e2.localizedMessage ?: "GitHub serveriga ulanib bo'lmadi"}")
            )
        }

        Result.failure(Exception("GitHub-dan so'nggi versiyani aniqlab bo'lmadi"))
    }

    /**
     * APK faylini yuklab olish va jarayonni (progress) kuzatish
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.externalCacheDir ?: context.cacheDir
            val outputFile = File(cacheDir, "PhotoCheck_Update.apk")
            if (outputFile.exists()) {
                outputFile.delete()
            }

            var currentUrl = downloadUrl
            var connection: HttpURLConnection
            var redirects = 0

            while (true) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "PhotoCheck-Android-App")
                    connectTimeout = 15000
                    readTimeout = 30000
                }

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == HttpURLConnection.HTTP_SEE_OTHER ||
                    status == 307 || status == 308
                ) {
                    val newUrl = connection.getHeaderField("Location")
                    if (newUrl != null && redirects < 5) {
                        currentUrl = newUrl
                        redirects++
                        continue
                    }
                }
                break
            }

            val totalLength = connection.contentLengthLong.takeIf { it > 0 } ?: 0L
            val inputStream: InputStream = connection.inputStream
            val outputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(8192)
            var downloadedBytes = 0L
            var read: Int

            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
                downloadedBytes += read

                val progress = if (totalLength > 0) {
                    downloadedBytes.toFloat() / totalLength.toFloat()
                } else {
                    0f
                }
                withContext(Dispatchers.Main) {
                    onProgress(progress, downloadedBytes, totalLength)
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * FileProvider orqali yuklab olingan APK-ni o'rnatish
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            if (!apkFile.exists() || apkFile.length() == 0L) {
                Toast.makeText(context, "APK fayli topilmadi yoki bo'sh!", Toast.LENGTH_SHORT).show()
                return
            }

            // Android 8.0+ (Oreo) da noma'lum manbalardan o'rnatish ruxsatini tekshirish
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Toast.makeText(
                        context,
                        "Iltimos, ilovani yangilash uchun o'rnatish ruxsatini bering",
                        Toast.LENGTH_LONG
                    ).show()
                    val permissionIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(permissionIntent)
                    return
                }
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "O'rnatishda xatolik: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
