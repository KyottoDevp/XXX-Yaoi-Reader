package com.independent.xxxyaoireader.model

data class Manga(
    val title: String,
    val url: String,
    val coverUrl: String = "",
    val synopsis: String = "",
    val author: String = "",
    val artist: String = "",
    val status: String = "Desconhecido",
    val genres: List<String> = emptyList(),
    val chapterCount: Int = 0,
)

data class Chapter(
    val title: String,
    val url: String,
    val mangaUrl: String,
    val publishedAt: String = "",
)

data class ReaderPage(
    val index: Int,
    val url: String,
)

data class DownloadedChapter(
    val mangaTitle: String,
    val chapterTitle: String,
    val folderPath: String,
    val pageCount: Int,
    val downloadedAt: Long = System.currentTimeMillis(),
)

enum class AppScreen {
    HOME, SEARCH, LIBRARY, DOWNLOADS, SETTINGS, DETAIL, READER
}
