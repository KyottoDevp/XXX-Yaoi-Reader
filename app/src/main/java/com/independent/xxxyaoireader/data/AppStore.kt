package com.independent.xxxyaoireader.data

import android.content.Context
import com.independent.xxxyaoireader.model.DownloadedChapter
import com.independent.xxxyaoireader.model.Manga
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AppStore(context: Context) {
    private val prefs = context.getSharedPreferences("xxxyaoi_reader", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun favorites(): List<Manga> = decode("favorites", object : TypeToken<List<Manga>>() {}.type) ?: emptyList()
    fun history(): List<Manga> = decode("history", object : TypeToken<List<Manga>>() {}.type) ?: emptyList()
    fun downloads(): List<DownloadedChapter> = decode("downloads", object : TypeToken<List<DownloadedChapter>>() {}.type) ?: emptyList()

    fun setFavorites(items: List<Manga>) = encode("favorites", items)
    fun setHistory(items: List<Manga>) = encode("history", items)
    fun setDownloads(items: List<DownloadedChapter>) = encode("downloads", items)

    fun cookieHeader(): String = prefs.getString("cookie_header", "").orEmpty()
    fun setCookieHeader(value: String) { prefs.edit().putString("cookie_header", value.trim()).apply() }

    private fun encode(key: String, value: Any) {
        prefs.edit().putString(key, gson.toJson(value)).apply()
    }

    private fun <T> decode(key: String, type: java.lang.reflect.Type): T? {
        val raw = prefs.getString(key, null) ?: return null
        return runCatching { gson.fromJson<T>(raw, type) }.getOrNull()
    }
}
