package com.example.ui.components

import android.Manifest
import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.example.R
import com.example.utils.FileUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class StoryStage { CAPTURE, EDITOR }
enum class StoryCameraMode { NORMAL, CREATE }

private data class StoryDimens(
    val iconButtonSize: Dp,
    val iconSize: Dp,
    val smallIconSize: Dp,
    val topBarVerticalPad: Dp,
    val bottomBarExtraPad: Dp,
    val badgeTopOffset: Dp,
    val swatchSize: Dp,
    val maxContentWidth: Dp
)

@Composable
private fun rememberStoryDimens(maxWidth: Dp, maxHeight: Dp): StoryDimens {
    return remember(maxWidth, maxHeight) {
        val wScale = (maxWidth.value / 390f).coerceIn(0.82f, 1.30f)
        val hScale = (maxHeight.value / 844f).coerceIn(0.75f, 1.20f)
        fun w(base: Int): Dp = (base * wScale).dp
        fun h(base: Int): Dp = (base * hScale).dp
        StoryDimens(
            iconButtonSize = w(40),
            iconSize = w(22),
            smallIconSize = w(18),
            topBarVerticalPad = h(12),
            bottomBarExtraPad = h(16),
            badgeTopOffset = (maxHeight.value * 0.07f).coerceIn(44f, 76f).dp,
            swatchSize = w(32),
            maxContentWidth = 480.dp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun StoryCreatorDialog(
    onDismiss: () -> Unit,
    onPublish: (mediaPath: String, caption: String?, textColor: String?, textStyle: String?, createdAt: Long) -> Unit,
    profileImagePath: String? = null
) {
    val quickPickGradients = remember {
        listOf(
            "gradient_sunset", "gradient_blue", "gradient_orange",
            "gradient_nature", "gradient_temple", "gradient_purple", "gradient_slate"
        )
    }

    var currentStage by rememberSaveable { mutableStateOf(StoryStage.CAPTURE) }
    var currentCameraMode by rememberSaveable { mutableStateOf(StoryCameraMode.NORMAL) }

    var selectedMedia by remember { mutableStateOf(quickPickGradients.first()) }
    var storyDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDateBadge by remember { mutableStateOf(true) }
    var showGridGuide by remember { mutableStateOf(false) }
    var activeFilterIndex by remember { mutableIntStateOf(0) }

    var imgScale by remember { mutableFloatStateOf(1f) }
    var imgRotation by remember { mutableFloatStateOf(0f) }
    var imgOffset by remember { mutableStateOf(Offset.Zero) }

    val textElements = remember { mutableStateListOf<StoryTextElementItem>() }
    val editorState = rememberStoryEditorState()

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var takePhotoTrigger by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val filters = listOf(
        stringResource(R.string.filter_normal),
        stringResource(R.string.filter_warm),
        stringResource(R.string.filter_cool),
        stringResource(R.string.filter_vintage),
        stringResource(R.string.filter_bw),
        stringResource(R.string.filter_dramatic)
    )
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    BackHandler {
        when {
            editorState.focusedElement != null -> {
                val focused = editorState.focusedElement
                if (focused != null) {
                    coroutineScope.launch {
                        focused.transformation.stopEdit(
                            maxExtentX = editorState.editorSize.width * 0.45f,
                            maxExtentY = editorState.editorSize.height * 0.45f
                        )
                        if (focused.text.isBlank()) {
                            textElements.remove(focused)
                        }
                        editorState.focusedElement = null
                    }
                }
            }
            currentStage == StoryStage.EDITOR -> currentStage = StoryStage.CAPTURE
            else -> onDismiss()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val localPath = FileUtils.saveUriToInternalStorage(context, uri)
            if (localPath != null) {
                selectedMedia = localPath
                imgScale = 1f; imgRotation = 0f; imgOffset = Offset.Zero
                currentStage = StoryStage.EDITOR
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            val view = LocalView.current
            SideEffect {
                val window = (view.parent as? DialogWindowProvider)?.window
                if (window != null) WindowCompat.setDecorFitsSystemWindows(window, false)
            }
            val dimens = rememberStoryDimens(maxWidth, maxHeight)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = dimens.maxContentWidth)
                    .align(Alignment.TopCenter)
            ) {
                when (currentStage) {
                    StoryStage.CAPTURE -> {
                        StoryCaptureScreen(
                            dimens = dimens,
                            cameraMode = currentCameraMode,
                            onCameraModeChange = { currentCameraMode = it },
                            lensFacing = lensFacing,
                            onToggleLensFacing = {
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                                    CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                            },
                            flashMode = flashMode,
                            onToggleFlashMode = {
                                flashMode = when (flashMode) {
                                    ImageCapture.FLASH_MODE_OFF  -> ImageCapture.FLASH_MODE_ON
                                    ImageCapture.FLASH_MODE_ON   -> ImageCapture.FLASH_MODE_AUTO
                                    else                         -> ImageCapture.FLASH_MODE_OFF
                                }
                            },
                            showGridGuide = showGridGuide,
                            onToggleGridGuide = { showGridGuide = !showGridGuide },
                            takePhotoTrigger = takePhotoTrigger,
                            onTakePhoto = {
                                if (currentCameraMode == StoryCameraMode.NORMAL) {
                                    if (cameraPermissionState.status.isGranted) takePhotoTrigger++
                                    else cameraPermissionState.launchPermissionRequest()
                                } else {
                                    currentStage = StoryStage.EDITOR
                                }
                            },
                            onImageCaptured = { photoFile ->
                                selectedMedia = photoFile.absolutePath
                                imgScale = 1f; imgRotation = 0f; imgOffset = Offset.Zero
                                currentStage = StoryStage.EDITOR
                            },
                            selectedGradient = selectedMedia,
                            onSelectGradient = { selectedMedia = it },
                            quickPickGradients = quickPickGradients,
                            hasText = textElements.isNotEmpty(),
                            onOpenTextEditor = {
                                val newItem = StoryTextElementItem()
                                textElements.add(newItem)
                                editorState.focusedElement = newItem
                                coroutineScope.launch {
                                    newItem.transformation.startEdit()
                                }
                            },
                            onOpenGallery = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            cameraPermissionGranted = cameraPermissionState.status.isGranted,
                            onRequestCameraPermission = { cameraPermissionState.launchPermissionRequest() },
                            onDismiss = onDismiss
                        )
                    }
                    StoryStage.EDITOR -> {
                        StoryEditorScreen(
                            dimens = dimens,
                            canvasWidth = this@BoxWithConstraints.maxWidth,
                            canvasHeight = this@BoxWithConstraints.maxHeight,
                            selectedMedia = selectedMedia,
                            editorState = editorState,
                            textElements = textElements,
                            storyDate = storyDate,
                            onStoryDateChange = { storyDate = it },
                            showDateBadge = showDateBadge,
                            onToggleDateBadge = { showDateBadge = !showDateBadge },
                            filters = filters,
                            activeFilterIndex = activeFilterIndex,
                            onCycleFilter = {
                                activeFilterIndex = (activeFilterIndex + 1) % filters.size
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.msg_filter_applied, filters[activeFilterIndex]),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            imgScale = imgScale,
                            imgRotation = imgRotation,
                            imgOffset = imgOffset,
                            onImgTransform = { zoom, rotate, pan ->
                                imgScale = (imgScale * zoom).coerceIn(0.5f, 5f)
                                imgRotation += rotate
                                imgOffset += pan
                            },
                            onResetTransform = {
                                imgScale = 1f; imgRotation = 0f; imgOffset = Offset.Zero
                            },
                            onAddNewText = {
                                val newItem = StoryTextElementItem()
                                textElements.add(newItem)
                                editorState.focusedElement = newItem
                                coroutineScope.launch {
                                    newItem.transformation.startEdit()
                                }
                            },
                            onSaveCompositeToGallery = { bitmap ->
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                                        val memoirDir = File(picturesDir, "Memoir").apply { if (!exists()) mkdirs() }
                                        val destFile = File(memoirDir, "IMG_STORY_${System.currentTimeMillis()}.jpg")
                                        FileOutputStream(destFile).use { out ->
                                            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                                        }
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, context.getString(R.string.story_saved_to_gallery), Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, context.getString(R.string.story_saved_to_gallery), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            onBackToCapture = { currentStage = StoryStage.CAPTURE },
                            onPublishComposite = { bitmap ->
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val cacheStoryFile = File(context.cacheDir, "story_render_${System.currentTimeMillis()}.jpg")
                                        FileOutputStream(cacheStoryFile).use { out ->
                                            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                                        }
                                        val firstEl = textElements.firstOrNull()
                                        val captionText = if (textElements.isNotEmpty()) {
                                            textElements.joinToString(" ") { it.text }
                                        } else null

                                        withContext(Dispatchers.Main) {
                                            onPublish(
                                                cacheStoryFile.absolutePath,
                                                captionText,
                                                firstEl?.colorScheme?.hex,
                                                firstEl?.fontStyle?.name,
                                                storyDate
                                            )
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        withContext(Dispatchers.Main) {
                                            val firstEl = textElements.firstOrNull()
                                            onPublish(
                                                selectedMedia,
                                                if (textElements.isNotEmpty()) textElements.joinToString(" ") { it.text } else null,
                                                firstEl?.colorScheme?.hex,
                                                firstEl?.fontStyle?.name,
                                                storyDate
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                editorState.focusedElement?.let { focusedItem ->
                    InstagramTextEditOverlay(
                        element = focusedItem,
                        canvasWidth = this@BoxWithConstraints.maxWidth,
                        onDone = { shouldKeep ->
                            coroutineScope.launch {
                                val maxExtentX = editorState.editorSize.width * 0.45f
                                val maxExtentY = editorState.editorSize.height * 0.45f
                                focusedItem.transformation.stopEdit(maxExtentX, maxExtentY)
                                if (!shouldKeep) {
                                    textElements.remove(focusedItem)
                                }
                                editorState.focusedElement = null
                                if (currentStage == StoryStage.CAPTURE && currentCameraMode == StoryCameraMode.CREATE) {
                                    currentStage = StoryStage.EDITOR
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StoryCaptureScreen(
    dimens: StoryDimens,
    cameraMode: StoryCameraMode,
    onCameraModeChange: (StoryCameraMode) -> Unit,
    lensFacing: Int,
    onToggleLensFacing: () -> Unit,
    flashMode: Int,
    onToggleFlashMode: () -> Unit,
    showGridGuide: Boolean,
    onToggleGridGuide: () -> Unit,
    takePhotoTrigger: Int,
    onTakePhoto: () -> Unit,
    onImageCaptured: (File) -> Unit,
    selectedGradient: String,
    onSelectGradient: (String) -> Unit,
    quickPickGradients: List<String>,
    hasText: Boolean,
    onOpenTextEditor: () -> Unit,
    onOpenGallery: () -> Unit,
    cameraPermissionGranted: Boolean,
    onRequestCameraPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        if (cameraMode == StoryCameraMode.NORMAL) {
            if (cameraPermissionGranted) {
                CameraViewFinder(
                    modifier = Modifier.fillMaxSize(),
                    lensFacing = lensFacing,
                    flashMode = flashMode,
                    takePhotoTrigger = takePhotoTrigger,
                    onImageCaptured = onImageCaptured
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Default.PhotoCamera, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(64.dp))
                        Text(stringResource(R.string.desc_camera_permission_required), color = Color.White, fontSize = 15.sp, textAlign = TextAlign.Center)
                        Button(onClick = onRequestCameraPermission, colors = ButtonDefaults.buttonColors(containerColor = Color.White)) {
                            Text(stringResource(R.string.btn_grant_permission), color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().clickable { onOpenTextEditor() }, contentAlignment = Alignment.Center) {
                MediaView(path = selectedGradient, modifier = Modifier.fillMaxSize())
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(alpha = 0.4f)).padding(horizontal = 24.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Aa", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = stringResource(R.string.label_tap_text),
                            color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium
                        )
                    }
                }
                Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 140.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 24.dp)) {
                        items(quickPickGradients) { gradientId ->
                            val isSelected = selectedGradient == gradientId
                            Box(
                                modifier = Modifier.size(dimens.swatchSize)
                                    .border(if (isSelected) 2.5.dp else 1.dp, if (isSelected) Color.White else Color.White.copy(alpha = 0.4f), CircleShape)
                                    .padding(3.dp).clip(CircleShape).clickable { onSelectGradient(gradientId) }
                            ) { MediaView(path = gradientId, modifier = Modifier.fillMaxSize()) }
                        }
                    }
                }
            }
        }

        if (showGridGuide) RuleOfThirdsGridOverlay()

        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = dimens.topBarVerticalPad),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleGridGuide,
                modifier = Modifier.size(dimens.iconButtonSize).clip(CircleShape)
                    .background(if (showGridGuide) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.45f))
            ) {
                Icon(Icons.Default.Settings, stringResource(R.string.label_extra_settings), tint = Color.White, modifier = Modifier.size(dimens.iconSize))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(
                    onClick = onOpenTextEditor,
                    modifier = Modifier.size(dimens.iconButtonSize).clip(CircleShape)
                        .background(if (hasText) Color.White else Color.Black.copy(alpha = 0.45f))
                ) {
                    Text("Aa", color = if (hasText) Color.Black else Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                }
                if (cameraMode == StoryCameraMode.NORMAL) {
                    IconButton(
                        onClick = onToggleFlashMode,
                        modifier = Modifier.size(dimens.iconButtonSize).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        val (flashIcon, flashColor) = when (flashMode) {
                            ImageCapture.FLASH_MODE_ON   -> Icons.Default.FlashOn  to Color(0xFFFFD700)
                            ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto to Color(0xFFFFD700)
                            else                         -> Icons.Default.FlashOff  to Color.White
                        }
                        Icon(flashIcon, stringResource(R.string.desc_flash_toggle), tint = flashColor, modifier = Modifier.size(dimens.iconSize))
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(dimens.iconButtonSize).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(Icons.Default.Close, stringResource(R.string.btn_done), tint = Color.White, modifier = Modifier.size(dimens.iconSize))
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = dimens.bottomBarExtraPad),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                        .clickable { onOpenGallery() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Image, stringResource(R.string.item_gallery), tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Box(
                    modifier = Modifier.size(76.dp).clip(CircleShape).border(4.dp, Color.White, CircleShape)
                        .clickable { onTakePhoto() }.padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.fillMaxSize().clip(CircleShape)
                        .background(if (cameraMode == StoryCameraMode.NORMAL) Color.White else Color(0xFFE1306C))) {
                        if (cameraMode == StoryCameraMode.CREATE) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White,
                                modifier = Modifier.size(28.dp).align(Alignment.Center))
                        }
                    }
                }
                IconButton(
                    onClick = onToggleLensFacing,
                    modifier = Modifier.size(46.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))
                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.Default.Cameraswitch, stringResource(R.string.desc_switch_camera), tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                StoreModeTab(label = stringResource(R.string.story_mode_normal), selected = cameraMode == StoryCameraMode.NORMAL) {
                    onCameraModeChange(StoryCameraMode.NORMAL)
                }
                Spacer(modifier = Modifier.width(16.dp))
                StoreModeTab(label = stringResource(R.string.story_mode_create), selected = cameraMode == StoryCameraMode.CREATE) {
                    onCameraModeChange(StoryCameraMode.CREATE)
                }
            }
        }
    }
}

@Composable
private fun StoreModeTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = 1.sp
        )
        if (selected) {
            Spacer(modifier = Modifier.height(3.dp))
            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color.White))
        }
    }
}

@Composable
private fun StoryEditorScreen(
    dimens: StoryDimens,
    canvasWidth: Dp,
    canvasHeight: Dp,
    selectedMedia: String,
    editorState: StoryEditorState,
    textElements: SnapshotStateList<StoryTextElementItem>,
    storyDate: Long,
    onStoryDateChange: (Long) -> Unit,
    showDateBadge: Boolean,
    onToggleDateBadge: () -> Unit,
    filters: List<String>,
    activeFilterIndex: Int,
    onCycleFilter: () -> Unit,
    imgScale: Float,
    imgRotation: Float,
    imgOffset: Offset,
    onImgTransform: (zoom: Float, rotate: Float, pan: Offset) -> Unit,
    onResetTransform: () -> Unit,
    onAddNewText: () -> Unit,
    onSaveCompositeToGallery: (Bitmap) -> Unit,
    onBackToCapture: () -> Unit,
    onPublishComposite: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isImgGesturing = imgScale != 1f || imgRotation != 0f || imgOffset != Offset.Zero

    val storyCanvasGraphicsLayer = rememberGraphicsLayer()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
                .clipToBounds()
                .onGloballyPositioned { coords ->
                    editorState.editorSize = coords.size
                }
                .then(
                    if (!selectedMedia.startsWith("gradient_")) {
                        Modifier.pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, rotate ->
                                onImgTransform(zoom, rotate, pan)
                            }
                        }
                    } else {
                        Modifier.clickable {
                            if (textElements.isEmpty()) onAddNewText()
                        }
                    }
                )
                .drawWithContent {
                    storyCanvasGraphicsLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(storyCanvasGraphicsLayer)
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
                    .graphicsLayer {
                        scaleX = imgScale; scaleY = imgScale
                        rotationZ = imgRotation
                        translationX = imgOffset.x; translationY = imgOffset.y
                    }
            ) {
                MediaView(path = selectedMedia, modifier = Modifier.fillMaxSize())

                val filterName = filters[activeFilterIndex]
                val filterColor = when {
                    filterName == context.getString(R.string.filter_warm)     -> Color(0x33FF9800)
                    filterName == context.getString(R.string.filter_cool)     -> Color(0x3300E5FF)
                    filterName == context.getString(R.string.filter_vintage)  -> Color(0x40795548)
                    filterName == context.getString(R.string.filter_bw)       -> Color(0x80000000)
                    filterName == context.getString(R.string.filter_dramatic) -> Color(0x55000000)
                    else                                                       -> Color.Transparent
                }
                if (filterColor != Color.Transparent) {
                    Box(modifier = Modifier.fillMaxSize().background(filterColor))
                }
            }

            AnimatedVisibility(visible = isImgGesturing, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
                RuleOfThirdsGridOverlay()
            }

            if (showDateBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = dimens.badgeTopOffset)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .clickable {
                            val calendar = Calendar.getInstance().apply { timeInMillis = storyDate }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    onStoryDateChange(
                                        Calendar.getInstance().apply {
                                            set(Calendar.YEAR, year)
                                            set(Calendar.MONTH, month)
                                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        }.timeInMillis
                                    )
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.DateRange, null, tint = Color.White, modifier = Modifier.size(dimens.smallIconSize))
                        val currentLocale = context.resources.configuration.locales[0]
                        Text(
                            SimpleDateFormat("dd MMM yyyy", currentLocale).format(Date(storyDate)).uppercase(),
                            color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            val maxExtentX = (editorState.editorSize.width * 0.45f).coerceAtLeast(200f)
            val maxExtentY = (editorState.editorSize.height * 0.45f).coerceAtLeast(300f)

            textElements.forEach { element ->
                CanvasInstagramTextElement(
                    element = element,
                    canvasWidth = canvasWidth,
                    isDragging = editorState.draggedElement?.id == element.id,
                    isFocused = editorState.focusedElement?.id == element.id,
                    onDragStart = {
                        editorState.draggedElement = element
                    },
                    onDragMove = { pointerRootPos ->
                        editorState.pointerPosition = pointerRootPos
                    },
                    onDragEnd = {
                        val dragged = editorState.draggedElement
                        if (dragged != null && editorState.isOverDelete) {
                            textElements.remove(dragged)
                        }
                        editorState.draggedElement = null
                        editorState.pointerPosition = Offset.Unspecified
                        editorState.isOverDelete = false
                    },
                    onTap = {
                        editorState.focusedElement = element
                        coroutineScope.launch {
                            element.transformation.startEdit()
                        }
                    },
                    onTransform = { zoom, rotate, pan ->
                        element.transformation.updateTransform(
                            zoom = zoom,
                            rotate = rotate,
                            pan = pan,
                            maxExtentX = maxExtentX,
                            maxExtentY = maxExtentY
                        )
                    }
                )
            }
        }

        EditorDeleteButton(
            isDragging = editorState.isDragging,
            pointerPosition = editorState.pointerPosition,
            isOverTrash = editorState.isOverDelete,
            onOverTrashChanged = { editorState.isOverDelete = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 100.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = dimens.topBarVerticalPad),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackToCapture,
                modifier = Modifier.size(dimens.iconButtonSize).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Close, stringResource(R.string.btn_previous), tint = Color.White, modifier = Modifier.size(dimens.iconSize))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleDateBadge,
                    modifier = Modifier.size(dimens.iconButtonSize).clip(CircleShape)
                        .background(if (showDateBadge) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.DateRange, stringResource(R.string.story_stickers), tint = Color.White, modifier = Modifier.size(dimens.smallIconSize + 2.dp))
                }

                IconButton(
                    onClick = onCycleFilter,
                    modifier = Modifier.size(dimens.iconButtonSize).clip(CircleShape)
                        .background(if (activeFilterIndex > 0) Color(0xFFE1306C) else Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.AutoAwesome, stringResource(R.string.story_effects), tint = Color.White, modifier = Modifier.size(dimens.smallIconSize + 2.dp))
                }

                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val imageBitmap = storyCanvasGraphicsLayer.toImageBitmap()
                                val androidBitmap = imageBitmap.asAndroidBitmap()
                                val finalBitmap = if (androidBitmap.config == Bitmap.Config.HARDWARE) {
                                    androidBitmap.copy(Bitmap.Config.ARGB_8888, false)
                                } else androidBitmap
                                onSaveCompositeToGallery(finalBitmap)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    modifier = Modifier.size(dimens.iconButtonSize).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Save, stringResource(R.string.story_save_download), tint = Color.White, modifier = Modifier.size(dimens.smallIconSize + 2.dp))
                }

                IconButton(
                    onClick = onResetTransform,
                    modifier = Modifier.size(dimens.iconButtonSize).clip(CircleShape)
                        .background(if (isImgGesturing) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Refresh, stringResource(R.string.label_reset), tint = Color.White, modifier = Modifier.size(dimens.smallIconSize + 2.dp))
                }

                Box(
                    modifier = Modifier.size(dimens.iconButtonSize).clip(CircleShape)
                        .background(if (textElements.isNotEmpty()) Color.White else Color.Black.copy(alpha = 0.5f))
                        .clickable { onAddNewText() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aa", color = if (textElements.isNotEmpty()) Color.Black else Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = dimens.bottomBarExtraPad),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(visible = !editorState.isDragging && !editorState.isEditing) {
                Surface(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val imageBitmap = storyCanvasGraphicsLayer.toImageBitmap()
                                val androidBitmap = imageBitmap.asAndroidBitmap()
                                val finalBitmap = if (androidBitmap.config == Bitmap.Config.HARDWARE) {
                                    androidBitmap.copy(Bitmap.Config.ARGB_8888, false)
                                } else androidBitmap
                                onPublishComposite(finalBitmap)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(stringResource(R.string.story_send_to), color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Preview(name = "Compact phone", showSystemUi = true, device = "spec:width=360dp,height=740dp")
@Composable
fun StoryCreatorPreviewCompact() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        StoryCreatorDialog(onDismiss = {}, onPublish = { _, _, _, _, _ -> })
    }
}

@Preview(name = "Normal phone", showSystemUi = true, device = "spec:width=411dp,height=891dp")
@Composable
fun StoryCreatorPreviewNormal() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        StoryCreatorDialog(onDismiss = {}, onPublish = { _, _, _, _, _ -> })
    }
}
