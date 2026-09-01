package com.fingo.photocheck.update

data class UpdateInfo(
    val latestVersion: String,
    val currentVersion: String,
    val changelog: String,
    val downloadUrl: String,
    val apkSize: Long = 0L,
    val hasUpdate: Boolean = false,
    val publishedAt: String = ""
) {
    companion object {
        fun isNewer(latest: String, current: String): Boolean {
            val cleanLatest = latest.trim().removePrefix("v").removePrefix("V")
            val cleanCurrent = current.trim().removePrefix("v").removePrefix("V")

            if (cleanLatest == cleanCurrent) return false

            val latestParts = cleanLatest.split(".", "-", "_").mapNotNull { it.toIntOrNull() }
            val currentParts = cleanCurrent.split(".", "-", "_").mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(latestParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val l = latestParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }

            return false
        }
    }
}
