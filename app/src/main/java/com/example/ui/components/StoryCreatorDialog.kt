package com.example.ui.components

import android.app.DatePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.example.utils.FileUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

private data class StoryDimens(
    val iconButtonSize: Dp,
    val iconSize: Dp,
    val smallIconSize: Dp,
    val topBarVerticalPad: Dp,
    val bottomBarExtraPad: Dp,
    val badgeTopOffset: Dp,
    val captionFontSize: TextUnit,
    val placeholderFontSize: TextUnit,
    val swatchSize: Dp,
    val galleryButtonSize: Dp,
    val bottomBtnFontSize: TextUnit,
    val bottomBtnHorizPad: Dp,
    val bottomBtnHeight: Dp,
    val publishBtnSize: Dp,
    val fontChipFontSize: TextUnit,
    val colorDotSize: Dp,
    val maxContentWidth: Dp,
    val bottomButtonGap: Dp,
    val gradientBarGap: Dp
)

private val ELEMENT_SAFE_BOUNDS = Rect(0.04f, 0.14f, 0.96f, 0.80f)
private val DEFAULT_TEXT_POSITION_FRACTION = Offset(0.5f, 0.42f)

@Composable
private fun rememberStoryDimens(maxWidth: Dp, maxHeight: Dp): StoryDimens {
    return remember(maxWidth, maxHeight) {
        val wScale = (maxWidth.value / 390f).coerceIn(0.82f, 1.30f)
        val hScale = (maxHeight.value / 844f).coerceIn(0.75f, 1.20f)
        fun w(base: Int): Dp = (base * wScale).dp
        fun h(base: Int): Dp = (base * hScale).dp
        fun sp(base: Int): TextUnit = (base * wScale).coerceIn(base * 0.82f, base * 1.25f).sp
        StoryDimens(
            iconButtonSize = w(38),
            iconSize = w(20),
            smallIconSize = w(16),
            topBarVerticalPad = h(8),
            bottomBarExtraPad = h(20),
            badgeTopOffset = (maxHeight.value * 0.065f).coerceIn(40f, 72f).dp,
            captionFontSize = sp(24),
            placeholderFontSize = sp(13),
            swatchSize = w(30),
            galleryButtonSize = w(34),
            bottomBtnFontSize = sp(12),
            bottomBtnHorizPad = w(12),
            bottomBtnHeight = h(38),
            publishBtnSize = w(36),
            fontChipFontSize = sp(11),
            colorDotSize = w(32),
            maxContentWidth = 480.dp,
            bottomButtonGap = h(16),
            gradientBarGap = h(80)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun StoryCreatorDialog(
    onDismiss: () -> Unit,
    onPublish: (mediaPath: String, caption: String?, textColor: String?, textStyle: String?, createdAt: Long) -> Unit
) {
    val quickPickGradients = listOf(
        "gradient_sunset", "gradient_blue", "gradient_orange",
        "gradient_nature", "gradient_temple", "gradient_purple", "gradient_slate"
    )
    val fontStyles = listOf("MODERN", "CLASSIC", "NEON", "TYPEWRITER", "SERIF", "STRONG", "ELEGANT")
    val textColors = listOf(
        "#FFFFFFFF" to stringResource(R.string.color_white),
        "#FF000000" to stringResource(R.string.color_black),
        "#FFFFEB3B" to stringResource(R.string.color_yellow),
        "#FF8BC34A" to stringResource(R.string.color_green),
        "#FFFF4081" to stringResource(R.string.color_pink),
        "#FF00E5FF" to stringResource(R.string.color_cyan),
        "#FFE040FB" to stringResource(R.string.color_purple),
        "#FFFF9800" to stringResource(R.string.color_orange),
        "#FFF44336" to stringResource(R.string.color_red),
        "#FF2196F3" to stringResource(R.string.color_blue)
    )
    val filters = listOf(
        stringResource(R.string.filter_normal),
        stringResource(R.string.filter_warm),
        stringResource(R.string.filter_cool),
        stringResource(R.string.filter_vintage),
        stringResource(R.string.filter_bw),
        stringResource(R.string.filter_dramatic)
    )
    var selectedGradient by remember { mutableStateOf(quickPickGradients.first()) }
    var captionText by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("MODERN") }
    var selectedColorPair by remember { mutableStateOf(textColors.first()) }
    var storyDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isEditingText by remember { mutableStateOf(false) }
    var textAlignState by remember { mutableStateOf(TextAlign.Center) }
    var textBgMode by remember { mutableIntStateOf(0) }
    var textPositionFraction by remember { mutableStateOf(DEFAULT_TEXT_POSITION_FRACTION) }
    var activeFilterIndex by remember { mutableIntStateOf(0) }
    var scale by remember { mutableFloatStateOf(1f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val isGesturing by remember {
        derivedStateOf { scale != 1f || rotation != 0f || offset != Offset.Zero }
    }
    var showDateBadge by remember { mutableStateOf(true) }
    var lastBackTime by remember { mutableLongStateOf(0L) }
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    androidx.activity.compose.BackHandler {
        if (isEditingText) {
            isEditingText = false
        } else {
            val now = System.currentTimeMillis()
            if (now - lastBackTime < 2000) {
                onDismiss()
            } else {
                lastBackTime = now
                Toast.makeText(context, context.getString(R.string.msg_cancel_story), Toast.LENGTH_SHORT).show()
            }
        }
    }
    var latestCapturedPhotoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val localPath = FileUtils.saveUriToInternalStorage(context, uri)
            if (localPath != null) {
                selectedGradient = localPath
                scale = 1f
                rotation = 0f
                offset = Offset.Zero
            }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            latestCapturedPhotoUri?.let { uri ->
                val localPath = FileUtils.saveUriToInternalStorage(context, uri)
                if (localPath != null) {
                    selectedGradient = localPath
                    scale = 1f
                    rotation = 0f
                    offset = Offset.Zero
                }
            }
        }
        latestCapturedPhotoUri = null
    }
    val parsedColor = remember(selectedColorPair.first) {
        try {
            Color(android.graphics.Color.parseColor(selectedColorPair.first))
        } catch (e: Exception) {
            Color.White
        }
    }
    val currentFontFamily = when (selectedStyle) {
        "CLASSIC", "SERIF" -> FontFamily.Serif
        "TYPEWRITER" -> FontFamily.Monospace
        "STRONG" -> FontFamily.SansSerif
        else -> FontFamily.Default
    }
    val currentFontStyle = if (selectedStyle == "ELEGANT") FontStyle.Italic else FontStyle.Normal
    val currentFontWeight = when (selectedStyle) {
        "STRONG", "MODERN" -> FontWeight.Black
        "NEON" -> FontWeight.Bold
        else -> FontWeight.Normal
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val view = LocalView.current
            SideEffect {
                val window = (view.parent as? DialogWindowProvider)?.window
                if (window != null) {
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                }
            }
            val dimens = rememberStoryDimens(maxWidth, maxHeight)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = dimens.maxContentWidth)
                    .align(Alignment.TopCenter)
            ) {
                StoryCreatorContent(
                    dimens = dimens,
                    canvasWidth = this@BoxWithConstraints.maxWidth,
                    canvasHeight = this@BoxWithConstraints.maxHeight,
                    textPositionFraction = textPositionFraction,
                    onTextPositionFractionChange = { textPositionFraction = it },
                    quickPickGradients = quickPickGradients,
                    fontStyles = fontStyles,
                    textColors = textColors,
                    filters = filters,
                    selectedGradient = selectedGradient,
                    onSelectedGradientChange = { selectedGradient = it },
                    captionText = captionText,
                    onCaptionTextChange = { captionText = it },
                    selectedStyle = selectedStyle,
                    onSelectedStyleChange = { selectedStyle = it },
                    selectedColorPair = selectedColorPair,
                    onSelectedColorPairChange = { selectedColorPair = it },
                    storyDate = storyDate,
                    onStoryDateChange = { storyDate = it },
                    isEditingText = isEditingText,
                    onIsEditingTextChange = { isEditingText = it },
                    textAlignState = textAlignState,
                    onTextAlignChange = { textAlignState = it },
                    textBgMode = textBgMode,
                    onTextBgModeChange = { textBgMode = it },
                    activeFilterIndex = activeFilterIndex,
                    onActiveFilterIndexChange = { activeFilterIndex = it },
                    scale = scale,
                    onScaleChange = { scale = it },
                    rotation = rotation,
                    onRotationChange = { rotation = it },
                    offset = offset,
                    onOffsetChange = { offset = it },
                    isGesturing = isGesturing,
                    showDateBadge = showDateBadge,
                    onShowDateBadgeChange = { showDateBadge = it },
                    context = context,
                    cameraPermissionState = cameraPermissionState,
                    latestCapturedPhotoUri = latestCapturedPhotoUri,
                    onLatestCapturedPhotoUriChange = { latestCapturedPhotoUri = it },
                    galleryLauncher = galleryLauncher,
                    cameraLauncher = cameraLauncher,
                    parsedColor = parsedColor,
                    currentFontFamily = currentFontFamily,
                    currentFontStyle = currentFontStyle,
                    currentFontWeight = currentFontWeight,
                    onDismiss = onDismiss,
                    onPublish = onPublish
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
private fun StoryCreatorContent(
    dimens: StoryDimens,
    canvasWidth: Dp,
    canvasHeight: Dp,
    textPositionFraction: Offset,
    onTextPositionFractionChange: (Offset) -> Unit,
    quickPickGradients: List<String>,
    fontStyles: List<String>,
    textColors: List<Pair<String, String>>,
    filters: List<String>,
    selectedGradient: String,
    onSelectedGradientChange: (String) -> Unit,
    captionText: String,
    onCaptionTextChange: (String) -> Unit,
    selectedStyle: String,
    onSelectedStyleChange: (String) -> Unit,
    selectedColorPair: Pair<String, String>,
    onSelectedColorPairChange: (Pair<String, String>) -> Unit,
    storyDate: Long,
    onStoryDateChange: (Long) -> Unit,
    isEditingText: Boolean,
    onIsEditingTextChange: (Boolean) -> Unit,
    textAlignState: TextAlign,
    onTextAlignChange: (TextAlign) -> Unit,
    textBgMode: Int,
    onTextBgModeChange: (Int) -> Unit,
    activeFilterIndex: Int,
    onActiveFilterIndexChange: (Int) -> Unit,
    scale: Float,
    onScaleChange: (Float) -> Unit,
    rotation: Float,
    onRotationChange: (Float) -> Unit,
    offset: Offset,
    onOffsetChange: (Offset) -> Unit,
    isGesturing: Boolean,
    showDateBadge: Boolean,
    onShowDateBadgeChange: (Boolean) -> Unit,
    context: android.content.Context,
    cameraPermissionState: com.google.accompanist.permissions.PermissionState,
    latestCapturedPhotoUri: Uri?,
    onLatestCapturedPhotoUriChange: (Uri?) -> Unit,
    galleryLauncher: androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest>,
    cameraLauncher: androidx.activity.result.ActivityResultLauncher<Uri>,
    parsedColor: Color,
    currentFontFamily: FontFamily,
    currentFontStyle: FontStyle,
    currentFontWeight: FontWeight,
    onDismiss: () -> Unit,
    onPublish: (mediaPath: String, caption: String?, textColor: String?, textStyle: String?, createdAt: Long) -> Unit
) {
    val density = LocalDensity.current
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        containerColor = Color.Black
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .clipToBounds()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, rotate ->
                            onScaleChange((scale * zoom).coerceIn(0.5f, 5f))
                            onRotationChange(rotation + rotate)
                            onOffsetChange(offset + pan)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 16f)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            rotationZ = rotation
                            translationX = offset.x
                            translationY = offset.y
                        }
                ) {
                    MediaView(path = selectedGradient, modifier = Modifier.fillMaxSize())
                    val filterName = filters[activeFilterIndex]
                    val filterColor = when {
                        filterName == context.getString(R.string.filter_warm) -> Color(0x33FF9800)
                        filterName == context.getString(R.string.filter_cool) -> Color(0x3300E5FF)
                        filterName == context.getString(R.string.filter_vintage) -> Color(0x40795548)
                        filterName == context.getString(R.string.filter_bw) -> Color(0x80000000)
                        filterName == context.getString(R.string.filter_dramatic) -> Color(0x55000000)
                        else -> Color.Transparent
                    }
                    if (filterColor != Color.Transparent) {
                        Box(modifier = Modifier.fillMaxSize().background(filterColor))
                    }
                }
                AnimatedVisibility(
                    visible = isGesturing,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
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
                                ).apply {
                                    datePicker.maxDate = System.currentTimeMillis()
                                }.show()
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.DateRange, null, tint = Color.White, modifier = Modifier.size(dimens.smallIconSize))
                            val currentLocale = context.resources.configuration.locales[0]
                            Text(
                                text = SimpleDateFormat("dd MMM yyyy", currentLocale).format(Date(storyDate)).uppercase(),
                                color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (captionText.isNotBlank() && !isEditingText) {
                    val textBgModifier = when (textBgMode) {
                        1 -> Modifier.background(parsedColor, RoundedCornerShape(8.dp)).padding(horizontal = 14.dp, vertical = 8.dp)
                        2 -> Modifier.background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp)).border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(horizontal = 14.dp, vertical = 8.dp)
                        else -> Modifier.padding(12.dp)
                    }
                    val textColorToUse = if (textBgMode == 1) {
                        if (selectedColorPair.first == "#FFFFFFFF" || selectedColorPair.first == "#FFFFEB3B") Color.Black else Color.White
                    } else parsedColor
                    val canvasWidthPx = with(density) { canvasWidth.toPx() }
                    val canvasHeightPx = with(density) { canvasHeight.toPx() }
                    var elementSizePx by remember { mutableStateOf(IntSize.Zero) }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .onGloballyPositioned { coords -> elementSizePx = coords.size }
                            .offset {
                                val targetX = textPositionFraction.x * canvasWidthPx - elementSizePx.width / 2f
                                val targetY = textPositionFraction.y * canvasHeightPx - elementSizePx.height / 2f
                                IntOffset(targetX.roundToInt(), targetY.roundToInt())
                            }
                            .pointerInput(canvasWidthPx, canvasHeightPx) {
                                detectDragGestures(
                                    onDragStart = { onIsEditingTextChange(false) }
                                ) { change, dragAmount ->
                                    change.consume()
                                    val newX = (textPositionFraction.x + dragAmount.x / canvasWidthPx)
                                        .coerceIn(ELEMENT_SAFE_BOUNDS.left, ELEMENT_SAFE_BOUNDS.right)
                                    val newY = (textPositionFraction.y + dragAmount.y / canvasHeightPx)
                                        .coerceIn(ELEMENT_SAFE_BOUNDS.top, ELEMENT_SAFE_BOUNDS.bottom)
                                    onTextPositionFractionChange(Offset(newX, newY))
                                }
                            }
                            .clickable { onIsEditingTextChange(true) }
                            .widthIn(max = canvasWidth * 0.85f)
                    ) {
                        Text(
                            text = captionText,
                            color = textColorToUse,
                            style = TextStyle(
                                fontSize = dimens.captionFontSize,
                                fontWeight = currentFontWeight,
                                fontFamily = currentFontFamily,
                                fontStyle = currentFontStyle,
                                textAlign = textAlignState,
                                shadow = if (textBgMode == 0) Shadow(color = Color.Black.copy(alpha = 0.9f), offset = Offset(2f, 2f), blurRadius = 6f) else null
                            ),
                            modifier = Modifier.then(textBgModifier)
                        )
                    }
                }
                if (captionText.isBlank() && !isEditingText) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset {
                                IntOffset(
                                    x = (DEFAULT_TEXT_POSITION_FRACTION.x * with(density) { canvasWidth.toPx() }).roundToInt(),
                                    y = (DEFAULT_TEXT_POSITION_FRACTION.y * with(density) { canvasHeight.toPx() }).roundToInt()
                                )
                            }
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .clickable { onIsEditingTextChange(true) }
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.TextFields, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(dimens.smallIconSize))
                            Text(stringResource(R.string.label_tap_text), color = Color.White.copy(alpha = 0.9f), fontSize = dimens.placeholderFontSize, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = !isEditingText,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = dimens.topBarVerticalPad),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onDismiss() },
                        modifier = Modifier.size(dimens.iconButtonSize).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Close, stringResource(R.string.btn_done), tint = Color.White, modifier = Modifier.size(dimens.iconSize))
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onShowDateBadgeChange(!showDateBadge) },
                            modifier = Modifier.size(dimens.iconButtonSize).clip(CircleShape).background(if (showDateBadge) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.DateRange, stringResource(R.string.label_date), tint = Color.White, modifier = Modifier.size(dimens.smallIconSize + 2.dp))
                        }
                        IconButton(
                            onClick = {
                                val next = (activeFilterIndex + 1) % filters.size
                                onActiveFilterIndexChange(next)
                                val msg = context.getString(R.string.msg_filter_applied, filters[next])
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(dimens.iconButtonSize).clip(CircleShape).background(if (activeFilterIndex > 0) Color(0xFFE1306C) else Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.AutoAwesome, "Filter", tint = Color.White, modifier = Modifier.size(dimens.smallIconSize + 2.dp))
                        }
                        if (scale != 1f || rotation != 0f || offset != Offset.Zero) {
                            IconButton(
                                onClick = { onScaleChange(1f); onRotationChange(0f); onOffsetChange(Offset.Zero) },
                                modifier = Modifier.size(dimens.iconButtonSize).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Refresh, stringResource(R.string.label_reset), tint = Color.White, modifier = Modifier.size(dimens.smallIconSize + 2.dp))
                            }
                        }
                    }
                }
            }
            if (!isEditingText) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = dimens.bottomBarExtraPad + dimens.bottomButtonGap)
                        .clip(CircleShape)
                        .background(if (captionText.isNotEmpty()) Color.White else Color.Black.copy(alpha = 0.6f))
                        .clickable { onIsEditingTextChange(true) }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.TextFields, stringResource(R.string.label_text), tint = if (captionText.isNotEmpty()) Color.Black else Color.White, modifier = Modifier.size(20.dp))
                        Text(if (captionText.isNotEmpty()) stringResource(R.string.btn_edit_text) else stringResource(R.string.btn_add_text), color = if (captionText.isNotEmpty()) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                IconButton(
                    onClick = {
                        onPublish(selectedGradient, if (captionText.isNotBlank()) captionText else null, selectedColorPair.first, selectedStyle, storyDate)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = 16.dp, bottom = dimens.bottomBarExtraPad + dimens.bottomButtonGap)
                        .size(dimens.publishBtnSize + 22.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .testTag("story_share_submit_fab")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.btn_publish),
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = 20.dp, bottom = dimens.bottomBarExtraPad + dimens.bottomButtonGap + 82.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (cameraPermissionState.status.isGranted) {
                                val uri = FileUtils.createImageFileUri(context)
                                onLatestCapturedPhotoUriChange(uri)
                                cameraLauncher.launch(uri)
                            } else {
                                cameraPermissionState.launchPermissionRequest()
                            }
                        },
                        modifier = Modifier
                            .size(dimens.galleryButtonSize + 4.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = stringResource(R.string.item_camera),
                            tint = Color.White,
                            modifier = Modifier.size(dimens.iconSize)
                        )
                    }
                    val isPhotoSelected = !selectedGradient.startsWith("gradient_")
                    IconButton(
                        onClick = {
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier
                            .size(dimens.galleryButtonSize + 14.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(if (isPhotoSelected) 2.dp else 1.dp, if (isPhotoSelected) Color.White else Color.White.copy(alpha = 0.5f), CircleShape)
                    ) {
                        if (isPhotoSelected) {
                            MediaView(path = selectedGradient, modifier = Modifier.fillMaxSize().clip(CircleShape))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = stringResource(R.string.item_gallery),
                                tint = Color.White,
                                modifier = Modifier.size(dimens.iconSize)
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .navigationBarsPadding()
                        .padding(start = 16.dp, bottom = dimens.bottomBarExtraPad + dimens.gradientBarGap)
                        .fillMaxWidth()
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(end = 120.dp)
                    ) {
                        items(quickPickGradients) { gradientId ->
                            val isSelected = selectedGradient == gradientId
                            Box(
                                modifier = Modifier
                                    .size(dimens.swatchSize + 4.dp)
                                    .border(if (isSelected) 2.dp else 1.dp, if (isSelected) Color.White else Color.White.copy(alpha = 0.4f), CircleShape)
                                    .padding(3.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        onSelectedGradientChange(gradientId)
                                        onScaleChange(1f); onRotationChange(0f); onOffsetChange(Offset.Zero)
                                    }
                            ) {
                                MediaView(path = gradientId, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
            if (isEditingText) {
                val textFieldFocusRequester = remember { FocusRequester() }
                val keyboardController = LocalSoftwareKeyboardController.current
                LaunchedEffect(Unit) {
                    textFieldFocusRequester.requestFocus()
                    keyboardController?.show()
                }
                Dialog(
                    onDismissRequest = { onIsEditingTextChange(false) },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.9f))
                            .systemBarsPadding()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    onTextAlignChange(
                                        when (textAlignState) {
                                            TextAlign.Left -> TextAlign.Center
                                            TextAlign.Center -> TextAlign.Right
                                            else -> TextAlign.Left
                                        }
                                    )
                                },
                                modifier = Modifier.size(dimens.iconButtonSize).clip(CircleShape).background(Color.White.copy(alpha = 0.2f))
                            ) {
                                Icon(
                                    imageVector = when (textAlignState) {
                                        TextAlign.Left -> Icons.Default.FormatAlignLeft
                                        TextAlign.Right -> Icons.Default.FormatAlignRight
                                        else -> Icons.Default.FormatAlignCenter
                                    },
                                    contentDescription = stringResource(R.string.label_align_text), tint = Color.White, modifier = Modifier.size(dimens.iconSize)
                                )
                            }
                            IconButton(
                                onClick = { onTextBgModeChange((textBgMode + 1) % 3) },
                                modifier = Modifier.size(dimens.iconButtonSize).clip(RoundedCornerShape(10.dp)).background(if (textBgMode > 0) Color.White else Color.White.copy(alpha = 0.2f))
                            ) {
                                Text("A", color = if (textBgMode > 0) Color.Black else Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            }
                            Button(
                                onClick = { onIsEditingTextChange(false) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(stringResource(R.string.btn_done), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth().align(Alignment.Center).padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val editTextBg = when (textBgMode) {
                                1 -> Modifier.background(parsedColor, RoundedCornerShape(10.dp)).padding(horizontal = 16.dp, vertical = 10.dp)
                                2 -> Modifier.background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(10.dp)).border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(10.dp)).padding(horizontal = 16.dp, vertical = 10.dp)
                                else -> Modifier.padding(12.dp)
                            }
                            val editTextColor = if (textBgMode == 1) {
                                if (selectedColorPair.first == "#FFFFFFFF" || selectedColorPair.first == "#FFFFEB3B") Color.Black else Color.White
                            } else parsedColor
                            BasicTextField(
                                value = captionText,
                                onValueChange = onCaptionTextChange,
                                textStyle = TextStyle(
                                    color = editTextColor,
                                    fontSize = dimens.captionFontSize,
                                    fontWeight = currentFontWeight,
                                    fontFamily = currentFontFamily,
                                    fontStyle = currentFontStyle,
                                    textAlign = textAlignState,
                                    shadow = if (textBgMode == 0) Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 6f) else null
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(editTextBg)
                                    .focusRequester(textFieldFocusRequester),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(editTextColor)
                            )
                            if (captionText.isEmpty()) {
                                Text(stringResource(R.string.placeholder_type_text), color = Color.White.copy(alpha = 0.5f), fontSize = dimens.captionFontSize, fontWeight = FontWeight.Bold, textAlign = textAlignState, modifier = Modifier.fillMaxWidth())
                            }
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 8.dp)) {
                                items(fontStyles) { fontName ->
                                    val isSelected = selectedStyle == fontName
                                    val styleRes = when (fontName) {
                                        "MODERN" -> R.string.style_modern
                                        "CLASSIC" -> R.string.style_classic
                                        "NEON" -> R.string.style_neon
                                        "TYPEWRITER" -> R.string.style_typewriter
                                        "SERIF" -> R.string.style_serif
                                        "STRONG" -> R.string.style_strong
                                        "ELEGANT" -> R.string.style_elegant
                                        else -> R.string.style_modern
                                    }
                                    Box(
                                        modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(if (isSelected) Color.White else Color.White.copy(alpha = 0.2f)).clickable { onSelectedStyleChange(fontName) }.padding(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        Text(stringResource(styleRes), fontSize = dimens.fontChipFontSize, fontWeight = FontWeight.Bold, color = if (isSelected) Color.Black else Color.White)
                                    }
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 8.dp)) {
                                items(textColors) { colorPair ->
                                    val isSelected = selectedColorPair.first == colorPair.first
                                    val dotColor = try { Color(android.graphics.Color.parseColor(colorPair.first)) } catch (e: Exception) { Color.White }
                                    Box(
                                        modifier = Modifier.size(dimens.colorDotSize).border(if (isSelected) 3.dp else 1.dp, if (isSelected) Color.White else Color.White.copy(alpha = 0.3f), CircleShape).padding(3.dp).clip(CircleShape).background(dotColor).clickable { onSelectedColorPairChange(colorPair) }
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

@Preview(name = "Short phone", showSystemUi = true, device = "spec:width=360dp,height=640dp")
@Composable
fun StoryCreatorPreviewShort() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        StoryCreatorDialog(onDismiss = {}, onPublish = { _, _, _, _, _ -> })
    }
}

@Preview(name = "Tablet", showSystemUi = true, device = "spec:width=800dp,height=1280dp")
@Composable
fun StoryCreatorPreviewTablet() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        StoryCreatorDialog(onDismiss = {}, onPublish = { _, _, _, _, _ -> })
    }
}
