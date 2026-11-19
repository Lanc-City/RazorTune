# RazorTune — Native YouTube Music Client

RazorTune is a native Android YouTube Music client built with Kotlin, Jetpack Compose, Material 3, `innertubex`, and AndroidX Media3. It focuses cleanly on four core workflows: real catalog search, YouTube Music WebView cookie authentication, synced library playlists and liked songs, and full-length background audio playback without mock data.

## User Review & Critical Decisions

> [!IMPORTANT]
> The core visual and interaction decisions below were confirmed during clarification and govern the implementation. All other technical constraints from your initial specification remain intact.

- **Confirmed Decision 1 — Dynamic Material You Dark Theme**: RazorTune uses a dark-first Material 3 theme powered by `dynamicDarkColorScheme` on Android 12+ so accents adapt sharply to the user's wallpaper, paired with a high-contrast Obsidian & Electric Crimson fallback palette on earlier Android versions.
- **Confirmed Decision 2 — Songs-Default Search with Quick Filter Chips**: The Search screen defaults to the Songs filter for immediate playback and provides horizontal filter chips (`Songs`, `Videos`, `Albums`, `Playlists`, `Artists`) to narrow or broaden InnerTube results.
- **Confirmed Decision 3 — Expandable Now Playing Sheet**: A persistent Mini-Player remains docked above the bottom navigation bar whenever a track is active; tapping it expands a full-height Now Playing sheet with full artwork, interactive scrubbing, transport controls, and the active queue.
- **Confirmed Decision 4 — Signed-Out Playlists Prompt**: Before signing in, the Playlists tab displays a focused sign-in card explaining library syncing with a direct action button that switches to the Account tab.

---

## 1. Overview & Core Concept

- **What It Does**: Lets listeners sign in to their YouTube Music account via an embedded WebView, browse their synced playlists and liked songs, search the live YouTube Music catalog, and stream full-length audio tracks continuously in the background with system media notification controls.
- **Target Audience / Persona**: Android listeners who want a fast, distraction-free, native Material 3 YouTube Music player with reliable background playback and zero algorithmic clutter.
- **Key Value**: Combines `innertubex` live catalog/library endpoints and cookie-authenticated requests with an AndroidX Media3 `MediaSessionService` and header-aware `ResolvingDataSource` pipeline to deliver uninterrupted, full-length streaming.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Account Sign-In & Session Persistence**: Opening the **Account** tab when signed out presents an embedded WebView pointed at `https://youtube.com`. Once `CookieManager` detects a cookie header containing `SAPISID=`, RazorTune automatically captures the cookie string, stores it in Jetpack DataStore, assigns `YouTube.cookie`, and transitions the Account screen to a signed-in session card with a Sign Out action. On subsequent launches, the stored cookie is restored into `YouTube.cookie` before network calls execute.
  2. **Search & Instant Playback**: On the **Search** tab (landing destination), the user types a query into a docked search bar, optionally switches quick filter chips (`Songs` selected by default), and browses live results featuring square rounded artwork, title, artist subtitle, and duration. Tapping any song resolves its playable stream and starts background playback while updating the Mini-Player; tapping a playlist result opens its tracklist.
  3. **Library & Playlist Sync**: On the **Playlists** tab, signed-out users see a clean prompt card with a button jumping straight to **Account**. Once signed in, RazorTune fetches the user's YouTube Music library (`YouTube.library(...)`), highlighting Liked Songs and synced playlists. Tapping a playlist navigates to a dedicated Playlist Detail view with a "Play All" action and individual track rows.
  4. **Mini-Player & Now Playing Sheet**: Whenever playback is active or paused with a loaded item, a compact Mini-Player sits directly above the bottom navigation bar showing artwork, scrolling/ellipsized title & artist, play/pause toggle, skip-next button, and a slim progress bar. Tapping the Mini-Player body opens a modal Now Playing sheet featuring large artwork, an interactive seek slider with elapsed/total timestamps, previous/play-pause/next controls, and the current queue.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Precision dark audio studio — sharp typographic hierarchy, deep obsidian surfaces, crisp 1dp hairline dividers, and vivid wallpaper-driven Material You accents.
  - *Color Palette & Mood*:
    - Primary dynamic posture (`dynamicDarkColorScheme` on API 31+) using semantic Material 3 roles (`surface`, `surfaceContainerLow`, `surfaceContainerHigh`, `primary`, `primaryContainer`, `onSurface`, `onSurfaceVariant`).
    - Fallback dark scheme (API < 31): Obsidian base (`#0B0C10` background, `#13151C` surface container, `#1C1F2A` elevated container) paired with a sharp Coral-Crimson primary accent (`#FF4D67`) and cool Slate text hierarchy (`#F4F5F8` / `#9CA3B4`).
  - *Typography & Hierarchy*:
    - Local bundled Google Fonts installed into app resources: **Space Grotesk** (display/headline & track titles for geometric precision), **Plus Jakarta Sans** (body, artist subtitles, chips, and labels), and **JetBrains Mono** (tabular track durations and seek timestamps).
  - *Component Styling & Layout*:
    - Edge-to-edge full-bleed layout respecting `WindowInsets.safeDrawing` and `navigationBars` padding.
    - Adaptive navigation chrome: standard Material 3 `NavigationBar` on compact phones (`Search`, `Playlists`, `Account`) and `NavigationRail` on medium/expanded tablet screens, keeping content constrained to readable widths.
    - Minimum `48dp` touch targets on all transport buttons, chips, and track rows with Material ripple feedback.

- **Interactive Feedback & Motion**:
  - Staggered `AnimatedVisibility` and `spring()` transitions for the Mini-Player entrance and Now Playing sheet expansion.
  - Shimmer/skeleton placeholders or crisp centered progress indicators while catalog queries, playlists, or stream URLs resolve.
  - Non-blocking snackbar error banners with retry actions for network failures, expired cookies, or unplayable tracks.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1 — Lazy Stream Resolution via `ResolvingDataSource` vs. Pre-Resolving Entire Playlists**:
  - *Chosen Approach*: Queue items store lightweight track metadata (`videoId`, title, artist, artwork URI, duration) with a custom `razortune://track/<videoId>` media URI, and a Media3 `ResolvingDataSource.Factory` calls `YouTube.player(videoId)` at the moment ExoPlayer prepares that item.
  - *Why*: YouTube audio stream URLs expire quickly and resolving 50+ tracks upfront would trigger rate limits and slow down playlist clicks. Lazy resolution guarantees fresh URLs, instant queue loading, and seamless skip-next behavior.
  - *Alternatives Considered*: Pre-fetching all stream URLs on playlist open (slow, high failure rate from stale URLs).

- **Decision 2 — Dedicated HTTP Header Configuration for Playback vs. Default ExoPlayer DataSource**:
  - *Chosen Approach*: Configure `DefaultHttpDataSource.Factory` (or OkHttp-backed DataSource) with YouTube Music desktop/Android-compatible headers (`User-Agent`, `Origin: https://music.youtube.com`, `Referer: https://music.youtube.com/`) and cross-protocol redirects enabled, paired with selecting the highest-quality adaptive audio-only format (`audio/mp4` / `audio/webm`) returned by `YouTube.player(videoId)`.
  - *Why*: Prevents HTTP 403 throttling and premature 1-minute stream cutoffs during background playback.
  - *Alternatives Considered*: Default ExoPlayer `DefaultHttpDataSource` without custom headers (frequently throttled or rejected with 403).

- **Decision 3 — Type-Safe Navigation Compose + Single Shared Playback Coordinator**:
  - *Chosen Approach*: Use `@Serializable` Navigation Compose routes (`Search`, `Playlists`, `PlaylistDetail(id, title)`, `Account`) alongside a singleton `PlaybackRepository` connected to `RazorTunePlaybackService` via `MediaController`.
  - *Why*: Keeps UI navigation cleanly separated from the background playback lifecycle so audio keeps running smoothly across tab switches, configuration changes, and app backgrounding.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:

```text
┌───────────────────────────────────────────────────────────────────────────┐
│                         Jetpack Compose UI Layer                          │
│  ┌──────────────┐  ┌─────────────────────┐  ┌──────────────────────────┐  │
│  │  SearchTab   │  │ PlaylistsTab        │  │       AccountTab         │  │
│  │ (Chips+List) │  │  └─ PlaylistDetail  │  │ (WebView + CookieCapture)│  │
│  └──────┬───────┘  └──────────┬──────────┘  └────────────┬─────────────┘  │
│         │                     │                          │                │
│  ┌──────┴─────────────────────┴──────────────────────────┴─────────────┐  │
│  │ Persistent Chrome: NavigationBar / Rail + MiniPlayer + NowPlayingSheet │
│  └────────────────────────────┬──────────────────────────┬─────────────┘  │
└───────────────────────────────┼──────────────────────────┼────────────────┘
                                │ StateFlow / Intents      │ Cookie State
┌───────────────────────────────▼──────────┐  ┌────────────▼────────────────┐
│            ViewModels (MVVM)             │  │   AuthPreferencesStore      │
│ SearchVM │ PlaylistsVM │ PlayerVM │ AccVM│  │   (Jetpack DataStore)       │
└─────┬────┴──────┬──────┴────┬─────┴──┬───┘  └────────────┬────────────────┘
      │           │           │        │                   │
┌─────▼───────────▼───────────┼────────▼───────────────────▼────────────────┐
│                 InnerTubeRepository (YouTube singleton)                   │
│  • Restores YouTube.cookie on startup & updates on WebView SAPISID match  │
│  • YouTube.search(query, filter) / YouTube.library() / YouTube.playlist() │
│  • YouTube.player(videoId) -> selects best adaptive audio stream URL      │
└─────────────────────────────┬────────────────────────────▲────────────────┘
                              │ MediaController            │ Resolves Stream
┌─────────────────────────────▼────────────────────────────┴────────────────┐
│        RazorTunePlaybackService (Media3 MediaSessionService)              │
│  • Foreground notification + MediaSession controls                        │
│  • ExoPlayer + ResolvingDataSource + DefaultHttpDataSource (YT headers)   │
└───────────────────────────────────────────────────────────────────────────┘
```

- **Data Model & State**:
  - `Track`: Immutable domain model (`videoId`, `title`, `artist`, `album`, `durationSeconds`, `thumbnailUrl`).
  - `PlaylistSummary`: Domain model for library items (`id`, `title`, `subtitle`, `songCount`, `thumbnailUrl`, `isLikedSongs`).
  - `SearchFilterOption`: Enum mapping UI chips (`Songs`, `Videos`, `Albums`, `Playlists`, `Artists`) to `YouTube.SearchFilter` values.
  - `PlaybackUiState`: `currentTrack`, `queue`, `currentIndex`, `isPlaying`, `isBuffering`, `positionMs`, `durationMs`, `errorMessage`.
  - `AuthState`: `isSignedIn` (`cookie.contains("SAPISID=")`), `cookiePreview` / `accountStatus`, `isLoading`.

- **Interactive Component & State Mapping**:
  - **Account WebView**: `WebViewClient.onPageFinished` / `shouldInterceptRequest` inspects `CookieManager.getInstance().getCookie("https://youtube.com")` and `https://music.youtube.com`; when `SAPISID=` appears, saves the full cookie string to DataStore, updates `YouTube.cookie`, and triggers a library refresh.
  - **Search Bar & Filter Chips**: Debounced or submit-triggered search invokes `InnerTubeRepository.search`, transitioning `SearchUiState` between `Idle`, `Loading`, `Success(items)`, and `Error(message)`.
  - **Playlist Grid & Detail**: `PlaylistsViewModel` observes `AuthState`; when signed in, loads `YouTube.library` playlists and Liked Songs. Tapping a card navigates to `PlaylistDetailRoute`, fetching `YouTube.playlist(playlistId)`.
  - **Mini-Player & Now Playing Sheet**: Observes `PlaybackRepository.playbackState` plus a 500ms position polling coroutine while `isPlaying` is true. Play/Pause, Next, Previous, and `seekTo(ms)` forward commands directly to the connected Media3 `MediaController`.

- **Key Sequences & Integrations**:
  - **Dependencies & Gradle**: Configured with JitPack (`https://jitpack.io`) filtered for `com.github.MetrolistGroup.innertubex` and `com.github.MetrolistGroup.quackjs`, adding `innertubex:v0.11.7`, AndroidX Media3 (`media3-exoplayer`, `media3-session`), Coil, Jetpack DataStore Preferences, and Navigation Compose.
  - **Manifest & Permissions**: Package renamed to `com.razortune.music`; declares `INTERNET`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS` (requested politely when starting playback on Android 13+), and registers `RazorTunePlaybackService` with `android:foregroundServiceType="mediaPlayback"`.
