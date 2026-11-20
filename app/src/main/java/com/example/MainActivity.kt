package com.example

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.OfflineDownloadManager
import com.example.model.DarkThemeMode
import com.example.model.DownloadStatus
import com.example.model.HomeMoodChip
import com.example.model.NavBarStyle
import com.example.model.SearchFilterOption
import com.example.model.Track
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.NowPlayingSheet
import com.example.ui.components.TrackActionsSheet
import com.example.ui.components.TrackDetailsDialog
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.ArtistScreen
import com.example.ui.screens.AudioEffectsScreen
import com.example.ui.screens.BatteryPerformanceScreen
import com.example.ui.screens.LookAndFeelScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.StorageScreen
import com.example.ui.theme.RazorTuneTheme
import com.example.ui.viewmodel.AccountViewModel
import com.example.ui.viewmodel.ArtistDetailViewModel
import com.example.ui.viewmodel.AudioEffectsViewModel
import com.example.ui.viewmodel.PerformanceViewModel
import com.example.ui.viewmodel.PlaylistDetailViewModel
import com.example.ui.viewmodel.PlaylistsUiState
import com.example.ui.viewmodel.PlaylistsViewModel
import com.example.ui.viewmodel.SearchViewModel
import com.example.ui.viewmodel.StatsViewModel
import com.example.ui.viewmodel.StorageViewModel
import com.example.ui.viewmodel.ThemeViewModel
import com.example.ui.viewmodel.TrackActionsViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import com.example.model.SearchBarStyle

@Serializable
data object SearchRoute

@Serializable
data object PlaylistsRoute

@Serializable
data object SettingsRoute

@Serializable
data object LookAndFeelRoute

@Serializable
data object AccountRoute

@Serializable
data object StorageRoute

@Serializable
data object StatsRoute

@Serializable
data object BatteryPerformanceRoute

@Serializable
data object AudioEffectsRoute

@Serializable
data object AboutRoute

@Serializable
data class PlaylistDetailRoute(
    val playlistId: String,
    val title: String,
)

@Serializable
data class ArtistRoute(
    val artistId: String,
    val name: String,
)

private data class TopLevelTab(
    val route: Any,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val themeSettings by themeViewModel.settings.collectAsStateWithLifecycle()
            val artworkColors by themeViewModel.artworkColors.collectAsStateWithLifecycle()

            val isDark = when (themeSettings.darkThemeMode) {
                DarkThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
                DarkThemeMode.ON -> true
                DarkThemeMode.OFF -> false
            }

            DisposableEffect(isDark) {
                val barStyle = if (isDark) {
                    SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                } else {
                    SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                }
                enableEdgeToEdge(
                    statusBarStyle = barStyle,
                    navigationBarStyle = barStyle,
                )
                onDispose {}
            }

            RazorTuneTheme(
                settings = themeSettings,
                artworkColors = artworkColors,
            ) {
                RazorTuneApp(themeViewModel = themeViewModel)
            }
        }
    }
}

@Composable
fun RazorTuneApp(
    themeViewModel: ThemeViewModel = viewModel(),
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val playbackController = RazorTuneApplication.instance.playbackController
    val coroutineScope = rememberCoroutineScope()

    val searchViewModel: SearchViewModel = viewModel()
    val playlistsViewModel: PlaylistsViewModel = viewModel()
    val accountViewModel: AccountViewModel = viewModel()
    val trackActionsViewModel: TrackActionsViewModel = viewModel()
    val storageViewModel: StorageViewModel = viewModel()
    val performanceViewModel: PerformanceViewModel = viewModel()
    val audioEffectsViewModel: AudioEffectsViewModel = viewModel()

    val playbackState by playbackController.playbackState.collectAsStateWithLifecycle()
    val sleepTimerState by playbackController.sleepTimerState.collectAsStateWithLifecycle()
    val audioSessionId by playbackController.audioSessionId.collectAsStateWithLifecycle()
    val visualizerMagnitudes by playbackController.audioVisualizerManager.magnitudes.collectAsStateWithLifecycle()
    val accountState by accountViewModel.uiState.collectAsStateWithLifecycle()
    val playlistsState by playlistsViewModel.uiState.collectAsStateWithLifecycle()
    val themeSettings by themeViewModel.settings.collectAsStateWithLifecycle()
    val artworkColors by themeViewModel.artworkColors.collectAsStateWithLifecycle()
    val performanceSettings by performanceViewModel.settings.collectAsStateWithLifecycle()
    val audioEffectsSettings by audioEffectsViewModel.settings.collectAsStateWithLifecycle()

    // In-App Update Checker state
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    LaunchedEffect(Unit) {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val currentVersion = pInfo.versionName ?: "1.0.0"
        updateInfo = GitHubUpdateChecker.checkForUpdate(currentVersion)
    }

    LaunchedEffect(themeSettings.customDnsEnabled) {
        RazorTuneApplication.instance.innerTubeRepository.setCustomDnsEnabled(themeSettings.customDnsEnabled)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val powerManager = remember(context) {
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    }
    var isSystemPowerSaveMode by remember {
        mutableStateOf(powerManager?.isPowerSaveMode ?: false)
    }
    var isUnrestrictedBattery by remember {
        mutableStateOf(powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false)
    }
    DisposableEffect(lifecycleOwner, context, powerManager) {
        fun refreshPowerFlags() {
            isSystemPowerSaveMode = powerManager?.isPowerSaveMode ?: false
            isUnrestrictedBattery =
                powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshPowerFlags()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                refreshPowerFlags()
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runCatching { context.unregisterReceiver(receiver) }
        }
    }
    val reduceVisualEffects = performanceSettings.reduceVisualEffects ||
            (performanceSettings.autoReduceOnLowBattery && isSystemPowerSaveMode)

    val downloadStates by trackActionsViewModel.downloadStates.collectAsStateWithLifecycle()
    val downloadedTracks by storageViewModel.downloadedTracks.collectAsStateWithLifecycle()
    val totalDownloadedBytes by storageViewModel.totalDownloadedBytes.collectAsStateWithLifecycle()
    val cacheSizeBytes by storageViewModel.cacheSizeBytes.collectAsStateWithLifecycle()
    val downloadOverWifiOnly by storageViewModel.downloadOverWifiOnly.collectAsStateWithLifecycle()
    val likedVideoIds by trackActionsViewModel.likedVideoIds.collectAsStateWithLifecycle()
    val lyricsState by trackActionsViewModel.lyricsState.collectAsStateWithLifecycle()
    val streamDetailsState by trackActionsViewModel.streamDetailsState.collectAsStateWithLifecycle()

    var showNowPlayingSheet by rememberSaveable { mutableStateOf(false) }
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    var notificationRequested by rememberSaveable { mutableStateOf(false) }
    var audioPermissionRequested by rememberSaveable { mutableStateOf(false) }

    var menuTrack by remember { mutableStateOf<Track?>(null) }
    var playlistPickerTrack by remember { mutableStateOf<Track?>(null) }
    var detailsTrack by remember { mutableStateOf<Track?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { _ -> }

    fun showFeedback(message: String) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    fun ensureNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationRequested) {
            notificationRequested = true
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun playTracks(tracks: List<Track>, startIndex: Int) {
        ensureNotificationPermissionIfNeeded()
        playbackController.playQueue(tracks, startIndex)
    }

    fun playSingleWithQueue(track: Track, queue: List<Track>) {
        ensureNotificationPermissionIfNeeded()
        playbackController.playTrack(track, queue)
    }

    fun shareTrack(track: Track) {
        val shareText = "${track.title} — ${track.artist}\nhttps://music.youtube.com/watch?v=${track.videoId}"
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${track.title} — ${track.artist}")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooser = Intent.createChooser(sendIntent, "Share track")
        runCatching { context.startActivity(chooser) }
            .onFailure { showFeedback("Unable to open share sheet") }
    }

    LaunchedEffect(playbackState.errorMessage) {
        val error = playbackState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error)
        playbackController.clearError()
    }

    LaunchedEffect(accountState.statusMessage) {
        val message = accountState.statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        accountViewModel.clearStatusMessage()
    }

    LaunchedEffect(showLyrics, playbackState.currentTrack?.videoId) {
        val currentId = playbackState.currentTrack?.videoId
        if (showLyrics && !currentId.isNullOrBlank()) {
            trackActionsViewModel.loadLyricsForTrack(currentId)
        }
    }

    val tabs = remember {
        listOf(
            TopLevelTab(
                route = SearchRoute,
                labelRes = R.string.nav_home,
                selectedIcon = Icons.Rounded.Home,
                unselectedIcon = Icons.Outlined.Home,
                testTag = "nav_tab_home",
            ),
            TopLevelTab(
                route = PlaylistsRoute,
                labelRes = R.string.nav_playlists,
                selectedIcon = Icons.AutoMirrored.Rounded.QueueMusic,
                unselectedIcon = Icons.AutoMirrored.Outlined.QueueMusic,
                testTag = "nav_tab_playlists",
            ),
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isInSettingsFlow = currentDestination?.hasRoute<SettingsRoute>() == true ||
            currentDestination?.hasRoute<LookAndFeelRoute>() == true ||
            currentDestination?.hasRoute<AccountRoute>() == true ||
            currentDestination?.hasRoute<StorageRoute>() == true ||
            currentDestination?.hasRoute<StatsRoute>() == true ||
            currentDestination?.hasRoute<BatteryPerformanceRoute>() == true ||
            currentDestination?.hasRoute<AudioEffectsRoute>() == true ||
            currentDestination?.hasRoute<AboutRoute>() == true

    fun navigateToTab(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val liquidGlass = themeSettings.liquidGlass
    val glassT = themeSettings.glassIntensity.coerceIn(0f, 1f)

    Box(modifier = Modifier.fillMaxSize()) {
        val useRail = false

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!useRail) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isInSettingsFlow) {
                                    Modifier.navigationBarsPadding()
                                } else {
                                    Modifier
                                },
                            ),
                    ) {
                        MiniPlayerBar(
                            playbackState = playbackState,
                            liquidGlass = liquidGlass,
                            glassIntensity = glassT,
                            playerDesignStyle = themeSettings.playerDesignStyle,
                            miniPlayerStyle = themeSettings.miniPlayerStyle,
                            cornerRadiusDp = themeSettings.miniPlayerCornerRadiusDp,
                            thumbnailCornerRadius = themeSettings.thumbnailCornerRadius,
                            showLike = themeSettings.miniPlayerShowLike,
                            showPrevious = themeSettings.miniPlayerShowPrevious,
                            swipeToSkip = themeSettings.miniPlayerSwipeToSkip,
                            isLiked = playbackState.currentTrack?.videoId in likedVideoIds,
                            onExpand = { showNowPlayingSheet = true },
                            onTogglePlayPause = { playbackController.togglePlayPause() },
                            onSkipNext = { playbackController.skipToNext() },
                            onSkipPrevious = { playbackController.skipToPrevious() },
                            onToggleLike = {
                                playbackState.currentTrack?.let { trackActionsViewModel.toggleLike(it, ::showFeedback) }
                            },
                        )

                        if (!isInSettingsFlow) {
                            RazorTuneBottomNavBar(
                                navBarStyle = themeSettings.navBarStyle,
                                liquidGlass = liquidGlass,
                                glassIntensity = glassT,
                                tabs = tabs,
                                currentDestination = currentDestination,
                                onSelectTab = ::navigateToTab,
                                onOpenQuickStats = { navController.navigate(StatsRoute) },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                if (useRail && !isInSettingsFlow) {
                    NavigationRail(
                        containerColor = if (liquidGlass) {
                            MaterialTheme.colorScheme.surfaceContainerLow.copy(
                                alpha = lerp(0.95f, 0.44f, glassT),
                            )
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        tabs.forEach { tab ->
                            val selected = when (tab.route) {
                                is SearchRoute -> currentDestination?.hasRoute<SearchRoute>() == true
                                is PlaylistsRoute ->
                                    currentDestination?.hasRoute<PlaylistsRoute>() == true ||
                                            currentDestination?.hasRoute<PlaylistDetailRoute>() == true
                                else -> false
                            }
                            val label = stringResource(tab.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label,
                                    )
                                },
                                label = { Text(text = label) },
                                modifier = Modifier.testTag(tab.testTag),
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        NavHost(
                            navController = navController,
                            startDestination = SearchRoute,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            composable<SearchRoute> {
                                val query by searchViewModel.query.collectAsStateWithLifecycle()
                                val selectedFilter by searchViewModel.selectedFilter.collectAsStateWithLifecycle()
                                val uiState by searchViewModel.uiState.collectAsStateWithLifecycle()
                                val homeState by searchViewModel.homeState.collectAsStateWithLifecycle()
                                val moodChips by searchViewModel.moodChips.collectAsStateWithLifecycle()
                                val selectedMood by searchViewModel.selectedMood.collectAsStateWithLifecycle()
                                val isLoadingMood by searchViewModel.isLoadingMood.collectAsStateWithLifecycle()

                                SearchScreen(
                                    query = query,
                                    selectedFilter = selectedFilter,
                                    uiState = uiState,
                                    homeState = homeState,
                                    currentTrackId = playbackState.currentTrack?.videoId,
                                    thumbnailCornerRadius = themeSettings.thumbnailCornerRadius,
                                    contentLayoutStyle = themeSettings.contentLayoutStyle,
                                    searchBarStyle = themeSettings.searchBarStyle,
                                    downloadStates = downloadStates,
                                    onQueryChange = { q: String -> searchViewModel.onQueryChange(q) },
                                    onSelectFilter = { f: SearchFilterOption -> searchViewModel.selectFilter(f) },
                                    onSubmitSearch = { searchViewModel.submitSearch() },
                                    onRefreshHome = { searchViewModel.refreshHome() },
                                    onOpenStats = { navController.navigate(StatsRoute) },
                                    onOpenSettings = { navController.navigate(SettingsRoute) },
                                    onPlayTrack = { track: Track, queue: List<Track> -> playSingleWithQueue(track, queue) },
                                    onOpenPlaylistOrAlbum = { id: String, title: String ->
                                        navController.navigate(PlaylistDetailRoute(id, title))
                                    },
                                    onOpenArtist = { artistId: String, artistName: String ->
                                        navController.navigate(ArtistRoute(artistId, artistName))
                                    },
                                    onLoadMore = { searchViewModel.loadMoreResults() },
                                    moodChips = moodChips,
                                    selectedMood = selectedMood,
                                    isLoadingMood = isLoadingMood,
                                    onSelectMoodChip = { chip: HomeMoodChip -> searchViewModel.selectMoodChip(chip) },
                                    onOpenTrackMenu = { track: Track -> menuTrack = track },
                                )
                            }

                            composable<PlaylistsRoute> {
                                PlaylistsScreen(
                                    uiState = playlistsState,
                                    downloadedTracks = downloadedTracks,
                                    downloadStates = downloadStates,
                                    currentTrackId = playbackState.currentTrack?.videoId,
                                    thumbnailCornerRadius = themeSettings.thumbnailCornerRadius,
                                    contentLayoutStyle = themeSettings.contentLayoutStyle,
                                    onRefresh = { playlistsViewModel.refresh() },
                                    onNavigateToAccount = { navController.navigate(AccountRoute) },
                                    onOpenPlaylist = { playlist ->
                                        navController.navigate(
                                            PlaylistDetailRoute(playlist.id, playlist.title),
                                        )
                                    },
                                    onPlayTracks = { tracks, idx -> playTracks(tracks, idx) },
                                    onOpenTrackMenu = { track -> menuTrack = track },
                                )
                            }

                            composable<PlaylistDetailRoute> { backStackEntry ->
                                val route = backStackEntry.toRoute<PlaylistDetailRoute>()
                                val detailViewModel: PlaylistDetailViewModel = viewModel(
                                    viewModelStoreOwner = backStackEntry,
                                )
                                val detailState by detailViewModel.uiState.collectAsStateWithLifecycle()

                                LaunchedEffect(route.playlistId) {
                                    detailViewModel.load(route.playlistId, route.title)
                                }

                                PlaylistDetailScreen(
                                    fallbackTitle = route.title,
                                    uiState = detailState,
                                    currentTrackId = playbackState.currentTrack?.videoId,
                                    thumbnailCornerRadius = themeSettings.thumbnailCornerRadius,
                                    contentLayoutStyle = themeSettings.contentLayoutStyle,
                                    downloadStates = downloadStates,
                                    onBack = { navController.popBackStack() },
                                    onRetry = {
                                        detailViewModel.reload(route.playlistId, route.title)
                                    },
                                    onPlayTracks = { tracks, idx -> playTracks(tracks, idx) },
                                    onDownloadAll = { tracks ->
                                        trackActionsViewModel.downloadAll(tracks, ::showFeedback)
                                    },
                                    onOpenTrackMenu = { track -> menuTrack = track },
                                )
                            }

                            composable<ArtistRoute> { backStackEntry ->
                                val route = backStackEntry.toRoute<ArtistRoute>()
                                val artistViewModel: ArtistDetailViewModel = viewModel(
                                    viewModelStoreOwner = backStackEntry,
                                )
                                val artistState by artistViewModel.uiState.collectAsStateWithLifecycle()

                                LaunchedEffect(route.artistId) {
                                    artistViewModel.load(route.artistId)
                                }

                                ArtistScreen(
                                    fallbackName = route.name,
                                    uiState = artistState,
                                    currentTrackId = playbackState.currentTrack?.videoId,
                                    thumbnailCornerRadius = themeSettings.thumbnailCornerRadius,
                                    contentLayoutStyle = themeSettings.contentLayoutStyle,
                                    downloadStates = downloadStates,
                                    onBack = { navController.popBackStack() },
                                    onRetry = { artistViewModel.reload(route.artistId) },
                                    onPlayTracks = { tracks, idx -> playTracks(tracks, idx) },
                                    onOpenAlbum = { albumBrowseId, albumTitle ->
                                        navController.navigate(PlaylistDetailRoute(albumBrowseId, albumTitle))
                                    },
                                    onOpenTrackMenu = { track -> menuTrack = track },
                                )
                            }

                            composable<SettingsRoute> {
                                val songWord = if (downloadedTracks.size == 1) "1 song" else "${downloadedTracks.size} songs"
                                val storageSummary = "${OfflineDownloadManager.formatBytes(totalDownloadedBytes)} offline • $songWord"
                                val batteryStatusText = if (isUnrestrictedBattery) {
                                    "Unrestricted battery"
                                } else {
                                    "Battery optimization active"
                                }
                                val batteryPerformanceSummary = "${performanceSettings.audioQuality.shortLabel} quality • $batteryStatusText"
                                val audioEffectsSummary = if (audioEffectsSettings.enabled) {
                                    buildString {
                                        append(audioEffectsSettings.preset.label)
                                        if (audioEffectsSettings.loudnessGainMb > 0) {
                                            append(" • +${String.format(java.util.Locale.US, "%.1f", audioEffectsSettings.loudnessGainMb / 100f)} dB")
                                        }
                                        if (audioEffectsSettings.bassBoostStrength > 0) {
                                            append(" • Bass ${audioEffectsSettings.bassBoostStrength / 10}%")
                                        }
                                    }
                                } else {
                                    "Off"
                                }
                                SettingsScreen(
                                    accountState = accountState,
                                    themeSettings = themeSettings,
                                    storageSummary = storageSummary,
                                    batteryPerformanceSummary = batteryPerformanceSummary,
                                    audioEffectsSummary = audioEffectsSummary,
                                    onBack = { navController.popBackStack() },
                                    onOpenAccountAndSync = { navController.navigate(AccountRoute) },
                                    onOpenLookAndFeel = { navController.navigate(LookAndFeelRoute) },
                                    onToggleCustomDns = { enabled: Boolean ->
                                        themeViewModel.setCustomDnsEnabled(enabled)
                                        showFeedback(if (enabled) "Cloudflare DoH Enabled" else "Cloudflare DoH Disabled")
                                    },
                                    onOpenStorage = { navController.navigate(StorageRoute) },
                                    onOpenStats = { navController.navigate(StatsRoute) },
                                    onOpenBatteryPerformance = { navController.navigate(BatteryPerformanceRoute) },
                                    onOpenAudioEffects = { navController.navigate(AudioEffectsRoute) },
                                    onOpenAbout = { navController.navigate(AboutRoute) },
                                )
                            }

                            composable<StatsRoute> { backStackEntry ->
                                val statsViewModel: StatsViewModel = viewModel(
                                    viewModelStoreOwner = backStackEntry,
                                )
                                val selectedRange by statsViewModel.selectedRange.collectAsStateWithLifecycle()
                                val statsUiState by statsViewModel.uiState.collectAsStateWithLifecycle()

                                StatsScreen(
                                    selectedRange = selectedRange,
                                    uiState = statsUiState,
                                    thumbnailCornerRadius = themeSettings.thumbnailCornerRadius,
                                    onBack = { navController.popBackStack() },
                                    onSelectRange = { range -> statsViewModel.selectRange(range) },
                                    onClearStats = {
                                        statsViewModel.clearStats()
                                        showFeedback("Cleared listening history")
                                    },
                                    onOpenArtist = { track ->
                                        trackActionsViewModel.resolveArtist(
                                            track = track,
                                            onResolved = { artistId, artistName ->
                                                navController.navigate(ArtistRoute(artistId, artistName))
                                            },
                                            onUnavailable = ::showFeedback,
                                        )
                                    },
                                    onPlayTrack = { track: Track, queue: List<Track> -> playSingleWithQueue(track, queue) },
                                    onShuffleTopTracks = { tracks ->
                                        val shuffled = tracks.shuffled()
                                        if (shuffled.isNotEmpty()) {
                                            playTracks(shuffled, 0)
                                        }
                                    },
                                    onExploreMusic = {
                                        navController.popBackStack(SearchRoute, inclusive = false)
                                    },
                                )
                            }

                            composable<LookAndFeelRoute> {
                                LookAndFeelScreen(
                                    settings = themeSettings,
                                    onBack = { navController.popBackStack() },
                                    onSetDynamicArtworkTheme = { b -> themeViewModel.setDynamicArtworkTheme(b) },
                                    onSetDarkThemeMode = { mode -> themeViewModel.setDarkThemeMode(mode) },
                                    onSetPureBlack = { b -> themeViewModel.setPureBlack(b) },
                                    onSetPlayerBackgroundStyle = { style -> themeViewModel.setPlayerBackgroundStyle(style) },
                                    onSetLiquidGlass = { b -> themeViewModel.setLiquidGlass(b) },
                                    onSetGlassIntensity = { intensity -> themeViewModel.setGlassIntensity(intensity) },
                                    onSetPlayerButtonsLayout = { layout -> themeViewModel.setPlayerButtonsLayout(layout) },
                                    onSetNowPlayingStyle = { style -> themeViewModel.setNowPlayingStyle(style) },
                                    onSetPlayerDesignStyle = { style -> themeViewModel.setPlayerDesignStyle(style) },
                                    onSetPlayerSliderStyle = { style -> themeViewModel.setPlayerSliderStyle(style) },
                                    onSetMiniPlayerStyle = { style -> themeViewModel.setMiniPlayerStyle(style) },
                                    onSetNavBarStyle = { style -> themeViewModel.setNavBarStyle(style) },
                                    onSetContentLayoutStyle = { style -> themeViewModel.setContentLayoutStyle(style) },
                                    onSetMiniPlayerCornerRadiusDp = { r -> themeViewModel.setMiniPlayerCornerRadiusDp(r) },
                                    onSetMiniPlayerShowLike = { b -> themeViewModel.setMiniPlayerShowLike(b) },
                                    onSetMiniPlayerShowPrevious = { b -> themeViewModel.setMiniPlayerShowPrevious(b) },
                                    onSetMiniPlayerSwipeToSkip = { b -> themeViewModel.setMiniPlayerSwipeToSkip(b) },
                                    onSetThumbnailCornerRadius = { r -> themeViewModel.setThumbnailCornerRadius(r) },
                                    onSetShowSpectralVisualizer = { b -> themeViewModel.setShowSpectralVisualizer(b) },
                                    onSetVisualizerStyle = { style -> themeViewModel.setVisualizerStyle(style) },
                                    onSetSearchBarStyle = { style -> themeViewModel.setSearchBarStyle(style) },
                                )
                            }

                            composable<AccountRoute> {
                                AccountScreen(
                                    uiState = accountState,
                                    isSyncingPlaylists = playlistsState is PlaylistsUiState.Loading,
                                    onBack = { navController.popBackStack() },
                                    onCookieCaptured = { cookie -> accountViewModel.onCookieCaptured(cookie) },
                                    onSyncPlaylistsNow = {
                                        playlistsViewModel.refresh()
                                        showFeedback("Syncing YouTube Music playlists…")
                                    },
                                    onSignOut = accountViewModel::signOut,
                                    onBrowsePlaylists = { navigateToTab(PlaylistsRoute) },
                                )
                            }

                            composable<StorageRoute> {
                                StorageScreen(
                                    downloadedTracks = downloadedTracks,
                                    totalDownloadedBytes = totalDownloadedBytes,
                                    cacheSizeBytes = cacheSizeBytes,
                                    downloadOverWifiOnly = downloadOverWifiOnly,
                                    onBack = { navController.popBackStack() },
                                    onRefreshStats = { storageViewModel.refreshStorageStats() },
                                    onSetDownloadOverWifiOnly = { b -> storageViewModel.setDownloadOverWifiOnly(b) },
                                    onRemoveTrack = { videoId ->
                                        storageViewModel.removeDownloadedTrack(videoId)
                                        showFeedback("Removed offline song")
                                    },
                                    onDeleteAllDownloads = {
                                        storageViewModel.deleteAllDownloads {
                                            showFeedback("Deleted all offline downloads")
                                        }
                                    },
                                    onClearCache = {
                                        storageViewModel.clearTemporaryCache { freedBytes ->
                                            showFeedback("Cleared ${OfflineDownloadManager.formatBytes(freedBytes)} of temporary cache")
                                        }
                                    },
                                )
                            }

                            composable<BatteryPerformanceRoute> {
                                BatteryPerformanceScreen(
                                    settings = performanceSettings,
                                    onBack = { navController.popBackStack() },
                                    onSetAudioQuality = { q -> performanceViewModel.setAudioQuality(q) },
                                    onSetSkipSilence = { b -> performanceViewModel.setSkipSilence(b) },
                                    onSetPreloadNextTrack = { b -> performanceViewModel.setPreloadNextTrack(b) },
                                    onSetReduceVisualEffects = { b -> performanceViewModel.setReduceVisualEffects(b) },
                                    onSetAutoReduceOnLowBattery = { b -> performanceViewModel.setAutoReduceOnLowBattery(b) },
                                )
                            }

                            composable<AudioEffectsRoute> {
                                AudioEffectsScreen(
                                    settings = audioEffectsSettings,
                                    audioSessionId = audioSessionId,
                                    onBack = { navController.popBackStack() },
                                    onSetEnabled = { b -> audioEffectsViewModel.setEnabled(b) },
                                    onSelectPreset = { p -> audioEffectsViewModel.selectPreset(p) },
                                    onSetBandLevel = { band, level -> audioEffectsViewModel.setBandLevel(band, level) },
                                    onSetBassBoost = { strength -> audioEffectsViewModel.setBassBoost(strength) },
                                    onSetVirtualizer = { strength -> audioEffectsViewModel.setVirtualizer(strength) },
                                    onSetLoudnessGain = { gain -> audioEffectsViewModel.setLoudnessGain(gain) },
                                    onResetAll = {
                                        audioEffectsViewModel.resetAll()
                                        showFeedback("Reset Audio Effects & EQ to Flat")
                                    },
                                )
                            }

                            composable<AboutRoute> {
                                AboutScreen(
                                    onBack = { navController.popBackStack() },
                                    onOpenLicenses = {
                                        showFeedback("Open source libraries & licenses")
                                    },
                                    onOpenGitHub = {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            "https://github.com".toUri(),
                                        )
                                        runCatching { context.startActivity(intent) }
                                    },
                                )
                            }
                        }
                    }

                    if (useRail) {
                        MiniPlayerBar(
                            playbackState = playbackState,
                            liquidGlass = liquidGlass,
                            glassIntensity = glassT,
                            playerDesignStyle = themeSettings.playerDesignStyle,
                            miniPlayerStyle = themeSettings.miniPlayerStyle,
                            cornerRadiusDp = themeSettings.miniPlayerCornerRadiusDp,
                            thumbnailCornerRadius = themeSettings.thumbnailCornerRadius,
                            showLike = themeSettings.miniPlayerShowLike,
                            showPrevious = themeSettings.miniPlayerShowPrevious,
                            swipeToSkip = themeSettings.miniPlayerSwipeToSkip,
                            isLiked = playbackState.currentTrack?.videoId in likedVideoIds,
                            onExpand = { showNowPlayingSheet = true },
                            onTogglePlayPause = { playbackController.togglePlayPause() },
                            onSkipNext = { playbackController.skipToNext() },
                            onSkipPrevious = { playbackController.skipToPrevious() },
                            onToggleLike = {
                                playbackState.currentTrack?.let { trackActionsViewModel.toggleLike(it, ::showFeedback) }
                            },
                            modifier = Modifier.navigationBarsPadding(),
                        )
                    }
                }
            }
        }
    }

    // In-App Update Dialog UI
    updateInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { updateInfo = null },
            title = { Text("Update Available (${info.version})") },
            text = { Text(info.releaseNotes) },
            confirmButton = {
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))
                    context.startActivity(intent)
                }) {
                    Text("Download Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { updateInfo = null }) {
                    Text("Later")
                }
            }
        )
    }

    val currentTrack = playbackState.currentTrack
    if (showNowPlayingSheet && currentTrack != null) {
        NowPlayingSheet(
            playbackState = playbackState,
            themeSettings = themeSettings,
            artworkColors = artworkColors,
            isLiked = currentTrack.videoId in likedVideoIds,
            showLyrics = showLyrics,
            lyricsState = lyricsState,
            sleepTimerState = sleepTimerState,
            downloadStates = downloadStates,
            reduceVisualEffects = reduceVisualEffects,
            visualizerMagnitudes = visualizerMagnitudes,
            hasPromptedAudioPermission = audioPermissionRequested,
            onMarkAudioPermissionPrompted = { audioPermissionRequested = true },
            onVisualizerActiveChange = { active -> playbackController.audioVisualizerManager.setActive(active) },
            onDismiss = { showNowPlayingSheet = false },
            onTogglePlayPause = { playbackController.togglePlayPause() },
            onSkipNext = { playbackController.skipToNext() },
            onSkipPrevious = { playbackController.skipToPrevious() },
            onSeekTo = { pos -> playbackController.seekTo(pos) },
            onSelectQueueIndex = { idx -> playbackController.skipToQueueIndex(idx) },
            onToggleLike = { trackActionsViewModel.toggleLike(currentTrack, ::showFeedback) },
            onShare = { shareTrack(currentTrack) },
            onToggleLyrics = { showLyrics = !showLyrics },
            onStartSleepTimerMinutes = { minutes ->
                playbackController.startSleepTimerMinutes(minutes)
                showFeedback("Sleep timer set for $minutes minutes")
            },
            onStartSleepTimerEndOfTrack = {
                playbackController.startSleepTimerEndOfTrack()
                showFeedback("Sleep timer set for end of track")
            },
            onCancelSleepTimer = {
                playbackController.cancelSleepTimer()
                showFeedback("Sleep timer turned off")
            },
            onOpenMoreMenu = { menuTrack = currentTrack },
            onOpenQueueTrackMenu = { track -> menuTrack = track },
        )
    }

    menuTrack?.let { selectedTrack ->
        val downloadStatus = downloadStates[selectedTrack.videoId] ?: DownloadStatus.NOT_DOWNLOADED
        TrackActionsSheet(
            track = selectedTrack,
            downloadStatus = downloadStatus,
            isLiked = selectedTrack.videoId in likedVideoIds,
            thumbnailCornerRadius = themeSettings.thumbnailCornerRadius,
            onDismiss = { menuTrack = null },
            onAddToQueue = {
                playbackController.addToQueue(selectedTrack)
                showFeedback("Added \"${selectedTrack.title}\" to queue")
            },
            onAddToPlaylist = {
                playlistPickerTrack = selectedTrack
            },
            onDownload = {
                trackActionsViewModel.downloadOrRemove(selectedTrack, ::showFeedback)
            },
            onAddToLibrary = {
                trackActionsViewModel.addToLibrary(selectedTrack, ::showFeedback)
            },
            onViewArtist = {
                trackActionsViewModel.resolveArtist(
                    track = selectedTrack,
                    onResolved = { artistId, artistName ->
                        showNowPlayingSheet = false
                        navController.navigate(ArtistRoute(artistId, artistName))
                    },
                    onUnavailable = ::showFeedback,
                )
            },
            onViewAlbum = {
                trackActionsViewModel.resolveAlbum(
                    track = selectedTrack,
                    onResolved = { albumBrowseId, albumTitle ->
                        showNowPlayingSheet = false
                        navController.navigate(PlaylistDetailRoute(albumBrowseId, albumTitle))
                    },
                    onUnavailable = ::showFeedback,
                )
            },
            onShare = {
                shareTrack(selectedTrack)
            },
            onToggleLyrics = {
                val activeId = playbackState.currentTrack?.videoId
                if (activeId == selectedTrack.videoId) {
                    showLyrics = !showLyrics
                } else {
                    playSingleWithQueue(selectedTrack, listOf(selectedTrack))
                    showLyrics = true
                }
                showNowPlayingSheet = true
            },
            onDetails = {
                trackActionsViewModel.loadStreamDetails(selectedTrack)
                detailsTrack = selectedTrack
            },
        )
    }

    playlistPickerTrack?.let { targetTrack ->
        AddToPlaylistDialog(
            track = targetTrack,
            playlistsState = playlistsState,
            onSelectPlaylist = { playlist ->
                trackActionsViewModel.addToPlaylist(playlist, targetTrack, ::showFeedback)
            },
            onOpenAccountSync = {
                showNowPlayingSheet = false
                navController.navigate(AccountRoute)
            },
            onDismiss = { playlistPickerTrack = null },
        )
    }

    detailsTrack?.let { targetTrack ->
        TrackDetailsDialog(
            track = targetTrack,
            downloadStatus = downloadStates[targetTrack.videoId] ?: DownloadStatus.NOT_DOWNLOADED,
            streamDetailsState = streamDetailsState,
            onDismiss = { detailsTrack = null },
        )
    }
}

private fun isTabSelected(
    tab: TopLevelTab,
    currentDestination: androidx.navigation.NavDestination?,
): Boolean = when (tab.route) {
    is SearchRoute -> currentDestination?.hasRoute<SearchRoute>() == true
    is PlaylistsRoute ->
        currentDestination?.hasRoute<PlaylistsRoute>() == true ||
                currentDestination?.hasRoute<PlaylistDetailRoute>() == true
    else -> false
}

@Composable
private fun RazorTuneBottomNavBar(
    navBarStyle: NavBarStyle,
    liquidGlass: Boolean,
    glassIntensity: Float,
    tabs: List<TopLevelTab>,
    currentDestination: androidx.navigation.NavDestination?,
    onSelectTab: (Any) -> Unit,
    onOpenQuickStats: () -> Unit,
) {
    val glassT = glassIntensity.coerceIn(0f, 1f)
    val glassBorder = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = lerp(0.08f, 0.56f, glassT)),
            Color.White.copy(alpha = lerp(0.02f, 0.16f, glassT)),
        ),
    )

    when (navBarStyle) {
        NavBarStyle.FLOATING_GLASS -> {
            val navShape = RoundedCornerShape(26.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 10.dp),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(navShape)
                        .then(
                            if (liquidGlass) {
                                Modifier
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = lerp(0f, 0.14f, glassT)),
                                                Color.Transparent,
                                            ),
                                        ),
                                    )
                                    .border(width = 1.dp, brush = glassBorder, shape = navShape)
                            } else {
                                Modifier.border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = navShape,
                                )
                            },
                        ),
                    color = if (liquidGlass) {
                        MaterialTheme.colorScheme.surfaceContainerLow.copy(
                            alpha = lerp(0.95f, 0.42f, glassT),
                        )
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    shape = navShape,
                    shadowElevation = if (liquidGlass) (6f + 10f * glassT).dp else 4.dp,
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets(0, 0, 0, 0),
                    ) {
                        tabs.forEach { tab ->
                            val selected = isTabSelected(tab, currentDestination)
                            val label = stringResource(tab.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onSelectTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label,
                                    )
                                },
                                label = { Text(text = label) },
                                modifier = Modifier.testTag(tab.testTag),
                            )
                        }
                    }
                }
            }
        }

        NavBarStyle.ARCHIVE_PILL -> {
            val pillShape = RoundedCornerShape(50.dp)
            val surfaceColor = if (liquidGlass) {
                MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                    alpha = lerp(0.95f, 0.48f, glassT),
                )
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
            val elevation = if (liquidGlass) (6f + 8f * glassT).dp else 4.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = pillShape,
                    color = surfaceColor,
                    shadowElevation = elevation,
                    modifier = Modifier
                        .clip(pillShape)
                        .then(
                            if (liquidGlass) {
                                Modifier.border(width = 1.dp, brush = glassBorder, shape = pillShape)
                            } else {
                                Modifier.border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = pillShape,
                                )
                            },
                        ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        tabs.forEach { tab ->
                            val selected = isTabSelected(tab, currentDestination)
                            val label = stringResource(tab.labelRes)
                            Surface(
                                shape = pillShape,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    Color.Transparent
                                },
                                modifier = Modifier
                                    .animateContentSize()
                                    .heightIn(min = 44.dp)
                                    .clip(pillShape)
                                    .clickable { onSelectTab(tab.route) }
                                    .testTag(tab.testTag),
                            ) {
                                if (selected) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = tab.selectedIcon,
                                            contentDescription = label,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            maxLines = 1,
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier.size(44.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = tab.unselectedIcon,
                                            contentDescription = label,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = surfaceColor,
                    shadowElevation = elevation,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .then(
                            if (liquidGlass) {
                                Modifier.border(width = 1.dp, brush = glassBorder, shape = CircleShape)
                            } else {
                                Modifier.border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape,
                                )
                            },
                        )
                        .clickable(onClick = onOpenQuickStats)
                        .testTag("nav_archive_stats_button"),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.BarChart,
                            contentDescription = "Listening Stats",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        NavBarStyle.COMPACT_PILL -> {
            val pillShape = RoundedCornerShape(50.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 48.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = pillShape,
                    color = if (liquidGlass) {
                        MaterialTheme.colorScheme.surfaceContainerLow.copy(
                            alpha = lerp(0.95f, 0.44f, glassT),
                        )
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    shadowElevation = if (liquidGlass) (6f + 8f * glassT).dp else 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(pillShape)
                        .then(
                            if (liquidGlass) {
                                Modifier.border(width = 1.dp, brush = glassBorder, shape = pillShape)
                            } else {
                                Modifier.border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = pillShape,
                                )
                            },
                        ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        tabs.forEach { tab ->
                            val selected = isTabSelected(tab, currentDestination)
                            val label = stringResource(tab.labelRes)
                            Surface(
                                shape = pillShape,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(pillShape)
                                    .clickable { onSelectTab(tab.route) }
                                    .testTag(tab.testTag),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label,
                                        tint = if (selected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        NavBarStyle.DOCKED -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                NavigationBar(
                    containerColor = if (liquidGlass) {
                        MaterialTheme.colorScheme.surfaceContainerLow.copy(
                            alpha = lerp(0.96f, 0.56f, glassT),
                        )
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    tonalElevation = 0.dp,
                ) {
                    tabs.forEach { tab ->
                        val selected = isTabSelected(tab, currentDestination)
                        val label = stringResource(tab.labelRes)
                        NavigationBarItem(
                            selected = selected,
                            onClick = { onSelectTab(tab.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = label,
                                )
                            },
                            label = { Text(text = label) },
                            modifier = Modifier.testTag(tab.testTag),
                        )
                    }
                }
            }
        }

        NavBarStyle.MINIMAL_ICONS -> {
            val barShape = RoundedCornerShape(28.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 40.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = barShape,
                    color = if (liquidGlass) {
                        MaterialTheme.colorScheme.surfaceContainerLow.copy(
                            alpha = lerp(0.95f, 0.42f, glassT),
                        )
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    shadowElevation = if (liquidGlass) (6f + 8f * glassT).dp else 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(barShape)
                        .then(
                            if (liquidGlass) {
                                Modifier.border(width = 1.dp, brush = glassBorder, shape = barShape)
                            } else {
                                Modifier.border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = barShape,
                                )
                            },
                        ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        tabs.forEach { tab ->
                            val selected = isTabSelected(tab, currentDestination)
                            val label = stringResource(tab.labelRes)
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { onSelectTab(tab.route) }
                                    .padding(horizontal = 20.dp, vertical = 6.dp)
                                    .testTag(tab.testTag),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label,
                                        tint = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (selected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    Color.Transparent
                                                },
                                            ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}