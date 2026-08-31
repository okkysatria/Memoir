package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.viewmodel.MemoirViewModel
import com.example.ui.viewmodel.Screen
import com.example.ui.theme.MemoirViolet
import com.example.ui.theme.NavyPrimary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: MemoirViewModel) {
    val context = LocalContext.current
    var inputPin by remember { mutableStateOf("") }
    val currentPin by viewModel.appPin.collectAsStateWithLifecycle()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    val isLocationHidden by viewModel.isLocationHidden.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val selectedColorTheme by viewModel.selectedColorTheme.collectAsStateWithLifecycle()
    val cacheSizeMb by viewModel.cacheSizeMb.collectAsStateWithLifecycle()
    val profileName by viewModel.profileName.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.language.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val backupRepo = remember {
        com.example.data.repository.BackupRepository(
            context,
            com.example.data.database.MemoirDatabase.getDatabase(context).memoirDao()
        )
    }

    var isOperatingFolder by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showResultDialog by remember { mutableStateOf(false) }
    var operationResultMessage by remember { mutableStateOf("") }

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
                        operationResultMessage = context.getString(R.string.msg_success_export, count)
                        showResultDialog = true
                    },
                    onFailure = { err ->
                        operationResultMessage = context.getString(R.string.msg_failed_export, err.localizedMessage)
                        showResultDialog = true
                    }
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 0.5.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.btn_cancel))
                }
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(stringResource(R.string.settings_title), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.settings_subtitle), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }

        
        SectionHeader(title = stringResource(R.string.sec_profile), icon = Icons.Default.Person)
        ListItem(
            headlineContent = { Text(profileName, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            supportingContent = { Text("@$username", fontSize = 12.sp) },
            leadingContent = { Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) } },
            trailingContent = { TextButton(onClick = { viewModel.selectTab("profile") }) { Text(stringResource(R.string.edit_profile)) } }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

        
        SectionHeader(title = stringResource(R.string.sec_appearance), icon = Icons.Default.Palette)
        ListItem(
            headlineContent = { Text(stringResource(R.string.pref_language), fontSize = 14.sp) },
            trailingContent = {
                var showLangMenu by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { showLangMenu = true }) {
                        val displayLang = when {
                            currentLanguage.startsWith("id") -> "Indonesia"
                            currentLanguage.startsWith("zh") -> "中文"
                            currentLanguage.startsWith("ja") -> "日本語"
                            currentLanguage.startsWith("ru") -> "Русский"
                            currentLanguage.startsWith("ar") -> "العربية"
                            else -> "English"
                        }
                        Text(displayLang, fontSize = 13.sp)
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = showLangMenu, onDismissRequest = { showLangMenu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.lang_en)) }, onClick = { viewModel.changeLanguage("en"); showLangMenu = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.lang_id)) }, onClick = { viewModel.changeLanguage("id"); showLangMenu = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.lang_zh)) }, onClick = { viewModel.changeLanguage("zh"); showLangMenu = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.lang_ja)) }, onClick = { viewModel.changeLanguage("ja"); showLangMenu = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.lang_ru)) }, onClick = { viewModel.changeLanguage("ru"); showLangMenu = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.lang_ar)) }, onClick = { viewModel.changeLanguage("ar"); showLangMenu = false })
                    }
                }
            },
            leadingContent = { Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary) }
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.pref_dark_mode), fontSize = 14.sp) },
            trailingContent = { Switch(checked = isDarkMode, onCheckedChange = { viewModel.toggleDarkMode() }) },
            leadingContent = { Icon(Icons.Default.Brightness4, null, tint = MaterialTheme.colorScheme.primary) }
        )
        
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(stringResource(R.string.pref_theme_color), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            val themeOptions = listOf(
                Triple("DEFAULT", stringResource(R.string.theme_signature), MemoirViolet),
                Triple("NAVY", stringResource(R.string.theme_navy), NavyPrimary),
                Triple("MONOCHROME", stringResource(R.string.theme_charcoal), Color(0xFF2D3142)),
                Triple("DEFAULT_ROSE", stringResource(R.string.theme_rose), Color(0xFFD81B60)),
                Triple("OCEAN", stringResource(R.string.theme_ocean), Color(0xFF4A7B9D)),
                Triple("EMERALD", stringResource(R.string.theme_emerald), Color(0xFF4A7C59)),
                Triple("SUNSET", stringResource(R.string.theme_sunset), Color(0xFFC86D51)),
                Triple("PURPLE", stringResource(R.string.theme_violet), Color(0xFF7A6C9B))
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(themeOptions) { (key, label, color) ->
                    val isSelected = selectedColorTheme == key
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { viewModel.setColorTheme(key) }) {
                        Box(Modifier.size(38.dp).clip(CircleShape).background(color).border(if (isSelected) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape), contentAlignment = Alignment.Center) {
                            if (isSelected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Text(label, fontSize = 9.sp, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray)
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))

        
        SectionHeader(title = stringResource(R.string.sec_security), icon = Icons.Default.Security)
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(stringResource(R.string.pref_pin_lock), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(if (currentPin != null) stringResource(R.string.status_active) else stringResource(R.string.status_inactive), fontSize = 11.sp, color = if (currentPin != null) Color(0xFF2E7D32) else Color.Gray)
                    }
                    if (currentPin != null) {
                        TextButton(onClick = { viewModel.setAppPin(null) }) { Text(stringResource(R.string.btn_remove_pin), color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = inputPin, onValueChange = { if (it.length <= 4) inputPin = it }, placeholder = { Text(stringResource(R.string.placeholder_new_pin)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    Button(onClick = { if (inputPin.length == 4) { viewModel.setAppPin(inputPin); inputPin = "" } }) { Text(stringResource(R.string.btn_save)) }
                }
            }
        }
        ListItem(
            headlineContent = { Text(stringResource(R.string.pref_biometric), fontSize = 14.sp) },
            trailingContent = { Switch(checked = isBiometricEnabled, onCheckedChange = { viewModel.toggleBiometric() }) },
            leadingContent = { Icon(Icons.Default.Fingerprint, null, tint = MaterialTheme.colorScheme.primary) }
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.pref_hide_location), fontSize = 14.sp) },
            supportingContent = { Text(stringResource(R.string.pref_hide_location_desc), fontSize = 11.sp) },
            trailingContent = { Switch(checked = isLocationHidden, onCheckedChange = { viewModel.toggleLocationHidden() }) },
            leadingContent = { Icon(Icons.Default.LocationOff, null, tint = MaterialTheme.colorScheme.primary) }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))

        
        SectionHeader(title = stringResource(R.string.sec_storage), icon = Icons.Default.Storage)
        
        ListItem(
            headlineContent = { Text(stringResource(R.string.pref_smb_explorer), fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            supportingContent = { Text(stringResource(R.string.pref_smb_explorer_desc), fontSize = 11.sp) },
            leadingContent = { Icon(Icons.Default.Explore, null, tint = MaterialTheme.colorScheme.primary) },
            trailingContent = { Button(onClick = { viewModel.navigateTo(Screen.SmbExplorer) }) { Text(stringResource(R.string.btn_open)) } },
            modifier = Modifier.clickable { viewModel.navigateTo(Screen.SmbExplorer) }
        )
        
        ListItem(
            headlineContent = { Text(stringResource(R.string.pref_manage_smb), fontSize = 14.sp) },
            supportingContent = { Text(stringResource(R.string.desc_manage_smb), fontSize = 11.sp) },
            leadingContent = { Icon(Icons.Default.Dns, null, tint = MaterialTheme.colorScheme.primary) },
            modifier = Modifier.clickable { viewModel.navigateTo(Screen.ManageConnections) }
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.pref_manual_backup), fontSize = 14.sp) },
            supportingContent = { Text(stringResource(R.string.desc_manual_backup), fontSize = 11.sp) },
            leadingContent = { Icon(Icons.Default.FolderZip, null, tint = MaterialTheme.colorScheme.primary) },
            trailingContent = { IconButton(onClick = { exportFolderLauncher.launch(null) }) { Icon(Icons.Default.FileUpload, null) } }
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.pref_clear_cache), fontSize = 14.sp) },
            supportingContent = { Text("${stringResource(R.string.status_active).split(":")[0]}: ${String.format("%.1f", cacheSizeMb)} MB", fontSize = 11.sp) },
            leadingContent = { Icon(Icons.Default.CleaningServices, null, tint = MaterialTheme.colorScheme.primary) },
            trailingContent = { OutlinedButton(onClick = { viewModel.clearCache() }) { Text(stringResource(R.string.btn_clean), fontSize = 11.sp) } }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))

        
        SectionHeader(title = stringResource(R.string.sec_others), icon = Icons.Default.Info)
        ListItem(
            headlineContent = { Text(stringResource(R.string.about_memoir), fontSize = 14.sp) },
            supportingContent = { Text(stringResource(R.string.memoir_version), fontSize = 11.sp) },
            leadingContent = { Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary) }
        )
        
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { showResetDialog = true },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.DeleteForever, null, Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.pref_reset_app), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }

    
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.confirm_reset_title)) },
            text = { Text(stringResource(R.string.confirm_reset_desc), fontSize = 13.sp) },
            confirmButton = { Button(onClick = { viewModel.resetDatabaseAndReSeed(); showResetDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.btn_reset)) } },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text(stringResource(R.string.btn_cancel)) } }
        )
    }

    if (showResultDialog) {
        AlertDialog(
            onDismissRequest = { showResultDialog = false },
            title = { Text(stringResource(R.string.operation_result), fontWeight = FontWeight.Bold) },
            text = { Text(operationResultMessage, fontSize = 13.sp) },
            confirmButton = { Button(onClick = { showResultDialog = false }) { Text(stringResource(R.string.msg_understand)) } }
        )
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(32.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}
