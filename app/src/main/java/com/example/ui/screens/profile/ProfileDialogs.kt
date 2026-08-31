package com.example.ui.screens.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.StoryHighlightEntity
import com.example.ui.components.MediaView
import com.example.ui.viewmodel.MemoirViewModel
import com.example.utils.FileUtils
import com.yalantis.ucrop.UCrop
import java.io.File
import java.text.SimpleDateFormat
import java.util.*




class UCropContract : ActivityResultContract<Pair<Uri, Uri>, Uri?>() {
    override fun createIntent(context: android.content.Context, input: Pair<Uri, Uri>): android.content.Intent {
        val (sourceUri, destinationUri) = input
        return UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f) 
            .withMaxResultSize(1000, 1000)
            .getIntent(context)
    }

    override fun parseResult(resultCode: Int, intent: android.content.Intent?): Uri? {
        return if (resultCode == android.app.Activity.RESULT_OK && intent != null) {
            UCrop.getOutput(intent)
        } else {
            null
        }
    }
}

@Composable
fun EditProfileDialog(
    currentName: String,
    currentUsername: String,
    currentBio: String,
    currentImagePath: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String?) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var uname by remember { mutableStateOf(currentUsername) }
    var bio by remember { mutableStateOf(currentBio) }
    var imagePath by remember { mutableStateOf(currentImagePath) }

    val context = LocalContext.current
    
    val cropLauncher = rememberLauncherForActivityResult(
        contract = UCropContract()
    ) { uri ->
        if (uri != null) {
            val file = File(uri.path!!)
            val profileDir = FileUtils.getProfileDir(context)
            val destFile = File(profileDir, "profile_${System.currentTimeMillis()}.jpg")
            
            try {
                file.copyTo(destFile, overwrite = true)
                imagePath = "profile/${destFile.name}"
            } catch (e: Exception) {
                val msg = context.getString(R.string.msg_save_profile_failed)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val destinationUri = Uri.fromFile(File(context.cacheDir, "temp_profile_crop.jpg"))
            cropLauncher.launch(Pair(uri, destinationUri))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_profile)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .border(
                            2.dp,
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            ),
                            CircleShape
                        )
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (!imagePath.isNullOrEmpty()) {
                        MediaView(
                            path = imagePath!!,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.label_me), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = stringResource(R.string.label_change_photo),
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                TextButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.testTag("change_profile_picture_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_pick_gallery), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.label_or_preset),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        val presets = listOf("gradient_sunset", "gradient_blue", "gradient_orange", "gradient_nature", "gradient_purple", "gradient_slate")
                        presets.forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (imagePath == preset) 2.5.dp else 0.dp,
                                        color = if (imagePath == preset) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { imagePath = preset }
                            ) {
                                MediaView(path = preset, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.label_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_name_input")
                )
                OutlinedTextField(
                    value = uname,
                    onValueChange = { uname = it },
                    label = { Text(stringResource(R.string.label_username)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_username_input")
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text(stringResource(R.string.label_bio)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("edit_bio_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, uname, bio, imagePath) },
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
fun AddHighlightDialog(
    viewModel: MemoirViewModel,
    onDismiss: () -> Unit,
    onSave: (String, String, List<Long>) -> Unit
) {
    var highlightName by remember { mutableStateOf("") }
    var selectedCover by remember { mutableStateOf("gradient_sunset") }
    val stories by viewModel.stories.collectAsStateWithLifecycle()
    val selectedStoryIds = remember { mutableStateListOf<Long>() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCoverPickerSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val localPath = FileUtils.saveUriToInternalStorage(context, uri)
            if (localPath != null) {
                selectedCover = localPath
            }
        }
    }

    val displayStories = remember(stories) {
        if (stories.isNotEmpty()) stories else listOf(
            com.example.data.database.StoryEntity(id = 101, mediaPath = "gradient_sunset", caption = "Liburan Sanur", createdAt = System.currentTimeMillis() - 86400000L),
            com.example.data.database.StoryEntity(id = 102, mediaPath = "gradient_blue", caption = "Danau Beratan", createdAt = System.currentTimeMillis() - 172800000L),
            com.example.data.database.StoryEntity(id = 103, mediaPath = "gradient_orange", caption = "Senja Cafe", createdAt = System.currentTimeMillis() - 259200000L),
            com.example.data.database.StoryEntity(id = 104, mediaPath = "gradient_nature", caption = "Ubud Rice Fields", createdAt = System.currentTimeMillis() - 345600000L),
            com.example.data.database.StoryEntity(id = 105, mediaPath = "gradient_temple", caption = "Candi Prambanan", createdAt = System.currentTimeMillis() - 432000000L),
            com.example.data.database.StoryEntity(id = 106, mediaPath = "gradient_purple", caption = "Night Vibes", createdAt = System.currentTimeMillis() - 518400000L)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.btn_cancel), color = Color.White, fontSize = 15.sp)
                    }

                    Text(
                        text = stringResource(R.string.new_highlight),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    TextButton(
                        onClick = {
                            val finalName = if (highlightName.isNotBlank()) highlightName else context.getString(R.string.label_highlight)
                            onSave(finalName, selectedCover, selectedStoryIds.toList())
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.btn_next_step),
                            color = Color(0xFF0095F6),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color(0xFF0095F6), CircleShape)
                            .clickable { showCoverPickerSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        MediaView(
                            path = selectedCover,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(R.string.label_edit_cover),
                        color = Color(0xFF0095F6),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { showCoverPickerSheet = true }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = highlightName,
                        onValueChange = { highlightName = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.placeholder_highlight_name_alt),
                                color = Color.Gray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E1E1E),
                            unfocusedContainerColor = Color(0xFF1E1E1E),
                            focusedIndicatorColor = Color(0xFF0095F6),
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .testTag("add_highlight_name_input")
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                 TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF121212),
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color.White
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                stringResource(R.string.tab_story),
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) Color.White else Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                stringResource(R.string.tab_selected, selectedStoryIds.size),
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) Color.White else Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    )
                }

                val storiesForCurrentTab = if (selectedTab == 0) {
                    displayStories
                } else {
                    displayStories.filter { selectedStoryIds.contains(it.id) }
                }

                if (storiesForCurrentTab.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedTab == 1) stringResource(R.string.msg_no_story_selected) else stringResource(R.string.msg_no_story_archive),
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(storiesForCurrentTab) { story ->
                            val isSelected = selectedStoryIds.contains(story.id)

                            Box(
                                modifier = Modifier
                                    .aspectRatio(0.68f)
                                    .clickable {
                                        if (isSelected) {
                                            selectedStoryIds.remove(story.id)
                                        } else {
                                            selectedStoryIds.add(story.id)
                                            if (selectedCover == "gradient_sunset") {
                                                selectedCover = story.mediaPath
                                            }
                                        }
                                    }
                            ) {
                                MediaView(
                                    path = story.mediaPath,
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.25f))
                                    )
                                }

                                val currentLocale = context.resources.configuration.locales[0]
                                val dateStr = SimpleDateFormat("dd MMM", currentLocale).format(Date(story.createdAt))
                                Text(
                                    text = dateStr,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(6.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color(0xFF0095F6) else Color.Black.copy(alpha = 0.4f)
                                        )
                                        .border(
                                            1.5.dp,
                                            if (isSelected) Color(0xFF0095F6) else Color.White,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
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

    if (showCoverPickerSheet) {
        AlertDialog(
            onDismissRequest = { showCoverPickerSheet = false },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text(stringResource(R.string.title_choose_cover), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                val coverCandidates = (displayStories.map { it.mediaPath } + listOf("gradient_sunset", "gradient_blue", "gradient_orange", "gradient_nature", "gradient_temple", "gradient_purple", "gradient_slate")).distinct()
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            galleryPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            showCoverPickerSheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0095F6))
                    ) {
                        Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_pick_gallery))
                    }

                    Text(stringResource(R.string.label_choose_story_preset), fontSize = 12.sp, color = Color.Gray)

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        items(coverCandidates) { path ->
                            val isSel = selectedCover == path
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isSel) 3.dp else 1.dp,
                                        color = if (isSel) Color(0xFF0095F6) else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        selectedCover = path
                                        showCoverPickerSheet = false
                                    }
                            ) {
                                MediaView(path = path, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCoverPickerSheet = false }) {
                    Text(stringResource(R.string.btn_done), color = Color(0xFF0095F6))
                }
            }
        )
    }
}

@Composable
fun EditHighlightDialog(
    highlight: StoryHighlightEntity,
    viewModel: MemoirViewModel,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onDelete: () -> Unit
) {
    var highlightName by remember { mutableStateOf(highlight.name) }
    var selectedCover by remember { mutableStateOf(highlight.coverPath) }
    val stories by viewModel.stories.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var showCoverPickerSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val localPath = FileUtils.saveUriToInternalStorage(context, uri)
            if (localPath != null) {
                selectedCover = localPath
            }
        }
    }

    val allStories = remember(stories) {
        if (stories.isNotEmpty()) stories else listOf(
            com.example.data.database.StoryEntity(id = 201, highlightId = highlight.id, mediaPath = highlight.coverPath, caption = "Moment 1", createdAt = System.currentTimeMillis() - 86400000L),
            com.example.data.database.StoryEntity(id = 202, highlightId = highlight.id, mediaPath = "gradient_blue", caption = "Moment 2", createdAt = System.currentTimeMillis() - 172800000L),
            com.example.data.database.StoryEntity(id = 203, highlightId = null, mediaPath = "gradient_orange", caption = "Moment 3", createdAt = System.currentTimeMillis() - 259200000L),
            com.example.data.database.StoryEntity(id = 204, highlightId = null, mediaPath = "gradient_nature", caption = "Moment 4", createdAt = System.currentTimeMillis() - 345600000L)
        )
    }

    val currentHighlightStories = allStories.filter { it.highlightId == highlight.id }
    val otherStories = allStories.filter { it.highlightId != highlight.id }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.btn_cancel), color = Color.White, fontSize = 15.sp)
                    }

                    Text(
                        text = stringResource(R.string.btn_edit_highlight),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    TextButton(
                        onClick = {
                            if (highlightName.isNotBlank()) {
                                onSave(highlightName, selectedCover)
                            }
                        },
                        modifier = Modifier.testTag("save_edit_highlight_button")
                    ) {
                        Text(
                            text = stringResource(R.string.btn_done),
                            color = Color(0xFF0095F6),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color(0xFF0095F6), CircleShape)
                            .clickable { showCoverPickerSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        MediaView(
                            path = selectedCover,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(R.string.label_edit_cover),
                        color = Color(0xFF0095F6),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { showCoverPickerSheet = true }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = highlightName,
                        onValueChange = { highlightName = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.placeholder_highlight_name_alt),
                                color = Color.Gray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E1E1E),
                            unfocusedContainerColor = Color(0xFF1E1E1E),
                            focusedIndicatorColor = Color(0xFF0095F6),
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .testTag("edit_highlight_name_input")
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF121212),
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color.White
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                stringResource(R.string.tab_selected, currentHighlightStories.size),
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) Color.White else Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                stringResource(R.string.tab_add, otherStories.size),
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) Color.White else Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    )
                }

                val activeStoriesTab = if (selectedTab == 0) currentHighlightStories else otherStories

                if (activeStoriesTab.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedTab == 0) stringResource(R.string.msg_no_story_highlight) else stringResource(R.string.msg_no_story_extra),
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(activeStoriesTab) { story ->
                            val isInHighlight = story.highlightId == highlight.id

                            Box(
                                modifier = Modifier
                                    .aspectRatio(0.68f)
                                    .clickable {
                                        if (isInHighlight) {
                                            viewModel.linkStoryToHighlight(story, null)
                                            val msg = context.getString(R.string.msg_story_removed_highlight)
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.linkStoryToHighlight(story, highlight.id)
                                            val msg = context.getString(R.string.msg_story_added_highlight)
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            ) {
                                MediaView(
                                    path = story.mediaPath,
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (isInHighlight) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.25f))
                                    )
                                }

                                val currentLocale = context.resources.configuration.locales[0]
                                val dateStr = SimpleDateFormat("dd MMM", currentLocale).format(Date(story.createdAt))
                                Text(
                                    text = dateStr,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(6.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isInHighlight) Color(0xFF0095F6) else Color.Black.copy(alpha = 0.4f)
                                        )
                                        .border(
                                            1.5.dp,
                                            if (isInHighlight) Color(0xFF0095F6) else Color.White,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isInHighlight) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "In Highlight",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDeleteConfirmDialog = true }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.btn_delete_highlight),
                        tint = Color(0xFFED4956),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_delete_highlight),
                        color = Color(0xFFED4956),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    if (showCoverPickerSheet) {
        AlertDialog(
            onDismissRequest = { showCoverPickerSheet = false },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text(stringResource(R.string.title_choose_cover), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                val coverCandidates = (allStories.map { it.mediaPath } + listOf("gradient_sunset", "gradient_blue", "gradient_orange", "gradient_nature", "gradient_temple", "gradient_purple", "gradient_slate")).distinct()
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            galleryPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            showCoverPickerSheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0095F6))
                    ) {
                        Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_pick_gallery))
                    }

                    Text(stringResource(R.string.label_choose_story_preset), fontSize = 12.sp, color = Color.Gray)

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        items(coverCandidates) { path ->
                            val isSel = selectedCover == path
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isSel) 3.dp else 1.dp,
                                        color = if (isSel) Color(0xFF0095F6) else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        selectedCover = path
                                        showCoverPickerSheet = false
                                    }
                            ) {
                                MediaView(path = path, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCoverPickerSheet = false }) {
                    Text(stringResource(R.string.btn_done), color = Color(0xFF0095F6))
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = Color(0xFF262626),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text(stringResource(R.string.title_delete_highlight), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.msg_delete_highlight)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
                    }
                ) {
                    Text(stringResource(R.string.btn_delete), color = Color(0xFFED4956), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.btn_cancel), color = Color.White)
                }
            }
        )
    }
}
