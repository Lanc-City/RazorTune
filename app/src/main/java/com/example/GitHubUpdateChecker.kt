package com.example

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

object GitHubUpdateChecker {
    private const val GITHUB_REPO = "Lanc-City/RazorTune"

    suspend fun checkForUpdate(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val latestTag = json.getString("tag_name")
                val downloadUrl = json.getJSONArray("assets").getJSONObject(0).getString("browser_download_url")
                val releaseNotes = json.optString("body", "Bug fixes and performance improvements.")

                val cleanLatest = latestTag.removePrefix("v")
                val cleanCurrent = currentVersion.removePrefix("v")

                if (cleanLatest != cleanCurrent) {
                    return@withContext UpdateInfo(cleanLatest, downloadUrl, releaseNotes)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }
}

data class UpdateInfo(
    val version: String,
    val downloadUrl: String,
    val releaseNotes: String
)