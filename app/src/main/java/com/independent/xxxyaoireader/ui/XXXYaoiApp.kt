package com.independent.xxxyaoireader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.independent.xxxyaoireader.model.AppScreen
import com.independent.xxxyaoireader.model.Chapter
import com.independent.xxxyaoireader.model.DownloadedChapter
import com.independent.xxxyaoireader.model.Manga
import com.independent.xxxyaoireader.model.ReaderPage

private val Ink = Color(0xFF0B0A10)
private val Panel = Color(0xFF17151E)
private val PanelSoft = Color(0xFF211D29)
private val Pink = Color(0xFFF0A4CE)
private val Muted = Color(0xFFAAA4B4)
private val White = Color(0xFFF8F5FA)

@Composable
fun XXXYaoiApp(viewModel: MainViewModel) {
    val state = viewModel.state.value
    val colors = darkColorScheme(
        primary = Pink,
        onPrimary = Color(0xFF351A2A),
        secondary = Color(0xFFC6A6E5),
        background = Ink,
        surface = Panel,
        surfaceVariant = PanelSoft,
        onBackground = White,
        onSurface = White,
        onSurfaceVariant = Muted,
        error = Color(0xFFFF8B9B),
    )

    MaterialTheme(colorScheme = colors) {
        Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
            val showBottom = state.screen in setOf(AppScreen.HOME, AppScreen.SEARCH, AppScreen.LIBRARY, AppScreen.DOWNLOADS, AppScreen.SETTINGS)
            Scaffold(
                containerColor = Ink,
                contentWindowInsets = WindowInsets.statusBars,
                bottomBar = {
                    if (showBottom) BottomNavigation(state.screen, viewModel::setScreen)
                },
            ) { padding ->
                when (state.screen) {
                    AppScreen.HOME -> HomeScreen(state.homeItems, state.loading, state.loadingMessage, padding, viewModel::loadHome, viewModel::openManga, viewModel::toggleFavorite)
                    AppScreen.SEARCH -> SearchScreen(state.query, state.searchItems, state.loading, padding, viewModel::setQuery, viewModel::search, viewModel::openManga)
                    AppScreen.LIBRARY -> LibraryScreen(state.favorites, state.history, padding, viewModel::openManga, viewModel::toggleFavorite)
                    AppScreen.DOWNLOADS -> DownloadsScreen(state.downloads, padding, viewModel::openDownloadedChapter)
                    AppScreen.SETTINGS -> SettingsScreen(state.cookieHeader, padding, viewModel::saveCookieHeader)
                    AppScreen.DETAIL -> DetailScreen(
                        manga = state.selectedManga,
                        chapters = state.chapters,
                        favorites = state.favorites,
                        loading = state.loading,
                        loadingMessage = state.loadingMessage,
                        onBack = viewModel::back,
                        onFavorite = viewModel::toggleFavorite,
                        onRead = viewModel::readChapter,
                        onDownload = viewModel::downloadChapter,
                    )
                    AppScreen.READER -> ReaderScreen(
                        chapter = state.selectedChapter,
                        pages = state.pages,
                        loading = state.loading,
                        loadingMessage = state.loadingMessage,
                        onBack = viewModel::back,
                    )
                }
            }

            state.error?.let { message ->
                AlertDialog(
                    onDismissRequest = viewModel::dismissMessage,
                    icon = { Icon(Icons.Default.Info, contentDescription = null, tint = Pink) },
                    title = { Text("Não foi possível carregar") },
                    text = { Text(message) },
                    confirmButton = { TextButton(onClick = viewModel::dismissMessage) { Text("Entendi") } },
                    containerColor = Panel,
                )
            }
            state.notice?.let { message ->
                AlertDialog(
                    onDismissRequest = viewModel::dismissMessage,
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Pink) },
                    title = { Text("Pronto") },
                    text = { Text(message) },
                    confirmButton = { TextButton(onClick = viewModel::dismissMessage) { Text("OK") } },
                    containerColor = Panel,
                )
            }
        }
    }
}

@Composable
private fun BottomNavigation(current: AppScreen, onNavigate: (AppScreen) -> Unit) {
    val entries = listOf(
        Triple(AppScreen.HOME, "Início", Icons.Default.Home),
        Triple(AppScreen.SEARCH, "Buscar", Icons.Default.Search),
        Triple(AppScreen.LIBRARY, "Biblioteca", Icons.Default.Bookmark),
        Triple(AppScreen.DOWNLOADS, "Baixados", Icons.Default.Download),
        Triple(AppScreen.SETTINGS, "Ajustes", Icons.Default.Settings),
    )
    NavigationBar(containerColor = Panel, windowInsets = WindowInsets.navigationBars) {
        entries.forEach { (screen, label, icon) ->
            NavigationBarItem(
                selected = current == screen,
                onClick = { onNavigate(screen) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, maxLines = 1, fontSize = 10.sp) },
                alwaysShowLabel = true,
            )
        }
    }
}

@Composable
private fun HomeScreen(
    items: List<Manga>, loading: Boolean, loadingMessage: String, padding: PaddingValues,
    onRefresh: () -> Unit, onOpen: (Manga) -> Unit, onFavorite: (Manga) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(padding)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("XXX YAOI", color = Pink, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Explore histórias", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            }
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = Pink) }
        }
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF40253F), Color(0xFF1D1A28), Color(0xFF17151E))))
                .padding(18.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = Pink, modifier = Modifier.size(23.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Leitura independente", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                Text("Catálogo, capítulos e leitor vertical em uma interface Android nativa.", color = Color(0xFFD8CFDA), fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Text("Fonte: 3xyaoi.com", color = Pink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Atualizações recentes", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${items.size} obras", color = Muted, fontSize = 12.sp)
        }
        if (loading && items.isEmpty()) LoadingBlock(loadingMessage)
        else if (items.isEmpty()) EmptyBlock("Nenhuma obra recebida", "Toque em atualizar para tentar novamente.", onRefresh)
        else MangaGrid(items, onOpen, onFavorite, Modifier.weight(1f))
    }
}

@Composable
private fun SearchScreen(
    query: String, items: List<Manga>, loading: Boolean, padding: PaddingValues,
    onQuery: (String) -> Unit, onSearch: () -> Unit, onOpen: (Manga) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(padding)) {
        Text("Pesquisar", modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 14.dp), fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nome da obra") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = onSearch, shape = RoundedCornerShape(15.dp), contentPadding = PaddingValues(14.dp)) {
                Icon(Icons.Default.Search, contentDescription = "Pesquisar")
            }
        }
        if (loading) LoadingBlock("Pesquisando...")
        else if (items.isNotEmpty()) MangaGrid(items, onOpen, {}, Modifier.weight(1f))
        else EmptyBlock("Pesquise no catálogo", "Os resultados aparecerão aqui.", null)
    }
}

@Composable
private fun LibraryScreen(
    favorites: List<Manga>, history: List<Manga>, padding: PaddingValues,
    onOpen: (Manga) -> Unit, onFavorite: (Manga) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(padding)) {
        Text("Minha biblioteca", modifier = Modifier.padding(20.dp), fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("Favoritos", modifier = Modifier.padding(start = 18.dp, bottom = 8.dp), color = Pink, fontWeight = FontWeight.Bold)
        if (favorites.isEmpty()) Text("Você ainda não adicionou favoritos.", color = Muted, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
        else MangaGrid(favorites, onOpen, onFavorite, Modifier.height(240.dp))
        Divider(color = PanelSoft, modifier = Modifier.padding(vertical = 14.dp))
        Text("Continuar lendo", modifier = Modifier.padding(start = 18.dp, bottom = 8.dp), color = Pink, fontWeight = FontWeight.Bold)
        if (history.isEmpty()) Text("Seu histórico aparecerá aqui.", color = Muted, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
        else LazyColumn(contentPadding = PaddingValues(bottom = 20.dp)) {
            items(history, key = { it.url }) { manga ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpen(manga) }.padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(model = manga.coverUrl, contentDescription = null, modifier = Modifier.size(width = 54.dp, height = 72.dp).clip(RoundedCornerShape(9.dp)), contentScale = ContentScale.Crop)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(manga.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("Abrir detalhes", color = Muted, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Muted)
                }
            }
        }
    }
}

@Composable
private fun DownloadsScreen(downloads: List<DownloadedChapter>, padding: PaddingValues, onOpen: (DownloadedChapter) -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding)) {
        Text("Downloads", modifier = Modifier.padding(20.dp), fontSize = 25.sp, fontWeight = FontWeight.Bold)
        if (downloads.isEmpty()) EmptyBlock("Nenhum capítulo baixado", "Use o ícone de download na lista de capítulos.", null)
        else LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            items(downloads.size) { index ->
                val item = downloads[index]
                Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp).clickable { onOpen(item) }) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Abrir capítulo baixado", tint = Pink, modifier = Modifier.size(26.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(item.mangaTitle, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.chapterTitle, color = Muted, fontSize = 12.sp)
                            Text("${item.pageCount} páginas salvas", color = Pink, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(cookie: String, padding: PaddingValues, onSaveCookie: (String) -> Unit) {
    var draft by remember(cookie) { mutableStateOf(cookie) }
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Configurações", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Pink)
                    Spacer(Modifier.width(9.dp))
                    Text("Sessão do site", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(9.dp))
                Text(
                    "O leitor usa HTTP nativo e não incorpora WebView. Se o site entregar cookies de sessão por um processo legítimo que você controla, eles podem ser informados aqui. O app não resolve nem contorna desafios anti-bot.",
                    color = Muted, fontSize = 13.sp,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Cookie HTTP (opcional)") },
                    placeholder = { Text("nome=valor; outro=valor") },
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                )
                Spacer(Modifier.height(10.dp))
                Button(onClick = { onSaveCookie(draft) }, modifier = Modifier.fillMaxWidth()) { Text("Salvar sessão neste aparelho") }
            }
        }
        Spacer(Modifier.height(14.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(16.dp)) {
                Text("Aplicativo", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                Text("XXX Yaoi Reader · 0.1.0-alpha01", color = Muted, fontSize = 13.sp)
                Text("Android nativo · Kotlin · Jetpack Compose · OkHttp", color = Muted, fontSize = 12.sp)
                Text("Sem WebView embutido", color = Pink, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DetailScreen(
    manga: Manga?, chapters: List<Chapter>, favorites: List<Manga>, loading: Boolean, loadingMessage: String,
    onBack: () -> Unit, onFavorite: (Manga) -> Unit, onRead: (Chapter) -> Unit, onDownload: (Chapter) -> Unit,
) {
    if (manga == null) return
    Column(Modifier.fillMaxSize()) {
        DetailTopBar(onBack)
        LazyColumn(contentPadding = PaddingValues(bottom = 30.dp)) {
            item {
                Box(Modifier.fillMaxWidth().height(270.dp)) {
                    if (manga.coverUrl.isNotBlank()) {
                        AsyncImage(model = manga.coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Ink.copy(alpha = 0.65f), Ink))))
                    Row(Modifier.align(Alignment.BottomStart).padding(18.dp), verticalAlignment = Alignment.Bottom) {
                        AsyncImage(model = manga.coverUrl, contentDescription = null, modifier = Modifier.size(width = 112.dp, height = 154.dp).clip(RoundedCornerShape(13.dp)), contentScale = ContentScale.Crop)
                        Column(Modifier.weight(1f).padding(start = 14.dp, bottom = 5.dp)) {
                            Text(manga.title, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Text(manga.status, color = Pink, fontSize = 12.sp)
                            Text("${manga.chapterCount} capítulos", color = Muted, fontSize = 12.sp)
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    val isFavorite = favorites.any { it.url == manga.url }
                    FilledTonalButton(onClick = { onFavorite(manga) }, modifier = Modifier.weight(1f)) {
                        Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null)
                        Spacer(Modifier.width(6.dp)); Text(if (isFavorite) "Salvo" else "Favoritar")
                    }
                }
            }
            item {
                Column(Modifier.padding(horizontal = 18.dp)) {
                    Text("Sinopse", color = Pink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(7.dp))
                    Text(manga.synopsis.ifBlank { "Sinopse não encontrada no HTML público." }, color = Color(0xFFD1CBD5), fontSize = 14.sp, lineHeight = 21.sp)
                    if (manga.author.isNotBlank()) Text("Autor: ${manga.author}", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                    if (manga.artist.isNotBlank()) Text("Artista: ${manga.artist}", color = Muted, fontSize = 12.sp)
                    if (manga.genres.isNotEmpty()) Text(manga.genres.joinToString(" · "), color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Capítulos", fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("${chapters.size}", color = Pink, fontSize = 13.sp)
                }
            }
            if (loading && chapters.isEmpty()) item { LoadingBlock(loadingMessage) }
            if (!loading && chapters.isEmpty()) item { Text("Nenhum capítulo encontrado.", color = Muted, modifier = Modifier.padding(18.dp)) }
            items(chapters.size) { index ->
                val chapter = chapters[index]
                Row(
                    Modifier.fillMaxWidth().clickable { onRead(chapter) }.padding(horizontal = 18.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(chapter.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (chapter.publishedAt.isNotBlank()) Text(chapter.publishedAt, color = Muted, fontSize = 11.sp)
                    }
                    IconButton(onClick = { onDownload(chapter) }) { Icon(Icons.Default.Download, contentDescription = "Baixar capítulo", tint = Pink) }
                    TextButton(onClick = { onRead(chapter) }) { Text("Ler") }
                }
                Divider(color = PanelSoft, modifier = Modifier.padding(horizontal = 18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTopBar(onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text("Detalhes da obra", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar") } },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Ink),
    )
}

@Composable
private fun ReaderScreen(
    chapter: Chapter?, pages: List<ReaderPage>, loading: Boolean,
    loadingMessage: String, onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().background(Panel).padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar") }
            Column(Modifier.weight(1f)) {
                Text("Leitor", fontWeight = FontWeight.Bold)
                Text(chapter?.title.orEmpty(), color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("${pages.size} pág.", color = Pink, fontSize = 12.sp, modifier = Modifier.padding(end = 12.dp))
        }
        when {
            loading -> LoadingBlock(loadingMessage)
            pages.isEmpty() -> EmptyBlock("Nenhuma página", "O capítulo não retornou imagens para este leitor.", null)
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                items(pages.size) { index ->
                    AsyncImage(
                        model = pages[index].url,
                        contentDescription = "Página ${index + 1}",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.FillWidth,
                    )
                }
            }
        }
    }
}

@Composable
private fun MangaGrid(items: List<Manga>, onOpen: (Manga) -> Unit, onFavorite: (Manga) -> Unit, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(items, key = { it.url }) { manga ->
            Column(Modifier.clickable { onOpen(manga) }) {
                Box {
                    if (manga.coverUrl.isNotBlank()) {
                        AsyncImage(
                            model = manga.coverUrl,
                            contentDescription = manga.title,
                            modifier = Modifier.fillMaxWidth().aspectRatio(0.68f).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Box(Modifier.fillMaxWidth().aspectRatio(0.68f).clip(RoundedCornerShape(12.dp)).background(PanelSoft), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = Muted, modifier = Modifier.size(28.dp))
                        }
                    }
                    IconButton(
                        onClick = { onFavorite(manga) },
                        modifier = Modifier.align(Alignment.TopEnd).padding(3.dp).size(30.dp).background(Color.Black.copy(alpha = 0.55f), CircleShape),
                    ) { Icon(Icons.Default.FavoriteBorder, contentDescription = "Favoritar", tint = Pink, modifier = Modifier.size(17.dp)) }
                }
                Spacer(Modifier.height(6.dp))
                Text(manga.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 15.sp)
            }
        }
    }
}

@Composable
private fun LoadingBlock(message: String) {
    Column(Modifier.fillMaxWidth().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = Pink, strokeWidth = 3.dp)
        Spacer(Modifier.height(12.dp))
        Text(message.ifBlank { "Carregando..." }, color = Muted, fontSize = 13.sp)
    }
}

@Composable
private fun EmptyBlock(title: String, description: String, action: (() -> Unit)?) {
    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.MenuBook, contentDescription = null, tint = Pink, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, fontWeight = FontWeight.Bold)
        Text(description, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        if (action != null) {
            Spacer(Modifier.height(13.dp))
            OutlinedButton(onClick = action) { Text("Tentar novamente") }
        }
    }
}
