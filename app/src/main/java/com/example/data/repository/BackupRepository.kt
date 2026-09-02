package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.database.PostTagEntity
import com.example.data.database.MemoirDao
import kotlinx.coroutines.flow.Flow
import com.example.utils.FileUtils
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupRepository(
    private val context: Context,
    private val dao: MemoirDao
) {
    fun getTagsForPost(postId: Long): Flow<List<PostTagEntity>> =
        dao.getTagsForPost(postId)

    suspend fun addTag(postId: Long, tagName: String): Long {
        val tag = PostTagEntity(postId = postId, tagName = tagName.trim())
        return dao.insertTag(tag)
    }

    suspend fun deleteTag(tag: PostTagEntity) = dao.deleteTag(tag)

    suspend fun clearTagsForPost(postId: Long) = dao.deleteTagsForPost(postId)

    suspend fun scanAndIndexMemoirFolder(context: Context): Int {
        var importedCount = 0
        try {
            val postsDir = FileUtils.getPostsDir(context)
            if (postsDir.exists() && postsDir.isDirectory) {
                val existingPosts = dao.getAllPosts().first()
                val postFolders = postsDir.listFiles { f -> f.isDirectory } ?: emptyArray()

                for (folder in postFolders) {
                    val metaFile = File(folder, "post.json")
                    if (!metaFile.exists()) continue

                    try {
                        val json = JSONObject(metaFile.readText(Charsets.UTF_8))
                        val createdAt = json.optLong("createdAt", folder.lastModified())
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
                                    mediaPaths.add("posts/${folder.name}/$fileName")
                                }
                            }
                        } else {
                            folder.listFiles { f -> f.isFile && f.name != "post.json" }?.sortedBy { it.name }?.forEach { f ->
                                mediaPaths.add("posts/${folder.name}/${f.name}")
                            }
                        }

                        val isDuplicate = existingPosts.any { p ->
                            p.createdAt == createdAt || (mediaPaths.isNotEmpty() && p.mediaPaths.contains(mediaPaths.first()))
                        }

                        if (!isDuplicate && mediaPaths.isNotEmpty()) {
                            dao.insertPost(
                                com.example.data.database.PostEntity(
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

            val storiesDir = FileUtils.getStoriesDir(context)
            if (storiesDir.exists() && storiesDir.isDirectory) {
                val existingStories = dao.getAllStories().first()
                val storyFolders = storiesDir.listFiles { f -> f.isDirectory } ?: emptyArray()

                for (folder in storyFolders) {
                    val metaFile = File(folder, "story.json")
                    if (!metaFile.exists()) continue
                    try {
                        val json = JSONObject(metaFile.readText(Charsets.UTF_8))
                        val createdAt = json.optLong("createdAt", folder.lastModified())
                        val expiresAt = json.optLong("expiresAt", createdAt + 24 * 60 * 60 * 1000L)
                        val caption = json.optString("caption", "").ifBlank { null }
                        val textColor = json.optString("textColor", "").ifBlank { null }
                        val textStyle = json.optString("textStyle", "").ifBlank { null }
                        val mediaFileName = json.optString("mediaFileName", "media.jpg")
                        val mediaRelPath = "stories/${folder.name}/$mediaFileName"

                        val isDuplicate = existingStories.any { s -> s.createdAt == createdAt || s.mediaPath == mediaRelPath }
                        if (!isDuplicate) {
                            dao.insertStory(
                                com.example.data.database.StoryEntity(
                                    mediaPath = mediaRelPath,
                                    caption = caption,
                                    textColor = textColor,
                                    textStyle = textStyle,
                                    createdAt = createdAt,
                                    expiresAt = expiresAt
                                )
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return importedCount
    }

    suspend fun exportMemoirDataToDocumentTree(context: Context, treeUri: Uri): Result<Int> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val rootDoc = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri)
                ?: return@withContext Result.failure(Exception("Tidak dapat mengakses folder tujuan."))

            val targetBase = rootDoc.findFile("memoir_data") ?: rootDoc.createDirectory("memoir_data")
                ?: return@withContext Result.failure(Exception("Gagal membuat folder 'memoir_data' di tujuan."))

            val memoirDir = FileUtils.getMemoirDataDir(context)
            var copiedCount = 0

            fun copyDirRecursive(srcDir: File, destDoc: androidx.documentfile.provider.DocumentFile) {
                srcDir.listFiles()?.forEach { file ->
                    if (file.isDirectory) {
                        val subDoc = destDoc.findFile(file.name) ?: destDoc.createDirectory(file.name)
                        if (subDoc != null) {
                            copyDirRecursive(file, subDoc)
                        }
                    } else if (file.isFile) {
                        val isVideo = FileUtils.isVideoPath(file.name)
                        val mime = if (isVideo) "video/mp4" else if (file.name.endsWith(".json")) "application/json" else "image/jpeg"
                        
                        val existingFile = destDoc.findFile(file.name)
                        if (existingFile == null || existingFile.length() != file.length()) {
                            existingFile?.delete()
                            val newFileDoc = destDoc.createFile(mime, file.name)
                            if (newFileDoc != null) {
                                context.contentResolver.openOutputStream(newFileDoc.uri)?.use { out ->
                                    file.inputStream().use { input -> input.copyTo(out) }
                                }
                                copiedCount++
                            }
                        }
                    }
                }
            }

            copyDirRecursive(memoirDir, targetBase)
            Result.success(copiedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importMemoirDataFromDocumentTree(context: Context, treeUri: Uri): Result<Int> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val rootDoc = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri)
                ?: return@withContext Result.failure(Exception("Tidak dapat mengakses folder sumber."))

            val memoirDataDoc = if (rootDoc.name == "memoir_data") rootDoc else (rootDoc.findFile("memoir_data") ?: rootDoc)
            val postsDoc = memoirDataDoc.findFile("posts")
                ?: return@withContext Result.failure(Exception("Folder 'posts' tidak ditemukan di direktori yang dipilih."))

            val localPostsDir = FileUtils.getPostsDir(context)

            postsDoc.listFiles().filter { it.isDirectory }.forEach { postFolderDoc ->
                val folderName = postFolderDoc.name ?: return@forEach
                val localPostDir = File(localPostsDir, folderName).also { if (!it.exists()) it.mkdirs() }

                postFolderDoc.listFiles().filter { it.isFile }.forEach { fileDoc ->
                    val fName = fileDoc.name ?: return@forEach
                    val localFile = File(localPostDir, fName)
                    if (!localFile.exists() || localFile.length() != fileDoc.length()) {
                        context.contentResolver.openInputStream(fileDoc.uri)?.use { input ->
                            FileOutputStream(localFile).use { output -> input.copyTo(output) }
                        }
                    }
                }
            }

            val storiesDoc = memoirDataDoc.findFile("stories")
            if (storiesDoc != null) {
                val localStoriesDir = FileUtils.getStoriesDir(context)
                storiesDoc.listFiles().filter { it.isDirectory }.forEach { storyFolderDoc ->
                    val folderName = storyFolderDoc.name ?: return@forEach
                    val localStoryDir = File(localStoriesDir, folderName).also { if (!it.exists()) it.mkdirs() }

                    storyFolderDoc.listFiles().filter { it.isFile }.forEach { fileDoc ->
                        val fName = fileDoc.name ?: return@forEach
                        val localFile = File(localStoryDir, fName)
                        if (!localFile.exists() || localFile.length() != fileDoc.length()) {
                            context.contentResolver.openInputStream(fileDoc.uri)?.use { input ->
                                FileOutputStream(localFile).use { output -> input.copyTo(output) }
                            }
                        }
                    }
                }
            }

            val count = scanAndIndexMemoirFolder(context)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
