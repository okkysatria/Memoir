package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.database.MemoirDatabase
import com.example.data.repository.MemoirRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.utils.SmbHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SmbSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "memoir_sync_channel"
        const val NOTIFICATION_ID = 1001
        const val WORK_NAME = "memoir_smb_periodic_sync"
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                createNotificationChannel()

                val db = MemoirDatabase.getDatabase(context)
                val repo = MemoirRepository(db.memoirDao())
                val prefsRepo = UserPreferencesRepository(context)

                
                val connections = repo.allConnections.first()
                if (connections.isEmpty()) return@withContext Result.success()

                var newFilesCount = 0

                for (conn in connections) {
                    if (conn.type != "SMB") continue
                    
                    val host = conn.host
                    
                    val credParts = conn.credentials.split(":")
                    val username = credParts.getOrNull(0) ?: ""
                    val password = credParts.getOrNull(1) ?: ""

                    val pathParts = conn.folderPath.split("/")
                    val shareName = pathParts.firstOrNull() ?: continue
                    val subfolder = pathParts.drop(1).joinToString("/")

                    
                    val filesRes = try {
                        SmbHelper.listMediaFiles(
                            host = host,
                            shareName = shareName,
                            folderPath = subfolder,
                            username = username,
                            password = password
                        )
                    } catch (e: Exception) {
                        continue 
                    }

                    val files = filesRes.getOrNull() ?: continue

                    
                    val currentPosts = repo.allPosts.first()
                    for (smbFile in files) {
                        val existsAlready = currentPosts.any { post ->
                            post.mediaPaths.contains(smbFile.name)
                        }
                        if (existsAlready) continue

                        val downloadedRes = SmbHelper.downloadMediaToCache(
                            context = context,
                            host = host,
                            shareName = shareName,
                            remoteFilePath = smbFile.fullPath,
                            username = username,
                            password = password
                        )
                        if (downloadedRes.isSuccess) newFilesCount++
                    }
                }

                if (newFilesCount > 0) {
                    showSyncNotification(newFilesCount)
                }

                Result.success()
            } catch (e: Exception) {
                Result.retry()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_sync_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_sync_desc)
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showSyncNotification(newFilesCount: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_app_logo)
            .setContentTitle("📸 " + context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.msg_smb_sync_title, newFilesCount))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(context.getString(R.string.msg_smb_sync_desc, newFilesCount)))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }
}
