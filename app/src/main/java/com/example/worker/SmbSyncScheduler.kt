package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SmbSyncScheduler {
    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) 
            .build()
        val syncRequest = PeriodicWorkRequestBuilder<SmbSyncWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SmbSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, 
            syncRequest
        )
    }
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(SmbSyncWorker.WORK_NAME)
    }
}
