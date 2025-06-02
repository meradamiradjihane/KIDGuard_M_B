package com.example.kidguard

data class NotificationModel(
    val userId: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
