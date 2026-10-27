package io.github.goodwind.hypermissions.update

import java.io.IOException
import org.json.JSONException
import org.json.JSONObject

/** A release of the app that can be installed. [sha256] is GitHub's digest of the file, or null if it gave none. */
data class Update(val version: String, val tag: String, val apkUrl: String, val apkName: String, val sha256: String?, val pageUrl: String)

sealed interface UpdateResult {
    data class Available(val update: Update) : UpdateResult
    data object UpToDate : UpdateResult

    /** There is no release to offer: none yet, a draft, a pre-release, or one without the file. */
    data object NoRelease : UpdateResult
    data object BadAnswer : UpdateResult
    data class Failed(val reason: String) : UpdateResult
}

/** Reads GitHub's "latest release" answer. */
object Updates {
    /** [installed] is the version now on the TV, or blank if there is none (the installer's case). [wanted] picks the file by name. */
    fun latest(json: String, installed: String, wanted: (String) -> Boolean): UpdateResult {
        try {
            val release = JSONObject(json)
            if (release.optBoolean("draft") || release.optBoolean("prerelease")) return UpdateResult.NoRelease
            val assets = release.optJSONArray("assets")
            var apk: JSONObject? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.optString("name").endsWith(".apk") && wanted(a.optString("name")) && a.optString("browser_download_url").isNotEmpty()) { apk = a; break }
                }
            }
            if (apk == null) return UpdateResult.NoRelease
            val tag = release.optString("tag_name")
            val version = Version.parse(tag) ?: return UpdateResult.BadAnswer
            if (version <= (Version.parse(installed) ?: Version(listOf(0)))) return UpdateResult.UpToDate
            val digest = apk.optString("digest").removePrefix("sha256:").lowercase().ifEmpty { null }
            return UpdateResult.Available(Update(version.toString(), tag, apk.getString("browser_download_url"), apk.getString("name"), digest, release.optString("html_url")))
        } catch (e: JSONException) {
            return UpdateResult.BadAnswer
        }
    }
}

/** Asks GitHub whether the repository has a newer release. */
class UpdateChecker(private val transport: Transport, private val repo: String) {
    fun check(installed: String, wanted: (String) -> Boolean): UpdateResult {
        val reply = try {
            transport.get(Request("https://api.github.com/repos/$repo/releases/latest", mapOf("Accept" to "application/vnd.github+json", "User-Agent" to repo.substringAfter('/'))))
        } catch (e: IOException) {
            return UpdateResult.Failed(e.message ?: "io")
        }
        return when (reply.code) {
            200 -> Updates.latest(reply.body, installed, wanted).let { if (it is UpdateResult.Available && !ours(it.update.apkUrl)) UpdateResult.BadAnswer else it }
            404 -> UpdateResult.NoRelease
            else -> UpdateResult.Failed("HTTP ${reply.code}")
        }
    }

    /** Only a file of this repository's own releases is offered, however the answer came to say otherwise. */
    private fun ours(url: String) = url.startsWith("https://github.com/$repo/releases/download/") && ".." !in url
}
