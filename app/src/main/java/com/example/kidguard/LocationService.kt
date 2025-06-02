package com.example.kidguard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class LocationService : Service() {

    private lateinit var tracker: LocationTracker

    override fun onCreate() {
        super.onCreate()
        tracker = LocationTracker(this)
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("KIDGuard - Suivi de localisation")
            .setContentText("Le suivi est actif.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
        tracker.startLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        tracker.stopLocationUpdates()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "location_channel",
                "Channel Suivi KIDGuard",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
