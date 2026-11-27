package com.independent.xxxyaoireader.data

import android.content.Context
import com.independent.xxxyaoireader.model.Chapter
import com.independent.xxxyaoireader.model.DownloadedChapter
import com.independent.xxxyaoireader.model.Manga
import com.independent.xxxyaoireader.model.ReaderPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class SiteBlockedException(message: String) : Exception(message)

/**
 * Pure HTTP implementation. It deliberately does not create or embed a WebView.
 * The source website may still require an interactive Cloudflare verification;
 * this client reports that state instead of attempting to evade it.
 */
class XXXYaoiClient(private val context: Context, private val store: AppStore, sharedClient: OkHttpClient? = null) {
    companion object {
        const val BASE_URL = "https://3xyaoi.com"
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 15; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
        private const val IMAGE_PATH = "/WP-manga/data/"
    }

    private val requestGate = Mutex()
    private var lastRequestAt = 0L
    private val cookieJar = object : CookieJar {
        private val sessionCookies = mutableListOf<Cookie>()

        @Synchronized
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookies.forEach { incoming ->
                sessionCookies.removeAll { it.name == incoming.name && it.domain == incoming.domain && it.path == incoming.path }
                if (incoming.expiresAt > System.currentTimeMillis()) sessionCookies += incoming
            }
        }

        @Synchronized
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val now = System.currentTimeMillis()
            sessionCookies.removeAll { it.expiresAt <= now }
            val stored = sessionCookies.filter { it.matches(url) }
            val isSiteHost = url.host == "3xyaoi.com" || url.host.endsWith(".3xyaoi.com")
            val manual = if (!isSiteHost) emptyList() else store.cookieHeader().split(';').mapNotNull { raw ->
                val separator = raw.indexOf('=')
                if (separator <= 0) return@mapNotNull null
                val name = raw.substring(0, separator).trim()
                val value = raw.substring(separator + 1).trim()
                if (name.isBlank() || name.any(Char::isWhitespace)) return@mapNotNull null
                runCatching {
                    Cookie.Builder().name(name).value(value).hostOnlyDomain(url.host).path("/").build()
                }.getOrNull()
            }
            return (stored + manual).distinctBy { it.name }
        }
    }

    val okHttpClient: OkHttpClient = sharedClient ?: OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(18, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.7")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Referer", "$BASE_URL/")
                .build()
            chain.proceed(request)
        }
        .build()

    suspend fun latest(page: Int = 1): List<Manga> = withContext(Dispatchers.IO) {
        val url = if (page <= 1) "$BASE_URL/biblioteca/" else "$BASE_URL/biblioteca/?pg=$page"
        parseListing(fetchDocument(url))
    }

    suspend fun search(query: String, page: Int = 1): List<Manga> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/biblioteca/?search=${encode(query)}" + if (page > 1) "&pg=$page" else ""
        parseListing(fetchDocument(url))
    }

    suspend fun mangaDetails(manga: Manga): Pair<Manga, List<Chapter>> = withContext(Dispatchers.IO) {
        val doc = fetchDocument(resolve(manga.url))
        val title = doc.selectFirst("h1")?.text()?.trim().orEmpty().ifBlank { manga.title }
        val cover = doc.selectFirst(".xyaoi-cover-img")?.let { absolute(it.attr("src").ifBlank { it.attr("data-src") }) }.orEmpty()
            .ifBlank { manga.coverUrl }
        val synopsis = doc.selectFirst(".xyaoi-synopsis-text")?.text()?.trim().orEmpty()
        val author = doc.select("a[href*=/author/]").joinToString { it.text().trim() }
        val artist = doc.select("a[href*=/artist/]").joinToString { it.text().trim() }
        val status = propertyAfterLabel(doc, "STATUS").ifBlank { "Desconhecido" }
        val genres = buildList {
            propertyAfterLabel(doc, "TIPO").takeIf { it.isNotBlank() }?.let { add(it) }
            doc.select("a[rel=tag][href*=genre]").eachText().map { it.trim() }.filter { it.isNotBlank() }.forEach { add(it) }
        }.distinct()

        val mangaPath = URI(resolve(manga.url)).path.trimEnd('/') + "/"
        val chapters = doc.select("li > a[href]").mapNotNull { link ->
            val full = link.absUrl("href").ifBlank { resolve(link.attr("href")) }
            val path = runCatching { URI(full).path }.getOrNull() ?: return@mapNotNull null
            if (!path.startsWith(mangaPath)) return@mapNotNull null
            val slug = path.removePrefix(mangaPath).trim('/')
            if (slug.isBlank() || '/' in slug) return@mapNotNull null
            val chapterTitle = link.selectFirst("header")?.text()?.trim().orEmpty()
                .ifBlank { link.text().trim() }
            if (chapterTitle.isBlank()) return@mapNotNull null
            Chapter(chapterTitle, slug, manga.url, link.selectFirst("time")?.text()?.trim().orEmpty())
        }.distinctBy { it.url }

        val updated = manga.copy(
            title = title,
            coverUrl = cover,
            synopsis = synopsis,
            author = author,
            artist = artist,
            status = status,
            genres = genres,
            chapterCount = chapters.size,
        )
        updated to chapters
    }

    suspend fun chapterPages(chapter: Chapter): List<ReaderPage> = withContext(Dispatchers.IO) {
        val chapterUrl = resolve(chapter.mangaUrl.trimEnd('/') + "/" + chapter.url.trim('/') + "/")
        val doc = fetchDocument(chapterUrl)
        val candidates = LinkedHashSet<String>()

        val imageSelectors = listOf(
            ".reading-content img", ".reader-area img", ".wp-manga-chapter-img",
            ".page-break img", "img[src*='$IMAGE_PATH']", "img[data-src*='$IMAGE_PATH']",
        )
        imageSelectors.forEach { selector ->
            doc.select(selector).forEach { image ->
                val raw = sequenceOf("data-src", "data-lazy-src", "data-original", "src")
                    .map { image.attr(it).trim() }.firstOrNull { it.isNotBlank() && !it.startsWith("data:") }
                if (raw != null) absolute(raw).takeIf(::isImageUrl)?.let(candidates::add)
            }
        }

        // Some versions publish page URLs in inline JSON/JavaScript rather than img tags.
        val scripts = doc.select("script").joinToString("\n") { it.data().ifBlank { it.html() } }
            .replace("\\/", "/")
        val urlRegex = Regex("(?:https?:)?//[^\\\"'\\s<>]+$IMAGE_PATH[^\\\"'\\s<>]+|/$IMAGE_PATH[^\\\"'\\s<>]+", RegexOption.IGNORE_CASE)
        urlRegex.findAll(scripts).forEach { match ->
            val raw = match.value.trimEnd(',', ';', ')', ']', '}')
            absolute(raw).takeIf(::isImageUrl)?.let(candidates::add)
        }

        if (candidates.isEmpty()) {
            val html = doc.outerHtml().lowercase()
            if (html.contains("challenges.cloudflare.com") || html.contains("cf-turnstile") || html.contains("cf-chl-")) {
                throw SiteBlockedException("O XXX Yaoi exigiu uma verificação anti-bot. Este leitor não usa WebView e não tenta contornar a verificação. Conclua o acesso pelo navegador e configure manualmente um cookie válido, se o site permitir.")
            }
            throw IllegalStateException("O capítulo abriu, mas o HTML não contém URLs de páginas reconhecíveis. O leitor do site pode estar gerando as imagens por JavaScript; este APK é 100% nativo e ainda não consegue executar esse leitor.")
        }
        candidates.mapIndexed { index, url -> ReaderPage(index, url) }
    }

    suspend fun downloadChapter(manga: Manga, chapter: Chapter): DownloadedChapter = withContext(Dispatchers.IO) {
        val pages = chapterPages(chapter)
        val safeManga = safeName(manga.title)
        val safeChapter = safeName(chapter.title)
        val baseDirectory = context.getExternalFilesDir(null) ?: context.filesDir
        val folder = File(baseDirectory, "downloads/$safeManga/$safeChapter")
        if (!folder.exists() && !folder.mkdirs()) error("Não foi possível criar a pasta de downloads")
        pages.forEachIndexed { index, page ->
            val request = Request.Builder().url(page.url).header("Referer", resolve(chapter.mangaUrl)).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Falha ao baixar a página ${index + 1}: HTTP ${response.code}")
                val bytes = response.body?.bytes() ?: error("Página ${index + 1} vazia")
                val extension = when {
                    response.header("Content-Type").orEmpty().contains("webp") -> "webp"
                    response.header("Content-Type").orEmpty().contains("png") -> "png"
                    else -> "jpg"
                }
                File(folder, "%03d.%s".format(index + 1, extension)).writeBytes(bytes)
            }
        }
        DownloadedChapter(manga.title, chapter.title, folder.absolutePath, pages.size)
    }

    private suspend fun fetchDocument(url: String): Document {
        requestGate.withLock {
            val elapsed = System.currentTimeMillis() - lastRequestAt
            if (elapsed < 1_100) delay(1_100 - elapsed)
            lastRequestAt = System.currentTimeMillis()
        }
        val request = Request.Builder().url(url).get().build()
        return okHttpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            val html = body.lowercase()
            if (response.code == 403 || response.code == 429 || response.code == 503 ||
                html.contains("cf-chl-") || html.contains("challenges.cloudflare.com") ||
                html.contains("just a moment") || html.contains("cf-turnstile")) {
                throw SiteBlockedException("O site bloqueou a requisição HTTP (HTTP ${response.code}). Pode ser uma verificação do Cloudflare, limite de acesso ou proteção temporária. O app não tenta burlar esse bloqueio.")
            }
            if (!response.isSuccessful) error("O site respondeu HTTP ${response.code}")
            Jsoup.parse(body, response.request.url.toString())
        }
    }

    private fun parseListing(doc: Document): List<Manga> {
        val found = LinkedHashMap<String, Manga>()
        doc.select("a[href*='/bl/']").forEach { link ->
            val href = link.absUrl("href").ifBlank { resolve(link.attr("href")) }
            val path = runCatching { URI(href).path }.getOrNull() ?: return@forEach
            if (!Regex("^/bl/[^/]+/?$").matches(path)) return@forEach
            val image = link.selectFirst("img[alt], img") ?: return@forEach
            val title = image.attr("alt").trim().ifBlank { link.text().trim() }
            if (title.isBlank()) return@forEach
            val cover = sequenceOf("data-src", "data-lazy-src", "src")
                .map { image.attr(it).trim() }.firstOrNull { it.isNotBlank() }?.let(::absolute).orEmpty()
            found[path] = Manga(title = title, url = path, coverUrl = cover)
        }
        return found.values.toList()
    }

    private fun propertyAfterLabel(doc: Document, label: String): String =
        doc.select("*:matchesOwn(^${Regex.escape(label)}$) + *").firstOrNull()?.text()?.trim().orEmpty()

    private fun resolve(path: String): String = runCatching { URI(BASE_URL).resolve(path).toString() }.getOrDefault(path)
    private fun absolute(raw: String): String = when {
        raw.startsWith("//") -> "https:$raw"
        raw.startsWith("http://") || raw.startsWith("https://") -> raw
        else -> resolve(raw)
    }
    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
    private fun isImageUrl(url: String): Boolean = url.startsWith("https://") &&
        (url.contains(IMAGE_PATH, ignoreCase = true) || url.substringBefore('?').matches(Regex(".*\\.(?:jpe?g|png|webp|avif)$", RegexOption.IGNORE_CASE)))
    private fun safeName(value: String): String = value.replace(Regex("[^A-Za-z0-9._-]+"), "_").trim('_').take(70).ifBlank { "item" }
}
