package com.example.utils

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.R
import com.example.data.database.MemoirDao
import com.example.data.database.PostEntity
import com.example.data.database.StoryEntity
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mserref.NtStatus
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.mssmb2.SMBApiException
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.*
import java.util.concurrent.TimeUnit

data class DiscoveredServer(
    val ip: String,
    val hostname: String = "",
    val port: Int = 445
)

data class SmbMediaItem(
    val name: String,
    val fullPath: String,
    val size: Long,
    val isVideo: Boolean
)

object SmbHelper {
    private fun buildConfig(): SmbConfig {
        return SmbConfig.builder()
            .withTimeout(10000, TimeUnit.MILLISECONDS)
            .withSoTimeout(35000, TimeUnit.MILLISECONDS)
            .withSigningRequired(false)
            .withDfsEnabled(false)
            .build()
    }

    private fun authenticateSession(
        connection: Connection,
        username: String,
        password: String,
        domain: String = ""
    ): Session {
        if (username.isNotBlank()) {
            return connection.authenticate(AuthenticationContext(username, password.toCharArray(), domain))
        }

        val guestContexts = listOf(
            AuthenticationContext("GUEST", charArrayOf(), ""),
            AuthenticationContext.guest(),
            AuthenticationContext("guest", charArrayOf(), ""),
            AuthenticationContext.anonymous(),
            AuthenticationContext("nobody", charArrayOf(), "")
        )

        var lastException: Exception? = null
        for (ctx in guestContexts) {
            try {
                return connection.authenticate(ctx)
            } catch (e: SMBApiException) {
                lastException = e
            } catch (e: Exception) {
                lastException = e
            }
        }

        throw lastException ?: Exception("Server rejected Guest access.")
    }

    suspend fun testConnection(
        context: Context,
        host: String,
        shareName: String,
        username: String = "",
        password: String = "",
        domain: String = ""
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val client = SMBClient(buildConfig())
        try {
            val cleanHost = host.replace("smb://", "").replace("/", "").trim()
            client.connect(cleanHost).use { connection ->
                val session = try {
                    authenticateSession(connection, username, password, domain)
                } catch (e: SMBApiException) {
                    if (e.status == NtStatus.STATUS_ACCESS_DENIED ||
                        e.status == NtStatus.STATUS_LOGON_FAILURE ||
                        e.status == NtStatus.STATUS_ACCOUNT_DISABLED
                    ) {
                        if (username.isBlank()) {
                            return@withContext Result.failure(
                                Exception(
                                    "Server menolak akses Tamu (Guest).\n" +
                                    "Server NAS / Windows Anda mematikan akses Guest (Password Protected Sharing aktif).\n" +
                                    "Silakan pilih tab 'Akun Pengguna' dan masukkan Username & Password komputer/NAS Anda."
                                )
                            )
                        } else {
                            return@withContext Result.failure(
                                Exception("Akses Ditolak: Username, Password, atau Domain salah. Pastikan akun dan sandi sesuai.")
                            )
                        }
                    }
                    throw e
                }
                val cleanShare = shareName.trim().removePrefix("/").removeSuffix("/")
                if (cleanShare.isBlank()) {
                    return@withContext Result.success(true)
                }
                session.connectShare(cleanShare).use { share ->
                    if (share is DiskShare) {
                        return@withContext Result.success(true)
                    } else {
                        return@withContext Result.failure(Exception(context.getString(R.string.err_invalid_share, cleanShare)))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure(e)
        } finally {
            try { client.close() } catch (_: Exception) {}
        }
    }

    suspend fun listShares(
        host: String,
        username: String = "",
        password: String = "",
        domain: String = ""
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        val cleanHost = host.replace("smb://", "").replace("/", "").trim()
        if (cleanHost.isBlank()) {
            return@withContext Result.failure(Exception("Host SMB tidak boleh kosong"))
        }

        try {
            val prop = Properties().apply {
                setProperty("jcifs.smb.client.enableSMB2", "true")
                setProperty("jcifs.smb.client.disableSMB1", "true")
                setProperty("jcifs.smb.client.minVersion", "SMB202")
                setProperty("jcifs.smb.client.maxVersion", "SMB311")
                setProperty("jcifs.smb.client.dfs.disabled", "true")
                setProperty("jcifs.smb.client.connTimeout", "8000")
                setProperty("jcifs.smb.client.soTimeout", "20000")
                setProperty("jcifs.smb.client.responseTimeout", "15000")
                setProperty("jcifs.smb.client.signingPreferred", "false")
                setProperty("jcifs.smb.client.signingEnforced", "false")
                setProperty("jcifs.smb.client.useRawNTLM", "true")
            }

            val baseContext = BaseContext(PropertyConfiguration(prop))

            val authenticators = if (username.isNotBlank()) {
                listOf(
                    NtlmPasswordAuthenticator(domain.ifBlank { null }, username, password)
                )
            } else {
                listOf(
                    NtlmPasswordAuthenticator(null, "GUEST", ""),
                    NtlmPasswordAuthenticator("", "", ""),
                    NtlmPasswordAuthenticator(null, "guest", "")
                )
            }

            val foundShares = mutableSetOf<String>()
            var lastException: Throwable? = null

            for (auth in authenticators) {
                try {
                    val cifsContext = baseContext.withCredentials(auth)
                    val rootFile = SmbFile("smb://$cleanHost/", cifsContext)
                    if (rootFile.exists()) {
                        val files = rootFile.listFiles()
                        if (files != null) {
                            for (file in files) {
                                val clean = file.name.trim('/')
                                if (clean.isNotBlank() && !clean.endsWith("$") && clean != "IPC") {
                                    foundShares.add(clean)
                                }
                            }
                            if (foundShares.isNotEmpty()) {
                                return@withContext Result.success(foundShares.sorted())
                            }
                        }
                    }
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    lastException = t
                }
            }

            val probeCandidates = listOf(
                "public", "share", "data", "photos", "movies", "komik",
                "nas", "storage", "backup", "memoir", "media", "documents",
                "download", "downloads", "shared"
            )
            val testAuth = if (username.isNotBlank()) {
                NtlmPasswordAuthenticator(domain.ifBlank { null }, username, password)
            } else {
                NtlmPasswordAuthenticator(null, "GUEST", "")
            }
            val probeContext = baseContext.withCredentials(testAuth)

            coroutineScope {
                val jobs = probeCandidates.map { candidate ->
                    async {
                        try {
                            val shareFile = SmbFile("smb://$cleanHost/$candidate/", probeContext)
                            if (shareFile.exists() && shareFile.isDirectory) {
                                candidate
                            } else null
                        } catch (_: Throwable) {
                            null
                        }
                    }
                }
                val probed = jobs.awaitAll().filterNotNull()
                foundShares.addAll(probed)
            }

            if (foundShares.isNotEmpty()) {
                return@withContext Result.success(foundShares.sorted())
            }

            return@withContext Result.failure(
                if (lastException is Exception) lastException else Exception(lastException?.message ?: "Tidak ada folder share yang terdeteksi pada host $cleanHost")
            )
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            return@withContext Result.failure(
                if (t is Exception) t else Exception(t.message ?: "Gagal memindai share SMB", t)
            )
        }
    }

    suspend fun listSubfoldersInShare(
        host: String,
        shareName: String,
        parentFolder: String = "",
        username: String = "",
        password: String = "",
        domain: String = ""
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        val client = SMBClient(buildConfig())
        try {
            val cleanHost = host.replace("smb://", "").replace("/", "").trim()
            val cleanShare = shareName.trim().trim('\\', '/')
            client.connect(cleanHost).use { connection ->
                val session = authenticateSession(connection, username, password, domain)
                (session.connectShare(cleanShare) as DiskShare).use { diskShare ->
                    val cleanParent = parentFolder.trim().trim('\\', '/').replace('/', '\\')
                    val files: List<FileIdBothDirectoryInformation> = diskShare.list(cleanParent)
                    val folderNames = files
                        .filter {
                            val isDir = (it.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L
                            isDir && it.fileName != "." && it.fileName != ".."
                        }
                        .map { it.fileName }
                        .sorted()
                    return@withContext Result.success(folderNames)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure(e)
        } finally {
            try { client.close() } catch (_: Exception) {}
        }
    }

    private fun ensureRemoteDirectoryExists(
        diskShare: DiskShare,
        folderPath: String,
        knownDirs: MutableSet<String>? = null
    ) {
        val clean = folderPath.trim().trim('\\', '/').replace('/', '\\')
        if (clean.isBlank()) return

        val parts = clean.split('\\').filter { it.isNotBlank() }
        var currentPath = ""
        for (part in parts) {
            currentPath = if (currentPath.isEmpty()) part else "$currentPath\\$part"
            if (knownDirs != null && knownDirs.contains(currentPath)) {
                continue
            }
            val exists = try {
                diskShare.folderExists(currentPath)
            } catch (_: Exception) {
                false
            }
            if (!exists) {
                try {
                    diskShare.mkdir(currentPath)
                } catch (e: Exception) {
                    val stillMissing = try {
                        !diskShare.folderExists(currentPath)
                    } catch (_: Exception) {
                        true
                    }
                    if (stillMissing) {
                        Log.w("SmbHelper", "Could not verify/create folder $currentPath: ${e.message}")
                    }
                }
            }
            knownDirs?.add(currentPath)
        }
    }

    suspend fun createFolderInShare(
        host: String,
        shareName: String,
        parentFolder: String = "",
        newFolderName: String,
        username: String = "",
        password: String = "",
        domain: String = ""
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val client = SMBClient(buildConfig())
        try {
            val cleanHost = host.replace("smb://", "").replace("/", "").trim()
            val cleanShare = shareName.trim().trim('\\', '/')
            client.connect(cleanHost).use { connection ->
                val session = authenticateSession(connection, username, password, domain)
                (session.connectShare(cleanShare) as DiskShare).use { diskShare ->
                    val cleanParent = parentFolder.trim().trim('\\', '/').replace('/', '\\')
                    val cleanNew = newFolderName.trim().trim('\\', '/')
                    val targetPath = if (cleanParent.isBlank()) cleanNew else "$cleanParent\\$cleanNew"
                    ensureRemoteDirectoryExists(diskShare, targetPath)
                    return@withContext Result.success(true)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure(e)
        } finally {
            try { client.close() } catch (_: Exception) {}
        }
    }

    suspend fun listMediaFiles(
        host: String,
        shareName: String,
        folderPath: String = "",
        username: String = "",
        password: String = "",
        domain: String = ""
    ): Result<List<SmbMediaItem>> = withContext(Dispatchers.IO) {
        val client = SMBClient(buildConfig())
        try {
            val cleanHost = host.replace("smb://", "").replace("/", "").trim()
            val cleanShare = shareName.trim().trim('\\', '/')
            val cleanFolder = folderPath.trim().trim('\\', '/').replace('/', '\\')
            client.connect(cleanHost).use { connection ->
                val session = authenticateSession(connection, username, password, domain)
                (session.connectShare(cleanShare) as DiskShare).use { diskShare ->
                    val items = mutableListOf<SmbMediaItem>()
                    val files: List<FileIdBothDirectoryInformation> = diskShare.list(cleanFolder)
                    for (fileInfo in files) {
                        val fileName = fileInfo.fileName
                        if (fileName == "." || fileName == "..") continue
                        val isDir = (fileInfo.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L
                        if (isDir) continue
                        val lower = fileName.lowercase()
                        val isImage = lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                                lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".heic")
                        val isVideo = lower.endsWith(".mp4") || lower.endsWith(".mkv") ||
                                lower.endsWith(".webm") || lower.endsWith(".mov") || lower.endsWith(".3gp")
                        if (isImage || isVideo) {
                            val fullPath = if (cleanFolder.isBlank()) fileName else "$cleanFolder\\$fileName"
                            items.add(
                                SmbMediaItem(
                                    name = fileName,
                                    fullPath = fullPath,
                                    size = fileInfo.endOfFile,
                                    isVideo = isVideo
                                )
                            )
                        }
                    }
                    return@withContext Result.success(items)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure(e)
        } finally {
            try { client.close() } catch (_: Exception) {}
        }
    }

    suspend fun downloadMediaToCache(
        context: Context,
        host: String,
        shareName: String,
        remoteFilePath: String,
        username: String = "",
        password: String = "",
        domain: String = ""
    ): Result<File> = withContext(Dispatchers.IO) {
        val client = SMBClient(buildConfig())
        try {
            val cleanHost = host.replace("smb://", "").replace("/", "").trim()
            val cleanShare = shareName.trim().trim('\\', '/')
            client.connect(cleanHost).use { connection ->
                val session = authenticateSession(connection, username, password, domain)
                (session.connectShare(cleanShare) as DiskShare).use { diskShare ->
                    val cleanPath = remoteFilePath.trim().trim('\\', '/').replace('/', '\\')
                    val fileName = cleanPath.substringAfterLast("\\").ifBlank { "media_${System.currentTimeMillis()}" }
                    val cacheDir = File(context.cacheDir, "smb_media_preview")
                    if (!cacheDir.exists()) cacheDir.mkdirs()
                    val localFile = File(cacheDir, fileName)
                    val smbFile = diskShare.openFile(
                        cleanPath,
                        setOf(AccessMask.FILE_READ_DATA),
                        null,
                        SMB2ShareAccess.ALL,
                        SMB2CreateDisposition.FILE_OPEN,
                        null
                    )
                    smbFile.inputStream.use { input ->
                        FileOutputStream(localFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    return@withContext Result.success(localFile)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure(e)
        } finally {
            try { client.close() } catch (_: Exception) {}
        }
    }

    suspend fun syncMemoirFolderToSmb(
        context: Context,
        host: String,
        shareName: String,
        remoteBaseFolder: String = "Memoir_Backup",
        username: String = "",
        password: String = "",
        domain: String = ""
    ): Result<Int> = withContext(Dispatchers.IO) {
        val client = SMBClient(buildConfig())
        try {
            val cleanHost = host.replace("smb://", "").replace("/", "").trim()
            val cleanShare = shareName.trim().trim('\\', '/')
            val cleanBase = remoteBaseFolder.trim().trim('\\', '/').replace('/', '\\')
            client.connect(cleanHost).use { connection ->
                val session = authenticateSession(connection, username, password, domain)
                (session.connectShare(cleanShare) as DiskShare).use { diskShare ->
                    val createdDirs = mutableSetOf<String>()
                    if (cleanBase.isNotBlank()) {
                        ensureRemoteDirectoryExists(diskShare, cleanBase, createdDirs)
                    }
                    val memoirDir = FileUtils.getMemoirDataDir(context)
                    var uploadedCount = 0

                    memoirDir.walkTopDown().filter { it.isFile }.forEach { localFile ->
                        val relPath = localFile.relativeTo(memoirDir).path.trim().trim('\\', '/').replace('/', '\\')
                        val remotePath = if (cleanBase.isBlank()) relPath else "$cleanBase\\$relPath"
                        
                        val parentPath = if (remotePath.contains('\\')) remotePath.substringBeforeLast('\\') else ""
                        if (parentPath.isNotBlank() && parentPath !in createdDirs) {
                            ensureRemoteDirectoryExists(diskShare, parentPath, createdDirs)
                        }

                        var needUpload = true
                        if (diskShare.fileExists(remotePath)) {
                            try {
                                val info = diskShare.getFileInformation(remotePath)
                                if (info.standardInformation.endOfFile == localFile.length()) {
                                    needUpload = false
                                }
                            } catch (_: Exception) {}
                        }
                        if (needUpload) {
                            val smbFile = diskShare.openFile(
                                remotePath,
                                setOf(AccessMask.FILE_WRITE_DATA, AccessMask.FILE_READ_DATA),
                                null,
                                SMB2ShareAccess.ALL,
                                SMB2CreateDisposition.FILE_OVERWRITE_IF,
                                null
                            )
                            localFile.inputStream().use { input ->
                                smbFile.outputStream.use { output ->
                                    input.copyTo(output)
                                }
                            }
                            uploadedCount++
                        }
                    }
                    return@withContext Result.success(uploadedCount)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure(e)
        } finally {
            try { client.close() } catch (_: Exception) {}
        }
    }
    suspend fun importMemoirFolderFromSmb(
        context: Context,
        dao: MemoirDao,
        host: String,
        shareName: String,
        remoteFolder: String = "Memoir_Backup",
        username: String = "",
        password: String = ""
    ): Result<Int> = withContext(Dispatchers.IO) {
        val client = SMBClient(buildConfig())
        try {
            val cleanHost = host.replace("smb://", "").replace("/", "").trim()
            val cleanShare = shareName.trim().trim('\\', '/')
            val cleanBase = remoteFolder.trim().trim('\\', '/').replace('/', '\\')
            client.connect(cleanHost).use { connection ->
                val session = authenticateSession(connection, username, password, "")
                (session.connectShare(cleanShare) as DiskShare).use { diskShare ->
                    val remotePostsFolder = if (cleanBase.isBlank()) "posts" else "$cleanBase\\posts"
                    if (!diskShare.folderExists(remotePostsFolder)) {
                        return@withContext Result.failure(Exception(context.getString(R.string.err_folder_not_found_smb, remotePostsFolder)))
                    }
                    var importedCount = 0
                    val postFolders = diskShare.list(remotePostsFolder)
                        .filter {
                            val isDir = (it.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L
                            isDir && it.fileName != "." && it.fileName != ".."
                        }
                    val localPostsDir = FileUtils.getPostsDir(context)
                    val existingPosts = dao.getAllPosts().first()
                    for (folderInfo in postFolders) {
                        val folderName = folderInfo.fileName
                        val remotePostDir = "$remotePostsFolder\\$folderName"
                        val localPostDir = File(localPostsDir, folderName).also { if (!it.exists()) it.mkdirs() }
                        val remoteJsonPath = "$remotePostDir\\post.json"
                        if (diskShare.fileExists(remoteJsonPath)) {
                            val localJsonFile = File(localPostDir, "post.json")
                            val smbJson = diskShare.openFile(
                                remoteJsonPath,
                                setOf(AccessMask.FILE_READ_DATA),
                                null,
                                SMB2ShareAccess.ALL,
                                SMB2CreateDisposition.FILE_OPEN,
                                null
                            )
                            smbJson.inputStream.use { input ->
                                FileOutputStream(localJsonFile).use { output -> input.copyTo(output) }
                            }
                            val filesInPost = diskShare.list(remotePostDir)
                            for (fileInfo in filesInPost) {
                                if (fileInfo.fileName != "." && fileInfo.fileName != ".." && fileInfo.fileName != "post.json") {
                                    val localMediaFile = File(localPostDir, fileInfo.fileName)
                                    if (!localMediaFile.exists() || localMediaFile.length() != fileInfo.endOfFile) {
                                        val smbMedia = diskShare.openFile(
                                            "$remotePostDir\\${fileInfo.fileName}",
                                            setOf(AccessMask.FILE_READ_DATA),
                                            null,
                                            SMB2ShareAccess.ALL,
                                            SMB2CreateDisposition.FILE_OPEN,
                                            null
                                        )
                                        smbMedia.inputStream.use { input ->
                                            FileOutputStream(localMediaFile).use { output -> input.copyTo(output) }
                                        }
                                    }
                                }
                            }
                            try {
                                val json = JSONObject(localJsonFile.readText(Charsets.UTF_8))
                                val createdAt = json.optLong("createdAt", localPostDir.lastModified())
                                val caption = json.optString("caption", "")
                                val location = json.optString("location", "")
                                val albumName = json.optString("albumName", "").ifBlank { null }
                                val aspectRatio = json.optString("aspectRatio", "ORIGINAL")
                                val isLiked = json.optBoolean("isLiked", false)
                                val isSaved = json.optBoolean("isSaved", false)
                                val isPinned = json.optBoolean("isPinned", false)
                                val isLocked = json.optBoolean("isLocked", false)
                                val mediaArray = json.optJSONArray("mediaFiles")
                                val mediaPaths = mutableListOf<String>()
                                if (mediaArray != null && mediaArray.length() > 0) {
                                    for (i in 0 until mediaArray.length()) {
                                        val obj = mediaArray.getJSONObject(i)
                                        val fileName = obj.optString("fileName")
                                        if (fileName.isNotBlank()) {
                                            mediaPaths.add("posts/$folderName/$fileName")
                                        }
                                    }
                                } else {
                                    localPostDir.listFiles { f -> f.isFile && f.name != "post.json" }?.sortedBy { it.name }?.forEach { f ->
                                        mediaPaths.add("posts/$folderName/${f.name}")
                                    }
                                }
                                val isDuplicate = existingPosts.any { p ->
                                    p.createdAt == createdAt || (mediaPaths.isNotEmpty() && p.mediaPaths.contains(mediaPaths.first()))
                                }
                                if (!isDuplicate && mediaPaths.isNotEmpty()) {
                                    dao.insertPost(
                                        PostEntity(
                                            caption = caption,
                                            createdAt = createdAt,
                                            location = location,
                                            mediaPaths = mediaPaths,
                                            isLiked = isLiked,
                                            isSaved = isSaved,
                                            isPinned = isPinned,
                                            isLocked = isLocked,
                                            aspectRatio = aspectRatio,
                                            mediaTransformsJson = mediaArray?.toString() ?: ""
                                        )
                                    )
                                    importedCount++
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    return@withContext Result.success(importedCount)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure(e)
        } finally {
            try { client.close() } catch (_: Exception) {}
        }
    }
    suspend fun discoverSmbServers(
        context: Context,
        onServerFound: (DiscoveredServer) -> Unit
    ): List<DiscoveredServer> = withContext(Dispatchers.IO) {
        val foundServers = Collections.synchronizedList(mutableListOf<DiscoveredServer>())
        val localIp = getLocalWifiIpAddress(context) ?: "192.168.1.1"
        val subnet = localIp.substringBeforeLast(".")
        val jobs = mutableListOf<Job>()
        for (i in 1..254) {
            val targetIp = "$subnet.$i"
            val job = launch {
                if (isSmbPortOpen(targetIp, 445, timeoutMs = 280) || isSmbPortOpen(targetIp, 139, timeoutMs = 280)) {
                    val server = DiscoveredServer(ip = targetIp, hostname = context.getString(R.string.label_nas_server_default, targetIp))
                    if (!foundServers.any { it.ip == targetIp }) {
                        foundServers.add(server)
                        withContext(Dispatchers.Main) {
                            onServerFound(server)
                        }
                    }
                }
            }
            jobs.add(job)
        }
        jobs.joinAll()
        foundServers.toList()
    }
    private fun isSmbPortOpen(ip: String, port: Int = 445, timeoutMs: Int = 280): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }
    private fun getLocalWifiIpAddress(context: Context): String? {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                return String.format(
                    Locale.getDefault(),
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff,
                    ipInt shr 24 and 0xff
                )
            }
        } catch (_: Exception) {}
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }
}
