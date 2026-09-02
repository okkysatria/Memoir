package com.example.ui.viewmodel

import android.app.Application
import com.example.R
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.core.net.toUri
import androidx.glance.appwidget.updateAll
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.data.database.*
import com.example.data.repository.MemoirRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

sealed class Screen {
    data object Home : Screen()
    data object Explore : Screen()
    data object AlbumList : Screen()
    data object Profile : Screen()
    data class AlbumDetail(val albumId: Long) : Screen()
    data object Settings : Screen()
    data object ManageConnections : Screen()
    data object AddPost : Screen()
    data class EditPost(val post: PostEntity) : Screen()
    data object Backup : Screen()
    data object SmbExplorer : Screen()
    data class ProfileFeed(val initialIndex: Int = 0, val isSavedTab: Boolean = false) : Screen()
    data class ExploreFeed(val postIds: List<Long>, val initialIndex: Int = 0) : Screen()
}

class MemoirViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MemoirRepository
    private val userPrefsRepository: UserPreferencesRepository

    val posts: StateFlow<List<PostEntity>>
    val stories: StateFlow<List<StoryEntity>>
    
    private val _homePosts = MutableStateFlow<List<PostEntity>>(emptyList())
    val homePosts: StateFlow<List<PostEntity>> = _homePosts.asStateFlow()

    private val _homeStories = MutableStateFlow<List<StoryEntity>>(emptyList())
    val homeStories: StateFlow<List<StoryEntity>> = _homeStories.asStateFlow()

    val albums: StateFlow<List<AlbumEntity>>
    val connections: StateFlow<List<SourceConnectionEntity>>
    val highlights: StateFlow<List<StoryHighlightEntity>>

    val pagedPosts: Flow<PagingData<PostEntity>>

    private val _hasSeenOnboarding = MutableStateFlow(true)
    val hasSeenOnboarding: StateFlow<Boolean> = _hasSeenOnboarding.asStateFlow()

    private val _hasGrantedPermissions = MutableStateFlow(false)
    val hasGrantedPermissions: StateFlow<Boolean> = _hasGrantedPermissions.asStateFlow()

    fun finishOnboarding() {
        viewModelScope.launch(Dispatchers.IO) {
            userPrefsRepository.saveHasSeenOnboarding(true)
        }
    }

    fun finishPermissions() {
        viewModelScope.launch(Dispatchers.IO) {
            userPrefsRepository.saveHasGrantedPermissions(true)
        }
    }

    private val _screenStack = MutableStateFlow<List<Screen>>(listOf(Screen.Home))
    val screenStack: StateFlow<List<Screen>> = _screenStack.asStateFlow()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _currentTab = MutableStateFlow("home")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    private val _scrollToTopTrigger = MutableStateFlow(0L)
    val scrollToTopTrigger: StateFlow<Long> = _scrollToTopTrigger.asStateFlow()

    fun triggerScrollToTop() {
        _scrollToTopTrigger.value = System.currentTimeMillis()
    }

    private val _addSheetOpen = MutableStateFlow(false)
    val addSheetOpen: StateFlow<Boolean> = _addSheetOpen.asStateFlow()

    private val _storyViewerOpen = MutableStateFlow(false)
    val storyViewerOpen: StateFlow<Boolean> = _storyViewerOpen.asStateFlow()

    private val _activeStories = MutableStateFlow<List<StoryEntity>>(emptyList())
    val activeStories: StateFlow<List<StoryEntity>> = _activeStories.asStateFlow()

    private val _activeStoryIndex = MutableStateFlow(0)
    val activeStoryIndex: StateFlow<Int> = _activeStoryIndex.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _exploreSortOrder = MutableStateFlow("RANDOM")
    val exploreSortOrder = _exploreSortOrder.asStateFlow()

    private val _exploreDateFilter = MutableStateFlow("ALL_TIME")
    val exploreDateFilter = _exploreDateFilter.asStateFlow()

    private val _exploreSelectedMonth = MutableStateFlow<Int?>(null) 
    val exploreSelectedMonth = _exploreSelectedMonth.asStateFlow()

    private val _exploreSelectedYear = MutableStateFlow<Int?>(null)
    val exploreSelectedYear = _exploreSelectedYear.asStateFlow()

    private val _albumSortOrder = MutableStateFlow("LATEST")
    val albumSortOrder = _albumSortOrder.asStateFlow()

    private val _albumSourceFilter = MutableStateFlow("ALL")
    val albumSourceFilter = _albumSourceFilter.asStateFlow()

    private val _albumSelectedMonth = MutableStateFlow<Int?>(null)
    val albumSelectedMonth = _albumSelectedMonth.asStateFlow()

    private val _albumSelectedYear = MutableStateFlow<Int?>(null)
    val albumSelectedYear = _albumSelectedYear.asStateFlow()

    private val _appPin = MutableStateFlow<String?>(null)
    val appPin: StateFlow<String?> = _appPin.asStateFlow()

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _profileName = MutableStateFlow("Pemilik Memoir")
    val profileName: StateFlow<String> = _profileName.asStateFlow()

    private val _username = MutableStateFlow("me_memoir")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _profileBio = MutableStateFlow("Ini adalah ruang memori offline pribadi saya.")
    val profileBio: StateFlow<String> = _profileBio.asStateFlow()

    private val _profileImagePath = MutableStateFlow<String?>("gradient_sunset")
    val profileImagePath: StateFlow<String?> = _profileImagePath.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(false)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _selectedColorTheme = MutableStateFlow("DEFAULT") 
    val selectedColorTheme: StateFlow<String> = _selectedColorTheme.asStateFlow()

    private val _cacheSizeMb = MutableStateFlow(0.0f)
    val cacheSizeMb: StateFlow<Float> = _cacheSizeMb.asStateFlow()

    private val _language = MutableStateFlow(UserPreferencesRepository.DEFAULT_LANGUAGE)
    val language: StateFlow<String> = _language.asStateFlow()

    init {
        val database = MemoirDatabase.getDatabase(application)
        repository = MemoirRepository(database.memoirDao())
        userPrefsRepository = UserPreferencesRepository(application)

        refreshCacheSize()

        viewModelScope.launch(Dispatchers.IO) {
            userPrefsRepository.userPrefsFlow.collect { prefs ->
                _profileName.value = prefs.profileName
                _username.value = prefs.username
                _profileBio.value = prefs.profileBio
                _profileImagePath.value = prefs.profileImagePath
                _appPin.value = prefs.appPin
                _isAppLocked.value = prefs.appPin != null
                _isDarkMode.value = prefs.isDarkMode
                _selectedColorTheme.value = prefs.colorTheme
                _isBiometricEnabled.value = prefs.isBiometricEnabled
                _hasSeenOnboarding.value = prefs.hasSeenOnboarding
                _hasGrantedPermissions.value = prefs.hasGrantedPermissions
                _language.value = prefs.language
            }
        }

        posts = repository.allPosts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        stories = repository.allStories.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            posts.collect { list ->
                if (_homePosts.value.isEmpty() || _homePosts.value.size != list.size) {
                    _homePosts.value = list.shuffled()
                } else {
                    val currentIds = _homePosts.value.map { it.id }
                    val newList = list.filter { it.id in currentIds }
                        .sortedBy { currentIds.indexOf(it.id) }
                    _homePosts.value = newList
                }
            }
        }
        viewModelScope.launch {
            stories.collect { list ->
                val sorted = list.filter { it.expiresAt > System.currentTimeMillis() }
                    .sortedWith(compareBy<StoryEntity> { it.isWatched }.thenByDescending { it.createdAt })
                _homeStories.value = sorted
            }
        }

        albums = repository.allAlbums.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        connections = repository.allConnections.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        highlights = repository.allHighlights.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        pagedPosts = Pager(
            config = PagingConfig(pageSize = 10, enablePlaceholders = false, initialLoadSize = 20)
        ) { MemoirDatabase.getDatabase(application).memoirDao().getPagedPosts() }
            .flow
            .cachedIn(viewModelScope)

        viewModelScope.launch(Dispatchers.IO) {
            if (repository.getPostCount() == 0) {
                seedDatabase()
            }
        }
    }

    private suspend fun seedDatabase() {
        val conn1 = SourceConnectionEntity(
            type = "SMB",
            name = "Home NAS Server",
            host = "smb://192.168.1.100/Photos",
            status = "ONLINE"
        )
        val conn2 = SourceConnectionEntity(
            type = "GOOGLE_DRIVE",
            name = "Google Drive Backup",
            host = "gdrive://Archive/2025",
            status = "OFFLINE"
        )
        repository.insertConnection(conn1)
        repository.insertConnection(conn2)

        val albumBaliId = repository.insertAlbum(
            AlbumEntity(
                name = "Trip Bali 2025",
                coverPath = "gradient_blue",
                sourceType = "SMB",
                sourcePath = "smb://192.168.1.100/Photos/Trip_Bali"
            )
        )
        val albumCityId = repository.insertAlbum(
            AlbumEntity(
                name = "Sudut Kota",
                coverPath = "gradient_orange",
                sourceType = "LOCAL",
                sourcePath = android.os.Environment.getExternalStorageDirectory().path + "/Pictures/City"
            )
        )

        val baliItems = listOf(
            AlbumItemEntity(albumId = albumBaliId, mediaPath = "gradient_blue", sourceType = "SMB", mimeType = "image"),
            AlbumItemEntity(albumId = albumBaliId, mediaPath = "gradient_sunset", sourceType = "SMB", mimeType = "image"),
            AlbumItemEntity(albumId = albumBaliId, mediaPath = "gradient_temple", sourceType = "SMB", mimeType = "image"),
            AlbumItemEntity(albumId = albumBaliId, mediaPath = "gradient_nature", sourceType = "SMB", mimeType = "image"),
            AlbumItemEntity(albumId = albumBaliId, mediaPath = "gradient_road", sourceType = "SMB", mimeType = "image")
        )
        repository.insertAlbumItems(baliItems)

        val cityItems = listOf(
            AlbumItemEntity(albumId = albumCityId, mediaPath = "gradient_orange", sourceType = "LOCAL", mimeType = "image"),
            AlbumItemEntity(albumId = albumCityId, mediaPath = "gradient_slate", sourceType = "LOCAL", mimeType = "image"),
            AlbumItemEntity(albumId = albumCityId, mediaPath = "gradient_purple", sourceType = "LOCAL", mimeType = "image")
        )
        repository.insertAlbumItems(cityItems)

        repository.insertPost(
            PostEntity(
                caption = "Trip to Bali 2025 🌴",
                location = "Melasti Beach, Bali",
                mediaPaths = listOf("gradient_blue", "gradient_sunset", "gradient_temple"),
                linkedAlbumId = albumBaliId,
                isLiked = true,
                isPinned = true
            )
        )

        repository.insertPost(
            PostEntity(
                caption = "Kopi pagi ini di sudut kota ☕️.",
                location = "Second Floor Cafe, Jakarta",
                mediaPaths = listOf("gradient_orange"),
                linkedAlbumId = albumCityId,
                isLiked = false
            )
        )

        repository.insertStory(
            StoryEntity(
                mediaPath = "gradient_purple",
                isWatched = false,
                caption = "Menikmati indahnya senja malam ini... ✨",
                textColor = "#FFFFFFFF",
                textStyle = "Serif"
            )
        )
    }

    fun resetDatabaseAndReSeed() {
        viewModelScope.launch(Dispatchers.IO) {
            FileUtils.deleteAllMemoirData(getApplication())
            FileUtils.clearAppCache(getApplication())
            repository.clearAllDatabase()
            seedDatabase()
            refreshCacheSize()
            updateWidget()
        }
    }

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value == screen) return
        val currentStack = _screenStack.value
        if (currentStack.lastOrNull() != screen) {
            val trimmedStack = if (currentStack.size >= 10) currentStack.drop(1) else currentStack
            _screenStack.value = trimmedStack + screen
        }
        _currentScreen.value = screen
        when (screen) {
            is Screen.Home -> _currentTab.value = "home"
            is Screen.Explore -> _currentTab.value = "explore"
            is Screen.AlbumList -> _currentTab.value = "album"
            is Screen.Profile -> _currentTab.value = "profile"
            else -> {} 
        }
    }

    fun navigateBack(): Boolean {
        if (_screenStack.value.size > 1) {
            val newStack = _screenStack.value.dropLast(1)
            _screenStack.value = newStack
            val previousScreen = newStack.last()
            _currentScreen.value = previousScreen
            when (previousScreen) {
                is Screen.Home -> _currentTab.value = "home"
                is Screen.Explore -> _currentTab.value = "explore"
                is Screen.AlbumList -> _currentTab.value = "album"
                is Screen.Profile -> _currentTab.value = "profile"
                else -> {}
            }
            return true
        }
        return false
    }

    fun selectTab(tab: String) {
        if (tab == "add") {
            navigateTo(Screen.AddPost)
        } else {
            _currentTab.value = tab
            val screen = when (tab) {
                "home" -> Screen.Home
                "explore" -> Screen.Explore
                "album" -> Screen.AlbumList
                "profile" -> Screen.Profile
                else -> Screen.Home
            }
            _screenStack.value = listOf(screen)
            _currentScreen.value = screen
        }
    }

    fun setAddSheetOpen(open: Boolean) {
        if (open) {
            navigateTo(Screen.AddPost)
        } else {
            if (_currentScreen.value is Screen.AddPost) {
                navigateBack()
            }
        }
        _addSheetOpen.value = open
    }

    fun openStoryViewer(allActiveStories: List<StoryEntity>, startIndex: Int) {
        _activeStories.value = allActiveStories
        _activeStoryIndex.value = startIndex
        _storyViewerOpen.value = true
        if (startIndex < allActiveStories.size) {
            markStoryAsWatched(allActiveStories[startIndex])
        }
    }

    fun closeStoryViewer() {
        _storyViewerOpen.value = false
    }

    fun nextStory() {
        val nextIndex = _activeStoryIndex.value + 1
        if (nextIndex < _activeStories.value.size) {
            _activeStoryIndex.value = nextIndex
            markStoryAsWatched(_activeStories.value[nextIndex])
        } else {
            closeStoryViewer()
        }
    }

    fun prevStory() {
        val prevIndex = _activeStoryIndex.value - 1
        if (prevIndex >= 0) {
            _activeStoryIndex.value = prevIndex
        }
    }

    private fun markStoryAsWatched(story: StoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateStory(story.copy(isWatched = true))
        }
    }

    fun toggleLike(post: PostEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePost(post.copy(isLiked = !post.isLiked))
        }
    }

    fun toggleSave(post: PostEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePost(post.copy(isSaved = !post.isSaved))
        }
    }

    fun togglePin(post: PostEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePost(post.copy(isPinned = !post.isPinned))
        }
    }

    fun deletePost(post: PostEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePost(post)
            updateWidget()
        }
    }

    fun getPostComments(postId: Long): Flow<List<CommentEntity>> {
        return repository.getCommentsForPost(postId)
    }

    fun addComment(postId: Long, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertComment(CommentEntity(postId = postId, text = text))
        }
    }

    fun deleteComment(comment: CommentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteComment(comment)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setExploreSortOrder(order: String) { _exploreSortOrder.value = order }
    fun setExploreDateFilter(filter: String) { 
        _exploreDateFilter.value = filter 
        if (filter == "ALL_TIME") {
            _exploreSelectedMonth.value = null
            _exploreSelectedYear.value = null
        }
    }
    fun setExploreSelectedMonth(month: Int?) { _exploreSelectedMonth.value = month }
    fun setExploreSelectedYear(year: Int?) { _exploreSelectedYear.value = year }

    fun setAlbumSortOrder(order: String) { _albumSortOrder.value = order }
    fun setAlbumSourceFilter(filter: String) {
        _albumSourceFilter.value = filter
    }
    fun setAlbumSelectedMonth(month: Int?) { _albumSelectedMonth.value = month }
    fun setAlbumSelectedYear(year: Int?) { _albumSelectedYear.value = year }

    fun updatePostFull(
        post: PostEntity,
        caption: String,
        location: String,
        mediaPaths: List<String>,
        existingAlbumId: Long? = null,
        createdAt: Long,
        aspectRatio: String,
        mediaTransformsJson: String,
        isPinned: Boolean,
        isLocked: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePost(
                post.copy(
                    caption = caption,
                    location = location,
                    mediaPaths = mediaPaths,
                    linkedAlbumId = existingAlbumId,
                    createdAt = createdAt,
                    aspectRatio = aspectRatio,
                    mediaTransformsJson = mediaTransformsJson,
                    isPinned = isPinned,
                    isLocked = isLocked
                )
            )
            updateWidget()
        }
    }

    fun reshuffle() {
        _homePosts.value = posts.value.shuffled()
        _homeStories.value = stories.value.filter { it.expiresAt > System.currentTimeMillis() }.shuffled()
    }

    private fun updateWidget() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.example.glance.MemoirWidget().updateAll(getApplication())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun exportMemoirDataToSmb(
        host: String,
        shareName: String,
        remoteFolder: String = "",
        username: String = "",
        password: String = "",
        onProgress: (String) -> Unit,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            onProgress(app.getString(R.string.msg_export_starting_folder, remoteFolder))
            val res = com.example.utils.SmbHelper.syncMemoirFolderToSmb(
                context = app, host = host, shareName = shareName, remoteBaseFolder = remoteFolder,
                username = username, password = password
            )
            res.fold(
                onSuccess = { count ->
                    val msg = app.getString(R.string.msg_export_success_files, count, remoteFolder)
                    onProgress(msg)
                    onResult(true, msg)
                },
                onFailure = { err ->
                    val msg = "❌ " + app.getString(R.string.msg_export_folder_failed, err.localizedMessage)
                    onProgress(msg)
                    onResult(false, msg)
                }
            )
        }
    }

    fun exportMemoirDataToSmbWithUserFolder(
        host: String,
        shareName: String,
        remoteParentFolder: String = "",
        username: String = "",
        password: String = "",
        onProgress: (String) -> Unit,
        onResult: (Boolean, String) -> Unit
    ) {
        val app = getApplication<Application>()
        val userFolderName = app.getString(R.string.label_memoir_user, _username.value)
        val fullRemotePath = if (remoteParentFolder.isBlank()) userFolderName else "$remoteParentFolder/$userFolderName"
        
        exportMemoirDataToSmb(
            host = host,
            shareName = shareName,
            remoteFolder = fullRemotePath,
            username = username,
            password = password,
            onProgress = onProgress,
            onResult = onResult
        )
    }

    fun importMemoirDataFromSmbUserFolder(
        host: String,
        shareName: String,
        remoteUserFolder: String, 
        username: String = "",
        password: String = "",
        onProgress: (String) -> Unit,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            onProgress(app.getString(R.string.msg_importing_from, remoteUserFolder))
            val db = MemoirDatabase.getDatabase(app)
            val res = com.example.utils.SmbHelper.importMemoirFolderFromSmb(
                context = app, dao = db.memoirDao(), host = host, shareName = shareName,
                remoteFolder = remoteUserFolder, username = username, password = password
            )
            res.fold(
                onSuccess = { count ->
                    val msg = app.getString(R.string.msg_import_success_from, count, remoteUserFolder)
                    onProgress(msg)
                    onResult(true, msg)
                },
                onFailure = { err ->
                    val msg = "❌ " + app.getString(R.string.msg_import_folder_failed, err.localizedMessage)
                    onProgress(msg)
                    onResult(false, msg)
                }
            )
        }
    }

    fun publishPost(
        caption: String,
        location: String,
        mediaPaths: List<String>,
        albumName: String?,
        sourceType: String,
        sourcePath: String,
        existingAlbumId: Long? = null,
        createdAt: Long = System.currentTimeMillis(),
        aspectRatio: String = "1:1",
        mediaTransformsJson: String = "",
        isPinned: Boolean = false,
        isLocked: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            var albumId: Long? = existingAlbumId
            if (!albumName.isNullOrBlank()) {
                albumId = repository.insertAlbum(AlbumEntity(name = albumName, coverPath = mediaPaths.firstOrNull() ?: "gradient_slate", sourceType = sourceType, sourcePath = sourcePath))
                val items = mediaPaths.map {
                    AlbumItemEntity(albumId = albumId!!, mediaPath = it, sourceType = sourceType, mimeType = if (FileUtils.isVideoPath(it)) "video" else "image")
                }
                repository.insertAlbumItems(items)
            } else if (existingAlbumId != null) {
                val items = mediaPaths.map {
                    AlbumItemEntity(albumId = existingAlbumId, mediaPath = it, sourceType = sourceType, mimeType = if (FileUtils.isVideoPath(it)) "video" else "image")
                }
                repository.insertAlbumItems(items)
            }
            repository.insertPost(PostEntity(caption = caption, location = location, mediaPaths = mediaPaths, linkedAlbumId = albumId, createdAt = createdAt, aspectRatio = aspectRatio, mediaTransformsJson = mediaTransformsJson, isPinned = isPinned, isLocked = isLocked))
            updateWidget()
        }
    }

    fun publishStory(
        mediaPath: String,
        caption: String? = null,
        textColor: String? = null,
        textStyle: String? = null,
        createdAt: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            var finalStoryMediaPath = mediaPath
            if (!mediaPath.startsWith("gradient_")) {
                val storyFolder = FileUtils.createStoryFolder(app, createdAt, java.util.UUID.randomUUID().toString().take(6))
                val isVideo = FileUtils.isVideoPath(mediaPath)
                val ext = if (isVideo) ".mp4" else ".jpg"
                val destFile = java.io.File(storyFolder, "media$ext")
                try {
                    if (mediaPath.startsWith("content://")) {
                        app.contentResolver.openInputStream(mediaPath.toUri())?.use { input -> java.io.FileOutputStream(destFile).use { output -> input.copyTo(output) } }
                    } else {
                        val srcFile = FileUtils.getLocalFile(app, mediaPath)
                        if (srcFile.exists() && srcFile.absolutePath != destFile.absolutePath) {
                            srcFile.copyTo(destFile, overwrite = true)
                        }
                    }
                    finalStoryMediaPath = "stories/${storyFolder.name}/media$ext"
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                val expiresAt = createdAt + 24 * 60 * 60 * 1000L
                FileUtils.writeStoryMetadata(storyFolder = storyFolder, storyId = 0, caption = caption, textColor = textColor, textStyle = textStyle, createdAt = createdAt, expiresAt = expiresAt, mediaFileName = "media$ext")
            }
            repository.insertStory(StoryEntity(mediaPath = finalStoryMediaPath, caption = caption, textColor = textColor, textStyle = textStyle, createdAt = createdAt, expiresAt = createdAt + 24 * 60 * 60 * 1000L))
            updateWidget()
        }
    }

    fun addConnection(name: String, type: String, host: String, folderPath: String, credentials: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertConnection(SourceConnectionEntity(type = type, name = name, host = host, folderPath = folderPath, credentials = credentials, status = "ONLINE"))
        }
    }

    fun testSmbConnection(
        host: String,
        shareName: String,
        username: String = "",
        password: String = "",
        domain: String = "",
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val res = com.example.utils.SmbHelper.testConnection(app, host, shareName, username, password, domain)
            res.fold(
                onSuccess = { onResult(true, app.getString(R.string.msg_test_conn_success)) },
                onFailure = { onResult(false, it.localizedMessage ?: app.getString(R.string.msg_test_conn_failed)) }
            )
        }
    }

    fun syncSmbMedia(
        connection: SourceConnectionEntity,
        shareName: String,
        username: String = "",
        password: String = "",
        domain: String = "",
        onProgress: (String) -> Unit
    ) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            onProgress(app.getString(R.string.msg_sync_searching, "${connection.host}/$shareName"))
            val listRes = com.example.utils.SmbHelper.listMediaFiles(host = connection.host, shareName = shareName, folderPath = connection.folderPath, username = username, password = password, domain = domain)
            listRes.fold(
                onSuccess = { items ->
                    if (items.isEmpty()) {
                        onProgress(app.getString(R.string.msg_sync_no_files))
                        return@fold
                    }
                    onProgress(app.getString(R.string.msg_sync_downloading, items.size))
                    val downloadedPaths = mutableListOf<String>()
                    for ((idx, item) in items.withIndex()) {
                        onProgress(app.getString(R.string.msg_sync_item_progress, idx + 1, items.size, item.name))
                        val dlRes = com.example.utils.SmbHelper.downloadMediaToCache(app, host = connection.host, shareName = shareName, remoteFilePath = item.fullPath, username = username, password = password, domain = domain)
                        dlRes.onSuccess { file -> downloadedPaths.add(file.absolutePath) }
                    }
                    if (downloadedPaths.isNotEmpty()) {
                        val albumId = repository.insertAlbum(AlbumEntity(name = connection.name, coverPath = downloadedPaths.first(), sourceType = "SMB", sourcePath = "${connection.host}/$shareName/${connection.folderPath}"))
                        downloadedPaths.forEach { path ->
                            repository.insertAlbumItem(AlbumItemEntity(albumId = albumId, mediaPath = path, sourceType = "SMB", mimeType = if (FileUtils.isVideoPath(path)) "video" else "image"))
                        }
                        repository.insertPost(PostEntity(caption = app.getString(R.string.label_nas_import_caption, connection.name), mediaPaths = downloadedPaths, location = connection.host, linkedAlbumId = albumId))
                        repository.updateConnection(connection.copy(status = "ONLINE"))
                        onProgress(app.getString(R.string.msg_sync_success_album, downloadedPaths.size))
                    } else {
                        onProgress(app.getString(R.string.msg_sync_failed_media))
                    }
                },
                onFailure = { err ->
                    repository.updateConnection(connection.copy(status = "OFFLINE"))
                    onProgress("Error SMB: ${err.localizedMessage}")
                }
            )
        }
    }

    fun deleteConnection(connection: SourceConnectionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteConnection(connection)
        }
    }

    fun createAlbum(name: String, coverPath: String, sourceType: String, sourcePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertAlbum(AlbumEntity(name = name, coverPath = coverPath, sourceType = sourceType, sourcePath = sourcePath))
        }
    }

    fun addItemsToAlbum(albumId: Long, mediaPaths: List<String>, sourceType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val items = mediaPaths.map {
                AlbumItemEntity(albumId = albumId, mediaPath = it, sourceType = sourceType, mimeType = "image")
            }
            repository.insertAlbumItems(items)
        }
    }

    fun getAlbumById(albumId: Long): Flow<AlbumEntity?> = repository.getAlbumById(albumId)

    fun getItemsForAlbum(albumId: Long): Flow<List<AlbumItemEntity>> = repository.getItemsForAlbum(albumId)

    fun deleteAlbum(album: AlbumEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAlbum(album)
        }
    }

    fun deleteAlbumItem(item: AlbumItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAlbumItem(item)
        }
    }

    fun updateProfile(name: String, uname: String, bio: String, imagePath: String? = null) {
        _profileName.value = name
        _username.value = uname
        _profileBio.value = bio
        imagePath?.let { _profileImagePath.value = it }
        viewModelScope.launch(Dispatchers.IO) {
            userPrefsRepository.saveProfileName(name)
            userPrefsRepository.saveUsername(uname)
            userPrefsRepository.saveProfileBio(bio)
            imagePath?.let { userPrefsRepository.saveProfileImagePath(it) }
        }
    }

    fun addHighlight(name: String, coverPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertHighlight(StoryHighlightEntity(name = name, coverPath = coverPath))
        }
    }

    fun addHighlightWithStories(name: String, coverPath: String, storyIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            val highlightId = repository.insertHighlight(StoryHighlightEntity(name = name, coverPath = coverPath))
            stories.value.forEach { story ->
                if (storyIds.contains(story.id)) {
                    repository.updateStory(story.copy(highlightId = highlightId))
                }
            }
        }
    }

    fun deleteHighlight(highlight: StoryHighlightEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteHighlight(highlight)
        }
    }

    fun updateHighlight(highlight: StoryHighlightEntity, newName: String, newCoverPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertHighlight(highlight.copy(name = newName, coverPath = newCoverPath))
        }
    }

    fun linkStoryToHighlight(story: StoryEntity, highlightId: Long?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateStory(story.copy(highlightId = highlightId))
        }
    }

    fun deleteStory(story: StoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteStory(story)
            updateWidget()
        }
    }

    fun setAppPin(pin: String?) {
        _appPin.value = pin
        _isAppLocked.value = pin != null
        viewModelScope.launch(Dispatchers.IO) { userPrefsRepository.saveAppPin(pin) }
    }

    fun toggleBiometric() {
        _isBiometricEnabled.value = !_isBiometricEnabled.value
        viewModelScope.launch(Dispatchers.IO) { userPrefsRepository.saveBiometric(_isBiometricEnabled.value) }
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
        viewModelScope.launch(Dispatchers.IO) { userPrefsRepository.saveDarkMode(_isDarkMode.value) }
    }

    fun setColorTheme(theme: String) {
        _selectedColorTheme.value = theme
        viewModelScope.launch(Dispatchers.IO) { userPrefsRepository.saveColorTheme(theme) }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            FileUtils.clearAppCache(getApplication())
            refreshCacheSize()
        }
    }

    private fun refreshCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            _cacheSizeMb.value = FileUtils.getCacheSize(getApplication())
        }
    }

    fun changeLanguage(langCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userPrefsRepository.saveLanguage(langCode)
        }
    }

    fun unlockApp(pin: String): Boolean {
        return if (pin == _appPin.value) {
            _isAppLocked.value = false
            true
        } else {
            false
        }
    }

    fun unlockAppWithBiometric() {
        _isAppLocked.value = false
    }
}

class MemoirViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MemoirViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MemoirViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
