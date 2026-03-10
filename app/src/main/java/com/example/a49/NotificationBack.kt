package com.example.a49

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class WorkerState(
    val isRunning: Boolean = false,
    val statusText: String = "",
    val isDone: Boolean = false,
    val isError: Boolean = false
)

data class ProcessingState(
    val worker1: WorkerState = WorkerState(),
    val worker2: WorkerState = WorkerState(),
    val worker3: WorkerState = WorkerState(),
    val resultText: String = "",
    val isRunning: Boolean = false,
    val isDone: Boolean = false,
    val isError: Boolean = false,
)


class NotificationBack : Service() {
    private val _state = MutableStateFlow(ProcessingState())
    val state: StateFlow<ProcessingState> = _state
    private val CHANNEL_ID = "4.9_channel"
    private lateinit var workManager: WorkManager
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    override fun onCreate() {
        super.onCreate()
        createChannel()
        workManager = WorkManager.getInstance(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "start" -> startWorkChain()
        }
        return START_STICKY
    }

    private fun startWorkChain() {
        startForeground(1, createNotification("Запуск задач..."))
        val worker1 = OneTimeWorkRequestBuilder<WorkerWeather>()
            .setInputData(workDataOf("city" to "Москва"))
            .build()
        val worker2 = OneTimeWorkRequestBuilder<WorkerWeather>()
            .setInputData(workDataOf("city" to "Лондон"))
            .build()
        val worker3 = OneTimeWorkRequestBuilder<WorkerWeather>()
            .setInputData(workDataOf("city" to "Париж"))
            .build()
        val merger = OneTimeWorkRequestBuilder<ResultWorker>()
            .build()
        workManager.beginWith(listOf(worker1, worker2, worker3)).then(merger).enqueue()
        observeWork(worker1.id, worker2.id,worker3.id, merger.id)
    }

    private fun observeWork(Worker1ID: UUID, Worker2ID: UUID, Worker3ID: UUID, merger: UUID) {
        serviceScope.launch {
            workManager.getWorkInfoByIdFlow(Worker1ID).collect { info ->
                if (info == null)
                    return@collect
                when (info.state) {
                    WorkInfo.State.RUNNING -> {
                        _state.value = _state.value.copy(
                            worker1 = _state.value.worker1.copy(
                                isRunning = true,
                                statusText = "Получаем погоду для Москвы…"
                            )
                        )
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val temp = info.outputData.getInt("temp", 0)
                        _state.value = _state.value.copy(
                            worker1 = _state.value.worker1.copy(
                                isRunning = false,
                                isDone = true,
                                statusText = "Москва: $temp°C"
                            )
                        )
                    }
                    WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                        _state.value = _state.value.copy(
                            worker1 = _state.value.worker1.copy(
                                isRunning = false,
                                isError = true,
                                statusText = "Ошибка при получении погоды Лондона"
                            )
                        )
                    }
                    else -> {}
                }
            }
        }
        serviceScope.launch {
            workManager.getWorkInfoByIdFlow(Worker2ID).collect { info ->
                if (info == null)
                    return@collect
                when (info.state) {
                    WorkInfo.State.RUNNING -> {
                        _state.value = _state.value.copy(
                            worker2 = _state.value.worker2.copy(
                                isRunning = true,
                                statusText = "Получаем погоду для Лондона…"
                            )
                        )
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val temp = info.outputData.getInt("temp", 0)
                        _state.value = _state.value.copy(
                            worker2 = _state.value.worker2.copy(
                                isRunning = false,
                                isDone = true,
                                statusText = "Лондон: $temp°C"
                            )
                        )
                    }
                    WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                        _state.value = _state.value.copy(
                            worker2 = _state.value.worker2.copy(
                                isRunning = false,
                                isError = true,
                                statusText = "Ошибка при получении погоды Лондона"
                            )
                        )
                    }
                    else -> {}
                }
            }
        }
        serviceScope.launch {
            workManager.getWorkInfoByIdFlow(Worker3ID).collect { info ->
                if (info == null)
                    return@collect
                when (info.state) {
                    WorkInfo.State.RUNNING -> {
                        _state.value = _state.value.copy(
                            worker3 = _state.value.worker3.copy(
                                isRunning = true,
                                statusText = "Получаем погоду для Парижа…"
                            )
                        )
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val temp = info.outputData.getInt("temp", 0)
                        _state.value = _state.value.copy(
                            worker3 = _state.value.worker3.copy(
                                isRunning = false,
                                isDone = true,
                                statusText = "Париж: $temp°C"
                            )
                        )
                    }
                    WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                        _state.value = _state.value.copy(
                            worker3 = _state.value.worker3.copy(
                                isRunning = false,
                                isError = true,
                                statusText = "Ошибка при получлении погоды Парижа"
                            )
                        )
                    }

                    else -> {}
                }
            }
        }

        serviceScope.launch {
            workManager.getWorkInfoByIdFlow(merger).collect { info ->
                if (info == null) return@collect
                when (info.state) {
                    WorkInfo.State.RUNNING -> {
                        _state.value = _state.value.copy(
                            resultText = "Формируем итоговый прогноз…",
                            isDone = true,
                        )
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val info1 = workManager.getWorkInfoById(Worker1ID).get()
                        val city1 = info1!!.outputData.getString("city")
                        val temp1 = info1.outputData.getInt("temp", 0)
                        val info2 = workManager.getWorkInfoById(Worker2ID).get()
                        val city2 = info2!!.outputData.getString("city")
                        val temp2 = info2.outputData.getInt("temp", 0)
                        val info3 = workManager.getWorkInfoById(Worker3ID).get()
                        val city3 = info3!!.outputData.getString("city")
                        val temp3 = info3.outputData.getInt("temp", 0)
                        _state.value = _state.value.copy(
                            isRunning = false,
                            isDone = true,
                            resultText =
                                "Итоговый прогноз:\n$city1: $temp1°C\n$city2: $temp2°C\n$city3: $temp3°C\nСредняя температура: ${(temp1 + temp2 + temp3) / 3}°C"
                        )
                        updateNotification("Задачи завершены!")
                        stopWork()
                    }
                    WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                        _state.value = _state.value.copy(
                            isRunning = false,
                            isError = true,
                            resultText = "Ошибка формирования результата"
                        )
                    }
                    else -> {}
                }
            }
        }

    }

    private fun stopWork() {
        workManager.cancelAllWork()
        stopSelf()
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Фоновая обработка")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, createNotification(text))
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Worker Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }


    inner class LocalBinder : Binder() {
        fun getService(): NotificationBack = this@NotificationBack
    }

    private val binder = LocalBinder()
    override fun onBind(intent: Intent?): IBinder = binder
}