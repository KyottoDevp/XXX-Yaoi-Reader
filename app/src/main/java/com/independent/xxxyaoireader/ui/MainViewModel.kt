package com.independent.xxxyaoireader.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.independent.xxxyaoireader.data.AppStore
import com.independent.xxxyaoireader.data.SiteBlockedException
import com.independent.xxxyaoireader.data.XXXYaoiClient
import com.independent.xxxyaoireader.XXXYaoiApplication
import com.independent.xxxyaoireader.model.AppScreen
import com.independent.xxxyaoireader.model.Chapter
import com.independent.xxxyaoireader.model.DownloadedChapter
import com.independent.xxxyaoireader.model.Manga
import com.independent.xxxyaoireader.model.ReaderPage
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReaderUiState(
    val screen: AppScreen = AppScreen.HOME,
    val query: String = "",
    val homeItems: List<Manga> = emptyList(),
    val searchItems: List<Manga> = emptyList(),
    val favorites: List<Manga> = emptyList(),
    val history: List<Manga> = emptyList(),
    val downloads: List<DownloadedChapter> = emptyList(),
    val selectedManga: Manga? = null,
    val chapters: List<Chapter> = emptyList(),
    val selectedChapter: Chapter? = null,
    val pages: List<ReaderPage> = emptyList(),
    val loading: Boolean = false,
    val loadingMessage: String = "",
    val error: String? = null,
    val notice: String? = null,
    val cookieHeader: String = "",
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val store = AppStore(application)
    private val client = XXXYaoiClient(application, store, (application as XXXYaoiApplication).networkClient)

    var state = androidx.compose.runtime.mutableStateOf(
        ReaderUiState(
            favorites = store.favorites(),
            history = store.history(),
            downloads = store.downloads(),
            cookieHeader = store.cookieHeader(),
        ),
    )
        private set

    init { loadHome() }

    fun setScreen(screen: AppScreen) {
        state.value = state.value.copy(screen = screen, error = null, notice = null)
        if (screen == AppScreen.LIBRARY) refreshLocalLists()
        if (screen == AppScreen.HOME && state.value.homeItems.isEmpty()) loadHome()
    }

    fun setQuery(value: String) { state.value = state.value.copy(query = value) }

    fun loadHome() {
        viewModelScope.launch {
            state.value = state.value.copy(loading = true, loadingMessage = "Carregando catálogo", error = null)
            runCatching { client.latest() }
                .onSuccess { state.value = state.value.copy(homeItems = it, loading = false, loadingMessage = "") }
                .onFailure { handleFailure(it) }
        }
    }

    fun search() {
        val query = state.value.query.trim()
        if (query.isBlank()) {
            state.value = state.value.copy(error = "Digite o nome de uma obra para pesquisar.")
            return
        }
        viewModelScope.launch {
            state.value = state.value.copy(loading = true, loadingMessage = "Pesquisando no XXX Yaoi", error = null, screen = AppScreen.SEARCH)
            runCatching { client.search(query) }
                .onSuccess { state.value = state.value.copy(searchItems = it, loading = false, loadingMessage = "") }
                .onFailure { handleFailure(it) }
        }
    }

    fun openManga(manga: Manga) {
        viewModelScope.launch {
            state.value = state.value.copy(
                screen = AppScreen.DETAIL,
                selectedManga = manga,
                chapters = emptyList(),
                selectedChapter = null,
                pages = emptyList(),
                loading = true,
                loadingMessage = "Carregando informações da obra",
                error = null,
            )
            runCatching { client.mangaDetails(manga) }
                .onSuccess { (updated, chapters) ->
                    addHistory(updated)
                    state.value = state.value.copy(selectedManga = updated, chapters = chapters, loading = false, loadingMessage = "")
                }
                .onFailure { handleFailure(it) }
        }
    }

    fun readChapter(chapter: Chapter) {
        viewModelScope.launch {
            state.value = state.value.copy(
                screen = AppScreen.READER,
                selectedChapter = chapter,
                pages = emptyList(),
                loading = true,
                loadingMessage = "Obtendo páginas do capítulo",
                error = null,
            )
            runCatching { client.chapterPages(chapter) }
                .onSuccess { pages ->
                    state.value = state.value.copy(pages = pages, loading = false, loadingMessage = "")
                }
                .onFailure {
                    state.value = state.value.copy(screen = AppScreen.DETAIL)
                    handleFailure(it)
                }
        }
    }

    fun downloadChapter(chapter: Chapter) {
        val manga = state.value.selectedManga ?: return
        viewModelScope.launch {
            state.value = state.value.copy(loading = true, loadingMessage = "Baixando ${chapter.title}", error = null)
            runCatching { client.downloadChapter(manga, chapter) }
                .onSuccess { downloaded ->
                    val updated = listOf(downloaded) + store.downloads().filterNot { it.folderPath == downloaded.folderPath }
                    store.setDownloads(updated)
                    state.value = state.value.copy(downloads = updated, loading = false, loadingMessage = "", notice = "Capítulo salvo no armazenamento do app.")
                }
                .onFailure { handleFailure(it) }
        }
    }

    fun openDownloadedChapter(download: DownloadedChapter) {
        viewModelScope.launch {
            state.value = state.value.copy(
                screen = AppScreen.READER,
                selectedChapter = Chapter(download.chapterTitle, "", ""),
                pages = emptyList(),
                loading = true,
                loadingMessage = "Abrindo capítulo offline",
                error = null,
            )
            runCatching {
                withContext(kotlinx.coroutines.Dispatchers.IO) {
                    File(download.folderPath).listFiles()
                        .orEmpty()
                        .filter { it.isFile && it.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp", "avif") }
                        .sortedBy { it.name }
                        .mapIndexed { index, file -> ReaderPage(index, file.toURI().toString()) }
                }
            }.onSuccess { pages ->
                if (pages.isEmpty()) {
                    state.value = state.value.copy(loading = false, loadingMessage = "", error = "Os arquivos deste download não foram encontrados.")
                } else {
                    state.value = state.value.copy(pages = pages, loading = false, loadingMessage = "")
                }
            }.onFailure { handleFailure(it) }
        }
    }

    fun toggleFavorite(manga: Manga) {
        val list = store.favorites().toMutableList()
        val exists = list.any { it.url == manga.url }
        if (exists) list.removeAll { it.url == manga.url } else list.add(0, manga)
        store.setFavorites(list)
        state.value = state.value.copy(favorites = list, notice = if (exists) "Removido dos favoritos" else "Adicionado aos favoritos")
    }

    fun saveCookieHeader(value: String) {
        store.setCookieHeader(value)
        state.value = state.value.copy(cookieHeader = value, notice = "Cabeçalho de cookie salvo localmente neste aparelho.")
    }

    fun back() {
        val current = state.value.screen
        state.value = when (current) {
            AppScreen.READER -> state.value.copy(screen = AppScreen.DETAIL, pages = emptyList(), selectedChapter = null, error = null)
            AppScreen.DETAIL -> state.value.copy(screen = AppScreen.HOME, selectedManga = null, chapters = emptyList(), error = null)
            else -> state.value.copy(screen = AppScreen.HOME, error = null)
        }
    }

    fun dismissMessage() { state.value = state.value.copy(error = null, notice = null) }

    private fun addHistory(manga: Manga) {
        val history = listOf(manga) + store.history().filterNot { it.url == manga.url }
        store.setHistory(history.take(80))
        state.value = state.value.copy(history = history.take(80))
    }

    private fun refreshLocalLists() {
        state.value = state.value.copy(favorites = store.favorites(), history = store.history(), downloads = store.downloads())
    }

    private fun handleFailure(error: Throwable) {
        val message = when (error) {
            is SiteBlockedException -> error.message ?: "O site recusou a conexão."
            else -> error.message ?: "Falha inesperada ao acessar o site."
        }
        state.value = state.value.copy(loading = false, loadingMessage = "", error = message)
    }
}
