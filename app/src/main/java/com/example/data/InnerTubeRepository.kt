package com.example.data

import android.util.LruCache
import com.example.model.AlbumSummary
import com.example.model.ArtistDetailData
import com.example.model.ArtistSummary
import com.example.model.HomeCardItem
import com.example.model.HomeFeedResult
import com.example.model.HomeMoodChip
import com.example.model.HomeShelf
import com.example.model.LyricLine
import com.example.model.LyricsData
import com.example.model.PlaylistSummary
import com.example.model.SearchFilterOption
import com.example.model.SearchPageResult
import com.example.model.SearchResultItem
import com.example.model.Track
import com.example.model.TrackStreamDetails
import com.metrolist.innertubex.InnerTube
import com.metrolist.innertubex.cipher.YouTubeCipherService
import com.metrolist.innertubex.extraction.AudioQuality
import com.metrolist.innertubex.extraction.ContentHints
import com.metrolist.innertubex.extraction.ExtractedStream
import com.metrolist.innertubex.extraction.InnerTubeExtractor
import com.metrolist.innertubex.extraction.YtConfigParserImpl
import com.metrolist.innertubex.models.AccountInfo
import com.metrolist.innertubex.models.YouTubeClient
import com.metrolist.innertubex.models.YouTubeLocale
import com.metrolist.innertubex.models.response.AccountMenuResponse
import com.metrolist.innertubex.models.response.AccountsListResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import java.net.InetAddress
import java.net.URLDecoder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.dnsoverhttps.DnsOverHttps

class InnerTubeRepository(
    initialCustomDnsEnabled: Boolean = false,
) {

    @Volatile
    private var customDnsEnabled: Boolean = initialCustomDnsEnabled

    private val bootstrapClient: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .build()

    private val cloudflareDoh: DnsOverHttps = DnsOverHttps.Builder()
        .client(bootstrapClient)
        .url("https://1.1.1.1/dns-query".toHttpUrl())
        .bootstrapDnsHosts(
            InetAddress.getByName("1.1.1.1"),
            InetAddress.getByName("1.0.0.1"),
        )
        .build()

    private val dynamicDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return if (customDnsEnabled) {
                runCatching { cloudflareDoh.lookup(hostname) }
                    .getOrElse { Dns.SYSTEM.lookup(hostname) }
            } else {
                Dns.SYSTEM.lookup(hostname)
            }
        }
    }

    val okHttpClient: OkHttpClient = bootstrapClient.newBuilder()
        .dns(dynamicDns)
        .build()

    fun setCustomDnsEnabled(enabled: Boolean) {
        customDnsEnabled = enabled
        synchronized(resolvedStreamCache) {
            resolvedStreamCache.evictAll()
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val httpClient: HttpClient = HttpClient(OkHttp) {
        engine {
            preconfigured = okHttpClient
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    val innerTube: InnerTube = InnerTube(httpClient = httpClient).apply {
        val currentLocale = Locale.getDefault()
        locale = YouTubeLocale(
            gl = currentLocale.country.ifBlank { "US" },
            hl = currentLocale.language.ifBlank { "en" },
        )
    }

    private val cipherService = YouTubeCipherService(innerTube.httpClient)
    private val ytConfigParser = YtConfigParserImpl(
        httpClient = innerTube.httpClient,
        innerTube = innerTube,
        cipherService = cipherService,
    )
    private val extractor = InnerTubeExtractor(
        configParser = ytConfigParser,
        cipherService = cipherService,
        innerTube = innerTube,
    )
    private val resolvedStreamCache = LruCache<String, CachedResolvedStream>(40)

    private data class CachedResolvedStream(
        val stream: ExtractedStream,
        val cachedAtMs: Long,
    )

    fun setCookie(cookie: String?) {
        val clean = deduplicateCookieString(cookie)
        innerTube.cookie = clean
        val hasSap = hasValidSapisidCookie(clean)
        innerTube.useLoginForBrowse = hasSap
        if (!hasSap) {
            innerTube.dataSyncId = null
            innerTube.visitorData = null
        } else if (innerTube.authUser.isBlank()) {
            innerTube.authUser = "0"
        }
    }

    private fun deduplicateCookieString(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val uniqueCookies = LinkedHashMap<String, String>()
        raw.split(";").forEach { segment ->
            val trimmed = segment.trim()
            val eqIdx = trimmed.indexOf('=')
            if (eqIdx > 0) {
                val key = trimmed.substring(0, eqIdx).trim()
                val value = trimmed.substring(eqIdx + 1).trim()
                if (key.isNotEmpty() && value.isNotEmpty()) {
                    uniqueCookies[key] = "$key=$value"
                }
            }
        }
        return uniqueCookies.values.joinToString("; ").takeIf { it.isNotBlank() }
    }

    fun isSignedIn(): Boolean = innerTube.hasSapCookieAuth()

    private fun hasValidSapisidCookie(cookie: String? = innerTube.cookie): Boolean {
        if (cookie.isNullOrBlank()) return false
        return innerTube.hasSapCookieAuth() ||
                cookie.contains("SAPISID=") ||
                cookie.contains("__Secure-3PAPISID=")
    }

    suspend fun syncAccountContext(): AccountInfo? = withContext(Dispatchers.IO) {
        innerTube.cookie = deduplicateCookieString(innerTube.cookie)
        if (!hasValidSapisidCookie()) return@withContext null

        if (innerTube.authUser.isBlank()) {
            innerTube.authUser = "0"
        }
        innerTube.useLoginForBrowse = true

        runCatching {
            val freshVisitor = innerTube.fetchFreshVisitorData()
            if (!freshVisitor.isNullOrBlank()) {
                innerTube.visitorData = freshVisitor
            }
        }

        runCatching {
            val accountsResp = innerTube.accountsList(YouTubeClient.WEB_REMIX)
            val accountsBody = accountsResp.bodyAsText()
            val parsedAccounts = json.decodeFromString<AccountsListResponse>(accountsBody)
            val accounts = parsedAccounts.extractAccounts()
            val selected = accounts.firstOrNull { it.isSelected } ?: accounts.firstOrNull()
            val authUserFromUrl = selected?.signinUrl
                ?.let { Regex("""[?&]authuser=(\d+)""").find(it)?.groupValues?.getOrNull(1) }
                ?.takeIf { it.isNotBlank() }
            if (authUserFromUrl != null) {
                innerTube.authUser = authUserFromUrl
            }

            val rawSyncToken = selected?.dataSyncId
                ?: ((json.parseToJsonElement(accountsBody) as? JsonObject)?.findStringByKey("datasyncIdToken"))
            val cleanedSyncId = rawSyncToken
                ?.substringBefore("||")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            innerTube.dataSyncId = cleanedSyncId
        }.onFailure {
            innerTube.dataSyncId = innerTube.dataSyncId?.trim()?.takeIf { id -> id.isNotBlank() }
        }

        fetchAccountInfo()
    }

    suspend fun fetchAccountInfo(): AccountInfo? = withContext(Dispatchers.IO) {
        if (!hasValidSapisidCookie()) return@withContext null
        innerTube.useLoginForBrowse = true
        innerTube.dataSyncId = innerTube.dataSyncId?.trim()?.takeIf { it.isNotBlank() }
        runCatching {
            val response = innerTube.accountMenu(YouTubeClient.WEB_REMIX)
            val body = response.bodyAsText()
            val menu = json.decodeFromString<AccountMenuResponse>(body)
            menu.actions
                .firstOrNull()
                ?.openPopupAction
                ?.popup
                ?.multiPageMenuRenderer
                ?.header
                ?.activeAccountHeaderRenderer
                ?.toAccountInfo()
        }.getOrNull()
    }

    suspend fun search(
        query: String,
        filter: SearchFilterOption,
    ): List<SearchResultItem> = search(
        query = query,
        filter = filter,
        continuationToken = null,
    ).items

    suspend fun search(
        query: String,
        filter: SearchFilterOption,
        continuationToken: String? = null,
    ): SearchPageResult = withContext(Dispatchers.IO) {
        val cleanToken = continuationToken?.trim()?.takeIf { it.isNotEmpty() }
        val trimmed = query.trim()
        if (cleanToken == null && trimmed.isEmpty()) {
            return@withContext SearchPageResult(emptyList(), null)
        }

        val filterParam = if (cleanToken == null) {
            when (filter) {
                SearchFilterOption.SONGS -> "EgWKAQIIAWoKEAkQBRAKEAMQBA=="
                SearchFilterOption.VIDEOS -> "EgWKAQIQAWoKEAkQChAFEAMQBA=="
                SearchFilterOption.ALBUMS -> "EgWKAQIYAWoKEAkQChAFEAMQBA=="
                SearchFilterOption.PLAYLISTS -> "EgWKAQIoAWoKEAkQChAFEAMQBA=="
                SearchFilterOption.ARTISTS -> "EgWKAQIgAWoKEAkQChAFEAMQBA=="
            }
        } else {
            null
        }

        val response = innerTube.search(
            client = YouTubeClient.WEB_REMIX,
            query = if (cleanToken == null) trimmed else null,
            params = filterParam,
            continuation = cleanToken,
            setLogin = innerTube.hasSapCookieAuth(),
        )
        val root = (json.parseToJsonElement(response.bodyAsText()) as? JsonObject) ?: JsonObject(emptyMap())
        val items = parseSearchResults(
            root = root,
            filter = filter,
            isContinuation = cleanToken != null,
        )
        val nextToken = extractContinuationToken(root)?.takeIf { it.isNotBlank() && it != cleanToken }
        SearchPageResult(
            items = items,
            continuationToken = nextToken,
        )
    }

    suspend fun fetchHomeFeed(params: String? = null): HomeFeedResult = withContext(Dispatchers.IO) {
        val signedIn = hasValidSapisidCookie()
        innerTube.dataSyncId = innerTube.dataSyncId?.trim()?.takeIf { it.isNotBlank() }
        if (signedIn) {
            innerTube.useLoginForBrowse = true
            if (innerTube.authUser.isBlank()) {
                innerTube.authUser = "0"
            }
            if (innerTube.visitorData.isNullOrBlank()) {
                runCatching { syncAccountContext() }
            }
        } else if (innerTube.visitorData.isNullOrBlank()) {
            runCatching {
                val freshVisitor = innerTube.fetchFreshVisitorData()
                if (!freshVisitor.isNullOrBlank()) {
                    innerTube.visitorData = freshVisitor
                }
            }
        }

        innerTube.useLoginForBrowse = true

        coroutineScope {
            val homeDeferred = async {
                runCatching {
                    fetchHomeShelvesAndMoodChips(
                        browseId = "FEmusic_home",
                        params = params,
                        signedIn = signedIn,
                    )
                }
            }
            val exploreDeferred = async {
                if (params.isNullOrBlank()) {
                    runCatching {
                        fetchBrowseShelvesWithContinuations(
                            browseId = "FEmusic_explore",
                            signedIn = signedIn,
                            maxContinuations = 1,
                        )
                    }
                } else {
                    Result.success(emptyList())
                }
            }

            val homeResult = homeDeferred.await()
            val exploreResult = exploreDeferred.await()

            val (homeShelves, parsedChips) = homeResult.getOrDefault(Pair(emptyList(), emptyList()))
            val exploreShelves = exploreResult.getOrDefault(emptyList())

            val combined = mutableListOf<HomeShelf>()
            combined += homeShelves
            if (params.isNullOrBlank()) {
                combined += exploreShelves
                val hasNewReleases = combined.any { isNewReleasesShelf(it) }
                if (!hasNewReleases) {
                    val newReleases = runCatching {
                        fetchBrowseShelvesWithContinuations(
                            browseId = "FEmusic_new_releases",
                            signedIn = signedIn,
                            maxContinuations = 0,
                        )
                    }.getOrDefault(emptyList())
                    combined += newReleases
                }
            }

            if (combined.isEmpty() && params.isNullOrBlank()) {
                homeResult.exceptionOrNull()?.let { throw it }
                exploreResult.exceptionOrNull()?.let { throw it }
            }

            val finalShelves = prioritizeAndDedupeShelves(combined)
            val fallbackChips = listOf(
                HomeMoodChip("Energize", "ggMIEAU%3D"),
                HomeMoodChip("Workout", "ggMIEAM%3D"),
                HomeMoodChip("Feel good", "ggMIEAI%3D"),
                HomeMoodChip("Chill", "ggMIEAQ%3D"),
                HomeMoodChip("Party", "ggMIEAY%3D"),
                HomeMoodChip("Focus", "ggMIEAc%3D"),
                HomeMoodChip("Romance", "ggMIEAg%3D"),
            )
            val mergedChips = (parsedChips + fallbackChips).distinctBy { it.title.lowercase(Locale.US) }

            HomeFeedResult(
                shelves = finalShelves,
                moodChips = mergedChips,
            )
        }
    }

    private suspend fun fetchHomeShelvesAndMoodChips(
        browseId: String,
        params: String?,
        signedIn: Boolean,
    ): Pair<List<HomeShelf>, List<HomeMoodChip>> {
        innerTube.useLoginForBrowse = true
        val response = innerTube.browse(
            client = YouTubeClient.WEB_REMIX,
            browseId = browseId,
            params = params,
            continuation = null,
            setLogin = signedIn,
        )
        if (response.status.value !in 200..299) {
            error("Browse $browseId failed with HTTP ${response.status.value}")
        }
        val root = (json.parseToJsonElement(response.bodyAsText()) as? JsonObject) ?: JsonObject(emptyMap())
        val shelves = parseShelvesFromRoot(root, defaultTitle = defaultTitleForBrowseId(browseId))
        val chips = parseMoodChipsFromRoot(root)
        return shelves to chips
    }

    private fun parseMoodChipsFromRoot(root: JsonObject): List<HomeMoodChip> {
        val results = mutableListOf<HomeMoodChip>()
        val chipsObjects = collectObjectsByKey(root, "chipCloudChipRenderer")
        for (chip in chipsObjects) {
            val title = extractRunsText(chip["text"]).ifBlank { continue }
            val endpoint = chip["navigationEndpoint"] as? JsonObject
            val browse = (endpoint?.get("browseEndpoint") as? JsonObject)
                ?: collectObjectsByKey(endpoint ?: JsonObject(emptyMap()), "browseEndpoint").firstOrNull()
            val params = (browse?.get("params") as? JsonPrimitive)?.contentOrNull
                ?: continue
            if (params.isNotBlank()) {
                results += HomeMoodChip(title = title, params = params)
            }
        }
        if (results.isEmpty()) {
            val basicHeaders = collectObjectsByKey(root, "musicCarouselShelfBasicHeaderRenderer")
            for (header in basicHeaders) {
                val subtitleRuns = (header["strapline"] as? JsonObject)?.get("runs") as? JsonArray
                    ?: (header["title"] as? JsonObject)?.get("runs") as? JsonArray
                subtitleRuns?.forEach { run ->
                    val nav = (run as? JsonObject)?.get("navigationEndpoint") as? JsonObject ?: return@forEach
                    val browse = (nav["browseEndpoint"] as? JsonObject)
                        ?: collectObjectsByKey(nav, "browseEndpoint").firstOrNull()
                    val title = extractRunsText((run as? JsonObject)?.get("text"))
                    val params = (browse?.get("params") as? JsonPrimitive)?.contentOrNull
                    if (!title.isNullOrBlank() && !params.isNullOrBlank()) {
                        results += HomeMoodChip(title = title, params = params)
                    }
                }
            }
        }
        return results.distinctBy { it.title.lowercase(Locale.US) }
    }

    private suspend fun fetchBrowseShelvesWithContinuations(
        browseId: String,
        signedIn: Boolean,
        maxContinuations: Int,
    ): List<HomeShelf> {
        innerTube.useLoginForBrowse = true
        val response = innerTube.browse(
            client = YouTubeClient.WEB_REMIX,
            browseId = browseId,
            params = null,
            continuation = null,
            setLogin = signedIn,
        )
        if (response.status.value !in 200..299) {
            error("Browse $browseId failed with HTTP ${response.status.value}")
        }
        val root = (json.parseToJsonElement(response.bodyAsText()) as? JsonObject) ?: JsonObject(emptyMap())
        val shelves = mutableListOf<HomeShelf>()
        shelves += parseShelvesFromRoot(root, defaultTitle = defaultTitleForBrowseId(browseId))

        var continuation = extractContinuationToken(root)
        val seenTokens = mutableSetOf<String>()
        var page = 0
        while (!continuation.isNullOrBlank() && page < maxContinuations && seenTokens.add(continuation)) {
            page++
            innerTube.useLoginForBrowse = true
            val contResponse = innerTube.browse(
                client = YouTubeClient.WEB_REMIX,
                browseId = null,
                params = null,
                continuation = continuation,
                setLogin = signedIn,
            )
            if (contResponse.status.value !in 200..299) break
            val contRoot = (json.parseToJsonElement(contResponse.bodyAsText()) as? JsonObject) ?: break
            val more = parseShelvesFromRoot(contRoot, defaultTitle = "Recommended")
            shelves += more
            continuation = extractContinuationToken(contRoot)
        }
        return shelves
    }

    private fun defaultTitleForBrowseId(browseId: String): String = when (browseId) {
        "FEmusic_new_releases" -> "New releases"
        "FEmusic_explore" -> "Explore"
        else -> "Recommended"
    }

    private fun parseShelvesFromRoot(
        root: JsonObject,
        defaultTitle: String,
    ): List<HomeShelf> {
        val shelves = mutableListOf<HomeShelf>()

        fun parseCarousel(renderer: JsonObject) {
            val headerObj = renderer["header"] as? JsonObject
            val header = (headerObj?.get("musicCarouselShelfBasicHeaderRenderer") as? JsonObject)
                ?: (headerObj?.values?.firstOrNull { it is JsonObject } as? JsonObject)
            val title = extractRunsText(header?.get("title"))
                .ifBlank { extractRunsText(renderer["title"]) }
                .ifBlank { defaultTitle }
            val strapline = extractRunsText(header?.get("strapline")).takeIf { it.isNotBlank() }
            val contents = renderer["contents"] as? JsonArray ?: return
            val items = parseShelfItems(contents)
            if (items.isNotEmpty()) {
                shelves += HomeShelf(
                    id = "carousel_${title}_${strapline.orEmpty()}_${shelves.size}",
                    title = title,
                    strapline = strapline,
                    items = items,
                )
            }
        }

        fun parseMusicShelf(renderer: JsonObject) {
            val headerObj = renderer["header"] as? JsonObject
            val header = headerObj?.values?.firstOrNull { it is JsonObject } as? JsonObject
            val title = extractRunsText(renderer["title"])
                .ifBlank { extractRunsText(header?.get("title")) }
                .ifBlank { defaultTitle }
            val contents = renderer["contents"] as? JsonArray ?: return
            val items = parseShelfItems(contents)
            if (items.isNotEmpty()) {
                shelves += HomeShelf(
                    id = "shelf_${title}_${shelves.size}",
                    title = title,
                    strapline = null,
                    items = items,
                )
            }
        }

        fun parseGrid(renderer: JsonObject) {
            val headerTitle = ((renderer["header"] as? JsonObject)
                ?.get("gridHeaderRenderer") as? JsonObject)
                ?.get("title")
                ?.let { extractRunsText(it) }
                ?.takeIf { it.isNotBlank() }
                ?: defaultTitle
            val itemsArray = renderer["items"] as? JsonArray ?: return
            val items = parseShelfItems(itemsArray)
            if (items.isNotEmpty()) {
                shelves += HomeShelf(
                    id = "grid_${headerTitle}_${shelves.size}",
                    title = headerTitle,
                    strapline = null,
                    items = items,
                )
            }
        }

        val orderedSectionNodes = extractOrderedSectionNodes(root)
        for (node in orderedSectionNodes) {
            (node["musicCarouselShelfRenderer"] as? JsonObject)?.let(::parseCarousel)
            (node["musicShelfRenderer"] as? JsonObject)?.let(::parseMusicShelf)
            (node["gridRenderer"] as? JsonObject)?.let(::parseGrid)
        }

        if (shelves.isEmpty()) {
            collectObjectsByKey(root, "musicCarouselShelfRenderer").forEach(::parseCarousel)
            collectObjectsByKey(root, "musicShelfRenderer").forEach(::parseMusicShelf)
            collectObjectsByKey(root, "gridRenderer").forEach(::parseGrid)
        }

        return shelves
    }

    private fun extractOrderedSectionNodes(root: JsonObject): List<JsonObject> {
        val collected = mutableListOf<JsonObject>()

        fun addFromArray(array: JsonArray?) {
            if (array == null) return
            for (el in array) {
                val obj = el as? JsonObject ?: continue
                val itemSection = obj["itemSectionRenderer"] as? JsonObject
                if (itemSection != null) {
                    addFromArray(itemSection["contents"] as? JsonArray)
                } else {
                    collected += obj
                }
            }
        }

        val tabs = ((root["contents"] as? JsonObject)
            ?.get("singleColumnBrowseResultsRenderer") as? JsonObject)
            ?.get("tabs") as? JsonArray
        tabs?.forEach { tabEl ->
            val sectionList = (((tabEl as? JsonObject)
                ?.get("tabRenderer") as? JsonObject)
                ?.get("content") as? JsonObject)
                ?.get("sectionListRenderer") as? JsonObject
            addFromArray(sectionList?.get("contents") as? JsonArray)
        }

        val continuationContents = ((root["continuationContents"] as? JsonObject)
            ?.get("sectionListContinuation") as? JsonObject)
            ?.get("contents") as? JsonArray
        addFromArray(continuationContents)

        val receivedActions = root["onResponseReceivedActions"] as? JsonArray
        receivedActions?.forEach { actionEl ->
            val continuationItems = ((actionEl as? JsonObject)
                ?.get("appendContinuationItemsAction") as? JsonObject)
                ?.get("continuationItems") as? JsonArray
            addFromArray(continuationItems)
        }

        return collected
    }

    private fun parseShelfItems(contents: JsonArray): List<HomeCardItem> {
        val parsed = mutableListOf<HomeCardItem>()
        for (element in contents) {
            val obj = element as? JsonObject ?: continue
            (obj["musicResponsiveListItemRenderer"] as? JsonObject)?.let { responsive ->
                parseTrackFromResponsiveItem(responsive)?.let { track ->
                    parsed += HomeCardItem.TrackCard(track = track)
                }
            }
            (obj["musicTwoRowItemRenderer"] as? JsonObject)?.let { twoRow ->
                parseHomeItemFromTwoRowItem(twoRow)?.let { item ->
                    parsed += item
                }
            }
        }
        return parsed.distinctBy { it.key }
    }

    private fun parseHomeItemFromTwoRowItem(item: JsonObject): HomeCardItem? {
        val title = extractRunsText(item["title"]).ifBlank { return null }
        val rawSubtitle = extractRunsText(item["subtitle"])
        val thumbnailUrl = extractBestThumbnail(item["thumbnailRenderer"])
            ?: extractBestThumbnail(item["thumbnail"])

        val navEndpoint = item["navigationEndpoint"] as? JsonObject
        val overlayWatchEndpoint = ((((item["thumbnailOverlay"] as? JsonObject)
            ?.get("musicItemThumbnailOverlayRenderer") as? JsonObject)
            ?.get("content") as? JsonObject)
            ?.get("musicPlayButtonRenderer") as? JsonObject)
            ?.let { it["playNavigationEndpoint"] as? JsonObject }

        val watchEndpoint = (navEndpoint?.get("watchEndpoint") as? JsonObject)
            ?: (overlayWatchEndpoint?.get("watchEndpoint") as? JsonObject)
        val videoId = (watchEndpoint?.get("videoId") as? JsonPrimitive)?.contentOrNull
        if (!videoId.isNullOrBlank()) {
            val subtitleParts = rawSubtitle.split("•", "·")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            val cleanArtist = subtitleParts.firstOrNull { part ->
                val lower = part.lowercase(Locale.US)
                lower != "song" &&
                        lower != "video" &&
                        !lower.endsWith("plays") &&
                        !lower.endsWith("views")
            } ?: subtitleParts.firstOrNull() ?: "YouTube Music"

            return HomeCardItem.TrackCard(
                track = Track(
                    videoId = videoId,
                    title = title,
                    artist = cleanArtist,
                    thumbnailUrl = thumbnailUrl,
                ),
                subtitleOverride = rawSubtitle.ifBlank { cleanArtist },
            )
        }

        val browseEndpoint = navEndpoint?.get("browseEndpoint") as? JsonObject
        val browseId = (browseEndpoint?.get("browseId") as? JsonPrimitive)?.contentOrNull
        if (!browseId.isNullOrBlank()) {
            val pageType = (((browseEndpoint["browseEndpointContextSupportedConfigs"] as? JsonObject)
                ?.get("browseEndpointContextMusicConfig") as? JsonObject)
                ?.get("pageType") as? JsonPrimitive)
                ?.contentOrNull

            val isAlbum = pageType == "MUSIC_PAGE_TYPE_ALBUM" ||
                    browseId.startsWith("MPRE") ||
                    browseId.startsWith("OLAK")
            val isPlaylist = pageType == "MUSIC_PAGE_TYPE_PLAYLIST" ||
                    browseId.startsWith("VL") ||
                    browseId.startsWith("PL") ||
                    browseId.startsWith("RDCLAK")

            if (isAlbum || isPlaylist) {
                return HomeCardItem.CollectionCard(
                    id = if (isAlbum) browseId else browseId.removePrefix("VL"),
                    title = title,
                    subtitle = rawSubtitle.ifBlank { if (isAlbum) "Album" else "Playlist" },
                    thumbnailUrl = thumbnailUrl,
                    isAlbum = isAlbum,
                )
            }
        }

        val watchPlaylistEndpoint = (navEndpoint?.get("watchPlaylistEndpoint") as? JsonObject)
            ?: (overlayWatchEndpoint?.get("watchPlaylistEndpoint") as? JsonObject)
        val radioPlaylistId = (watchPlaylistEndpoint?.get("playlistId") as? JsonPrimitive)?.contentOrNull
        if (!radioPlaylistId.isNullOrBlank()) {
            return HomeCardItem.CollectionCard(
                id = radioPlaylistId.removePrefix("VL"),
                title = title,
                subtitle = rawSubtitle.ifBlank { "Mix" },
                thumbnailUrl = thumbnailUrl,
                isAlbum = false,
            )
        }

        return null
    }

    private fun prioritizeAndDedupeShelves(raw: List<HomeShelf>): List<HomeShelf> {
        val unique = raw
            .filter { it.items.isNotEmpty() }
            .distinctBy { "${it.strapline.orEmpty().lowercase(Locale.US)}|{}${it.title.lowercase(Locale.US)}" }

        val hasExplicitQuickPicks = unique.any {
            it.title.contains("quick picks", ignoreCase = true) ||
                    it.title.contains("speed dial", ignoreCase = true)
        }

        val withQuickPicks = if (!hasExplicitQuickPicks) {
            val candidateTracks = unique
                .flatMap { it.items }
                .filterIsInstance<HomeCardItem.TrackCard>()
                .distinctBy { it.track.videoId }
                .take(12)

            if (candidateTracks.size >= 4) {
                val quickPicksShelf = HomeShelf(
                    id = "synthesized_quick_picks",
                    title = "Quick picks",
                    strapline = "START RADIO FROM A SONG",
                    items = candidateTracks,
                )
                listOf(quickPicksShelf) + unique
            } else {
                unique
            }
        } else {
            unique
        }

        val keepListening = withQuickPicks.filter { isKeepListeningOrQuickPicks(it) }
        val similar = withQuickPicks.filter { !isKeepListeningOrQuickPicks(it) && isSimilarMusicShelf(it) }
        val trending = withQuickPicks.filter {
            !isKeepListeningOrQuickPicks(it) && !isSimilarMusicShelf(it) && isTrendingShelf(it)
        }
        val newReleases = withQuickPicks.filter {
            !isKeepListeningOrQuickPicks(it) &&
                    !isSimilarMusicShelf(it) &&
                    !isTrendingShelf(it) &&
                    isNewReleasesShelf(it)
        }
        val others = withQuickPicks.filter {
            !isKeepListeningOrQuickPicks(it) &&
                    !isSimilarMusicShelf(it) &&
                    !isTrendingShelf(it) &&
                    !isNewReleasesShelf(it)
        }

        return (keepListening + similar + trending + newReleases + others)
            .distinctBy { it.id }
            .take(18)
    }

    private fun isKeepListeningOrQuickPicks(shelf: HomeShelf): Boolean {
        val t = shelf.title.lowercase(Locale.US)
        return t.contains("keep listening") ||
                t.contains("listen again") ||
                t.contains("quick picks") ||
                t.contains("speed dial") ||
                t.contains("from your library") ||
                t.contains("forgotten favorites")
    }

    private fun isSimilarMusicShelf(shelf: HomeShelf): Boolean {
        val t = shelf.title.lowercase(Locale.US)
        val s = shelf.strapline?.lowercase(Locale.US).orEmpty()
        return t.contains("similar") ||
                s.contains("similar") ||
                t.contains("mixed for you") ||
                t.contains("recommended") ||
                t.contains("start radio") ||
                t.contains("for you") ||
                s.contains("because you")
    }

    private fun isTrendingShelf(shelf: HomeShelf): Boolean {
        val t = shelf.title.lowercase(Locale.US)
        return t.contains("trending") ||
                t.contains("top music") ||
                t.contains("top songs") ||
                t.contains("charts")
    }

    private fun isNewReleasesShelf(shelf: HomeShelf): Boolean {
        val t = shelf.title.lowercase(Locale.US)
        return t.contains("new release") ||
                t.contains("new albums") ||
                t.contains("albums & singles") ||
                t.contains("new music")
    }

    suspend fun fetchLibraryPlaylists(): List<PlaylistSummary> = withContext(Dispatchers.IO) {
        if (!hasValidSapisidCookie()) return@withContext emptyList()
        innerTube.useLoginForBrowse = true
        innerTube.dataSyncId = innerTube.dataSyncId?.trim()?.takeIf { it.isNotBlank() }
        if (innerTube.authUser.isBlank()) {
            innerTube.authUser = "0"
        }
        if (innerTube.visitorData.isNullOrBlank()) {
            syncAccountContext()
        }

        val likedPlaylistsResponse = innerTube.browse(
            client = YouTubeClient.WEB_REMIX,
            browseId = "FEmusic_liked_playlists",
            params = null,
            continuation = null,
            setLogin = true,
        )
        val likedPlaylistsRoot = (json.parseToJsonElement(likedPlaylistsResponse.bodyAsText()) as? JsonObject)
            ?: JsonObject(emptyMap())
        val parsed = parseLibraryPlaylists(likedPlaylistsRoot).ifEmpty {
            val landingResponse = innerTube.browse(
                client = YouTubeClient.WEB_REMIX,
                browseId = "FEmusic_library_landing",
                params = null,
                continuation = null,
                setLogin = true,
            )
            val landingRoot = (json.parseToJsonElement(landingResponse.bodyAsText()) as? JsonObject)
                ?: JsonObject(emptyMap())
            parseLibraryPlaylists(landingRoot)
        }

        val hasLikedSongs = parsed.any {
            it.id == "LM" || it.id == "VLLM" || it.id == "FEmusic_liked_videos" || it.isLikedSongs
        }
        if (hasLikedSongs) {
            parsed
        } else {
            listOf(
                PlaylistSummary(
                    id = "VLLM",
                    title = "Liked Music",
                    subtitle = "Your liked YouTube Music tracks",
                    songCountText = "Auto playlist",
                    thumbnailUrl = null,
                    isLikedSongs = true,
                ),
            ) + parsed
        }
    }

    suspend fun fetchPlaylistTracks(
        playlistId: String,
    ): PlaylistDetailData = withContext(Dispatchers.IO) {
        if (hasValidSapisidCookie()) {
            innerTube.useLoginForBrowse = true
            innerTube.dataSyncId = innerTube.dataSyncId?.trim()?.takeIf { it.isNotBlank() }
            if (innerTube.authUser.isBlank()) {
                innerTube.authUser = "0"
            }
            if (innerTube.visitorData.isNullOrBlank()) {
                syncAccountContext()
            }
        }

        val isLikedMusic = playlistId == "LM" ||
                playlistId == "VLLM" ||
                playlistId == "FEmusic_liked_videos"

        if (isLikedMusic) {
            val vllmResult = runCatching { fetchBrowseTracks("VLLM") }.getOrNull()
            if (vllmResult != null && vllmResult.tracks.isNotEmpty()) {
                return@withContext vllmResult
            }
            val fallbackResult = runCatching { fetchBrowseTracks("FEmusic_liked_videos") }.getOrNull()
            if (fallbackResult != null && fallbackResult.tracks.isNotEmpty()) {
                return@withContext fallbackResult
            }
            return@withContext vllmResult ?: fallbackResult ?: fetchBrowseTracks("VLLM")
        }

        val browseId = when {
            playlistId.startsWith("VL") -> playlistId
            playlistId.startsWith("MPRE") -> playlistId
            playlistId.startsWith("FE") -> playlistId
            else -> "VL$playlistId"
        }

        fetchBrowseTracks(browseId)
    }

    private suspend fun fetchBrowseTracks(browseId: String): PlaylistDetailData {
        val signedIn = hasValidSapisidCookie()
        innerTube.useLoginForBrowse = signedIn
        innerTube.dataSyncId = innerTube.dataSyncId?.trim()?.takeIf { it.isNotBlank() }
        val response = innerTube.browse(
            client = YouTubeClient.WEB_REMIX,
            browseId = browseId,
            params = null,
            continuation = null,
            setLogin = signedIn,
        )
        val root = (json.parseToJsonElement(response.bodyAsText()) as? JsonObject) ?: JsonObject(emptyMap())
        val header = extractPlaylistHeader(root)
        val tracks = mutableListOf<Track>()
        tracks += extractTracksFromNode(root)

        var continuation = extractContinuationToken(root)
        var pageCount = 0
        while (!continuation.isNullOrBlank() && pageCount < 3) {
            pageCount++
            val contResponse = innerTube.browse(
                client = YouTubeClient.WEB_REMIX,
                browseId = null,
                params = null,
                continuation = continuation,
                setLogin = signedIn,
            )
            if (contResponse.status.value !in 200..299) break
            val contRoot = (json.parseToJsonElement(contResponse.bodyAsText()) as? JsonObject) ?: break
            val more = extractTracksFromNode(contRoot)
            if (more.isEmpty()) break
            tracks += more
            continuation = extractContinuationToken(contRoot)
        }

        return PlaylistDetailData(
            title = header.title,
            subtitle = header.subtitle,
            thumbnailUrl = header.thumbnailUrl,
            tracks = tracks.distinctBy { it.videoId },
        )
    }

    suspend fun resolveAudioStream(
        videoId: String,
        audioQuality: AudioQualityMode = AudioQualityMode.HIGH,
    ): ExtractedStream = withContext(Dispatchers.IO) {
        val cacheKey = "$videoId:${audioQuality.name}"
        val now = System.currentTimeMillis()
        val cached = synchronized(resolvedStreamCache) {
            resolvedStreamCache.get(cacheKey)
        }
        if (cached != null && now - cached.cachedAtMs < STREAM_CACHE_TTL_MS) {
            return@withContext cached.stream
        }

        val resolved = when (audioQuality) {
            AudioQualityMode.HIGH -> {
                extractor.extract(
                    videoId,
                    ContentHints(),
                    emptySet(),
                    AudioQuality.HIGH,
                ) ?: error("No playable audio stream found for $videoId")
            }

            AudioQualityMode.SAVER -> {
                extractor.extract(
                    videoId,
                    ContentHints(),
                    emptySet(),
                    AudioQuality.LOW,
                ) ?: error("No playable audio stream found for $videoId")
            }

            AudioQualityMode.BALANCED -> {
                val candidates = mutableListOf<ExtractedStream>()
                val autoStream = runCatching {
                    extractor.extract(videoId, ContentHints(), emptySet(), AudioQuality.AUTO)
                }.getOrNull()
                if (autoStream != null) {
                    candidates += autoStream
                }

                val autoBitrateBps = autoStream?.let(::estimateBitrateBps)
                if (autoStream == null || (autoBitrateBps != null && autoBitrateBps > BALANCED_MAX_BITRATE_BPS)) {
                    val mp4Stream = runCatching {
                        extractor.extract(videoId, ContentHints(), emptySet(), AudioQuality.MP4)
                    }.getOrNull()
                    if (mp4Stream != null) {
                        candidates += mp4Stream
                    }

                    val bestUnderCap = candidates
                        .filter { (estimateBitrateBps(it) ?: Int.MAX_VALUE) <= BALANCED_MAX_BITRATE_BPS }
                        .maxByOrNull { estimateBitrateBps(it) ?: 0 }

                    if (bestUnderCap == null) {
                        runCatching {
                            extractor.extract(videoId, ContentHints(), emptySet(), AudioQuality.LOW)
                        }.getOrNull()?.let { candidates += it }
                    }
                }

                val withinCap = candidates.filter {
                    val bps = estimateBitrateBps(it)
                    bps != null && bps <= BALANCED_MAX_BITRATE_BPS
                }
                withinCap.maxByOrNull { estimateBitrateBps(it) ?: 0 }
                    ?: candidates.minByOrNull { estimateBitrateBps(it) ?: Int.MAX_VALUE }
                    ?: error("No playable audio stream found for $videoId")
            }
        }

        synchronized(resolvedStreamCache) {
            resolvedStreamCache.put(cacheKey, CachedResolvedStream(resolved, System.currentTimeMillis()))
        }
        resolved
    }

    suspend fun prewarmAudioStream(
        videoId: String,
        audioQuality: AudioQualityMode,
    ): Result<ExtractedStream> = runCatching {
        resolveAudioStream(videoId, audioQuality)
    }

    private fun estimateBitrateBps(stream: ExtractedStream): Int? {
        val reported = stream.bitrate
        if (reported != null && reported > 0) return reported
        return when (stream.itag) {
            141 -> 256_000
            251 -> 160_000
            140 -> 128_000
            250 -> 70_000
            249 -> 50_000
            139 -> 48_000
            else -> null
        }
    }

    suspend fun fetchStreamDetails(
        track: Track,
        audioQuality: AudioQualityMode = AudioQualityMode.HIGH,
    ): TrackStreamDetails = withContext(Dispatchers.IO) {
        val stream = resolveAudioStream(track.videoId, audioQuality)
        val url = stream.audioUrl
        val queryParams = parseUrlQueryParams(url)

        val itag = queryParams["itag"]?.toIntOrNull()
        val mimeFromParam = queryParams["mime"]?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrNull() }
        val clenFromParam = queryParams["clen"]?.toLongOrNull()

        var contentType = mimeFromParam
        var contentLength = clenFromParam

        if (contentType == null || contentLength == null) {
            runCatching {
                val reqBuilder = Request.Builder().url(url).head()
                reqBuilder.header("Origin", "https://music.youtube.com")
                reqBuilder.header("Referer", "https://music.youtube.com/")
                stream.headers.forEach { (k, v) -> if (v.isNotBlank()) reqBuilder.header(k, v) }
                okHttpClient.newCall(reqBuilder.build()).execute().use { resp ->
                    if (contentType == null) contentType = resp.header("Content-Type")
                    if (contentLength == null) contentLength = resp.header("Content-Length")?.toLongOrNull()
                }
            }
        }

        val codecLabel = when {
            itag == 251 || contentType?.contains("webm", true) == true || contentType?.contains("opus", true) == true ->
                "Opus (WebM)"
            itag == 250 -> "Opus Low (WebM)"
            itag == 249 -> "Opus Ultra-Low (WebM)"
            itag == 141 -> "AAC High (M4A)"
            itag == 140 || contentType?.contains("mp4", true) == true || contentType?.contains("m4a", true) == true ->
                "AAC-LC (M4A)"
            itag == 139 -> "HE-AAC (M4A)"
            !contentType.isNullOrBlank() -> contentType
            else -> "Audio Stream"
        }

        val estimatedBitrate = when (itag) {
            251 -> 160
            250 -> 70
            249 -> 50
            141 -> 256
            140 -> 128
            139 -> 48
            else -> {
                val durSec = track.durationSeconds
                if (contentLength != null && durSec != null && durSec > 0) {
                    ((contentLength * 8L) / durSec / 1000L).toInt().coerceAtLeast(32)
                } else {
                    null
                }
            }
        }

        TrackStreamDetails(
            codec = codecLabel,
            bitrateKbps = estimatedBitrate,
            mimeType = contentType,
            contentLengthBytes = contentLength,
        )
    }

    suspend fun fetchLyrics(videoId: String): LyricsData? = withContext(Dispatchers.IO) {
        val signedIn = hasValidSapisidCookie()
        innerTube.useLoginForBrowse = signedIn
        val nextResponse = innerTube.next(
            client = YouTubeClient.WEB_REMIX,
            videoId = videoId,
            playlistId = null,
            playlistSetVideoId = null,
            index = null,
            params = null,
            continuation = null,
        )
        val nextRoot = (json.parseToJsonElement(nextResponse.bodyAsText()) as? JsonObject)
            ?: return@withContext null

        val lyricsBrowseId = collectObjectsByKey(nextRoot, "browseEndpoint")
            .mapNotNull { (it["browseId"] as? JsonPrimitive)?.contentOrNull }
            .firstOrNull { it.startsWith("MPLYt") }
            ?: return@withContext null

        val browseResponse = innerTube.browse(
            client = YouTubeClient.WEB_REMIX,
            browseId = lyricsBrowseId,
            params = null,
            continuation = null,
            setLogin = signedIn,
        )
        val lyricsRoot = (json.parseToJsonElement(browseResponse.bodyAsText()) as? JsonObject)
            ?: return@withContext null

        val timedRenderer = collectObjectsByKey(lyricsRoot, "timedLyricsRenderer").firstOrNull()
        if (timedRenderer != null) {
            val timedArray = timedRenderer["timedLyricsData"] as? JsonArray
            val syncedLines = timedArray?.mapNotNull { el ->
                val obj = el as? JsonObject ?: return@mapNotNull null
                val text = (obj["lyricLine"] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
                if (text.isEmpty()) return@mapNotNull null
                val cue = obj["cueRange"] as? JsonObject
                val startMs = (cue?.get("startTimeMilliseconds") as? JsonPrimitive)?.contentOrNull?.toLongOrNull()
                val endMs = (cue?.get("endTimeMilliseconds") as? JsonPrimitive)?.contentOrNull?.toLongOrNull()
                LyricLine(text = text, startTimeMs = startMs, endTimeMs = endMs)
            }.orEmpty()

            if (syncedLines.isNotEmpty()) {
                val source = extractRunsText(timedRenderer["footer"]).ifBlank { "YouTube Music" }
                return@withContext LyricsData(
                    lines = syncedLines,
                    source = source,
                    isSynced = syncedLines.any { it.startTimeMs != null },
                )
            }
        }

        val descShelf = collectObjectsByKey(lyricsRoot, "musicDescriptionShelfRenderer").firstOrNull()
            ?: return@withContext null
        val rawLyrics = extractRunsText(descShelf["description"])
        if (rawLyrics.isBlank()) return@withContext null
        val plainLines = rawLyrics.lines().map { LyricLine(text = it) }
        val source = extractRunsText(descShelf["footer"]).ifBlank { "YouTube Music" }
        LyricsData(
            lines = plainLines,
            source = source,
            isSynced = false,
        )
    }

    suspend fun setLikeStatus(videoId: String, like: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (!hasValidSapisidCookie()) {
            error("Sign in on Settings > Account & Sync to save liked songs.")
        }
        innerTube.useLoginForBrowse = true
        innerTube.dataSyncId = innerTube.dataSyncId?.trim()?.takeIf { it.isNotBlank() }
        if (like) {
            innerTube.likeVideo(YouTubeClient.WEB_REMIX, videoId)
        } else {
            innerTube.unlikeVideo(YouTubeClient.WEB_REMIX, videoId)
        }
        like
    }

    suspend fun addTrackToPlaylist(playlistId: String, videoId: String) = withContext(Dispatchers.IO) {
        if (!hasValidSapisidCookie()) {
            error("Sign in on Settings > Account & Sync to edit playlists.")
        }
        innerTube.useLoginForBrowse = true
        innerTube.dataSyncId = innerTube.dataSyncId?.trim()?.takeIf { it.isNotBlank() }
        val cleanPlaylistId = playlistId.removePrefix("VL")
        innerTube.addToPlaylist(YouTubeClient.WEB_REMIX, cleanPlaylistId, videoId)
    }

    suspend fun resolveArtistIdForTrack(track: Track): String? = withContext(Dispatchers.IO) {
        if (!track.artistId.isNullOrBlank() && track.artistId.startsWith("UC")) {
            return@withContext track.artistId
        }
        val fromNext = runCatching {
            val resp = innerTube.next(
                client = YouTubeClient.WEB_REMIX,
                videoId = track.videoId,
                playlistId = null,
                playlistSetVideoId = null,
                index = null,
                params = null,
                continuation = null,
            )
            val root = json.parseToJsonElement(resp.bodyAsText()) as? JsonObject ?: return@runCatching null
            collectObjectsByKey(root, "browseEndpoint")
                .mapNotNull { (it["browseId"] as? JsonPrimitive)?.contentOrNull }
                .firstOrNull { it.startsWith("UC") }
        }.getOrNull()

        if (!fromNext.isNullOrBlank()) return@withContext fromNext

        val artistQuery = track.artist.split(",", "&", "feat.", "ft.").firstOrNull()?.trim().orEmpty()
            .ifBlank { track.artist }
        runCatching {
            search(artistQuery, SearchFilterOption.ARTISTS)
                .filterIsInstance<SearchResultItem.ArtistResult>()
                .firstOrNull()
                ?.artist
                ?.id
        }.getOrNull()
    }

    suspend fun resolveAlbumBrowseIdForTrack(track: Track): String? = withContext(Dispatchers.IO) {
        if (!track.albumId.isNullOrBlank() && track.albumId.startsWith("MPRE")) {
            return@withContext track.albumId
        }
        val fromNext = runCatching {
            val resp = innerTube.next(
                client = YouTubeClient.WEB_REMIX,
                videoId = track.videoId,
                playlistId = null,
                playlistSetVideoId = null,
                index = null,
                params = null,
                continuation = null,
            )
            val root = json.parseToJsonElement(resp.bodyAsText()) as? JsonObject ?: return@runCatching null
            collectObjectsByKey(root, "browseEndpoint")
                .mapNotNull { (it["browseId"] as? JsonPrimitive)?.contentOrNull }
                .firstOrNull { it.startsWith("MPRE") }
        }.getOrNull()

        if (!fromNext.isNullOrBlank()) return@withContext fromNext

        val albumQuery = buildString {
            append(track.album?.takeIf { it.isNotBlank() } ?: track.title)
            append(" ")
            append(track.artist)
        }.trim()
        runCatching {
            search(albumQuery, SearchFilterOption.ALBUMS)
                .filterIsInstance<SearchResultItem.AlbumResult>()
                .firstOrNull()
                ?.album
                ?.browseId
        }.getOrNull()
    }

    private enum class ArtistDiscographyKind {
        ALBUMS,
        SINGLES,
        FEATURED,
    }

    private data class DiscographySeeAllRequest(
        val browseId: String,
        val params: String?,
        val kind: ArtistDiscographyKind,
    )

    suspend fun fetchArtistDetails(artistId: String): ArtistDetailData = withContext(Dispatchers.IO) {
        val signedIn = hasValidSapisidCookie()
        innerTube.useLoginForBrowse = signedIn
        val response = innerTube.browse(
            client = YouTubeClient.WEB_REMIX,
            browseId = artistId,
            params = null,
            continuation = null,
            setLogin = signedIn,
        )
        val root = (json.parseToJsonElement(response.bodyAsText()) as? JsonObject) ?: JsonObject(emptyMap())

        val header = collectObjectsByKey(root, "musicImmersiveHeaderRenderer").firstOrNull()
            ?: collectObjectsByKey(root, "musicVisualHeaderRenderer").firstOrNull()
            ?: collectObjectsByKey(root, "musicHeaderRenderer").firstOrNull()
            ?: collectObjectsByKey(root, "musicDetailHeaderRenderer").firstOrNull()
        val name = extractRunsText(header?.get("title")).ifBlank { "Artist" }
        val description = extractRunsText(header?.get("description")).takeIf { it.isNotBlank() }
        val subscriberCount = header?.findStringByKey("subscriberCountText")
            ?.takeIf { it.isNotBlank() }
            ?: extractRunsText(
                (header?.get("subscriptionButton") as? JsonObject)
                    ?.let { it["subscribeButtonRenderer"] as? JsonObject }
                    ?.get("subscriberCountText"),
            ).takeIf { it.isNotBlank() }
        val thumbnailUrl = extractBestThumbnail(header?.get("thumbnail"))

        val inlineTopSongs = mutableListOf<Track>()
        val videoTracks = mutableListOf<Track>()
        val inlineAlbums = mutableListOf<AlbumSummary>()
        val inlineSingles = mutableListOf<AlbumSummary>()
        val inlineFeatured = mutableListOf<AlbumSummary>()

        var topSongsSeeAllEndpoint: Pair<String, String?>? = null
        val seeAllDiscographyRequests = mutableListOf<DiscographySeeAllRequest>()

        fun parseBrowsePair(endpointObj: JsonObject?): Pair<String, String?>? {
            val browse = (endpointObj?.get("browseEndpoint") as? JsonObject) ?: endpointObj ?: return null
            val bId = (browse["browseId"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() } ?: return null
            val params = (browse["params"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
            return bId to params
        }

        fun extractTitleEndpoint(titleElement: JsonElement?): Pair<String, String?>? {
            val runs = (titleElement as? JsonObject)?.get("runs") as? JsonArray ?: return null
            for (run in runs) {
                val nav = (run as? JsonObject)?.get("navigationEndpoint") as? JsonObject ?: continue
                parseBrowsePair(nav)?.let { return it }
            }
            return null
        }

        fun classifyCollectionTarget(
            shelfTitleLower: String,
            item: AlbumSummary,
        ): ArtistDiscographyKind = when {
            shelfTitleLower.contains("single") || shelfTitleLower.contains("ep") -> ArtistDiscographyKind.SINGLES
            shelfTitleLower.contains("album") -> ArtistDiscographyKind.ALBUMS
            shelfTitleLower.contains("featured") ||
                    item.browseId.startsWith("VL") ||
                    item.browseId.startsWith("PL") ||
                    item.browseId.startsWith("RDCLAK") -> ArtistDiscographyKind.FEATURED
            item.artist.contains("Single", ignoreCase = true) ||
                    item.artist.contains("EP", ignoreCase = true) -> ArtistDiscographyKind.SINGLES
            else -> ArtistDiscographyKind.ALBUMS
        }

        val sectionNodes = extractOrderedSectionNodes(root).ifEmpty {
            collectObjectsByKey(root, "musicShelfRenderer").map { JsonObject(mapOf("musicShelfRenderer" to it)) } +
                    collectObjectsByKey(root, "musicCarouselShelfRenderer").map { JsonObject(mapOf("musicCarouselShelfRenderer" to it)) }
        }

        for (node in sectionNodes) {
            (node["musicShelfRenderer"] as? JsonObject)?.let { shelf ->
                val contents = shelf["contents"] as? JsonArray
                contents?.forEach { el ->
                    ((el as? JsonObject)?.get("musicResponsiveListItemRenderer") as? JsonObject)
                        ?.let { parseTrackFromResponsiveItem(it) }
                        ?.let { inlineTopSongs += it }
                }
                if (topSongsSeeAllEndpoint == null) {
                    val headerRenderer = (shelf["header"] as? JsonObject)
                        ?.values?.firstOrNull { it is JsonObject } as? JsonObject
                    topSongsSeeAllEndpoint = parseBrowsePair(shelf["bottomEndpoint"] as? JsonObject)
                        ?: extractTitleEndpoint(shelf["bottomText"])
                                ?: parseBrowsePair(
                            (headerRenderer?.get("moreContentButton") as? JsonObject)
                                ?.let { it["buttonRenderer"] as? JsonObject }
                                ?.let { it["navigationEndpoint"] as? JsonObject },
                        )
                                ?: extractTitleEndpoint(shelf["title"])
                                ?: extractTitleEndpoint(headerRenderer?.get("title"))
                }
            }

            (node["musicCarouselShelfRenderer"] as? JsonObject)?.let { shelf ->
                val headerObj = shelf["header"] as? JsonObject
                val basicHeader = (headerObj?.get("musicCarouselShelfBasicHeaderRenderer") as? JsonObject)
                    ?: (headerObj?.values?.firstOrNull { it is JsonObject } as? JsonObject)
                val shelfTitle = extractRunsText(basicHeader?.get("title"))
                    .ifBlank { extractRunsText(shelf["title"]) }
                    .lowercase(Locale.US)
                val contents = shelf["contents"] as? JsonArray

                contents?.forEach { el ->
                    val obj = el as? JsonObject ?: return@forEach
                    (obj["musicResponsiveListItemRenderer"] as? JsonObject)?.let { responsive ->
                        parseTrackFromResponsiveItem(responsive)?.let { inlineTopSongs += it }
                    }
                    (obj["musicTwoRowItemRenderer"] as? JsonObject)?.let { twoRow ->
                        val watchTrack = parseTrackFromTwoRowItem(twoRow, defaultArtist = name)
                        if (watchTrack != null) {
                            if (shelfTitle.contains("song")) {
                                inlineTopSongs += watchTrack
                            } else {
                                videoTracks += watchTrack
                            }
                        } else {
                            val collection = parseArtistCollectionFromTwoRowItem(twoRow, defaultArtist = name)
                            if (collection != null && !collection.browseId.startsWith("UC")) {
                                when (classifyCollectionTarget(shelfTitle, collection)) {
                                    ArtistDiscographyKind.ALBUMS -> inlineAlbums += collection
                                    ArtistDiscographyKind.SINGLES -> inlineSingles += collection
                                    ArtistDiscographyKind.FEATURED -> inlineFeatured += collection
                                }
                            }
                        }
                    }
                }

                val moreEndpoint = (basicHeader?.get("moreContentButton") as? JsonObject)
                    ?.let { it["buttonRenderer"] as? JsonObject }
                    ?.let { it["navigationEndpoint"] as? JsonObject }
                val seeAllPair = parseBrowsePair(moreEndpoint)
                    ?: extractTitleEndpoint(basicHeader?.get("title"))

                if (seeAllPair != null) {
                    val (bId, bParams) = seeAllPair
                    when {
                        shelfTitle.contains("song") && topSongsSeeAllEndpoint == null -> {
                            topSongsSeeAllEndpoint = bId to bParams
                        }
                        shelfTitle.contains("single") || shelfTitle.contains("ep") -> {
                            seeAllDiscographyRequests += DiscographySeeAllRequest(
                                browseId = bId,
                                params = bParams,
                                kind = ArtistDiscographyKind.SINGLES,
                            )
                        }
                        shelfTitle.contains("album") -> {
                            seeAllDiscographyRequests += DiscographySeeAllRequest(
                                browseId = bId,
                                params = bParams,
                                kind = ArtistDiscographyKind.ALBUMS,
                            )
                        }
                        shelfTitle.contains("featured") -> {
                            seeAllDiscographyRequests += DiscographySeeAllRequest(
                                browseId = bId,
                                params = bParams,
                                kind = ArtistDiscographyKind.FEATURED,
                            )
                        }
                    }
                }
            }
        }

        coroutineScope {
            val allSongsDeferred = topSongsSeeAllEndpoint?.let { (songsBrowseId, songsParams) ->
                async {
                    runCatching {
                        fetchAllArtistTracksFromBrowse(
                            browseId = songsBrowseId,
                            params = songsParams,
                            signedIn = signedIn,
                            maxContinuations = 6,
                        )
                    }.getOrDefault(emptyList())
                }
            }

            val discographyDeferred = seeAllDiscographyRequests
                .distinctBy { it.browseId to it.params }
                .map { req ->
                    async {
                        val items = runCatching {
                            fetchAllArtistCollectionsFromBrowse(
                                browseId = req.browseId,
                                params = req.params,
                                defaultArtist = name,
                                signedIn = signedIn,
                                maxContinuations = 6,
                            )
                        }.getOrDefault(emptyList())
                        req.kind to items
                    }
                }

            val expandedSongs = allSongsDeferred?.await().orEmpty()
            val mergedTopSongs = (expandedSongs + inlineTopSongs + videoTracks)
                .distinctBy { it.videoId }

            val expandedAlbums = mutableListOf<AlbumSummary>()
            val expandedSingles = mutableListOf<AlbumSummary>()
            val expandedFeatured = mutableListOf<AlbumSummary>()

            discographyDeferred.forEach { deferred ->
                val (kind, items) = deferred.await()
                val validItems = items.filter { !it.browseId.startsWith("UC") }
                when (kind) {
                    ArtistDiscographyKind.ALBUMS -> expandedAlbums += validItems
                    ArtistDiscographyKind.SINGLES -> expandedSingles += validItems
                    ArtistDiscographyKind.FEATURED -> expandedFeatured += validItems
                }
            }

            val mergedAlbums = (inlineAlbums + expandedAlbums).distinctBy { it.browseId }
            val mergedSingles = (inlineSingles + expandedSingles).distinctBy { it.browseId }
            val mergedFeatured = (inlineFeatured + expandedFeatured).distinctBy { it.browseId }

            ArtistDetailData(
                browseId = artistId,
                name = name,
                description = description,
                subscriberCount = subscriberCount,
                thumbnailUrl = thumbnailUrl,
                topSongs = mergedTopSongs,
                albums = mergedAlbums,
                singles = mergedSingles,
                featuredOn = mergedFeatured,
            )
        }
    }

    private suspend fun fetchAllArtistTracksFromBrowse(
        browseId: String,
        params: String?,
        signedIn: Boolean,
        maxContinuations: Int,
    ): List<Track> {
        innerTube.useLoginForBrowse = signedIn
        val response = innerTube.browse(
            client = YouTubeClient.WEB_REMIX,
            browseId = browseId,
            params = params,
            continuation = null,
            setLogin = signedIn,
        )
        if (response.status.value !in 200..299) return emptyList()
        val firstRoot = (json.parseToJsonElement(response.bodyAsText()) as? JsonObject) ?: return emptyList()

        val allTracks = mutableListOf<Track>()
        allTracks += extractArtistTracksFromPage(firstRoot)

        var continuation = extractContinuationToken(firstRoot)
        val seenTokens = mutableSetOf<String>()
        var page = 0
        while (!continuation.isNullOrBlank() && page < maxContinuations && seenTokens.add(continuation)) {
            page++
            val contResp = innerTube.browse(
                client = YouTubeClient.WEB_REMIX,
                browseId = null,
                params = null,
                continuation = continuation,
                setLogin = signedIn,
            )
            if (contResp.status.value !in 200..299) break
            val contRoot = (json.parseToJsonElement(contResp.bodyAsText()) as? JsonObject) ?: break
            val more = extractArtistTracksFromPage(contRoot)
            allTracks += more
            continuation = extractContinuationToken(contRoot)
        }

        return allTracks.distinctBy { it.videoId }
    }

    private fun extractArtistTracksFromPage(root: JsonObject): List<Track> {
        val tracks = mutableListOf<Track>()

        fun extractFromItemsArray(array: JsonArray?) {
            if (array == null) return
            for (el in array) {
                val obj = el as? JsonObject ?: continue
                (obj["musicResponsiveListItemRenderer"] as? JsonObject)?.let { responsive ->
                    parseTrackFromResponsiveItem(responsive)?.let { tracks += it }
                }
                (obj["musicTwoRowItemRenderer"] as? JsonObject)?.let { twoRow ->
                    parseTrackFromTwoRowItem(twoRow, defaultArtist = "YouTube Music")?.let { tracks += it }
                }
            }
        }

        collectObjectsByKey(root, "musicPlaylistShelfRenderer").forEach {
            extractFromItemsArray(it["contents"] as? JsonArray)
        }
        collectObjectsByKey(root, "musicShelfRenderer").forEach {
            extractFromItemsArray(it["contents"] as? JsonArray)
        }
        collectObjectsByKey(root, "musicCarouselShelfRenderer").forEach {
            extractFromItemsArray(it["contents"] as? JsonArray)
        }
        collectObjectsByKey(root, "gridRenderer").forEach {
            extractFromItemsArray(it["items"] as? JsonArray)
        }

        if (tracks.isEmpty()) {
            tracks += extractTracksFromNode(root)
        }

        return tracks.distinctBy { it.videoId }
    }

    private suspend fun fetchAllArtistCollectionsFromBrowse(
        browseId: String,
        params: String?,
        defaultArtist: String,
        signedIn: Boolean,
        maxContinuations: Int,
    ): List<AlbumSummary> {
        innerTube.useLoginForBrowse = signedIn
        val response = innerTube.browse(
            client = YouTubeClient.WEB_REMIX,
            browseId = browseId,
            params = params,
            continuation = null,
            setLogin = signedIn,
        )
        if (response.status.value !in 200..299) return emptyList()
        val firstRoot = (json.parseToJsonElement(response.bodyAsText()) as? JsonObject) ?: return emptyList()

        val allItems = mutableListOf<AlbumSummary>()
        allItems += extractArtistCollectionsFromPage(firstRoot, defaultArtist = defaultArtist)

        var continuation = extractContinuationToken(firstRoot)
        val seenTokens = mutableSetOf<String>()
        var page = 0
        while (!continuation.isNullOrBlank() && page < maxContinuations && seenTokens.add(continuation)) {
            page++
            val contResp = innerTube.browse(
                client = YouTubeClient.WEB_REMIX,
                browseId = null,
                params = null,
                continuation = continuation,
                setLogin = signedIn,
            )
            if (contResp.status.value !in 200..299) break
            val contRoot = (json.parseToJsonElement(contResp.bodyAsText()) as? JsonObject) ?: break
            allItems += extractArtistCollectionsFromPage(contRoot, defaultArtist = defaultArtist)
            continuation = extractContinuationToken(contRoot)
        }

        return allItems.distinctBy { it.browseId }
    }

    private fun extractArtistCollectionsFromPage(
        root: JsonObject,
        defaultArtist: String,
    ): List<AlbumSummary> {
        val collections = mutableListOf<AlbumSummary>()

        fun extractFromArray(array: JsonArray?) {
            if (array == null) return
            for (el in array) {
                val obj = el as? JsonObject ?: continue
                (obj["musicTwoRowItemRenderer"] as? JsonObject)?.let { twoRow ->
                    parseArtistCollectionFromTwoRowItem(twoRow, defaultArtist = defaultArtist)?.let { collections += it }
                }
                (obj["musicResponsiveListItemRenderer"] as? JsonObject)?.let { responsive ->
                    parseAlbumFromResponsiveItem(responsive)?.let { collections += it }
                }
            }
        }

        collectObjectsByKey(root, "gridRenderer").forEach {
            extractFromArray(it["items"] as? JsonArray)
        }
        collectObjectsByKey(root, "musicPlaylistShelfRenderer").forEach {
            extractFromArray(it["contents"] as? JsonArray)
        }
        collectObjectsByKey(root, "musicShelfRenderer").forEach {
            extractFromArray(it["contents"] as? JsonArray)
        }
        collectObjectsByKey(root, "musicCarouselShelfRenderer").forEach {
            extractFromArray(it["contents"] as? JsonArray)
        }

        if (collections.isEmpty()) {
            collectObjectsByKey(root, "musicTwoRowItemRenderer").forEach { twoRow ->
                parseArtistCollectionFromTwoRowItem(twoRow, defaultArtist = defaultArtist)?.let { collections += it }
            }
            collectObjectsByKey(root, "musicResponsiveListItemRenderer").forEach { responsive ->
                parseAlbumFromResponsiveItem(responsive)?.let { collections += it }
            }
        }

        return collections.distinctBy { it.browseId }
    }

    private fun parseTrackFromTwoRowItem(item: JsonObject, defaultArtist: String): Track? {
        val navEndpoint = item["navigationEndpoint"] as? JsonObject
        val overlayWatchEndpoint = ((((item["thumbnailOverlay"] as? JsonObject)
            ?.get("musicItemThumbnailOverlayRenderer") as? JsonObject)
            ?.get("content") as? JsonObject)
            ?.get("musicPlayButtonRenderer") as? JsonObject)
            ?.let { it["playNavigationEndpoint"] as? JsonObject }

        val watchEndpoint = (navEndpoint?.get("watchEndpoint") as? JsonObject)
            ?: (overlayWatchEndpoint?.get("watchEndpoint") as? JsonObject)
            ?: return null
        val videoId = (watchEndpoint["videoId"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: return null
        val title = extractRunsText(item["title"]).ifBlank { return null }
        val rawSubtitle = extractRunsText(item["subtitle"])
        val parts = rawSubtitle.split("•", "·")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val cleanArtist = parts.firstOrNull { part ->
            val lower = part.lowercase(Locale.US)
            lower != "song" &&
                    lower != "video" &&
                    !lower.endsWith("plays") &&
                    !lower.endsWith("views")
        } ?: defaultArtist
        val thumb = extractBestThumbnail(item["thumbnailRenderer"])
            ?: extractBestThumbnail(item["thumbnail"])
        return Track(
            videoId = videoId,
            title = title,
            artist = cleanArtist,
            thumbnailUrl = thumb,
        )
    }

    private fun parseArtistCollectionFromTwoRowItem(item: JsonObject, defaultArtist: String): AlbumSummary? {
        val navEndpoint = item["navigationEndpoint"] as? JsonObject
        val titleRunEndpoint = (((item["title"] as? JsonObject)
            ?.get("runs") as? JsonArray)
            ?.firstOrNull() as? JsonObject)
            ?.get("navigationEndpoint") as? JsonObject
        val browseEndpoint = ((navEndpoint?.get("browseEndpoint") as? JsonObject)
            ?: (titleRunEndpoint?.get("browseEndpoint") as? JsonObject))
            ?: return null
        val rawBrowseId = (browseEndpoint["browseId"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: return null

        val pageType = (((browseEndpoint["browseEndpointContextSupportedConfigs"] as? JsonObject)
            ?.get("browseEndpointContextMusicConfig") as? JsonObject)
            ?.get("pageType") as? JsonPrimitive)
            ?.contentOrNull

        val isSupportedId = rawBrowseId.startsWith("MPRE") ||
                rawBrowseId.startsWith("OLAK") ||
                rawBrowseId.startsWith("VL") ||
                rawBrowseId.startsWith("PL") ||
                rawBrowseId.startsWith("RDCLAK") ||
                rawBrowseId.startsWith("UC") ||
                pageType == "MUSIC_PAGE_TYPE_ALBUM" ||
                pageType == "MUSIC_PAGE_TYPE_PLAYLIST" ||
                pageType == "MUSIC_PAGE_TYPE_ARTIST"
        if (!isSupportedId) return null

        val title = extractRunsText(item["title"]).ifBlank { return null }
        val subtitle = extractRunsText(item["subtitle"])
        val subtitleParts = subtitle.split("•", "·").map { it.trim() }.filter { it.isNotEmpty() }
        val year = subtitleParts.lastOrNull()?.toIntOrNull()
        val thumb = extractBestThumbnail(item["thumbnailRenderer"])
            ?: extractBestThumbnail(item["thumbnail"])

        return AlbumSummary(
            browseId = rawBrowseId,
            playlistId = null,
            title = title,
            artist = subtitle.ifBlank { defaultArtist },
            year = year,
            thumbnailUrl = thumb,
        )
    }

    private fun parseUrlQueryParams(url: String): Map<String, String> {
        val query = url.substringAfter('?', "")
        if (query.isBlank()) return emptyMap()
        return query.split('&').mapNotNull { part ->
            val idx = part.indexOf('=')
            if (idx > 0) part.substring(0, idx) to part.substring(idx + 1) else null
        }.toMap()
    }

    private fun parseSearchResults(
        root: JsonObject,
        filter: SearchFilterOption,
        isContinuation: Boolean = false,
    ): List<SearchResultItem> {
        val renderers = if (isContinuation) {
            val continuationRoot = root["continuationContents"] as? JsonObject
            val shelfContents = (continuationRoot?.get("musicShelfContinuation") as? JsonObject)
                ?.get("contents")
            val sectionContents = (continuationRoot?.get("sectionListContinuation") as? JsonObject)
                ?.get("contents")
            val appendActions = root["onResponseReceivedActions"]

            val primaryNodes = listOfNotNull(shelfContents, sectionContents, appendActions)
                .flatMap { collectObjectsByKey(it, "musicResponsiveListItemRenderer") }

            if (primaryNodes.isNotEmpty()) {
                primaryNodes
            } else {
                collectObjectsByKey(root, "musicResponsiveListItemRenderer")
            }
        } else {
            collectObjectsByKey(root, "musicResponsiveListItemRenderer")
        }

        return renderers.mapNotNull { renderer ->
            when (filter) {
                SearchFilterOption.SONGS, SearchFilterOption.VIDEOS -> {
                    parseTrackFromResponsiveItem(renderer)?.let { SearchResultItem.SongResult(it) }
                }
                SearchFilterOption.ALBUMS -> {
                    parseAlbumFromResponsiveItem(renderer)?.let { SearchResultItem.AlbumResult(it) }
                }
                SearchFilterOption.PLAYLISTS -> {
                    parsePlaylistFromResponsiveItem(renderer)?.let { SearchResultItem.PlaylistResult(it) }
                }
                SearchFilterOption.ARTISTS -> {
                    parseArtistFromResponsiveItem(renderer)?.let { SearchResultItem.ArtistResult(it) }
                }
            }
        }
    }

    private fun parseLibraryPlaylists(root: JsonObject): List<PlaylistSummary> {
        val twoRowItems = collectObjectsByKey(root, "musicTwoRowItemRenderer")
            .mapNotNull { parsePlaylistFromTwoRowItem(it) }
        val listItems = collectObjectsByKey(root, "musicResponsiveListItemRenderer")
            .mapNotNull { parsePlaylistFromResponsiveItem(it) }
        return (twoRowItems + listItems).distinctBy { it.id.removePrefix("VL") }
    }

    private fun parseTrackFromResponsiveItem(item: JsonObject): Track? {
        val overlayVideoId = ((((item["overlay"] as? JsonObject)
            ?.get("musicItemThumbnailOverlayRenderer") as? JsonObject)
            ?.get("content") as? JsonObject)
            ?.get("musicPlayButtonRenderer") as? JsonObject)
            ?.let { it["playNavigationEndpoint"] as? JsonObject }
            ?.let { it["watchEndpoint"] as? JsonObject }
            ?.let { (it["videoId"] as? JsonPrimitive)?.contentOrNull }

        val videoId = ((item["playlistItemData"] as? JsonObject)?.get("videoId") as? JsonPrimitive)?.contentOrNull
            ?: overlayVideoId
            ?: item.findStringByKey("videoId")
            ?: return null

        val flexRuns = extractFlexColumnRuns(item)
        val title = flexRuns.getOrNull(0)?.joinText().orEmpty().ifBlank { "Untitled Track" }
        val secondaryRuns = flexRuns.getOrNull(1) ?: emptyList()
        val secondaryParts = splitRunsByBullet(secondaryRuns)

        val filteredParts = secondaryParts.filterNot { part ->
            val lower = part.lowercase(Locale.US)
            lower == "song" ||
                    lower == "video" ||
                    lower == "album" ||
                    lower == "single" ||
                    lower == "ep" ||
                    lower == "playlist" ||
                    lower.endsWith("plays") ||
                    lower.endsWith("views") ||
                    parseDurationSeconds(part) != null
        }
        val artist = filteredParts.firstOrNull().orEmpty().ifBlank {
            secondaryRuns.joinText().ifBlank { "YouTube Music" }
        }
        val album = filteredParts.getOrNull(1)
            ?: flexRuns.drop(2)
                .flatMap { splitRunsByBullet(it) }
                .firstOrNull { part ->
                    val lower = part.lowercase(Locale.US)
                    lower.isNotEmpty() &&
                            lower != "song" &&
                            lower != "video" &&
                            !lower.endsWith("plays") &&
                            !lower.endsWith("views") &&
                            parseDurationSeconds(part) == null
                }

        val durationSeconds = secondaryParts
            .asReversed()
            .firstNotNullOfOrNull { parseDurationSeconds(it) }
            ?: extractFixedColumnText(item)?.let { parseDurationSeconds(it) }

        val thumbnailUrl = extractBestThumbnail(item["thumbnail"])
        val browseIds = collectObjectsByKey(item, "browseEndpoint")
            .mapNotNull { (it["browseId"] as? JsonPrimitive)?.contentOrNull }
        val artistId = browseIds.firstOrNull { it.startsWith("UC") }
        val albumId = browseIds.firstOrNull { it.startsWith("MPRE") }

        return Track(
            videoId = videoId,
            title = title,
            artist = artist,
            album = album,
            durationSeconds = durationSeconds,
            thumbnailUrl = thumbnailUrl,
            artistId = artistId,
            albumId = albumId,
        )
    }

    private fun parseAlbumFromResponsiveItem(item: JsonObject): AlbumSummary? {
        val browseId = ((item["navigationEndpoint"] as? JsonObject)
            ?.get("browseEndpoint") as? JsonObject)
            ?.get("browseId")
            ?.let { (it as? JsonPrimitive)?.contentOrNull }
            ?: return null

        val flexRuns = extractFlexColumnRuns(item)
        val title = flexRuns.getOrNull(0)?.joinText().orEmpty().ifBlank { return null }
        val secondaryParts = splitRunsByBullet(flexRuns.getOrNull(1) ?: emptyList())
        val artist = secondaryParts.firstOrNull {
            !it.equals("Album", true) && !it.equals("EP", true) && !it.equals("Single", true)
        } ?: "Unknown Artist"
        val year = secondaryParts.lastOrNull()?.toIntOrNull()
        val playlistId = item.findStringByKey("playlistId")

        return AlbumSummary(
            browseId = browseId,
            playlistId = playlistId,
            title = title,
            artist = artist,
            year = year,
            thumbnailUrl = extractBestThumbnail(item["thumbnail"]),
        )
    }

    private fun parsePlaylistFromResponsiveItem(item: JsonObject): PlaylistSummary? {
        val browseId = ((item["navigationEndpoint"] as? JsonObject)
            ?.get("browseEndpoint") as? JsonObject)
            ?.get("browseId")
            ?.let { (it as? JsonPrimitive)?.contentOrNull }
            ?: item.findStringByKey("playlistId")?.let { "VL$it" }
            ?: return null

        val cleanId = browseId.removePrefix("VL")
        val flexRuns = extractFlexColumnRuns(item)
        val title = flexRuns.getOrNull(0)?.joinText().orEmpty().ifBlank { return null }
        val subtitle = flexRuns.getOrNull(1)?.joinText().orEmpty().ifBlank { "Playlist" }

        return PlaylistSummary(
            id = cleanId,
            title = title,
            subtitle = subtitle,
            songCountText = null,
            thumbnailUrl = extractBestThumbnail(item["thumbnail"]),
            isLikedSongs = cleanId == "LM",
        )
    }

    private fun parsePlaylistFromTwoRowItem(item: JsonObject): PlaylistSummary? {
        val browseId = ((item["navigationEndpoint"] as? JsonObject)
            ?.get("browseEndpoint") as? JsonObject)
            ?.get("browseId")
            ?.let { (it as? JsonPrimitive)?.contentOrNull }
            ?: return null

        if (!browseId.startsWith("VL") && browseId != "LM" && !browseId.startsWith("PL")) {
            return null
        }

        val cleanId = browseId.removePrefix("VL")
        val title = extractRunsText(item["title"]).ifBlank { return null }
        val subtitle = extractRunsText(item["subtitle"]).ifBlank { "YouTube Music Playlist" }

        return PlaylistSummary(
            id = cleanId,
            title = title,
            subtitle = subtitle,
            songCountText = null,
            thumbnailUrl = extractBestThumbnail(item["thumbnailRenderer"]),
            isLikedSongs = cleanId == "LM",
        )
    }

    private fun parseArtistFromResponsiveItem(item: JsonObject): ArtistSummary? {
        val directBrowseId = ((item["navigationEndpoint"] as? JsonObject)
            ?.get("browseEndpoint") as? JsonObject)
            ?.get("browseId")
            ?.let { (it as? JsonPrimitive)?.contentOrNull }
            ?.takeIf { it.isNotBlank() }
        val browseId = directBrowseId ?: collectObjectsByKey(item, "browseEndpoint")
            .firstNotNullOfOrNull { ep ->
                (ep["browseId"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.startsWith("UC") }
            }
        ?: return null
        val flexRuns = extractFlexColumnRuns(item)
        val name = flexRuns.getOrNull(0)?.joinText().orEmpty().ifBlank { return null }
        val subtitle = flexRuns.getOrNull(1)?.joinText()
        return ArtistSummary(
            id = browseId,
            name = name,
            subtitle = subtitle,
            thumbnailUrl = extractBestThumbnail(item["thumbnail"]),
        )
    }

    private fun extractTracksFromNode(root: JsonObject): List<Track> {
        val responsive = collectObjectsByKey(root, "musicResponsiveListItemRenderer")
            .mapNotNull { parseTrackFromResponsiveItem(it) }
        if (responsive.isNotEmpty()) return responsive

        val panelTracks = collectObjectsByKey(root, "playlistPanelVideoRenderer")
            .mapNotNull { renderer ->
                val videoId = (renderer["videoId"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
                val title = extractRunsText(renderer["title"]).ifBlank { "Untitled Track" }
                val artist = extractRunsText(renderer["shortBylineText"])
                    .ifBlank { extractRunsText(renderer["longBylineText"]) }
                    .ifBlank { "YouTube Music" }
                val duration = extractRunsText(renderer["lengthText"])
                    .let { parseDurationSeconds(it) }
                val thumb = extractBestThumbnail(renderer["thumbnail"])
                Track(
                    videoId = videoId,
                    title = title,
                    artist = artist,
                    durationSeconds = duration,
                    thumbnailUrl = thumb,
                )
            }
        if (panelTracks.isNotEmpty()) return panelTracks

        val playlistVideos = collectObjectsByKey(root, "playlistVideoRenderer")
            .mapNotNull { renderer ->
                val videoId = (renderer["videoId"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
                val title = extractRunsText(renderer["title"]).ifBlank { "Untitled Track" }
                val artist = extractRunsText(renderer["shortBylineText"])
                    .ifBlank { "YouTube Music" }
                val duration = (renderer["lengthSeconds"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull()
                    ?: extractRunsText(renderer["lengthText"]).let { parseDurationSeconds(it) }
                val thumb = extractBestThumbnail(renderer["thumbnail"])
                Track(
                    videoId = videoId,
                    title = title,
                    artist = artist,
                    durationSeconds = duration,
                    thumbnailUrl = thumb,
                )
            }
        if (playlistVideos.isNotEmpty()) return playlistVideos

        return collectObjectsByKey(root, "musicTwoRowItemRenderer")
            .mapNotNull { renderer ->
                val videoId = renderer.findStringByKey("videoId") ?: return@mapNotNull null
                val title = extractRunsText(renderer["title"]).ifBlank { "Untitled Track" }
                val artist = extractRunsText(renderer["subtitle"]).ifBlank { "YouTube Music" }
                val thumb = extractBestThumbnail(renderer["thumbnailRenderer"])
                Track(
                    videoId = videoId,
                    title = title,
                    artist = artist,
                    thumbnailUrl = thumb,
                )
            }
    }

    private fun extractPlaylistHeader(root: JsonObject): PlaylistHeaderMeta {
        val responsiveHeader = collectObjectsByKey(root, "musicResponsiveHeaderRenderer").firstOrNull()
        if (responsiveHeader != null) {
            return PlaylistHeaderMeta(
                title = extractRunsText(responsiveHeader["title"]).ifBlank { null },
                subtitle = extractRunsText(responsiveHeader["subtitle"]).ifBlank { null },
                thumbnailUrl = extractBestThumbnail(responsiveHeader["thumbnail"]),
            )
        }
        val detailHeader = collectObjectsByKey(root, "musicDetailHeaderRenderer").firstOrNull()
        if (detailHeader != null) {
            return PlaylistHeaderMeta(
                title = extractRunsText(detailHeader["title"]).ifBlank { null },
                subtitle = extractRunsText(detailHeader["subtitle"]).ifBlank { null },
                thumbnailUrl = extractBestThumbnail(detailHeader["thumbnail"]),
            )
        }
        return PlaylistHeaderMeta(null, null, null)
    }

    private fun extractContinuationToken(root: JsonObject): String? {
        fun fromContinuationsArray(array: JsonArray?): String? {
            if (array == null) return null
            for (el in array) {
                val obj = el as? JsonObject ?: continue
                val token = ((obj["nextContinuationData"] as? JsonObject)
                    ?.get("continuation") as? JsonPrimitive)
                    ?.contentOrNull
                if (!token.isNullOrBlank()) return token
            }
            return null
        }

        collectObjectsByKey(root, "musicShelfRenderer").forEach { shelf ->
            fromContinuationsArray(shelf["continuations"] as? JsonArray)?.let { return it }
        }

        collectObjectsByKey(root, "musicShelfContinuation").forEach { shelf ->
            fromContinuationsArray(shelf["continuations"] as? JsonArray)?.let { return it }
        }

        collectObjectsByKey(root, "sectionListRenderer").forEach { sectionList ->
            fromContinuationsArray(sectionList["continuations"] as? JsonArray)?.let { return it }
        }

        collectObjectsByKey(root, "sectionListContinuation").forEach { sectionList ->
            fromContinuationsArray(sectionList["continuations"] as? JsonArray)?.let { return it }
        }

        collectObjectsByKey(root, "musicPlaylistShelfRenderer").forEach { playlistShelf ->
            fromContinuationsArray(playlistShelf["continuations"] as? JsonArray)?.let { return it }
        }

        val nextCont = (collectObjectsByKey(root, "nextContinuationData").firstOrNull()
            ?.get("continuation") as? JsonPrimitive)?.contentOrNull
        if (!nextCont.isNullOrBlank()) return nextCont

        collectObjectsByKey(root, "continuationItemRenderer").forEach { itemRenderer ->
            val token = (((itemRenderer["continuationEndpoint"] as? JsonObject)
                ?.get("continuationCommand") as? JsonObject)
                ?.get("token") as? JsonPrimitive)
                ?.contentOrNull
            if (!token.isNullOrBlank()) return token
        }

        return (collectObjectsByKey(root, "continuationCommand").firstOrNull()
            ?.get("token") as? JsonPrimitive)?.contentOrNull
    }

    private fun extractFlexColumnRuns(item: JsonObject): List<List<String>> {
        val cols = item["flexColumns"] as? JsonArray ?: return emptyList()
        return cols.map { col ->
            val colObj = col as? JsonObject
            val renderer = colObj?.get("musicResponsiveListItemFlexColumnRenderer") as? JsonObject
            val textObj = renderer?.get("text") as? JsonObject
            val runs = textObj?.get("runs") as? JsonArray
            if (runs != null) {
                runs.mapNotNull { run ->
                    ((run as? JsonObject)?.get("text") as? JsonPrimitive)?.contentOrNull
                }
            } else {
                val simple = (textObj?.get("simpleText") as? JsonPrimitive)?.contentOrNull
                if (!simple.isNullOrBlank()) listOf(simple) else emptyList()
            }
        }
    }

    private fun extractFixedColumnText(item: JsonObject): String? {
        val cols = item["fixedColumns"] as? JsonArray ?: return null
        return (cols.firstOrNull() as? JsonObject)
            ?.let { it["musicResponsiveListItemFixedColumnRenderer"] as? JsonObject }
            ?.get("text")
            ?.let { extractRunsText(it) }
    }

    private fun splitRunsByBullet(runs: List<String>): List<String> {
        val result = mutableListOf<String>()
        val buffer = StringBuilder()
        for (token in runs) {
            if (token.trim() == "•" || token.trim() == "·") {
                val built = buffer.toString().trim()
                if (built.isNotEmpty()) result += built
                buffer.clear()
            } else {
                buffer.append(token)
            }
        }
        val tail = buffer.toString().trim()
        if (tail.isNotEmpty()) result += tail
        return result
    }

    private fun List<String>.joinText(): String = joinToString(separator = "").trim()

    private fun extractRunsText(element: JsonElement?): String {
        val obj = element as? JsonObject ?: return ""
        val simpleText = (obj["simpleText"] as? JsonPrimitive)?.contentOrNull
        if (!simpleText.isNullOrBlank()) return simpleText.trim()
        val runs = obj["runs"] as? JsonArray ?: return ""
        return runs.mapNotNull { run ->
            ((run as? JsonObject)?.get("text") as? JsonPrimitive)?.contentOrNull
        }.joinToString("").trim()
    }

    private fun extractBestThumbnail(element: JsonElement?): String? {
        val obj = element as? JsonObject ?: return null
        val thumbs = obj.findFirstArrayByKey("thumbnails") ?: return null
        return ((thumbs.lastOrNull() as? JsonObject)
            ?.get("url") as? JsonPrimitive)
            ?.contentOrNull
    }

    private fun parseDurationSeconds(text: String): Int? {
        val clean = text.trim()
        val parts = clean.split(":")
        if (parts.size !in 2..3) return null
        val nums = parts.map { it.toIntOrNull() ?: return null }
        return when (nums.size) {
            2 -> nums[0] * 60 + nums[1]
            3 -> nums[0] * 3600 + nums[1] * 60 + nums[2]
            else -> null
        }
    }

    private fun collectObjectsByKey(element: JsonElement, targetKey: String): List<JsonObject> {
        val results = mutableListOf<JsonObject>()
        fun walk(node: JsonElement) {
            when (node) {
                is JsonObject -> {
                    (node[targetKey] as? JsonObject)?.let { results += it }
                    for ((_, value) in node) {
                        walk(value)
                    }
                }
                is JsonArray -> {
                    for (item in node) {
                        walk(item)
                    }
                }
                else -> Unit
            }
        }
        walk(element)
        return results
    }

    private fun JsonObject.findStringByKey(targetKey: String): String? {
        for ((key, value) in this) {
            if (key == targetKey) {
                (value as? JsonPrimitive)?.contentOrNull?.let { return it }
            }
            when (value) {
                is JsonObject -> value.findStringByKey(targetKey)?.let { return it }
                is JsonArray -> {
                    for (child in value) {
                        if (child is JsonObject) {
                            child.findStringByKey(targetKey)?.let { return it }
                        }
                    }
                }
                else -> Unit
            }
        }
        return null
    }

    private fun JsonObject.findFirstArrayByKey(targetKey: String): JsonArray? {
        for ((key, value) in this) {
            if (key == targetKey && value is JsonArray) return value
            when (value) {
                is JsonObject -> value.findFirstArrayByKey(targetKey)?.let { return it }
                is JsonArray -> {
                    for (child in value) {
                        if (child is JsonObject) {
                            child.findFirstArrayByKey(targetKey)?.let { return it }
                        }
                    }
                }
                else -> Unit
            }
        }
        return null
    }

    data class PlaylistDetailData(
        val title: String?,
        val subtitle: String?,
        val thumbnailUrl: String?,
        val tracks: List<Track>,
    )

    private data class PlaylistHeaderMeta(
        val title: String?,
        val subtitle: String?,
        val thumbnailUrl: String?,
    )

    private companion object {
        const val STREAM_CACHE_TTL_MS = 20 * 60 * 1000L
        const val BALANCED_MAX_BITRATE_BPS = 165_000
    }
}
