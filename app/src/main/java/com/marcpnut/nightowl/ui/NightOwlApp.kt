package com.marcpnut.nightowl.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.marcpnut.nightowl.R
import com.marcpnut.nightowl.data.BrowseCategory
import com.marcpnut.nightowl.data.HomeData
import com.marcpnut.nightowl.data.MediaItem
import com.marcpnut.nightowl.data.NightOwlApi
import com.marcpnut.nightowl.data.NightOwlApiException
import com.marcpnut.nightowl.data.NightOwlSession
import com.marcpnut.nightowl.data.ProfileInfo
import com.marcpnut.nightowl.data.SeasonInfo
import com.marcpnut.nightowl.data.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import android.util.LruCache

private val NightBlack = Color(0xFF080A0F)
private val NightCard = Color(0xFF171A21)
private val NightCyan = Color(0xFF22D6F5)
private val NightPurple = Color(0xFFAC50FF)

private enum class AppTab(val label: String) {
    HOME("Home"), WHATS_NEW("What's New"), SHOWS("Shows"), MOVIES("Movies"), LIVE("Live TV"), MY_LIST("My List"), SEARCH("Search"), SETTINGS("Settings")
}

private data class PlayerRequest(val item: MediaItem, val url: String, val nextEpisode: MediaItem?)

@Composable
fun NightOwlApp(sessionStore: SessionStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var session by remember { mutableStateOf(sessionStore.load()) }
    val api = remember(session) { session?.let(::NightOwlApi) }
    var profile by remember { mutableStateOf<ProfileInfo?>(null) }
    var activeTab by remember { mutableStateOf(AppTab.HOME) }
    var home by remember { mutableStateOf<HomeData?>(null) }
    var categories by remember { mutableStateOf<List<BrowseCategory>>(emptyList()) }
    var categoryId by remember { mutableStateOf("") }
    var catalogItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var catalogPage by remember { mutableStateOf(1) }
    var catalogHasMore by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<MediaItem?>(null) }
    var detail by remember { mutableStateOf<MediaItem?>(null) }
    var seasons by remember { mutableStateOf<List<SeasonInfo>>(emptyList()) }
    var selectedSeason by remember { mutableStateOf<Int?>(null) }
    var episodes by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var favoriteKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var playerRequest by remember { mutableStateOf<PlayerRequest?>(null) }
    var loading by remember { mutableStateOf(false) }
    var signInError by remember { mutableStateOf("") }
    var screenError by remember { mutableStateOf("") }

    LaunchedEffect(api) {
        if (api == null) { profile = null; home = null; return@LaunchedEffect }
        loading = true
        try {
            api.health()
            val availableProfiles = api.profiles()
            val chosen = availableProfiles.firstOrNull() ?: api.createProfile("Default")
            profile = chosen
            favoriteKeys = api.myList(chosen.id).map { favoriteKey(it) }.toSet()
            signInError = ""
        } catch (error: Exception) {
            sessionStore.clear()
            session = null
            signInError = error.message ?: "Could not connect to NightOwl."
        } finally { loading = false }
    }

    LaunchedEffect(api, profile?.id, activeTab, categoryId, searchText) {
        val currentApi = api ?: return@LaunchedEffect
        val currentProfile = profile ?: return@LaunchedEffect
        if (activeTab == AppTab.SEARCH) {
            delay(320)
            if (searchText.trim().length < 2) { catalogItems = emptyList(); loading = false; return@LaunchedEffect }
        }
        if (activeTab == AppTab.SETTINGS) return@LaunchedEffect
        loading = true
        catalogItems = emptyList()
        catalogPage = 1
        catalogHasMore = false
        screenError = ""
        try {
            when (activeTab) {
                AppTab.HOME -> home = currentApi.home(currentProfile.id)
                AppTab.WHATS_NEW -> home = currentApi.whatsNew()
                AppTab.SHOWS -> {
                    categories = currentApi.categories("series")
                    val page = currentApi.browse("series", categoryId = categoryId)
                    catalogItems = page.first; catalogHasMore = page.second
                }
                AppTab.MOVIES -> {
                    categories = currentApi.categories("movies")
                    val page = currentApi.browse("movies", categoryId = categoryId)
                    catalogItems = page.first; catalogHasMore = page.second
                }
                AppTab.LIVE -> {
                    categories = currentApi.categories("live")
                    val page = currentApi.browse("live", categoryId = categoryId)
                    catalogItems = page.first; catalogHasMore = page.second
                }
                AppTab.MY_LIST -> catalogItems = currentApi.myList(currentProfile.id)
                AppTab.SEARCH -> {
                    val page = currentApi.search(searchText.trim())
                    catalogItems = page.first; catalogHasMore = page.second
                }
                AppTab.SETTINGS -> Unit
            }
        } catch (error: Exception) {
            screenError = error.message ?: "Could not load this page."
            if (error is NightOwlApiException && error.statusCode == 401) {
                sessionStore.clear(); session = null; signInError = "Your sign-in expired. Please sign in again."
            }
        } finally { loading = false }
    }

    LaunchedEffect(api, selectedItem?.id, selectedItem?.kind) {
        val source = selectedItem ?: run { detail = null; seasons = emptyList(); episodes = emptyList(); return@LaunchedEffect }
        val currentApi = api ?: return@LaunchedEffect
        detail = null; seasons = emptyList(); episodes = emptyList(); selectedSeason = null
        try {
            val loaded = currentApi.detail(source)
            detail = loaded
            if (loaded.kind == "series") {
                seasons = currentApi.seasons(loaded.id)
                selectedSeason = seasons.firstOrNull()?.number
            }
        } catch (error: Exception) { screenError = error.message ?: "Could not load details." }
    }

    LaunchedEffect(api, detail?.id, selectedSeason) {
        val currentApi = api ?: return@LaunchedEffect
        val series = detail?.takeIf { it.kind == "series" } ?: return@LaunchedEffect
        val season = selectedSeason ?: return@LaunchedEffect
        try {
            val loaded = mutableListOf<MediaItem>()
            for (page in 1..100) {
                val result = currentApi.episodes(series.id, season, page)
                loaded.addAll(result.items)
                if (!result.hasMore) break
            }
            episodes = loaded
        } catch (error: Exception) { screenError = error.message ?: "Could not load episodes." }
    }

    fun openTab(tab: AppTab) {
        selectedItem = null
        activeTab = tab
        categoryId = ""
        screenError = ""
    }

    fun startPlayback(item: MediaItem) {
        val currentApi = api ?: return
        scope.launch {
            loading = true
            screenError = ""
            try {
                val url = currentApi.resolve(item)
                val next = if (item.kind == "episode") currentApi.followingEpisode(item) else null
                playerRequest = PlayerRequest(item, url, next)
            } catch (error: Exception) { screenError = error.message ?: "Could not start playback." }
            finally { loading = false }
        }
    }

    fun updateFavorite(item: MediaItem) {
        val currentApi = api ?: return
        val currentProfile = profile ?: return
        val key = favoriteKey(item)
        val save = key !in favoriteKeys
        favoriteKeys = if (save) favoriteKeys + key else favoriteKeys - key
        scope.launch {
            try { currentApi.setFavorite(currentProfile.id, item, save) }
            catch (error: Exception) {
                favoriteKeys = if (save) favoriteKeys - key else favoriteKeys + key
                screenError = error.message ?: "Could not update My List."
            }
        }
    }

    fun loadMore() {
        val currentApi = api ?: return
        if (loadingMore || !catalogHasMore || activeTab !in listOf(AppTab.SHOWS, AppTab.MOVIES, AppTab.LIVE, AppTab.SEARCH)) return
        val nextPage = catalogPage + 1
        val currentTab = activeTab
        val currentCategory = categoryId
        val currentQuery = searchText.trim()
        loadingMore = true
        scope.launch {
            try {
                val page = when (currentTab) {
                    AppTab.SHOWS -> currentApi.browse("series", nextPage, currentCategory)
                    AppTab.MOVIES -> currentApi.browse("movies", nextPage, currentCategory)
                    AppTab.LIVE -> currentApi.browse("live", nextPage, currentCategory)
                    AppTab.SEARCH -> currentApi.search(currentQuery, nextPage)
                    else -> emptyList<MediaItem>() to false
                }
                if (activeTab == currentTab && categoryId == currentCategory && (currentTab != AppTab.SEARCH || searchText.trim() == currentQuery)) {
                    catalogItems = catalogItems + page.first
                    catalogPage = nextPage
                    catalogHasMore = page.second
                }
            } catch (error: Exception) { screenError = error.message ?: "Could not load more results." }
            finally { loadingMore = false }
        }
    }

    fun signIn(serverUrl: String, username: String, password: String) {
        scope.launch {
            loading = true
            signInError = ""
            try {
                val signedIn = NightOwlApi.login(serverUrl, username, password, sessionStore.deviceId())
                sessionStore.save(signedIn)
                session = signedIn
                activeTab = AppTab.HOME
            } catch (error: Exception) { signInError = error.message ?: "Sign-in failed." }
            finally { loading = false }
        }
    }

    fun signOut() {
        val currentApi = api
        scope.launch { try { currentApi?.requestLogout() } catch (_: Exception) { } }
        sessionStore.clear()
        session = null
        profile = null
        playerRequest = null
        selectedItem = null
        activeTab = AppTab.HOME
    }

    val request = playerRequest
    if (request != null && api != null && profile != null) {
        PlayerScreen(
            request = request,
            profileId = profile!!.id,
            api = api,
            onBack = { item, position, duration ->
                scope.launch { try { api.progress(profile!!.id, item, position, duration, false) } catch (_: Exception) { } }
                playerRequest = null
            },
            onAdvance = { current, next, position, duration ->
                scope.launch {
                    try { api.progress(profile!!.id, current, position, duration, true) } catch (_: Exception) { }
                    try { playerRequest = PlayerRequest(next, api.resolve(next), api.followingEpisode(next)) }
                    catch (error: Exception) { screenError = error.message ?: "Could not start the next episode."; playerRequest = null }
                }
            },
            onFinished = { item, position, duration ->
                scope.launch { try { api.progress(profile!!.id, item, position, duration, true) } catch (_: Exception) { } }
                playerRequest = null
            }
        )
        return
    }

    if (session == null) {
        SignInScreen(loading = loading, error = signInError, onSignIn = ::signIn)
        return
    }

    if (profile == null) {
        Box(Modifier.fillMaxSize().background(NightBlack), contentAlignment = Alignment.Center) {
            if (loading) CircularProgressIndicator(color = NightCyan)
            else Text(signInError.ifBlank { "Connecting to NightOwl…" }, color = Color.White)
        }
        return
    }

    val detailItem = selectedItem
    Scaffold(
        containerColor = NightBlack,
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            AppHeader(
                onSearch = { openTab(AppTab.SEARCH) },
                onSettings = { openTab(AppTab.SETTINGS) },
                onMyList = { openTab(AppTab.MY_LIST) },
                showBack = detailItem != null,
                onBack = { selectedItem = null }
            )
        },
        bottomBar = {
            if (detailItem == null && activeTab != AppTab.SETTINGS) {
                BottomNavigation(activeTab = activeTab, onSelect = ::openTab)
            }
        }
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            if (screenError.isNotBlank()) ErrorBanner(screenError, onDismiss = { screenError = "" })
            if (detailItem != null) {
                val loadedDetail = detail
                if (loadedDetail == null) LoadingScreen("Loading details…")
                else DetailScreen(
                    item = loadedDetail,
                    seasons = seasons,
                    selectedSeason = selectedSeason,
                    episodes = episodes,
                    isFavorite = favoriteKey(loadedDetail) in favoriteKeys,
                    onSeason = { selectedSeason = it; episodes = emptyList() },
                    onPlay = { startPlayback(loadedDetail) },
                    onFavorite = { updateFavorite(loadedDetail) },
                    onPlayEpisode = ::startPlayback
                )
            } else if (activeTab == AppTab.SETTINGS) {
                SettingsScreen(serverUrl = session?.serverUrl.orEmpty(), onSignOut = ::signOut)
            } else when (activeTab) {
                AppTab.HOME -> HomeScreen(
                    home = home,
                    loading = loading,
                    onOpen = { item -> if (item.kind == "live" || item.kind == "episode") startPlayback(item) else selectedItem = item },
                    onPlay = ::startPlayback
                )
                AppTab.WHATS_NEW -> HomeScreen(
                    home = home,
                    loading = loading,
                    onOpen = { item -> if (item.kind == "live" || item.kind == "episode") startPlayback(item) else selectedItem = item },
                    onPlay = ::startPlayback
                )
                AppTab.SHOWS, AppTab.MOVIES, AppTab.LIVE, AppTab.MY_LIST, AppTab.SEARCH -> BrowseScreen(
                    tab = activeTab,
                    items = catalogItems,
                    categories = categories,
                    selectedCategory = categoryId,
                    query = searchText,
                    loading = loading,
                    hasMore = catalogHasMore,
                    loadingMore = loadingMore,
                    onCategory = { categoryId = it },
                    onQuery = { searchText = it },
                    onLoadMore = ::loadMore,
                    onOpen = { item -> if (item.kind == "live" || item.kind == "episode") startPlayback(item) else selectedItem = item },
                    onFavorite = ::updateFavorite,
                    favoriteKeys = favoriteKeys
                )
                AppTab.SETTINGS -> Unit
            }
        }
    }
}

private fun favoriteKey(item: MediaItem) = "${item.kind}:${item.id}"

@Composable
private fun AppHeader(onSearch: () -> Unit, onSettings: () -> Unit, onMyList: () -> Unit, showBack: Boolean, onBack: () -> Unit) {
    val compact = LocalConfiguration.current.screenWidthDp < 380
    val actionSize = if (compact) 44.dp else 48.dp
    Row(
        modifier = Modifier.fillMaxWidth().height(62.dp).background(Color(0xFF0C0F15)).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBack) {
            IconButton(onClick = onBack, modifier = Modifier.size(actionSize)) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
        } else {
            Image(painterResource(R.drawable.nightowl_logo), "NightOwl", Modifier.width(if (compact) 106.dp else 142.dp).height(38.dp), contentScale = ContentScale.Fit)
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onSearch, modifier = Modifier.size(actionSize)) { Icon(Icons.Default.Search, "Search", tint = Color.White) }
        IconButton(onClick = onMyList, modifier = Modifier.size(actionSize)) { Icon(Icons.Default.Bookmarks, "My List", tint = Color.White) }
        IconButton(onClick = onSettings, modifier = Modifier.size(actionSize)) { Icon(Icons.Default.Settings, "Settings", tint = Color.White) }
    }
}

@Composable
private fun BottomNavigation(activeTab: AppTab, onSelect: (AppTab) -> Unit) {
    NavigationBar(containerColor = Color(0xFF0E1118), tonalElevation = 0.dp, windowInsets = WindowInsets.navigationBars) {
        val tabs = listOf(
            Triple(AppTab.HOME, Icons.Default.Home, "Home"),
            Triple(AppTab.WHATS_NEW, Icons.Default.NewReleases, "What's New"),
            Triple(AppTab.SHOWS, Icons.Default.Tv, "Shows"),
            Triple(AppTab.MOVIES, Icons.Default.Movie, "Movies"),
            Triple(AppTab.LIVE, Icons.Default.LiveTv, "Live TV")
        )
        tabs.forEach { (tab, icon, label) ->
            NavigationBarItem(
                selected = activeTab == tab,
                onClick = { onSelect(tab) },
                icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp)) },
                label = { Text(label, fontSize = 11.sp, maxLines = 1, softWrap = false) },
                alwaysShowLabel = true
            )
        }
    }
}

@Composable
private fun SignInScreen(loading: Boolean, error: String, onSignIn: (String, String, String) -> Unit) {
    var server by remember { mutableStateOf("https://[redacted]") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().background(NightBlack), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).clip(RoundedCornerShape(22.dp))
                .background(NightCard).padding(horizontal = 22.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Image(painterResource(R.drawable.nightowl_logo), "NightOwl", Modifier.fillMaxWidth().height(58.dp), contentScale = ContentScale.Fit)
            Text("Sign in", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Connect to your NightOwl library.", color = Color(0xFFBFC6D2), fontSize = 15.sp)
            OutlinedTextField(server, { server = it }, Modifier.fillMaxWidth(), label = { Text("NightOwl server") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next))
            OutlinedTextField(username, { username = it }, Modifier.fillMaxWidth(), label = { Text("Username") }, singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
            OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done))
            if (error.isNotBlank()) Text(error, color = Color(0xFFFF8A8A), fontSize = 14.sp)
            Button(
                onClick = { onSignIn(server, username, password); password = "" },
                enabled = !loading && username.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NightCyan, contentColor = Color(0xFF071016)),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color(0xFF071016), strokeWidth = 2.dp)
                else Text("Sign in", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text("Your password is used only to sign in. The session token is stored encrypted on this device.", color = Color(0xFF9CA5B3), fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun HomeScreen(home: HomeData?, loading: Boolean, onOpen: (MediaItem) -> Unit, onPlay: (MediaItem) -> Unit) {
    if (home == null && loading) { LoadingScreen("Loading your library…"); return }
    if (home == null) { EmptyScreen("Your library could not be loaded."); return }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        home.featured?.let { featured ->
            item(key = "featured") {
                FeaturedCard(featured, onOpen = { onOpen(featured) }, onPlay = { onPlay(featured) })
            }
        }
        items(home.rows.filter { it.items.isNotEmpty() }, key = { it.id }) { shelf ->
            ShelfRow(shelf = shelf, onOpen = onOpen)
        }
        if (home.featured == null && home.rows.isEmpty()) item { EmptyScreen("Your library is empty.") }
    }
}

@Composable
private fun FeaturedCard(item: MediaItem, onOpen: () -> Unit, onPlay: () -> Unit) {
    // Keep the hero compact on a phone so the first shelf is reachable without a long scroll.
    val heroHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.4f).coerceIn(280.dp, 380.dp)
    Box(Modifier.fillMaxWidth().height(heroHeight).padding(bottom = 2.dp)) {
        RemoteArtwork(item.backdropUrl.ifBlank { item.posterUrl }, "", Modifier.fillMaxSize(), ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x33101218), Color(0xEE080A0F)), startY = 80f)))
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text("FEATURED", color = NightCyan, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text(item.title, color = Color.White, fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(mediaMeta(item), color = Color(0xFFE5E7ED), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (item.description.isNotBlank()) Text(item.description, color = Color(0xFFD1D4DC), fontSize = 14.sp, lineHeight = 20.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onPlay, modifier = Modifier.heightIn(min = 48.dp), colors = ButtonDefaults.buttonColors(containerColor = NightCyan, contentColor = Color(0xFF071016))) {
                    Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("Play", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onOpen, modifier = Modifier.heightIn(min = 48.dp)) { Text("More info", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun ShelfRow(shelf: com.marcpnut.nightowl.data.Shelf, onOpen: (MediaItem) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(shelf.title, Modifier.padding(horizontal = 18.dp), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(shelf.items, key = { "${it.kind}:${it.id}" }) { item ->
                MediaCard(item, Modifier.width(220.dp), onClick = { onOpen(item) }, compact = false)
            }
        }
    }
}

@Composable
private fun BrowseScreen(
    tab: AppTab,
    items: List<MediaItem>,
    categories: List<BrowseCategory>,
    selectedCategory: String,
    query: String,
    loading: Boolean,
    hasMore: Boolean,
    loadingMore: Boolean,
    onCategory: (String) -> Unit,
    onQuery: (String) -> Unit,
    onLoadMore: () -> Unit,
    onOpen: (MediaItem) -> Unit,
    onFavorite: (MediaItem) -> Unit,
    favoriteKeys: Set<String>
) {
    Column(Modifier.fillMaxSize()) {
        when (tab) {
            AppTab.SEARCH -> OutlinedTextField(
                value = query, onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                label = { Text("Search movies and shows") }, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
            AppTab.MY_LIST -> Text("My List", Modifier.padding(horizontal = 18.dp, vertical = 12.dp), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            else -> {
                Text(tab.label, Modifier.padding(horizontal = 18.dp, vertical = 10.dp), color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
                if (categories.isNotEmpty()) {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                        item { FilterChip(selected = selectedCategory.isBlank(), onClick = { onCategory("") }, label = { Text("All categories", maxLines = 1) }) }
                        items(categories, key = { it.id }) { category ->
                            FilterChip(selected = selectedCategory == category.id, onClick = { onCategory(category.id) }, label = { Text(category.name, maxLines = 1) })
                        }
                    }
                }
            }
        }
        when {
            loading && items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = NightCyan) }
            tab == AppTab.SEARCH && query.trim().length < 2 -> EmptyScreen("Type at least two characters to search.")
            items.isEmpty() -> EmptyScreen(if (tab == AppTab.MY_LIST) "Your list is empty." else "No results found.")
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(items, key = { "${it.kind}:${it.id}" }) { item ->
                    MediaCard(item, Modifier.fillMaxWidth(), onClick = { onOpen(item) }, compact = true,
                        fitArtwork = item.kind == "live", isFavorite = favoriteKey(item) in favoriteKeys,
                        onFavorite = if (item.kind == "movie" || item.kind == "series") ({ onFavorite(item) }) else null)
                }
                if (hasMore) item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Button(
                        onClick = onLoadMore,
                        enabled = !loadingMore,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp).heightIn(min = 52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NightCard, contentColor = Color.White)
                    ) {
                        if (loadingMore) CircularProgressIndicator(Modifier.size(20.dp), color = NightCyan, strokeWidth = 2.dp)
                        else Text("Load more", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaCard(
    item: MediaItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    compact: Boolean,
    fitArtwork: Boolean = false,
    isFavorite: Boolean = false,
    onFavorite: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier.clip(shape).background(NightCard).clickable(onClick = onClick)
    ) {
        Box(Modifier.fillMaxWidth().height(if (compact) 116.dp else 126.dp)) {
            RemoteArtwork(item.backdropUrl.ifBlank { item.posterUrl }, item.title, Modifier.fillMaxSize(), if (fitArtwork) ContentScale.Fit else ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC05070A)))))
            if (onFavorite != null) {
                IconButton(onClick = onFavorite, modifier = Modifier.align(Alignment.TopEnd).size(44.dp)) {
                    Icon(if (isFavorite) Icons.Default.Check else Icons.Default.Bookmarks, if (isFavorite) "In My List" else "Add to My List", tint = if (isFavorite) NightCyan else Color.White)
                }
            }
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp)) {
                val displayTitle = if (item.kind == "episode" && item.seriesTitle.isNotBlank()) item.seriesTitle else item.title
                Text(displayTitle, color = Color.White, fontSize = if (compact) 14.sp else 15.sp, lineHeight = 17.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (item.kind == "episode") Text(item.title, color = Color(0xFFE0E3E8), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(mediaMeta(item), Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = Color(0xFFB8BFCA), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DetailScreen(
    item: MediaItem,
    seasons: List<SeasonInfo>,
    selectedSeason: Int?,
    episodes: List<MediaItem>,
    isFavorite: Boolean,
    onSeason: (Int) -> Unit,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onPlayEpisode: (MediaItem) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 26.dp)) {
        item(key = "detail-art") {
            Box(Modifier.fillMaxWidth().height(250.dp)) {
                RemoteArtwork(item.backdropUrl.ifBlank { item.posterUrl }, "", Modifier.fillMaxSize(), ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x22000000), NightBlack))))
            }
        }
        item(key = "detail-copy") {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (item.kind == "series") "SERIES" else "FILM", color = NightCyan, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text(item.title, color = Color.White, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black)
                    Text(mediaMeta(item), color = Color(0xFFCFD3DC), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                if (item.posterUrl.isNotBlank()) {
                    RemoteArtwork(item.posterUrl, "${item.title} poster", Modifier.width(92.dp).height(138.dp).clip(RoundedCornerShape(8.dp)), ContentScale.Crop)
                }
            }
            if (item.description.isNotBlank()) Text(item.description, Modifier.padding(horizontal = 18.dp, vertical = 14.dp), color = Color(0xFFE2E5EA), fontSize = 15.sp, lineHeight = 22.sp)
            Row(Modifier.padding(horizontal = 16.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (item.kind == "movie") Button(onClick = onPlay, modifier = Modifier.heightIn(min = 48.dp), colors = ButtonDefaults.buttonColors(containerColor = NightCyan, contentColor = Color(0xFF071016))) {
                    Icon(Icons.Default.PlayArrow, null); Text("Play", fontWeight = FontWeight.Bold)
                }
                Button(onClick = onFavorite, modifier = Modifier.heightIn(min = 48.dp), colors = ButtonDefaults.buttonColors(containerColor = NightCard)) {
                    Icon(if (isFavorite) Icons.Default.Check else Icons.Default.Bookmarks, null); Spacer(Modifier.width(5.dp)); Text(if (isFavorite) "In My List" else "My List")
                }
            }
        }
        if (item.kind == "series" && seasons.isNotEmpty()) {
            item(key = "season-label") { Text("Seasons", Modifier.padding(start = 18.dp, top = 22.dp, bottom = 8.dp), color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold) }
            item(key = "season-list") {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(seasons, key = { it.number }) { season ->
                        FilterChip(selected = selectedSeason == season.number, onClick = { onSeason(season.number) }, label = { Text("Season ${season.number}") })
                    }
                }
            }
            if (episodes.isEmpty()) item(key = "episodes-loading") { Text("Loading episodes…", Modifier.padding(18.dp), color = Color(0xFFB8BFCA)) }
            items(episodes, key = { "episode-${it.id}" }) { episode ->
                Row(
                    Modifier.fillMaxWidth().clickable { onPlayEpisode(episode) }.padding(horizontal = 18.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RemoteArtwork(episode.posterUrl.ifBlank { item.backdropUrl }, "", Modifier.width(112.dp).height(70.dp).clip(RoundedCornerShape(7.dp)), ContentScale.Crop)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${episode.episodeNumber ?: ""}. ${episode.title}", color = Color.White, fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (episode.description.isNotBlank()) Text(episode.description, color = Color(0xFFB8BFCA), fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Default.PlayArrow, "Play episode", tint = NightCyan)
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(serverUrl: String, onSignOut: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text("NightOwl server", color = Color(0xFFAEB6C3), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(serverUrl, color = Color.White, fontSize = 16.sp)
        Text("Your sign-in session is encrypted on this device.", color = Color(0xFFAEB6C3), fontSize = 14.sp)
        Button(onClick = onSignOut, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF292D36))) {
            Text("Log out", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LoadingScreen(message: String) {
    Box(Modifier.fillMaxSize().background(NightBlack), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CircularProgressIndicator(color = NightCyan)
            Text(message, color = Color(0xFFC4CAD4), fontSize = 14.sp)
        }
    }
}

@Composable
private fun EmptyScreen(message: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Text(message, color = Color(0xFFB8BFCA), fontSize = 15.sp) }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFF402329)).clickable(onClick = onDismiss).padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(message, Modifier.weight(1f), color = Color(0xFFFFD5D8), fontSize = 13.sp, lineHeight = 17.sp)
        Text("Dismiss", color = NightCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun RemoteArtwork(url: String, description: String, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = url) {
        value = if (url.isBlank()) null else withContext(Dispatchers.IO) { ArtworkCache.load(url)?.asImageBitmap() }
    }
    if (bitmap != null) Image(bitmap!!, contentDescription = description, modifier = modifier, contentScale = contentScale)
    else Box(modifier.background(Brush.linearGradient(listOf(Color(0xFF282E3A), Color(0xFF10131A)))))
}

private object ArtworkCache {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun load(address: String): Bitmap? {
        cache.get(address)?.let { return it }
        return try {
            val connection = (URL(address).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000; readTimeout = 12_000; instanceFollowRedirects = true
            }
            try {
                if (connection.responseCode !in 200..299) return null
                connection.inputStream.use { BitmapFactory.decodeStream(it) }?.also { cache.put(address, it) }
            } finally { connection.disconnect() }
        } catch (_: Exception) { null }
    }
}

private fun mediaMeta(item: MediaItem): String = listOfNotNull(
    item.releaseYear?.toString(), item.genre.takeIf { it.isNotBlank() }
).joinToString(" · ").ifBlank { if (item.kind == "live") "Live TV" else item.kind.replaceFirstChar { it.uppercase() } }

private fun errorText(error: Throwable): String = when (error) {
    is NightOwlApiException -> error.message ?: "NightOwl request failed."
    else -> error.message ?: "Could not connect to NightOwl. Check your connection and try again."
}

@Composable
private fun PlayerScreen(
    request: PlayerRequest,
    profileId: String,
    api: NightOwlApi,
    onBack: (MediaItem, Long, Long?) -> Unit,
    onAdvance: (MediaItem, MediaItem, Long, Long?) -> Unit,
    onFinished: (MediaItem, Long, Long?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val player = remember(request.url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(request.url))
            playWhenReady = true
            prepare()
        }
    }
    val playbackPlayer = player
    var positionMs by remember(request.url) { mutableStateOf(0L) }
    var durationMs by remember(request.url) { mutableStateOf(0L) }
    var dismissedNext by remember(request.url) { mutableStateOf(false) }
    var advanced by remember(request.url) { mutableStateOf(false) }
    var showControls by remember(request.url) { mutableStateOf(true) }

    BackHandler { onBack(request.item, positionMs / 1000, durationMs.takeIf { it > 0 }?.div(1000)) }

    LaunchedEffect(player, request.url) {
        var lastSaveAt = 0L
        while (isActive) {
            positionMs = player.currentPosition.coerceAtLeast(0)
            durationMs = player.duration.takeIf { it > 0 } ?: 0
            val now = System.currentTimeMillis()
            if (now - lastSaveAt >= 15_000 && positionMs > 0) {
                lastSaveAt = now
                try { api.progress(profileId, request.item, positionMs / 1000, durationMs.takeIf { it > 0 }?.div(1000), false) } catch (_: Exception) { }
            }
            delay(500)
        }
    }

    DisposableEffect(player) { onDispose { player.release() } }

    LaunchedEffect(player, request.url) {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && !advanced) {
                    advanced = true
                    val next = if (!dismissedNext) request.nextEpisode else null
                    if (next != null) onAdvance(request.item, next, player.currentPosition.coerceAtLeast(0) / 1000, player.duration.takeIf { it > 0 }?.div(1000))
                    else onFinished(request.item, player.currentPosition.coerceAtLeast(0) / 1000, player.duration.takeIf { it > 0 }?.div(1000))
                }
            }
        })
    }

    val remainingMs = (durationMs - positionMs).coerceAtLeast(0)
    val showNext = request.item.kind == "episode" && request.nextEpisode != null && !dismissedNext && !advanced && durationMs > 0 && remainingMs <= 10_000 && remainingMs > 0
    val nextProgress = ((10_000 - remainingMs).coerceIn(0, 10_000).toFloat() / 10_000f)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { viewContext -> PlayerView(viewContext).apply {
                player = playbackPlayer
                useController = true
                controllerShowTimeoutMs = 4_000
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setOnClickListener { showControls = !showControls }
            } },
            modifier = Modifier.fillMaxSize(),
            update = { it.player = playbackPlayer }
        )
        if (showControls) {
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp).shadow(4.dp, RoundedCornerShape(24.dp)),
                color = Color(0x99090B10), shape = RoundedCornerShape(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 14.dp)) {
                    IconButton(onClick = { onBack(request.item, positionMs / 1000, durationMs.takeIf { it > 0 }?.div(1000)) }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                    Column {
                        Text(request.item.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (request.item.kind == "episode") Text(request.item.seriesTitle, color = Color(0xFFD0D4DC), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (showNext) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 78.dp),
                shape = RoundedCornerShape(16.dp), color = Color(0xF0141720), tonalElevation = 8.dp
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Next episode", color = Color(0xFFCDD2DB), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(request.nextEpisode.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    LinearProgressIndicator(progress = { nextProgress }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(5.dp)), color = NightCyan, trackColor = Color(0xFF444A57))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            if (!advanced) { advanced = true; onAdvance(request.item, request.nextEpisode, positionMs / 1000, durationMs.takeIf { it > 0 }?.div(1000)) }
                        }, modifier = Modifier.weight(1f).heightIn(min = 46.dp), colors = ButtonDefaults.buttonColors(containerColor = NightCyan, contentColor = Color(0xFF071016))) {
                            Text("Play now", fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { dismissedNext = true; showControls = true }) {
                            Text("Cancel", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
