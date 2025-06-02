package com.example.kidguard

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class LocationTracker(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationRequest = LocationRequest.create().apply {
        interval = 600000  // 10 minutes
        fastestInterval = 5000
        priority = Priority.PRIORITY_HIGH_ACCURACY
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {
                saveLocationToFirestore(location)
                updateCurrentLocation(location)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            context.mainLooper
        )
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun saveLocationToFirestore(location: Location) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val data = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "timestamp" to Date()
        )

        db.collection("users")
            .document(userId)
            .collection("historique")
            .add(data)
            .addOnSuccessListener {
                Log.d("LocationTracker", "Position ajoutée à l'historique")
            }
            .addOnFailureListener {
                Log.e("LocationTracker", "Erreur historique: ${it.message}")
            }
    }

    private fun updateCurrentLocation(location: Location) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val currentLocation = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "timestamp" to Date()
        )

        db.collection("users")
            .document(userId)
            .collection("position_actuelle")
            .document("dernier")  // Un seul document mis à jour à chaque fois
            .set(currentLocation)
            .addOnSuccessListener {
                Log.d("LocationTracker", "Position actuelle mise à jour")
            }
            .addOnFailureListener {
                Log.e("LocationTracker", "Erreur position actuelle: ${it.message}")
            }
    }
}
