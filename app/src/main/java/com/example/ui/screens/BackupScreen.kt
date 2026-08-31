package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.database.PostEntity
import com.example.data.repository.BackupRepository
import com.example.ui.viewmodel.MemoirViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.res.stringResource
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(viewModel: MemoirViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    val backupRepo = remember {
        BackupRepository(context, com.example.data.database.MemoirDatabase.getDatabase(context).memoirDao())
    }

    val scope = rememberCoroutineScope()
    var isOperatingFolder by remember { mutableStateOf(false) }

    val exportFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            isOperatingFolder = true
            scope.launch {
                val res = backupRepo.exportMemoirDataToDocumentTree(context, it)
                isOperatingFolder = false
                res.fold(
                    onSuccess = { count ->
                        val msg = context.getString(R.string.msg_export_folder_success, count)
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    },
                    onFailure = { err ->
                        val msg = context.getString(R.string.msg_export_folder_failed, err.localizedMessage)
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    val importFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            isOperatingFolder = true
            scope.launch {
                val res = backupRepo.importMemoirDataFromDocumentTree(context, it)
                isOperatingFolder = false
                res.fold(
                    onSuccess = { count ->
                        val msg = context.getString(R.string.msg_import_folder_success, count)
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    },
                    onFailure = { err ->
                        val msg = context.getString(R.string.msg_import_folder_failed, err.localizedMessage)
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.title_backup_slideshow),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_cancel),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val tabLabels = listOf(stringResource(R.string.tab_backup_folder), stringResource(R.string.tab_slideshow))
                        tabLabels.forEachIndexed { i, label ->
                            val isSelected = selectedTab == i
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                shadowElevation = if (isSelected) 1.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTab = i }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                
                item {
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.FolderSpecial,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        stringResource(R.string.title_folder_backup),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        stringResource(R.string.label_post_folder_structure, posts.size),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            
                            Button(
                                onClick = { exportFolderLauncher.launch(null) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                enabled = !isOperatingFolder
                            ) {
                                if (isOperatingFolder) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.msg_processing_folder))
                                } else {
                                    Icon(Icons.Default.DriveFolderUpload, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.btn_export_folder_data), fontWeight = FontWeight.Bold)
                                }
                            }

                            
                            OutlinedButton(
                                onClick = { importFolderLauncher.launch(null) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                enabled = !isOperatingFolder
                            ) {
                                Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.title_import_folder))
                            }

                            
                            var isScanningFolder by remember { mutableStateOf(false) }
                            OutlinedButton(
                                onClick = { isScanningFolder = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                if (isScanningFolder) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.msg_scanning_folder))
                                } else {
                                    Icon(Icons.Default.Sync, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.btn_rescan_folder))
                                }
                            }

                            if (isScanningFolder) {
                                LaunchedEffect(isScanningFolder) {
                                    val count = backupRepo.scanAndIndexMemoirFolder(context)
                                    isScanningFolder = false
                                    val msg = context.getString(R.string.msg_rescan_success, count)
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }

                
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                stringResource(R.string.title_data_folder_guide),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp
                            )
                            Text(
                                stringResource(R.string.desc_data_folder_guide),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

            } else {
                
                item {
                    SlideshowSection(posts = posts, context = context)
                }
            }
        }
    }
}

@Composable
private fun SlideshowSection(posts: List<PostEntity>, context: android.content.Context) {
    val recentPosts = remember(posts) { posts.take(10) }
    var selectedPosts by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableIntStateOf(0) }

    
    LaunchedEffect(isPlaying) {
        if (isPlaying && recentPosts.isNotEmpty()) {
            while (isPlaying) {
                kotlinx.coroutines.delay(2500)
                currentIndex = (currentIndex + 1) % recentPosts.size
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            stringResource(R.string.title_slideshow_generator),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 16.sp
        )
        Text(
            stringResource(R.string.desc_slideshow_generator),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )

        
        if (recentPosts.isNotEmpty()) {
            val displayPosts = if (selectedPosts.isEmpty()) recentPosts else recentPosts.filter { it.id in selectedPosts }
            val current = displayPosts.getOrNull(currentIndex % displayPosts.size.coerceAtLeast(1))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (current != null) {
                    val firstPath = current.mediaPaths.firstOrNull()
                    if (!firstPath.isNullOrBlank()) {
                        AsyncImage(
                            model = com.example.utils.FileUtils.getLocalFile(context, firstPath),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        Box(
                            modifier = Modifier.fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f))))
                        )
                        Column(
                            modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (!current.caption.isNullOrBlank()) {
                                Text(current.caption, color = Color.White, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            val currentLocale = context.resources.configuration.locales[0]
                            Text(
                                SimpleDateFormat("dd MMM yyyy", currentLocale).format(Date(current.createdAt)),
                                color = Color.White.copy(0.6f), fontSize = 11.sp
                            )
                        }
                    }
                }

                
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                        .clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(0.6f)).padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("${(currentIndex % displayPosts.size.coerceAtLeast(1)) + 1}/${displayPosts.size}", color = Color.White, fontSize = 11.sp)
                }
            }

            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    currentIndex = (currentIndex - 1 + recentPosts.size) % recentPosts.size
                    isPlaying = false
                }) {
                    Icon(Icons.Default.SkipPrevious, stringResource(R.string.btn_previous), tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(16.dp))
                IconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                IconButton(onClick = {
                    currentIndex = (currentIndex + 1) % recentPosts.size
                    isPlaying = false
                }) {
                    Icon(Icons.Default.SkipNext, stringResource(R.string.btn_next), tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(28.dp))
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.msg_no_slideshow_post), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
        }

        
        Text(stringResource(R.string.label_select_photo_count, selectedPosts.size), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(recentPosts) { post ->
                val isSelected = post.id in selectedPosts
                val firstPath = post.mediaPaths.firstOrNull()
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            if (isSelected) 2.dp else 0.dp,
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            selectedPosts = if (isSelected) selectedPosts - post.id else selectedPosts + post.id
                        }
                ) {
                    AsyncImage(
                        model = firstPath?.let { com.example.utils.FileUtils.getLocalFile(context, it) },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
