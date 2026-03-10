package com.example.a49

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


//class NotificationViewModel(private val notificationService: NotificationBack) : ViewModel() {
//
//    private val _state = MutableStateFlow(ProcessingState())
//    val state: StateFlow<ProcessingState> = _state
//    init {
//        // Подписка на состояние сервиса
//        viewModelScope.launch {
//            notificationService.state.collectLatest { processingState ->
//                _state.value = processingState
//            }
//        }
//    }

//    fun startWork() {
//        // Отправляем intent сервису
//        notificationService.startService(Intent(notificationService, NotificationBack::class.java).apply {
//            action = "start"
//        })
//    }
//
//    fun stopWork() {
//        notificationService.stopService(Intent(notificationService, NotificationBack::class.java))
//    }
//}