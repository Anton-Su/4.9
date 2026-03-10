package com.example.a49

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay


class WorkerWeather(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val city = inputData.getString("city") ?: return Result.failure()
        delay(15500)
        val temp = (-5..30).random()
        return Result.success(
            workDataOf(
                "city" to city,
                "temp" to temp
            )
        )
    }
}