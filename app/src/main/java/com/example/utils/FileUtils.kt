package com.example.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object FileUtils {

    fun getMemoirDataDir(context: Context): File {
        val dir = File(context.filesDir, "memoir_data")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getPostsDir(context: Context): File {
        val dir = File(getMemoirDataDir(context), "posts")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getStoriesDir(context: Context): File {
        val dir = File(getMemoirDataDir(context), "stories")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getProfileDir(context: Context): File {
        val dir = File(getMemoirDataDir(context), "profile")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getLocalFile(context: Context, path: String): File {
        if (path.startsWith("/")) return File(path)
        if (path.startsWith("file://")) return File(path.removePrefix("file://"))
        
        if (path.startsWith("posts/") || path.startsWith("stories/") || path.startsWith("profile/")) {
            val fileInMemoirData = File(getMemoirDataDir(context), path)
            if (fileInMemoirData.exists()) return fileInMemoirData
        }

        if (path.startsWith("memoir_data/")) {
            val fileInRoot = File(context.filesDir, path)
            if (fileInRoot.exists()) return fileInRoot
        }

        val fileInMemoir = File(getMemoirDataDir(context), path)
        if (fileInMemoir.exists()) return fileInMemoir

        return File(context.filesDir, path)
    }

    private const val AUTHORITY = "com.memoir.offlineig.nrcoxe.fileprovider"

    fun createPostFolder(context: Context, timestamp: Long, uniqueId: String): File {
        val folderName = "post_${timestamp}_$uniqueId"
        val postDir = File(getPostsDir(context), folderName)
        if (!postDir.exists()) postDir.mkdirs()
        return postDir
    }

    fun createStoryFolder(context: Context, timestamp: Long, uniqueId: String): File {
        val folderName = "story_${timestamp}_$uniqueId"
        val storyDir = File(getStoriesDir(context), folderName)
        if (!storyDir.exists()) storyDir.mkdirs()
        return storyDir
    }

    fun writePostMetadata(
        postFolder: File,
        postId: Long = 0,
        caption: String,
        location: String,
        albumName: String?,
        createdAt: Long,
        aspectRatio: String,
        isLiked: Boolean,
        isSaved: Boolean,
        isPinned: Boolean,
        isLocked: Boolean,
        mediaFilesJson: org.json.JSONArray
    ) {
        try {
            val jsonObject = org.json.JSONObject().apply {
                put("id", postId)
                put("folderName", postFolder.name)
                put("caption", caption)
                put("location", location)
                put("albumName", albumName ?: "")
                put("createdAt", createdAt)
                put("aspectRatio", aspectRatio)
                put("isLiked", isLiked)
                put("isSaved", isSaved)
                put("isPinned", isPinned)
                put("isLocked", isLocked)
                put("mediaFiles", mediaFilesJson)
            }
            val metaFile = File(postFolder, "post.json")
            metaFile.writeText(jsonObject.toString(2), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun writeStoryMetadata(
        storyFolder: File,
        storyId: Long = 0,
        caption: String?,
        textColor: String?,
        textStyle: String?,
        createdAt: Long,
        expiresAt: Long,
        mediaFileName: String
    ) {
        try {
            val jsonObject = org.json.JSONObject().apply {
                put("id", storyId)
                put("folderName", storyFolder.name)
                put("caption", caption ?: "")
                put("textColor", textColor ?: "")
                put("textStyle", textStyle ?: "")
                put("createdAt", createdAt)
                put("expiresAt", expiresAt)
                put("mediaFileName", mediaFileName)
            }
            val metaFile = File(storyFolder, "story.json")
            metaFile.writeText(jsonObject.toString(2), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isVideoPath(path: String): Boolean {
        if (path.isBlank()) return false
        val lower = path.lowercase()
        return lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") ||
                lower.endsWith(".mov") || lower.endsWith(".3gp") || lower.endsWith(".avi") ||
                lower.contains("video")
    }

    fun processAndSaveMediaInFolder(
        context: Context,
        targetFolder: File,
        fileNameWithoutExt: String,
        pathOrUri: String,
        targetRatioStr: String,
        scale: Float = 1f,
        rotation: Float = 0f,
        offsetX: Float = 0f,
        offsetY: Float = 0f,
        vw: Float = 0f,
        vh: Float = 0f
    ): Pair<String, org.json.JSONObject> {
        val isVideo = isVideoPath(pathOrUri)
        val ext = if (isVideo) ".mp4" else ".jpg"
        val fileName = "$fileNameWithoutExt$ext"
        val destFile = File(targetFolder, fileName)

        val metadataObj = org.json.JSONObject().apply {
            put("fileName", fileName)
            put("isVideo", isVideo)
            put("scale", scale)
            put("rotation", rotation)
            put("offsetX", offsetX)
            put("offsetY", offsetY)
            put("viewportWidth", vw)
            put("viewportHeight", vh)
            put("filterName", "Normal")
        }

        val relativePath = "posts/${targetFolder.name}/$fileName"

        if (isVideo) {
            try {
                if (pathOrUri.startsWith("content://")) {
                    context.contentResolver.openInputStream(Uri.parse(pathOrUri))?.use { input ->
                        FileOutputStream(destFile).use { output -> input.copyTo(output) }
                    }
                } else {
                    val srcFile = getLocalFile(context, pathOrUri)
                    if (srcFile.exists() && srcFile.absolutePath != destFile.absolutePath) {
                        srcFile.copyTo(destFile, overwrite = true)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return Pair(relativePath, metadataObj)
        }

        val sourceBitmap = decodeBitmapWithOrientation(context, pathOrUri) ?: return Pair(relativePath, metadataObj)
        
        val srcWidth = sourceBitmap.width.toFloat()
        val srcHeight = sourceBitmap.height.toFloat()

        val targetRatio = when {
            targetRatioStr == "1:1" -> 1.0f
            targetRatioStr == "4:5" -> 0.8f
            targetRatioStr == "16:9" -> 16f / 9f
            targetRatioStr == "9:16" -> 9f / 16f
            targetRatioStr == "Asli" || targetRatioStr.startsWith("Asli") -> srcWidth / srcHeight
            targetRatioStr.contains(":") -> {
                val parts = targetRatioStr.split(":")
                val w = parts.getOrNull(0)?.toFloatOrNull() ?: 1f
                val h = parts.getOrNull(1)?.toFloatOrNull() ?: 1f
                if (w > 0f && h > 0f) w / h else srcWidth / srcHeight
            }
            else -> srcWidth / srcHeight
        }

        val outWidth: Int
        val outHeight: Int
        if (targetRatio > 1f) {
            outWidth = 1440
            outHeight = (1440 / targetRatio).toInt()
        } else {
            outHeight = 1440
            outWidth = (1440 * targetRatio).toInt()
        }

        val outputBitmap = android.graphics.Bitmap.createBitmap(outWidth, outHeight, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(outputBitmap)
        val matrix = android.graphics.Matrix()

        matrix.postTranslate(-srcWidth / 2f, -srcHeight / 2f)
        matrix.postRotate(rotation)

        val imageRatio = srcWidth / srcHeight
        val baseScale = if (imageRatio > targetRatio) {
            outHeight.toFloat() / srcHeight
        } else {
            outWidth.toFloat() / srcWidth
        }
        val finalScale = baseScale * scale
        matrix.postScale(finalScale, finalScale)

        val uiViewportWidth = if (vw > 0) vw else 400f
        val offsetScaleFactor = outWidth.toFloat() / uiViewportWidth
        matrix.postTranslate(outWidth / 2f + (offsetX * offsetScaleFactor), outHeight / 2f + (offsetY * offsetScaleFactor))

        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(sourceBitmap, matrix, paint)

        try {
            FileOutputStream(destFile).use { out ->
                outputBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
            }
            outputBitmap.recycle()
            sourceBitmap.recycle()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Pair(relativePath, metadataObj)
    }

    fun processAndSaveMedia(
        context: Context,
        pathOrUri: String,
        targetRatioStr: String,
        scale: Float = 1f,
        rotation: Float = 0f,
        offsetX: Float = 0f,
        offsetY: Float = 0f
    ): String {
        if (isVideoPath(pathOrUri)) {
            return if (pathOrUri.startsWith("content://")) {
                saveUriToInternalStorage(context, Uri.parse(pathOrUri)) ?: pathOrUri
            } else {
                pathOrUri
            }
        }

        val sourceBitmap = decodeBitmapWithOrientation(context, pathOrUri) ?: return if (pathOrUri.startsWith("content://")) {
            saveUriToInternalStorage(context, Uri.parse(pathOrUri)) ?: pathOrUri
        } else {
            pathOrUri
        }

        val srcWidth = sourceBitmap.width
        val srcHeight = sourceBitmap.height

        val targetRatio = when {
            targetRatioStr == "1:1" -> 1.0f
            targetRatioStr == "4:5" -> 4f / 5f
            targetRatioStr == "16:9" -> 16f / 9f
            targetRatioStr == "9:16" -> 9f / 16f
            else -> srcWidth.toFloat() / srcHeight.toFloat() 
        }

        val currentRatio = srcWidth.toFloat() / srcHeight.toFloat()
        var cropWidth = srcWidth
        var cropHeight = srcHeight
        var cropX = 0
        var cropY = 0

        if (targetRatioStr != "ORIGINAL" && !targetRatioStr.startsWith("Asli") && targetRatioStr != "Asli" && !targetRatioStr.startsWith("Original") && targetRatioStr != "Original" && Math.abs(currentRatio - targetRatio) > 0.01f) {
            if (currentRatio > targetRatio) {
                cropWidth = (srcHeight * targetRatio).toInt().coerceIn(1, srcWidth)
                cropX = (srcWidth - cropWidth) / 2
            } else {
                cropHeight = (srcWidth / targetRatio).toInt().coerceIn(1, srcHeight)
                cropY = (srcHeight - cropHeight) / 2
            }
        }

        val matrix = android.graphics.Matrix()
        if (rotation != 0f) {
            matrix.postRotate(rotation)
        }

        val processedBitmap = try {
            android.graphics.Bitmap.createBitmap(
                sourceBitmap,
                cropX,
                cropY,
                cropWidth,
                cropHeight,
                if (rotation != 0f) matrix else null,
                true
            )
        } catch (e: Exception) {
            sourceBitmap
        }

        val mediaDir = File(context.filesDir, "media")
        if (!mediaDir.exists()) mediaDir.mkdirs()
        val fileName = "memoir_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(6)}.jpg"
        val destFile = File(mediaDir, fileName)

        return try {
            FileOutputStream(destFile).use { out ->
                processedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, out)
            }
            if (processedBitmap != sourceBitmap) processedBitmap.recycle()
            sourceBitmap.recycle()
            "media/$fileName"
        } catch (e: Exception) {
            e.printStackTrace()
            pathOrUri
        }
    }

    private fun decodeBitmapWithOrientation(context: Context, pathOrUri: String): android.graphics.Bitmap? {
        return try {
            var orientation = ExifInterface.ORIENTATION_NORMAL
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }

            if (pathOrUri.startsWith("content://")) {
                val uri = Uri.parse(pathOrUri)
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, boundsOptions)
                }
                try {
                    context.contentResolver.openInputStream(uri)?.use {
                        orientation = ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    }
                } catch (e: Exception) { }
            } else {
                val file = getLocalFile(context, pathOrUri)
                if (!file.exists()) return null
                BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
                try {
                    orientation = ExifInterface(file.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } catch (e: Exception) { }
            }

            val sampleSize = calculateInSampleSize(boundsOptions, 2560, 2560)
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }

            val bitmap = if (pathOrUri.startsWith("content://")) {
                val uri = Uri.parse(pathOrUri)
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOptions)
                }
            } else {
                val file = getLocalFile(context, pathOrUri)
                BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
            } ?: return null

            val matrix = android.graphics.Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postScale(-1f, 1f)
                    matrix.postRotate(90f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postScale(-1f, 1f)
                    matrix.postRotate(270f)
                }
                else -> {} 
            }

            if (!matrix.isIdentity) {
                val transformed = android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (transformed != bitmap) bitmap.recycle()
                transformed
            } else {
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun createCameraImageFile(context: Context): File {
        val cameraDir = File(context.filesDir, "camera")
        if (!cameraDir.exists()) cameraDir.mkdirs()
        return File(cameraDir, "cam_${System.currentTimeMillis()}.jpg")
    }

    fun getFileProviderUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, AUTHORITY, file)
    }

    fun prepareCameraCapture(context: Context): Pair<File, Uri> {
        val file = createCameraImageFile(context)
        val uri = getFileProviderUri(context, file)
        return Pair(file, uri)
    }

    fun createImageFileUri(context: Context): Uri {
        val tempFile = File.createTempFile(
            "JPEG_${System.currentTimeMillis()}_",
            ".jpg",
            context.cacheDir
        ).apply {
            createNewFile()
        }
        return FileProvider.getUriForFile(
            context,
            AUTHORITY,
            tempFile
        )
    }

    fun saveUriToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val mediaDir = File(context.filesDir, "media")
            if (!mediaDir.exists()) mediaDir.mkdirs()

            val mimeType = context.contentResolver.getType(uri)
            val isVideo = mimeType?.startsWith("video") == true || isVideoPath(uri.toString())
            val ext = if (isVideo) ".mp4" else ".jpg"

            val fileName = "memoir_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(6)}$ext"
            val destFile = File(mediaDir, fileName)

            val copied = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                true
            } ?: false

            if (copied) "media/$fileName" else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getImageAspectRatio(context: Context, path: String): Float {
        if (path.isBlank() || path.startsWith("gradient_")) return 1.0f

        if (isVideoPath(path)) {
            val retriever = MediaMetadataRetriever()
            try {
                if (path.startsWith("content://")) {
                    retriever.setDataSource(context, Uri.parse(path))
                } else {
                    val file = getLocalFile(context, path)
                    if (file.exists()) {
                        retriever.setDataSource(file.absolutePath)
                    } else return 1.0f
                }
                val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)

                val w = widthStr?.toFloatOrNull() ?: 0f
                val h = heightStr?.toFloatOrNull() ?: 0f
                val rotation = rotationStr?.toIntOrNull() ?: 0

                if (w > 0f && h > 0f) {
                    return if (rotation == 90 || rotation == 270) {
                        h / w
                    } else {
                        w / h
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    retriever.release()
                } catch (e: Exception) { }
            }
        }

        return try {
            var width = 0
            var height = 0
            var orientation = ExifInterface.ORIENTATION_NORMAL

            if (path.startsWith("content://")) {
                val uri = Uri.parse(path)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(stream, null, options)
                    width = options.outWidth
                    height = options.outHeight
                }
                try {
                    context.contentResolver.openInputStream(uri)?.use { exifStream ->
                        val exif = ExifInterface(exifStream)
                        orientation = exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                    }
                } catch (e: Exception) { }
            } else {
                val file = getLocalFile(context, path)
                if (file.exists()) {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, options)
                    width = options.outWidth
                    height = options.outHeight
                    try {
                        val exif = ExifInterface(file.absolutePath)
                        orientation = exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                    } catch (e: Exception) { }
                }
            }

            if (width > 0 && height > 0) {
                val isRotated = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                                orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
                                orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
                                orientation == ExifInterface.ORIENTATION_TRANSVERSE
                if (isRotated) {
                    height.toFloat() / width.toFloat()
                } else {
                    width.toFloat() / height.toFloat()
                }
            } else 1.0f
        } catch (e: Exception) {
            1.0f
        }
    }
}
