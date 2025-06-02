package com.example.kidguard

data class ConversationData(
    val id: String,
    val contactName: String,
    val contactUserId: String,
    val lastMessage: String,
    val lastTimestamp: Long
)
