package com.example.ui.components

import android.app.DatePickerDialog
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.screens.profile.UCropContract
import com.example.ui.screens.profile.UCropOptions
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.database.AlbumEntity
import com.example.data.database.PostEntity
import com.example.data.database.SourceConnectionEntity
import com.example.ui.viewmodel.MemoirViewModel
import com.example.ui.viewmodel.Screen
import com.example.utils.FileUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

private val instagramBlue = Color(0xFF0095F6)

data class MediaTransformData(
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val filterName: String = "Normal",
    val viewportWidth: Float = 0f,
    val viewportHeight: Float = 0f,
    val isContentFit: Boolean = false
)

data class GalleryMediaItem(
    val uriString: String,
    val isVideo: Boolean,
    val durationSeconds: Long = 0,
    val albumName: String = ""
)

data class GalleryAlbum(
    val name: String,
    val count: Int = 0,
    val thumbnailUri: String? = null
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AddPostScreen(
    viewModel: MemoirViewModel,
    initialPost: PostEntity? = null,
    onDismiss: () -> Unit = { viewModel.navigateBack() },
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(1) }
    var isPublishing by remember { mutableStateOf(false) }
    val isPublishingState = rememberUpdatedState(isPublishing)
    var lastBackTime by remember { mutableLongStateOf(0L) }
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary

    val processedMediaPaths = remember { 
        mutableStateListOf<String>().apply {
            initialPost?.mediaPaths?.let { addAll(it) }
        }
    }
    var processedMediaTransformsJson by remember { 
        mutableStateOf(initialPost?.mediaTransformsJson ?: "[]") 
    }
    var currentPostFolder by remember { mutableStateOf<File?>(null) }
    var isProcessingMedia by remember { mutableStateOf(false) }
    val isProcessingMediaState = rememberUpdatedState(isProcessingMedia)

    androidx.activity.compose.BackHandler {
        if (!isPublishingState.value && !isProcessingMediaState.value) {
            val now = System.currentTimeMillis()
            if (now - lastBackTime < 2000) {
                if (step > 1) step-- else onDismiss()
            } else {
                lastBackTime = now
                val msg = if (step > 1) context.getString(R.string.msg_back_confirm) else context.getString(R.string.msg_cancel_confirm)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val selectedMedia = remember { 
        mutableStateListOf<String>().apply {
            initialPost?.mediaPaths?.let { addAll(it) }
        }
    }
    var selectedRatio by remember { 
        mutableStateOf(
            if (initialPost?.aspectRatio == "Asli" || initialPost?.aspectRatio == "Original" || initialPost?.aspectRatio == "ORIGINAL") "ORIGINAL" 
            else initialPost?.aspectRatio ?: "ORIGINAL"
        ) 
    }
    var isContentFit by remember { mutableStateOf(false) }
    var multiSelectMode by remember { mutableStateOf((initialPost?.mediaPaths?.size ?: 0) > 1) }
    val mediaTransforms = remember { 
        mutableStateMapOf<String, MediaTransformData>().apply {
            initialPost?.let { post ->
                try {
                    val arr = org.json.JSONArray(post.mediaTransformsJson)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val path = post.mediaPaths.getOrNull(i) ?: continue
                        put(path, MediaTransformData(
                            scale = obj.optDouble("scale", 1.0).toFloat(),
                            rotation = obj.optDouble("rotation", 0.0).toFloat(),
                            offsetX = obj.optDouble("offsetX", 0.0).toFloat(),
                            offsetY = obj.optDouble("offsetY", 0.0).toFloat(),
                            filterName = obj.optString("filterName", "Normal"),
                            viewportWidth = obj.optDouble("viewportWidth", 0.0).toFloat(),
                            viewportHeight = obj.optDouble("viewportHeight", 0.0).toFloat(),
                            isContentFit = obj.optBoolean("isContentFit", false)
                        ))
                    }
                } catch (e: Exception) { }
            }
        }
    }

    var caption by remember { mutableStateOf(initialPost?.caption ?: "") }
    var location by remember { mutableStateOf(initialPost?.location ?: "") }
    var createNewAlbum by remember { mutableStateOf(false) }
    var albumName by remember { mutableStateOf("") }
    var sourceType by remember { mutableStateOf("LOCAL") }
    var sourcePath by remember { mutableStateOf("") }
    var selectedExistingAlbumId by remember { mutableStateOf(initialPost?.linkedAlbumId) }
    var postDate by remember { mutableLongStateOf(initialPost?.createdAt ?: System.currentTimeMillis()) }
    var isPinned by remember { mutableStateOf(initialPost?.isPinned ?: false) }
    var isLocked by remember { mutableStateOf(initialPost?.isLocked ?: false) }
    var mediaToRemove by remember { mutableStateOf<String?>(null) }
    var showCameraDialog by remember { mutableStateOf(false) }
    var showConnectionBrowser by remember { mutableStateOf(false) }
    var selectedBrowserType by remember { mutableStateOf("SMB") }

    val coroutineScope = rememberCoroutineScope()
    val connections by viewModel.connections.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                val localPaths = withContext(Dispatchers.IO) {
                    uris.map { uri -> FileUtils.saveUriToInternalStorage(context, uri) ?: uri.toString() }
                }
                localPaths.forEach { localPath ->
                    if (!selectedMedia.contains(localPath)) selectedMedia.add(localPath)
                }
                if (step == 1 && selectedMedia.isNotEmpty()) step = 2
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 0.5.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (step > 1) {
                            IconButton(onClick = { if (!isPublishing && !isProcessingMedia) step-- }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.btn_cancel), tint = MaterialTheme.colorScheme.onSurface)
                            }
                        } else {
                            IconButton(onClick = { if (!isPublishing && !isProcessingMedia) onDismiss() }) {
                                Icon(Icons.Default.Close, stringResource(R.string.btn_done), tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        Text(
                            text = when (step) {
                                1 -> stringResource(R.string.step_select_media)
                                2 -> stringResource(R.string.step_adjust_media)
                                else -> if (initialPost != null) stringResource(R.string.step_edit_memory) else stringResource(R.string.step_share_memory)
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (step < 3) {
                        TextButton(
                            onClick = {
                                if (selectedMedia.isNotEmpty()) {
                                    if (step == 2) {
                                        isProcessingMedia = true
                                        coroutineScope.launch {
                                            try {
                                                val postFolder = withContext(Dispatchers.IO) {
                                                    currentPostFolder ?: if (initialPost != null) {
                                                        val oldPath = initialPost.mediaPaths.firstOrNull() ?: ""
                                                        if (oldPath.startsWith("posts/")) {
                                                            val folderName = oldPath.split("/").getOrNull(1)
                                                            if (folderName != null) {
                                                                File(FileUtils.getPostsDir(context), folderName)
                                                            } else FileUtils.createPostFolder(context, postDate, java.util.UUID.randomUUID().toString().take(6))
                                                        } else FileUtils.createPostFolder(context, postDate, java.util.UUID.randomUUID().toString().take(6))
                                                    } else {
                                                        FileUtils.createPostFolder(context, postDate, java.util.UUID.randomUUID().toString().take(6))
                                                    }
                                                }
                                                currentPostFolder = postFolder

                                                val mediaMetadataArray = JSONArray()
                                                val finalMediaPaths = withContext(Dispatchers.IO) {
                                                    selectedMedia.mapIndexed { idx, path ->
                                                        val t = mediaTransforms[path] ?: MediaTransformData()
                                                        val (relPath, metaObj) = FileUtils.processAndSaveMediaInFolder(
                                                            context = context,
                                                            targetFolder = postFolder,
                                                            fileNameWithoutExt = "media_$idx",
                                                            pathOrUri = path,
                                                            targetRatioStr = selectedRatio,
                                                            scale = t.scale,
                                                            rotation = t.rotation,
                                                            offsetX = t.offsetX,
                                                            offsetY = t.offsetY,
                                                            vw = t.viewportWidth,
                                                            vh = t.viewportHeight,
                                                            isContentFit = t.isContentFit
                                                        )
                                                        metaObj.put("filterName", t.filterName)
                                                        mediaMetadataArray.put(metaObj)
                                                        relPath
                                                    }
                                                }
                                                processedMediaPaths.clear()
                                                processedMediaPaths.addAll(finalMediaPaths)
                                                processedMediaTransformsJson = mediaMetadataArray.toString()
                                                isProcessingMedia = false
                                                step = 3
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                isProcessingMedia = false
                                                Toast.makeText(context, (context.getString(R.string.operation_result) + ": " + (e.localizedMessage ?: "")), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        step++
                                    }
                                } else {
                                    Toast.makeText(context, context.getString(R.string.msg_min_media), Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = selectedMedia.isNotEmpty() && !isProcessingMedia && !isPublishing
                        ) {
                            if (isProcessingMedia && step == 2) {
                                CircularProgressIndicator(color = primaryColor, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text(
                                    stringResource(R.string.btn_next), 
                                    fontWeight = FontWeight.Bold, 
                                    fontSize = 15.sp, 
                                    color = if (selectedMedia.isNotEmpty()) primaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                        }
                    } else {
                        Button(
                            modifier = Modifier.testTag("publish_post_button"),
                            enabled = !isPublishing && !isProcessingMedia && (processedMediaPaths.isNotEmpty() || selectedMedia.isNotEmpty()),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(8.dp),
                            onClick = {
                                isPublishing = true
                                coroutineScope.launch {
                                    try {
                                        val postFolder = withContext(Dispatchers.IO) {
                                            currentPostFolder ?: if (initialPost != null) {
                                                val oldPath = initialPost.mediaPaths.firstOrNull() ?: ""
                                                if (oldPath.startsWith("posts/")) {
                                                    val folderName = oldPath.split("/").getOrNull(1)
                                                    if (folderName != null) {
                                                        File(FileUtils.getPostsDir(context), folderName)
                                                    } else FileUtils.createPostFolder(context, postDate, java.util.UUID.randomUUID().toString().take(6))
                                                } else FileUtils.createPostFolder(context, postDate, java.util.UUID.randomUUID().toString().take(6))
                                            } else {
                                                FileUtils.createPostFolder(context, postDate, java.util.UUID.randomUUID().toString().take(6))
                                            }
                                        }
                                        currentPostFolder = postFolder

                                        val (finalMediaPaths, finalMetaArray) = if (processedMediaPaths.isNotEmpty()) {
                                            Pair(processedMediaPaths.toList(), JSONArray(processedMediaTransformsJson.ifEmpty { "[]" }))
                                        } else {
                                            val mediaMetadataArray = JSONArray()
                                            val baked = withContext(Dispatchers.IO) {
                                                selectedMedia.mapIndexed { idx, path ->
                                                    val t = mediaTransforms[path] ?: MediaTransformData()
                                                    val (relPath, metaObj) = FileUtils.processAndSaveMediaInFolder(
                                                        context = context,
                                                        targetFolder = postFolder,
                                                        fileNameWithoutExt = "media_$idx",
                                                        pathOrUri = path,
                                                        targetRatioStr = selectedRatio,
                                                        scale = t.scale,
                                                        rotation = t.rotation,
                                                        offsetX = t.offsetX,
                                                        offsetY = t.offsetY,
                                                        vw = t.viewportWidth,
                                                        vh = t.viewportHeight,
                                                        isContentFit = t.isContentFit
                                                    )
                                                    metaObj.put("filterName", t.filterName)
                                                    mediaMetadataArray.put(metaObj)
                                                    relPath
                                                }
                                            }
                                            Pair(baked, mediaMetadataArray)
                                        }

                                        val finalRatioToSave = selectedRatio
                                        val albumNameToUse = if (createNewAlbum && albumName.isNotBlank()) albumName.trim() else null

                                        withContext(Dispatchers.IO) {
                                            FileUtils.writePostMetadata(
                                                postFolder = postFolder,
                                                postId = initialPost?.id ?: 0,
                                                caption = caption.trim(),
                                                location = location.trim(),
                                                albumName = albumNameToUse,
                                                createdAt = postDate,
                                                aspectRatio = finalRatioToSave,
                                                isLiked = initialPost?.isLiked ?: false,
                                                isSaved = initialPost?.isSaved ?: false,
                                                isPinned = isPinned,
                                                isLocked = isLocked,
                                                mediaFilesJson = finalMetaArray
                                            )
                                        }

                                        if (initialPost != null) {
                                            viewModel.updatePostFull(
                                                post = initialPost,
                                                caption = caption.trim(),
                                                location = location.trim(),
                                                mediaPaths = finalMediaPaths,
                                                existingAlbumId = if (!createNewAlbum) selectedExistingAlbumId else null,
                                                createdAt = postDate,
                                                aspectRatio = finalRatioToSave,
                                                mediaTransformsJson = finalMetaArray.toString(),
                                                isPinned = isPinned,
                                                isLocked = isLocked
                                            )
                                        } else {
                                            viewModel.publishPost(
                                                caption = caption.trim(),
                                                location = location.trim(),
                                                mediaPaths = finalMediaPaths,
                                                albumName = albumNameToUse,
                                                sourceType = sourceType,
                                                sourcePath = sourcePath,
                                                existingAlbumId = if (!createNewAlbum) selectedExistingAlbumId else null,
                                                createdAt = postDate,
                                                aspectRatio = finalRatioToSave,
                                                mediaTransformsJson = finalMetaArray.toString(),
                                                isPinned = isPinned,
                                                isLocked = isLocked
                                            )
                                        }

                                        isPublishing = false
                                        val toastMsg = if (initialPost != null) context.getString(R.string.msg_memory_updated) else context.getString(R.string.msg_memory_shared)
                                        Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        isPublishing = false
                                        Toast.makeText(context, (context.getString(R.string.operation_result) + ": " + (e.localizedMessage ?: "")), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            if (isPublishing) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            else {
                                val btnText = if (initialPost != null) stringResource(R.string.btn_save) else stringResource(R.string.btn_publish)
                                Text(btnText, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (scaleIn(initialScale = 0.92f, animationSpec = tween(350, easing = LinearOutSlowInEasing)) + fadeIn(animationSpec = tween(350)))
                                .togetherWith(scaleOut(targetScale = 1.08f, animationSpec = tween(350)) + fadeOut(animationSpec = tween(350)))
                        } else {
                            (scaleIn(initialScale = 1.08f, animationSpec = tween(350, easing = LinearOutSlowInEasing)) + fadeIn(animationSpec = tween(350)))
                                .togetherWith(scaleOut(targetScale = 0.92f, animationSpec = tween(350)) + fadeOut(animationSpec = tween(350)))
                        }
                    },
                    label = "AddPostStepTransition"
                ) { currentStep ->
                    when (currentStep) {
                        1 -> StepChooseSource(
                            selectedMedia = selectedMedia,
                            selectedRatio = selectedRatio,
                            onRatioChange = { selectedRatio = it },
                            isContentFit = isContentFit,
                            onContentFitToggle = { isContentFit = !isContentFit },
                            multiSelectMode = multiSelectMode,
                            onMultiSelectToggle = {
                                multiSelectMode = !multiSelectMode
                                if (!multiSelectMode && selectedMedia.size > 1) {
                                    val first = selectedMedia.first()
                                    selectedMedia.clear()
                                    selectedMedia.add(first)
                                }
                            },
                            mediaTransforms = mediaTransforms,
                            onTransformChange = { path, s, r, ox, oy, vw, vh, fit ->
                                val currentFilter = mediaTransforms[path]?.filterName ?: "Normal"
                                mediaTransforms[path] = MediaTransformData(s, r, ox, oy, currentFilter, vw, vh, fit)
                            },
                            onSelectMediaItem = { mediaItemPath ->
                                if (multiSelectMode) {
                                    if (selectedMedia.contains(mediaItemPath)) {
                                        selectedMedia.remove(mediaItemPath)
                                        mediaTransforms.remove(mediaItemPath)
                                    } else {
                                        if (selectedMedia.size >= 10) {
                                            Toast.makeText(context, context.getString(R.string.msg_max_media), Toast.LENGTH_SHORT).show()
                                        } else {
                                            selectedMedia.add(mediaItemPath)
                                        }
                                    }
                                } else {
                                    selectedMedia.clear()
                                    selectedMedia.add(mediaItemPath)
                                }
                            },
                            onPickGallery = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                            onOpenCamera = {
                                if (cameraPermissionState.status.isGranted) showCameraDialog = true
                                else cameraPermissionState.launchPermissionRequest()
                            },
                            onOpenSMB = { selectedBrowserType = "SMB"; showConnectionBrowser = true },
                            onOpenCloud = { selectedBrowserType = "CLOUD"; showConnectionBrowser = true }
                        )
                        2 -> StepAdjustMedia(
                            selectedMedia = selectedMedia,
                            selectedRatio = selectedRatio,
                            onRatioChange = { selectedRatio = it },
                            mediaTransforms = mediaTransforms,
                            onTransformChange = { path, s, r, ox, oy, filter, vw, vh, fit ->
                                mediaTransforms[path] = MediaTransformData(s, r, ox, oy, filter, vw, vh, fit)
                            },
                            onRemoveMedia = { pathToRemove ->
                                mediaToRemove = pathToRemove
                            }
                        )
                        3 -> StepAddMetadata(
                            selectedMedia = if (processedMediaPaths.isNotEmpty()) processedMediaPaths else selectedMedia,
                            caption = caption,
                            onCaptionChange = { caption = it },
                            location = location,
                            onLocationChange = { location = it },
                            createNewAlbum = createNewAlbum,
                            onCreateNewAlbumChange = { createNewAlbum = it },
                            albumName = albumName,
                            onAlbumNameChange = { albumName = it },
                            sourceType = sourceType,
                            onSourceTypeChange = { sourceType = it },
                            sourcePath = sourcePath,
                            onSourcePathChange = { sourcePath = it },
                            albums = albums,
                            selectedExistingAlbumId = selectedExistingAlbumId,
                            onSelectExistingAlbum = { selectedExistingAlbumId = it },
                            postDate = postDate,
                            onPostDateChange = { postDate = it },
                            isPinned = isPinned,
                            onIsPinnedChange = { isPinned = it },
                            isLocked = isLocked,
                            onIsLockedChange = { isLocked = it }
                        )
                    }
                }

                if (isPublishing || isProcessingMedia) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)), contentAlignment = Alignment.Center) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                CircularProgressIndicator(color = instagramBlue)
                                Text(stringResource(R.string.msg_saving_memory), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }

    if (mediaToRemove != null) {
        AlertDialog(
            onDismissRequest = { mediaToRemove = null },
            title = { Text(stringResource(R.string.title_delete_media)) },
            text = { Text(stringResource(R.string.msg_delete_media_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    mediaToRemove?.let { path ->
                        selectedMedia.remove(path)
                        mediaTransforms.remove(path)
                    }
                    mediaToRemove = null
                }) {
                    Text(stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { mediaToRemove = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    if (showCameraDialog) {
        CameraView(
            onImageCaptured = { photoFile ->
                showCameraDialog = false
                coroutineScope.launch {
                    val savedRelativePath = withContext(Dispatchers.IO) {
                        FileUtils.saveUriToInternalStorage(context, Uri.fromFile(photoFile)) ?: photoFile.absolutePath
                    }
                    if (!multiSelectMode) selectedMedia.clear()
                    selectedMedia.add(savedRelativePath)
                    step = 2
                }
            },
            onClose = { showCameraDialog = false }
        )
    }

    if (showConnectionBrowser) {
        ConnectionFileBrowserDialog(
            type = selectedBrowserType,
            connections = connections.filter { if (selectedBrowserType == "SMB") it.type == "SMB" else it.type != "SMB" },
            onDismiss = { showConnectionBrowser = false },
            onFilesSelected = { paths ->
                paths.forEach { path -> if (!selectedMedia.contains(path)) selectedMedia.add(path) }
                showConnectionBrowser = false
                if (selectedMedia.isNotEmpty()) step = 2
            },
            onNavigateToConnections = { onDismiss(); viewModel.navigateTo(Screen.ManageConnections) },
            onQuickAddConnection = {
                viewModel.addConnection(
                    name = if (selectedBrowserType == "SMB") "NAS Rumah (SMB)" else "Google Drive",
                    type = if (selectedBrowserType == "SMB") "SMB" else "GOOGLE_DRIVE",
                    host = if (selectedBrowserType == "SMB") "192.168.1.100" else "gdrive.google.com",
                    folderPath = if (selectedBrowserType == "SMB") "/volume1/Photos" else "/My Drive/Memoir"
                )
            }
        )
    }
}

@Composable
fun StepChooseSource(
    selectedMedia: List<String>,
    selectedRatio: String,
    onRatioChange: (String) -> Unit,
    isContentFit: Boolean,
    onContentFitToggle: () -> Unit,
    multiSelectMode: Boolean,
    onMultiSelectToggle: () -> Unit,
    mediaTransforms: Map<String, MediaTransformData>,
    onTransformChange: (path: String, scale: Float, rotation: Float, offsetX: Float, offsetY: Float, vw: Float, vh: Float, fit: Boolean) -> Unit,
    onSelectMediaItem: (String) -> Unit,
    onPickGallery: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenSMB: () -> Unit,
    onOpenCloud: () -> Unit
) {
    val context = LocalContext.current

    fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_VIDEO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_VIDEO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    var hasPermission by remember { mutableStateOf(checkPermission()) }
    var isLoadingMedia by remember { mutableStateOf(false) }
    val deviceMediaItems = remember { mutableStateListOf<GalleryMediaItem>() }
    val galleryAlbums = remember { mutableStateListOf<GalleryAlbum>() }
    var selectedAlbumName by remember { mutableStateOf("Semua") }

    val filteredMedia = remember(selectedAlbumName, deviceMediaItems.size) {
        if (selectedAlbumName == "Semua") {
            deviceMediaItems
        } else {
            deviceMediaItems.filter { it.albumName == selectedAlbumName }
        }
    }

    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES, android.Manifest.permission.READ_MEDIA_VIDEO, android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES, android.Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions()) { hasPermission = checkPermission() }

    suspend fun loadDeviceMedia() {
        isLoadingMedia = true
        val mediaList = mutableListOf<Triple<GalleryMediaItem, Long, Boolean>>()
        val albumMap = mutableMapOf<String, Int>()
        val albumThumbnails = mutableMapOf<String, String>()
        
        try {
            withContext(Dispatchers.IO) {
                val imageProjection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME
                )
                context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    imageProjection,
                    null,
                    null,
                    "${MediaStore.Images.Media.DATE_ADDED} DESC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                    
                    var count = 0
                    while (cursor.moveToNext() && count < 1000) {
                        val id = cursor.getLong(idCol)
                        val date = cursor.getLong(dateCol)
                        val album = cursor.getString(albumCol) ?: "Other"
                        val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id).toString()
                        
                        val item = GalleryMediaItem(
                            uriString = uri,
                            isVideo = false,
                            albumName = album
                        )
                        mediaList.add(Triple(item, date, false))
                        albumMap[album] = (albumMap[album] ?: 0) + 1
                        if (!albumThumbnails.containsKey(album)) albumThumbnails[album] = uri
                        count++
                    }
                }

                val videoProjection = arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.DATE_ADDED,
                    MediaStore.Video.Media.BUCKET_DISPLAY_NAME
                )
                context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    videoProjection,
                    null,
                    null,
                    "${MediaStore.Video.Media.DATE_ADDED} DESC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                    val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                    
                    var count = 0
                    while (cursor.moveToNext() && count < 500) {
                        val id = cursor.getLong(idCol)
                        val date = cursor.getLong(dateCol)
                        val dur = cursor.getLong(durCol) / 1000
                        val album = cursor.getString(albumCol) ?: "Other"
                        val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString()
                        
                        val item = GalleryMediaItem(
                            uriString = uri,
                            isVideo = true,
                            durationSeconds = dur,
                            albumName = album
                        )
                        mediaList.add(Triple(item, date, true))
                        albumMap[album] = (albumMap[album] ?: 0) + 1
                        if (!albumThumbnails.containsKey(album)) albumThumbnails[album] = uri
                        count++
                    }
                }
            }

            val sortedItems = mediaList.sortedByDescending { it.second }.map { it.first }
            deviceMediaItems.clear()
            deviceMediaItems.addAll(sortedItems)
            
            galleryAlbums.clear()
            galleryAlbums.add(GalleryAlbum("Semua", sortedItems.size, sortedItems.firstOrNull()?.uriString))
            galleryAlbums.addAll(albumMap.map { GalleryAlbum(it.key, it.value, albumThumbnails[it.key]) }.sortedByDescending { it.count })
            
            isLoadingMedia = false
        } catch (e: Exception) {
            e.printStackTrace()
            isLoadingMedia = false
        }
    }

    LaunchedEffect(hasPermission) { if (hasPermission) loadDeviceMedia() }

    var previewMediaPath by remember { mutableStateOf<String?>(null) }
    var showAlbumDropdown by remember { mutableStateOf(false) }
    var showSourceDropdown by remember { mutableStateOf(false) }
    var viewportSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    var cropBoxSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    Column(modifier = Modifier.fillMaxSize()) {
        val activeMedia = previewMediaPath?.takeIf { selectedMedia.contains(it) } ?: selectedMedia.lastOrNull()
        val originalRatio = remember(activeMedia) { if (activeMedia != null) FileUtils.getImageAspectRatio(context, activeMedia) else 1.0f }
        val ratioValue = when (selectedRatio) {
            "ORIGINAL" -> originalRatio
            "4:5" -> 0.8f
            "16:9" -> 16f / 9f
            "9:16" -> 9f / 16f
            else -> 1f
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.15f)
                .background(if (activeMedia != null) Color.Black else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .onGloballyPositioned { viewportSize = it.size },
            contentAlignment = Alignment.Center
        ) {
            if (activeMedia != null) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val containerRatio = this.maxWidth / this.maxHeight
                    val targetRatio = ratioValue.coerceIn(0.4f, 2.5f)
                    val previewModifier = if (targetRatio > containerRatio) Modifier.fillMaxWidth().aspectRatio(targetRatio) else Modifier.fillMaxHeight().aspectRatio(targetRatio)
                    Box(
                        modifier = previewModifier
                            .clipToBounds()
                            .background(Color.Black)
                            .onGloballyPositioned { cropBoxSize = it.size },
                        contentAlignment = Alignment.Center
                    ) {
                        MediaView(
                            path = activeMedia,
                            modifier = Modifier.fillMaxSize(),
                            autoPlay = false,
                            allowManualPlay = true,
                            contentScale = if (isContentFit) ContentScale.Fit else ContentScale.Crop
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable {
                            onRatioChange(when (selectedRatio) {
                                "ORIGINAL" -> "1:1"
                                "1:1" -> "4:5"
                                "4:5" -> "16:9"
                                "16:9" -> "9:16"
                                else -> "ORIGINAL"
                            })
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AspectRatio, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (selectedRatio == "ORIGINAL") stringResource(R.string.label_aspect_original) else selectedRatio, 
                        color = Color.White, 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 90.dp, bottom = 12.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable { onContentFitToggle() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = if (isContentFit) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, contentDescription = stringResource(R.string.label_fit_fill), tint = Color.White, modifier = Modifier.size(15.dp))
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(if (multiSelectMode) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.7f))
                        .clickable { onMultiSelectToggle() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Layers, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Text(
                            text = if (multiSelectMode) stringResource(R.string.label_selected_count, selectedMedia.size) else stringResource(R.string.btn_select_some),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(24.dp)) {
                    Box(modifier = Modifier.size(60.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AddPhotoAlternate, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(stringResource(R.string.step_select_media), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        if (selectedMedia.size > 1) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(selectedMedia) { index, path ->
                    val isActive = path == activeMedia
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (isActive) 2.dp else 1.dp,
                                color = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { previewMediaPath = path }
                    ) {
                        MediaView(path = path, modifier = Modifier.fillMaxSize(), autoPlay = false)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(2.dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${index + 1}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable { onSelectMediaItem(path) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 0.5.dp) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Box {
                    Row(modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { showAlbumDropdown = true }.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        val albumDisplay = when(selectedAlbumName) {
                            "Semua" -> stringResource(R.string.label_device_gallery)
                            else -> selectedAlbumName
                        }
                        Text(albumDisplay, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(
                        expanded = showAlbumDropdown,
                        onDismissRequest = { showAlbumDropdown = false },
                        modifier = Modifier
                            .widthIn(min = 220.dp)
                            .heightIn(max = 400.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    ) {
                        galleryAlbums.forEachIndexed { index, album ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically, 
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            album.thumbnailUri?.let { uri ->
                                                MediaView(
                                                    path = uri, 
                                                    modifier = Modifier.fillMaxSize(), 
                                                    autoPlay = false,
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            val isSelected = selectedAlbumName == album.name
                                            Text(
                                                text = if(album.name == "Semua") stringResource(R.string.label_device_gallery) else album.name,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = stringResource(R.string.media_count, album.count),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                        if (selectedAlbumName == album.name) {
                                            Icon(
                                                Icons.Default.Check, 
                                                null, 
                                                tint = MaterialTheme.colorScheme.primary, 
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedAlbumName = album.name
                                    showAlbumDropdown = false
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            )
                            if (index < galleryAlbums.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
                
                Box {
                    IconButton(
                        onClick = { showSourceDropdown = true },
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Tune, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                    }
                    
                    DropdownMenu(
                        expanded = showSourceDropdown,
                        onDismissRequest = { showSourceDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.item_gallery)) },
                            leadingIcon = { Icon(Icons.Default.Collections, null) },
                            onClick = { showSourceDropdown = false; onPickGallery() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.item_smb)) },
                            leadingIcon = { Icon(Icons.Default.Dns, null) },
                            onClick = { showSourceDropdown = false; onOpenSMB() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.item_cloud)) },
                            leadingIcon = { Icon(Icons.Default.CloudQueue, null) },
                            onClick = { showSourceDropdown = false; onOpenCloud() }
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f).background(MaterialTheme.colorScheme.background)) {
            if (!hasPermission) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Button(onClick = { permissionLauncher.launch(permissionsToRequest) }) { Text(stringResource(R.string.btn_grant_permission)) }
                }
            } else if (isLoadingMedia) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(36.dp)) }
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(4), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxSize()) {
                    item {
                        Box(modifier = Modifier.aspectRatio(1f).background(MaterialTheme.colorScheme.surfaceVariant).clickable { onOpenCamera() }, contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.PhotoCamera, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp)) }
                        }
                    }
                    items(filteredMedia) { item ->
                        val selectedIdx = selectedMedia.indexOf(item.uriString)
                        val isSelected = selectedIdx != -1
                        val isVid = item.isVideo || FileUtils.isVideoPath(item.uriString)
                        Box(modifier = Modifier.aspectRatio(1f).clickable { previewMediaPath = item.uriString; onSelectMediaItem(item.uriString) }) {
                            MediaView(
                                path = item.uriString, 
                                modifier = Modifier.fillMaxSize(), 
                                autoPlay = false,
                                allowManualPlay = false
                            )
                            if (isVid) {
                                Box(modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.65f)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Icon(Icons.Default.Videocam, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                        if (item.durationSeconds > 0) { Text(text = "%02d:%02d".format(item.durationSeconds / 60, item.durationSeconds % 60), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
                                    }
                                }
                            }
                            if (isSelected) {
                                Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.22f)))
                                Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).border(1.5.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                                    Text("${selectedIdx + 1}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepAdjustMedia(
    selectedMedia: List<String>,
    selectedRatio: String,
    onRatioChange: (String) -> Unit,
    mediaTransforms: Map<String, MediaTransformData>,
    onTransformChange: (path: String, scale: Float, rotation: Float, offsetX: Float, offsetY: Float, filter: String, vw: Float, vh: Float, fit: Boolean) -> Unit,
    onRemoveMedia: (String) -> Unit
) {
    var activeIndex by remember { mutableIntStateOf(0) }
    val safeActiveIndex = activeIndex.coerceIn(0, (selectedMedia.size - 1).coerceAtLeast(0))
    val activeMedia = selectedMedia.getOrNull(safeActiveIndex) ?: selectedMedia.firstOrNull() ?: "gradient_sunset"
    val existingTransform = mediaTransforms[activeMedia] ?: MediaTransformData()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (selectedMedia.size > 1) {
            LazyRow(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                itemsIndexed(selectedMedia) { index, path ->
                    val isActive = index == safeActiveIndex
                    Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).border(width = if (isActive) 2.dp else 1.dp, color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp)).clickable { activeIndex = index }) {
                        MediaView(path = path, modifier = Modifier.fillMaxSize(), autoPlay = false)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                                .clickable { onRemoveMedia(path) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black)) {
            key(activeMedia) {
                InstagramImageEditorCanvas(
                    mediaPath = activeMedia,
                    selectedRatio = selectedRatio,
                    onRatioChange = onRatioChange,
                    initialScale = existingTransform.scale,
                    initialRotation = existingTransform.rotation,
                    initialOffset = Offset(existingTransform.offsetX, existingTransform.offsetY),
                    isContentFit = existingTransform.isContentFit,
                    onTransformChange = { s: Float, r: Float, ox: Float, oy: Float, vw: Float, vh: Float, fit: Boolean -> 
                        onTransformChange(activeMedia, s, r, ox, oy, existingTransform.filterName, vw, vh, fit) 
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (selectedMedia.size > 1) {
            Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { if (activeIndex > 0) activeIndex-- }, enabled = safeActiveIndex > 0) { Text(stringResource(R.string.btn_previous), color = if (safeActiveIndex > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) }
                Text("${safeActiveIndex + 1} ${stringResource(R.string.label_from)} ${selectedMedia.size}", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = { if (activeIndex < selectedMedia.size - 1) activeIndex++ }, enabled = safeActiveIndex < selectedMedia.size - 1) { Text(stringResource(R.string.btn_next_item), color = if (safeActiveIndex < selectedMedia.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) }
            }
        }
    }
}

@Composable
fun StepAddMetadata(
    selectedMedia: List<String>,
    caption: String,
    onCaptionChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    createNewAlbum: Boolean,
    onCreateNewAlbumChange: (Boolean) -> Unit,
    albumName: String,
    onAlbumNameChange: (String) -> Unit,
    sourceType: String,
    onSourceTypeChange: (String) -> Unit,
    sourcePath: String,
    onSourcePathChange: (String) -> Unit,
    albums: List<AlbumEntity>,
    selectedExistingAlbumId: Long?,
    onSelectExistingAlbum: (Long?) -> Unit,
    postDate: Long,
    onPostDateChange: (Long) -> Unit,
    isPinned: Boolean,
    onIsPinnedChange: (Boolean) -> Unit,
    isLocked: Boolean,
    onIsLockedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    LazyColumn(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentPadding = PaddingValues(bottom = 36.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))) {
                    MediaView(path = selectedMedia.firstOrNull() ?: "gradient_sunset", modifier = Modifier.fillMaxSize(), autoPlay = false)
                }
                OutlinedTextField(
                    value = caption,
                    onValueChange = onCaptionChange,
                    placeholder = { Text(stringResource(R.string.placeholder_caption), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 13.sp) },
                    modifier = Modifier.weight(1f).height(110.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface, focusedBorderColor = primaryColor, unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
        item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)) }
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(stringResource(R.string.label_location), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = location, onValueChange = onLocationChange, placeholder = { Text(stringResource(R.string.placeholder_location), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 13.sp) }, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = primaryColor, modifier = Modifier.size(20.dp)) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface, focusedBorderColor = primaryColor, unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp))
            }
        }
        item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)) }
        item {
            val cal = Calendar.getInstance().apply { timeInMillis = postDate }
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(stringResource(R.string.label_memory_date), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)).clickable {
                    DatePickerDialog(context, { _, y, m, d -> onPostDateChange(Calendar.getInstance().also { it.set(y, m, d) }.timeInMillis) }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                }.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.CalendarToday, null, tint = primaryColor, modifier = Modifier.size(18.dp))
                        val currentLocale = LocalContext.current.resources.configuration.locales[0]
                        val dateStr = SimpleDateFormat("dd/MM/yyyy", currentLocale).format(Date(postDate))
                        Text(dateStr, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    Text(stringResource(R.string.btn_change), color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)) }
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(stringResource(R.string.label_album_collection), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(selected = !createNewAlbum && selectedExistingAlbumId == null, onClick = { onCreateNewAlbumChange(false); onSelectExistingAlbum(null) }, label = { Text(stringResource(R.string.item_no_album), fontSize = 12.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = primaryColor, selectedLabelColor = Color.White), modifier = Modifier.weight(1f))
                    FilterChip(selected = createNewAlbum, onClick = { onCreateNewAlbumChange(true); onSelectExistingAlbum(null) }, label = { Text(stringResource(R.string.item_create_new), fontSize = 12.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = primaryColor, selectedLabelColor = Color.White), modifier = Modifier.weight(1f))
                }
                if (createNewAlbum) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = albumName, onValueChange = onAlbumNameChange, placeholder = { Text(stringResource(R.string.placeholder_new_album), fontSize = 13.sp) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor), shape = RoundedCornerShape(12.dp))
                } else if (albums.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    albums.forEach { album ->
                        val isSelected = selectedExistingAlbumId == album.id
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(10.dp)).background(if (isSelected) primaryColor.copy(alpha = 0.12f) else Color.Transparent).clickable { onSelectExistingAlbum(if (isSelected) null else album.id); onCreateNewAlbumChange(false) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhotoAlbum, null, tint = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(album.name, color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                            if (isSelected) {
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.Check, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
        item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)) }
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.label_extra_settings), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)).clickable { onIsPinnedChange(!isPinned) }.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.PushPin, null, tint = if (isPinned) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Text(stringResource(R.string.item_pin_feed), color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                    }
                    Switch(checked = isPinned, onCheckedChange = onIsPinnedChange)
                }
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)).clickable { onIsLockedChange(!isLocked) }.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Lock, null, tint = if (isLocked) Color(0xFFFF4444) else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Text(stringResource(R.string.item_lock_private), color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                    }
                    Switch(checked = isLocked, onCheckedChange = onIsLockedChange)
                }
            }
        }
    }
}

@Composable
fun ConnectionFileBrowserDialog(
    type: String,
    connections: List<SourceConnectionEntity>,
    onDismiss: () -> Unit,
    onFilesSelected: (List<String>) -> Unit,
    onNavigateToConnections: () -> Unit,
    onQuickAddConnection: () -> Unit
) {
    var selectedConn by remember { mutableStateOf<SourceConnectionEntity?>(null) }
    var currentPath by remember { mutableStateOf("") }
    var folders by remember { mutableStateOf<List<String>>(emptyList()) }
    var mediaFiles by remember { mutableStateOf<List<com.example.utils.SmbMediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun loadContent(conn: SourceConnectionEntity, path: String) {
        val credParts = conn.credentials.split(":")
        val user = credParts.getOrNull(0) ?: ""
        val pass = credParts.getOrNull(1) ?: ""
        val dom = credParts.getOrNull(2) ?: ""
        val shareName = conn.folderPath.split("/").firstOrNull() ?: ""
        
        isLoading = true
        scope.launch {
            val fRes = com.example.utils.SmbHelper.listSubfoldersInShare(conn.host, shareName, path, user, pass, dom)
            val mRes = com.example.utils.SmbHelper.listMediaFiles(conn.host, shareName, path, user, pass, dom)
            isLoading = false
            folders = fRes.getOrDefault(emptyList())
            mediaFiles = mRes.getOrDefault(emptyList())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text(
                    text = if (selectedConn == null) {
                        if (type == "SMB") stringResource(R.string.title_choose_smb) else stringResource(R.string.title_choose_cloud)
                    } else selectedConn!!.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (selectedConn != null) {
                    Text(
                        text = if (currentPath.isEmpty()) "/" else "/$currentPath",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 450.dp)) {
                if (selectedConn == null) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (connections.isEmpty()) {
                            item {
                                Button(onClick = onQuickAddConnection, modifier = Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.btn_quick_add))
                                }
                            }
                        } else {
                            items(connections) { conn ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selectedConn = conn
                                        loadContent(conn, "")
                                    },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Dns, null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(12.dp))
                                        Text(conn.name, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column {
                        if (isLoading) {
                            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(24.dp))
                        } else {
                            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (currentPath.isNotEmpty()) {
                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                currentPath = if (currentPath.contains("/")) currentPath.substringBeforeLast("/") else ""
                                                loadContent(selectedConn!!, currentPath)
                                            }.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.ArrowUpward, null, Modifier.size(18.dp))
                                            Spacer(Modifier.width(12.dp))
                                            Text(".. (Kembali)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                    }
                                }
                                items(folders) { folder ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            currentPath = if (currentPath.isEmpty()) folder else "$currentPath/$folder"
                                            loadContent(selectedConn!!, currentPath)
                                        }.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text(folder, fontSize = 14.sp)
                                        Spacer(Modifier.weight(1f))
                                        Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                    }
                                }
                                items(mediaFiles) { file ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            onFilesSelected(listOf(file.fullPath)) 
                                        }.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (file.isVideo) Icons.Default.Videocam else Icons.Default.Image,
                                            null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(file.name, fontSize = 13.sp)
                                        Spacer(Modifier.weight(1f))
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                if (folders.isEmpty() && mediaFiles.isEmpty()) {
                                    item {
                                        Text(
                                            stringResource(R.string.msg_empty_folder),
                                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                                            textAlign = TextAlign.Center,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedConn == null) {
                TextButton(onClick = onNavigateToConnections) { Text(stringResource(R.string.btn_manage_all)) }
            } else {
                TextButton(onClick = { selectedConn = null; currentPath = "" }) { Text("Ganti Server") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        }
    )
}
