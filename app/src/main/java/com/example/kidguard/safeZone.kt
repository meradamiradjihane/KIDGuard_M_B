package com.example.kidguard
data class SafeZone(
    val adresse: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val rayon: Int = 100
)
