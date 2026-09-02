package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.BackupRepository
import com.example.ui.viewmodel.MemoirViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(viewModel: MemoirViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val posts by viewModel.posts.collectAsStateWithLifecycle()
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
                        stringResource(R.string.title_folder_backup),
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
        }
    }
}
