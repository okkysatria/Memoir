package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.database.SourceConnectionEntity
import com.example.ui.viewmodel.MemoirViewModel
import com.example.ui.viewmodel.Screen
import com.example.utils.DiscoveredServer
import com.example.utils.SmbHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageConnectionsScreen(viewModel: MemoirViewModel) {
    val connections by viewModel.connections.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isAddingNew by remember { mutableStateOf(false) }

    var wizardStep by remember { mutableIntStateOf(1) }

    var serverHost by remember { mutableStateOf("") }
    var serverName by remember { mutableStateOf("") }
    var isGuestMode by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    var isDiscovering by remember { mutableStateOf(false) }
    var discoveredServers by remember { mutableStateOf<List<DiscoveredServer>>(emptyList()) }

    var shareNameInput by remember { mutableStateOf("") }
    var browserCurrentPath by remember { mutableStateOf("") }
    var browserFolders by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingBrowser by remember { mutableStateOf(false) }
    var selectedSavePath by remember { mutableStateOf("") }

    var isScanningShares by remember { mutableStateOf(false) }
    var discoveredShares by remember { mutableStateOf<List<String>>(emptyList()) }
    var scanSharesError by remember { mutableStateOf<String?>(null) }

    var isVerifying by remember { mutableStateOf(false) }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }
    var authSuccessMessage by remember { mutableStateOf<String?>(null) }
    var connectionToDelete by remember { mutableStateOf<SourceConnectionEntity?>(null) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderNameInput by remember { mutableStateOf("") }
    var isCreatingFolder by remember { mutableStateOf(false) }

    var testingConnectionId by remember { mutableStateOf<Long?>(null) }

    val sharePresets = remember { listOf("public", "share", "data", "photos", "nas", "storage", "backup", "memoir") }

    fun startDiscovery() {
        isDiscovering = true
        discoveredServers = emptyList()
        scope.launch {
            SmbHelper.discoverSmbServers(context) { server ->
                if (!discoveredServers.any { it.ip == server.ip }) {
                    discoveredServers = discoveredServers + server
                }
            }
            isDiscovering = false
        }
    }

    fun refreshBrowser(path: String, targetShare: String = shareNameInput) {
        if (serverHost.isBlank() || targetShare.isBlank()) return
        isLoadingBrowser = true
        browserCurrentPath = path
        scope.launch {
            val res = SmbHelper.listSubfoldersInShare(
                host = serverHost,
                shareName = targetShare,
                parentFolder = path,
                username = if (isGuestMode) "" else username,
                password = if (isGuestMode) "" else password,
                domain = domain
            )
            isLoadingBrowser = false
            res.fold(
                onSuccess = { browserFolders = it },
                onFailure = { err ->
                    browserFolders = emptyList()
                    Toast.makeText(context, "Error: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    fun scanShares() {
        if (serverHost.isBlank()) return
        isScanningShares = true
        scanSharesError = null
        viewModel.listSmbShares(
            host = serverHost,
            username = if (isGuestMode) "" else username,
            password = if (isGuestMode) "" else password,
            domain = domain
        ) { shares, err ->
            isScanningShares = false
            if (!shares.isNullOrEmpty()) {
                discoveredShares = shares
                if (shareNameInput.isBlank() && shares.size == 1) {
                    shareNameInput = shares.first()
                    refreshBrowser("", shares.first())
                }
            } else {
                scanSharesError = err ?: context.getString(R.string.msg_no_shares_found)
            }
        }
    }

    fun resetWizard() {
        serverHost = ""
        serverName = ""
        isGuestMode = false
        username = ""
        password = ""
        domain = ""
        showPassword = false
        shareNameInput = ""
        discoveredShares = emptyList()
        isScanningShares = false
        scanSharesError = null
        browserCurrentPath = ""
        browserFolders = emptyList()
        selectedSavePath = ""
        wizardStep = 1
        isAddingNew = false
    }

    LaunchedEffect(isAddingNew) {
        if (isAddingNew && discoveredServers.isEmpty()) {
            startDiscovery()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isAddingNew) {
                            when (wizardStep) {
                                1 -> stringResource(R.string.title_smb_step1)
                                else -> stringResource(R.string.title_smb_step2)
                            }
                        } else {
                            stringResource(R.string.title_manage_nas)
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isAddingNew) {
                            if (wizardStep > 1) {
                                wizardStep--
                            } else {
                                isAddingNew = false
                            }
                        } else {
                            viewModel.navigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                },
                actions = {
                    if (isAddingNew) {
                        TextButton(onClick = { resetWizard() }) {
                            Text(stringResource(R.string.btn_cancel), color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isAddingNew) {
                LinearProgressIndicator(
                    progress = { wizardStep / 2f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            AnimatedContent(
                targetState = isAddingNew,
                label = "ScreenModeTransition"
            ) { adding ->
                if (!adding) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Dns,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                stringResource(R.string.title_manage_nas),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                stringResource(R.string.desc_manage_smb),
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            isAddingNew = true
                                            wizardStep = 1
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            stringResource(R.string.btn_add_smb_connection),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                stringResource(R.string.label_saved_connections),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        if (connections.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CloudQueue,
                                            null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                        Text(
                                            stringResource(R.string.msg_no_conn_saved),
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            items(connections) { conn ->
                                val isTesting = testingConnectionId == conn.id
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Dns,
                                                    null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(
                                                        conn.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = if (conn.status == "ONLINE") MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = conn.status,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (conn.status == "ONLINE") MaterialTheme.colorScheme.primary else Color.Gray,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    "\\\\${conn.host}\\${conn.folderPath}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            IconButton(onClick = { connectionToDelete = conn }) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = stringResource(R.string.btn_delete),
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    testingConnectionId = conn.id
                                                    val credParts = conn.credentials.split(":")
                                                    val u = credParts.getOrNull(0) ?: ""
                                                    val p = credParts.getOrNull(1) ?: ""
                                                    val d = credParts.getOrNull(2) ?: ""
                                                    val share = conn.folderPath.split("/").firstOrNull() ?: ""
                                                    viewModel.testSmbConnection(
                                                        host = conn.host,
                                                        shareName = share,
                                                        username = u,
                                                        password = p,
                                                        domain = d
                                                    ) { success, msg ->
                                                        testingConnectionId = null
                                                        Toast.makeText(context, msg, if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
                                                    }
                                                },
                                                modifier = Modifier.weight(1f).height(40.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                enabled = !isTesting
                                            ) {
                                                if (isTesting) {
                                                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                                } else {
                                                    Icon(Icons.Default.Sync, null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Tes Koneksi", fontSize = 12.sp)
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    viewModel.navigateTo(Screen.SmbExplorer)
                                                },
                                                modifier = Modifier.weight(1f).height(40.dp),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Buka Folder", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (wizardStep) {
                            1 -> {
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            stringResource(R.string.title_smb_step1),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            stringResource(R.string.desc_smb_step1),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                    ) {
                                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    if (isDiscovering) {
                                                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                                    } else {
                                                        Icon(Icons.Default.Radar, null, tint = MaterialTheme.colorScheme.primary)
                                                    }
                                                    Text(
                                                        if (isDiscovering) "Sedang memindai jaringan..." else stringResource(R.string.label_discovered_servers),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                                TextButton(
                                                    onClick = { startDiscovery() },
                                                    enabled = !isDiscovering
                                                ) {
                                                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(stringResource(R.string.btn_scan_again), fontSize = 12.sp)
                                                }
                                            }

                                            if (discoveredServers.isNotEmpty()) {
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    discoveredServers.forEach { server ->
                                                        val isSelected = serverHost == server.ip
                                                        Card(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    serverHost = server.ip
                                                                    if (serverName.isBlank()) serverName = "NAS ${server.ip}"
                                                                },
                                                            shape = RoundedCornerShape(12.dp),
                                                            colors = CardDefaults.cardColors(
                                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                                            ),
                                                            border = BorderStroke(
                                                                if (isSelected) 2.dp else 1.dp,
                                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                                            )
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(12.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                            ) {
                                                                Icon(
                                                                    Icons.Default.Dns,
                                                                    null,
                                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    Text(
                                                                        server.ip,
                                                                        fontWeight = FontWeight.Bold,
                                                                        fontSize = 14.sp
                                                                    )
                                                                    Text(
                                                                        "Port ${server.port} (SMB aktif)",
                                                                        fontSize = 11.sp,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                    )
                                                                }
                                                                if (isSelected) {
                                                                    Icon(
                                                                        Icons.Default.CheckCircle,
                                                                        null,
                                                                        tint = MaterialTheme.colorScheme.primary
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } else if (!isDiscovering) {
                                                Text(
                                                    "Belum ada server NAS yang terdeteksi otomatis. Pastikan perangkat tersambung ke WiFi yang sama dengan NAS.",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Text(
                                                stringResource(R.string.label_manual_ip),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            OutlinedTextField(
                                                value = serverHost,
                                                onValueChange = { serverHost = it },
                                                label = { Text(stringResource(R.string.label_ip_hostname)) },
                                                placeholder = { Text("Contoh: 192.168.1.100 atau truenas.local") },
                                                leadingIcon = { Icon(Icons.Default.Lan, null) },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                singleLine = true
                                            )
                                        }
                                    }
                                }
                                 item {
                                     Button(
                                         onClick = {
                                             if (serverHost.isBlank()) {
                                                 Toast.makeText(context, context.getString(R.string.placeholder_ip), Toast.LENGTH_SHORT).show()
                                                 return@Button
                                             }
                                             if (serverName.isBlank()) serverName = "NAS $serverHost"
                                             wizardStep = 2
                                             scanShares()
                                         },
                                         modifier = Modifier
                                             .fillMaxWidth()
                                             .height(52.dp),
                                         shape = RoundedCornerShape(14.dp),
                                         enabled = serverHost.isNotBlank()
                                     ) {
                                         Text(
                                             "Lanjut ke Pengaturan & Lokasi Simpan",
                                             fontWeight = FontWeight.Bold,
                                             fontSize = 15.sp
                                         )
                                         Spacer(Modifier.width(8.dp))
                                         Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                                     }
                                 }
                            }

                            2 -> {
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            stringResource(R.string.title_smb_step2),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            stringResource(R.string.desc_smb_step2),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                item {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(Icons.Default.Dns, null, tint = MaterialTheme.colorScheme.primary)
                                            Text(
                                                "Server: $serverHost",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    isGuestMode = true
                                                    scanShares()
                                                },
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isGuestMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                            ),
                                            border = BorderStroke(
                                                if (isGuestMode) 2.dp else 1.dp,
                                                if (isGuestMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Icon(
                                                        Icons.Default.PersonOutline,
                                                        null,
                                                        tint = if (isGuestMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Text(stringResource(R.string.mode_guest), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                }
                                                Text(stringResource(R.string.mode_guest_desc), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }

                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { isGuestMode = false },
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (!isGuestMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                            ),
                                            border = BorderStroke(
                                                if (!isGuestMode) 2.dp else 1.dp,
                                                if (!isGuestMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Icon(
                                                        Icons.Default.Lock,
                                                        null,
                                                        tint = if (!isGuestMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Text(stringResource(R.string.mode_user), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                }
                                                Text(stringResource(R.string.mode_user_desc), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }

                                if (!isGuestMode) {
                                    item {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                OutlinedTextField(
                                                    value = username,
                                                    onValueChange = { username = it },
                                                    label = { Text(stringResource(R.string.label_user)) },
                                                    leadingIcon = { Icon(Icons.Default.Person, null) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(12.dp),
                                                    singleLine = true
                                                )

                                                OutlinedTextField(
                                                    value = password,
                                                    onValueChange = { password = it },
                                                    label = { Text(stringResource(R.string.label_pass)) },
                                                    leadingIcon = { Icon(Icons.Default.Key, null) },
                                                    trailingIcon = {
                                                        IconButton(onClick = { showPassword = !showPassword }) {
                                                            Icon(
                                                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                                contentDescription = null
                                                            )
                                                        }
                                                    },
                                                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(12.dp),
                                                    singleLine = true
                                                )

                                                OutlinedTextField(
                                                    value = domain,
                                                    onValueChange = { domain = it },
                                                    label = { Text("${stringResource(R.string.label_domain)} (Opsional)") },
                                                    placeholder = { Text("WORKGROUP atau kosongkan") },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(12.dp),
                                                    singleLine = true
                                                )
                                            }
                                        }
                                    }
                                }

                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(Icons.Default.FolderShared, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                    Text(
                                                        stringResource(R.string.label_discovered_shares),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                }

                                                TextButton(
                                                    onClick = { scanShares() },
                                                    enabled = !isScanningShares && (isGuestMode || username.isNotBlank())
                                                ) {
                                                    if (isScanningShares) {
                                                        CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                                                        Spacer(Modifier.width(6.dp))
                                                        Text("Memindai...", fontSize = 12.sp)
                                                    } else {
                                                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text(stringResource(R.string.btn_scan_shares), fontSize = 12.sp)
                                                    }
                                                }
                                            }

                                            if (isScanningShares) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                     CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                                    Spacer(Modifier.width(10.dp))
                                                    Text(
                                                        stringResource(R.string.label_scanning_shares),
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            val sharesToShow = if (discoveredShares.isNotEmpty()) discoveredShares else sharePresets
                                            Text(
                                                if (discoveredShares.isNotEmpty()) "Share terdeteksi (ketuk untuk memilih):" else stringResource(R.string.label_share_presets),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (discoveredShares.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                items(sharesToShow) { share ->
                                                    FilterChip(
                                                        selected = shareNameInput.equals(share, ignoreCase = true),
                                                        onClick = {
                                                            shareNameInput = share
                                                            refreshBrowser("", share)
                                                        },
                                                        label = { Text(share, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                                        leadingIcon = if (shareNameInput.equals(share, ignoreCase = true)) {
                                                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) }
                                                        } else {
                                                            { Icon(Icons.Default.Folder, null, modifier = Modifier.size(14.dp)) }
                                                        }
                                                    )
                                                }
                                            }

                                            OutlinedTextField(
                                                value = shareNameInput,
                                                onValueChange = {
                                                    shareNameInput = it.trim()
                                                    if (it.isNotBlank()) {
                                                        refreshBrowser("", it.trim())
                                                    }
                                                },
                                                label = { Text("Nama Share / Folder Bersama") },
                                                placeholder = { Text("Pilih dari chip di atas atau ketik manual") },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                singleLine = true,
                                                trailingIcon = {
                                                    if (shareNameInput.isNotBlank()) {
                                                        IconButton(onClick = { shareNameInput = "" }) {
                                                            Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                }
                                            )

                                            OutlinedButton(
                                                onClick = {
                                                    if (!isGuestMode && username.isBlank()) {
                                                        Toast.makeText(context, context.getString(R.string.msg_fill_name_host), Toast.LENGTH_SHORT).show()
                                                        return@OutlinedButton
                                                    }
                                                    isVerifying = true
                                                    viewModel.testSmbConnection(
                                                        host = serverHost,
                                                        shareName = shareNameInput.trim(),
                                                        username = if (isGuestMode) "" else username,
                                                        password = if (isGuestMode) "" else password,
                                                        domain = domain
                                                    ) { success, msg ->
                                                        isVerifying = false
                                                        if (success) {
                                                            authSuccessMessage = msg
                                                            if (shareNameInput.isNotBlank()) {
                                                                refreshBrowser("", shareNameInput)
                                                            }
                                                        } else {
                                                            authErrorMessage = msg
                                                        }
                                                    }
                                                },
                                                enabled = !isVerifying && shareNameInput.isNotBlank() && (isGuestMode || username.isNotBlank()),
                                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                if (isVerifying) {
                                                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("Memverifikasi...")
                                                } else {
                                                    Icon(Icons.Default.CheckCircleOutline, null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(stringResource(R.string.btn_verify_share), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    if (browserCurrentPath.isNotEmpty()) {
                                                        IconButton(
                                                            onClick = {
                                                                val parent = if (browserCurrentPath.contains("/")) browserCurrentPath.substringBeforeLast("/") else ""
                                                                refreshBrowser(parent)
                                                            },
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Icon(Icons.Default.ArrowUpward, "Naik satu tingkat", tint = MaterialTheme.colorScheme.primary)
                                                        }
                                                    }
                                                    Text(
                                                        text = if (browserCurrentPath.isEmpty()) {
                                                            "${shareNameInput.ifBlank { "Share" }} ${stringResource(R.string.label_root_share)}"
                                                        } else {
                                                            "$shareNameInput / $browserCurrentPath"
                                                        },
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                if (shareNameInput.isNotBlank()) {
                                                    IconButton(onClick = { showNewFolderDialog = true }) {
                                                        Icon(Icons.Default.CreateNewFolder, "Buat Folder Baru", tint = MaterialTheme.colorScheme.primary)
                                                    }
                                                }
                                            }

                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 100.dp, max = 220.dp)
                                            ) {
                                                if (shareNameInput.isBlank()) {
                                                    Text(
                                                        "Pilih atau ketik nama share di atas untuk menjelajahi folder.",
                                                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                                                        textAlign = TextAlign.Center,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                } else if (isLoadingBrowser) {
                                                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                                                } else if (browserFolders.isEmpty()) {
                                                    Column(
                                                        modifier = Modifier.align(Alignment.Center),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Icon(Icons.Default.FolderOpen, null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
                                                        Text(
                                                            stringResource(R.string.msg_no_folders),
                                                            fontSize = 12.sp,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                } else {
                                                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        items(browserFolders) { folderName ->
                                                            val folderRelPath = if (browserCurrentPath.isEmpty()) folderName else "$browserCurrentPath/$folderName"
                                                            val isCurrentSave = selectedSavePath == folderRelPath

                                                            Surface(
                                                                shape = RoundedCornerShape(10.dp),
                                                                color = if (isCurrentSave) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .clickable {
                                                                        refreshBrowser(folderRelPath)
                                                                    }
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                                ) {
                                                                    Icon(
                                                                        Icons.Default.Folder,
                                                                        null,
                                                                        tint = if (isCurrentSave) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                                                        modifier = Modifier.size(20.dp)
                                                                    )
                                                                    Text(
                                                                        folderName,
                                                                        fontSize = 13.sp,
                                                                        fontWeight = if (isCurrentSave) FontWeight.Bold else FontWeight.Medium,
                                                                        modifier = Modifier.weight(1f)
                                                                    )
                                                                    Icon(
                                                                        Icons.Default.ChevronRight,
                                                                        null,
                                                                        tint = Color.Gray.copy(alpha = 0.5f),
                                                                        modifier = Modifier.size(16.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            if (shareNameInput.isNotBlank()) {
                                                Button(
                                                    onClick = {
                                                        selectedSavePath = browserCurrentPath
                                                        Toast.makeText(context, "Folder terpilih sebagai tempat save!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                ) {
                                                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        stringResource(R.string.btn_select_this_folder),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    val finalPath = if (selectedSavePath.isNotBlank()) "$shareNameInput/$selectedSavePath" else shareNameInput
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                stringResource(R.string.label_selected_save_path),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                "\\\\$serverHost\\${finalPath.replace("/", "\\")}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            OutlinedTextField(
                                                value = serverName,
                                                onValueChange = { serverName = it },
                                                label = { Text(stringResource(R.string.label_friendly_name)) },
                                                placeholder = { Text("Contoh: NAS $serverHost") },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                singleLine = true
                                            )

                                            Button(
                                                onClick = {
                                                    val finalName = if (serverName.isNotBlank()) serverName else "NAS $serverHost"
                                                    val finalPath = if (selectedSavePath.isNotBlank()) "$shareNameInput/$selectedSavePath" else shareNameInput
                                                    val creds = if (!isGuestMode) {
                                                        if (domain.isNotBlank()) "$username:$password:$domain" else "$username:$password"
                                                    } else ""

                                                    viewModel.addConnection(
                                                        name = finalName,
                                                        type = "SMB",
                                                        host = serverHost,
                                                        folderPath = finalPath,
                                                        credentials = creds
                                                    )
                                                    Toast.makeText(context, context.getString(R.string.msg_server_saved), Toast.LENGTH_SHORT).show()
                                                    resetWizard()
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(52.dp),
                                                shape = RoundedCornerShape(14.dp),
                                                enabled = shareNameInput.isNotBlank()
                                            ) {
                                                Icon(Icons.Default.Save, null)
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    stringResource(R.string.btn_save_connection),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
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
        }
    }

    connectionToDelete?.let { conn ->
        AlertDialog(
            onDismissRequest = { connectionToDelete = null },
            title = { Text(stringResource(R.string.title_delete_conn)) },
            text = { Text(stringResource(R.string.msg_delete_conn, conn.name, conn.host)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteConnection(conn)
                    connectionToDelete = null
                }) {
                    Text(stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { connectionToDelete = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = {
                showNewFolderDialog = false
                newFolderNameInput = ""
            },
            title = { Text(stringResource(R.string.title_create_new_folder)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Lokasi: $shareNameInput/${browserCurrentPath.ifEmpty { "(Root)" }}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newFolderNameInput,
                        onValueChange = { newFolderNameInput = it },
                        placeholder = { Text(stringResource(R.string.placeholder_new_folder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val folderName = newFolderNameInput.trim()
                        if (folderName.isBlank()) return@Button
                        isCreatingFolder = true
                        scope.launch {
                            val res = SmbHelper.createFolderInShare(
                                host = serverHost,
                                shareName = shareNameInput,
                                parentFolder = browserCurrentPath,
                                newFolderName = folderName,
                                username = if (isGuestMode) "" else username,
                                password = if (isGuestMode) "" else password,
                                domain = domain
                            )
                            isCreatingFolder = false
                            showNewFolderDialog = false
                            newFolderNameInput = ""
                            res.fold(
                                onSuccess = {
                                    Toast.makeText(context, context.getString(R.string.msg_folder_created), Toast.LENGTH_SHORT).show()
                                    refreshBrowser(browserCurrentPath, shareNameInput)
                                },
                                onFailure = { err ->
                                    val msg = context.getString(R.string.msg_folder_create_failed, err.localizedMessage)
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    enabled = !isCreatingFolder && newFolderNameInput.isNotBlank()
                ) {
                    if (isCreatingFolder) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.btn_create))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNewFolderDialog = false
                    newFolderNameInput = ""
                }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    if (authErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { authErrorMessage = null },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(if (isGuestMode) "Akses Tamu (Guest) Ditolak" else "Koneksi SMB Gagal", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(authErrorMessage ?: "", fontSize = 13.sp, lineHeight = 18.sp)
                    if (isGuestMode) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text(
                                    "Solusi: Ganti ke tab 'Akun Pengguna' dan masukkan akun login komputer/NAS Anda.",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isGuestMode) {
                        OutlinedButton(
                            onClick = {
                                authErrorMessage = null
                                if (shareNameInput.isNotBlank()) {
                                    refreshBrowser("", shareNameInput)
                                }
                            }
                        ) {
                            Text("Tetap Lanjut ke Folder")
                        }
                        Button(
                            onClick = {
                                isGuestMode = false
                                authErrorMessage = null
                            }
                        ) {
                            Text("Akun Pengguna")
                        }
                    } else {
                        Button(onClick = { authErrorMessage = null }) {
                            Text("Mengerti")
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { authErrorMessage = null }) {
                    Text("Tutup")
                }
            }
        )
    }

    if (authSuccessMessage != null) {
        AlertDialog(
            onDismissRequest = { authSuccessMessage = null },
            icon = { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Koneksi Berhasil Terverifikasi", fontWeight = FontWeight.Bold) },
            text = { Text(authSuccessMessage ?: "Koneksi SMB berhasil terhubung.", fontSize = 13.sp) },
            confirmButton = {
                Button(onClick = { authSuccessMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}
