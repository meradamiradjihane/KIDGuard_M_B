package com.example.kidguard

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.pow
import kotlin.math.sqrt

class GeolocalisationActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val db = FirebaseFirestore.getInstance()
    private var userid: String? = null
    private lateinit var locationTracker: LocationTracker
    private lateinit var Username: String

    private val safeZones = mutableListOf<SafeZone>()

    companion object {
        private const val PERMISSION_REQUEST_LOCATION = 100
        private const val CHANNEL_ID = "sos_channel_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.maps_activity)
        showSOSNotification()
        val userNameTextView = findViewById<TextView>(R.id.childNameTextView)


       val username = intent.getStringExtra("username")
        if (username == null) {
            Toast.makeText(this, "Aucun nom d'utilisateur fourni", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    userid = document.getString("uid")
                    Username=username
                    userNameTextView.text = username
                    loadSafeZones()
                } else {
                    userNameTextView.text = username
                    Toast.makeText(this, "Utilisateur non trouvé", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                userNameTextView.text = username
                Toast.makeText(
                    this,
                    "Erreur lors de la récupération utilisateur",
                    Toast.LENGTH_SHORT
                ).show()
            }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        locationTracker = LocationTracker(this)


        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 10f
        }
        val drawable2 = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 10f
        }

        val boutonItineraire = findViewById<Button>(R.id.ItButton)
        val boutonHistorique = findViewById<Button>(R.id.HistoriqueButton)

        fun setupButton(
            button: Button,
            drawable: GradientDrawable,
            iconResId: Int,
            x: Int,
            y: Int
        ) {
            button.background = drawable
            button.backgroundTintList = null
            button.elevation = 10f

            val logo = resources.getDrawable(iconResId, theme)
            logo.setBounds(-20, 0, x, y)
            button.setCompoundDrawables(logo, null, null, null)
            button.setCompoundDrawablePadding(-130)

            button.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> button.elevation = 12f
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> button.elevation = 8f
                }
                false
            }
        }

        setupButton(boutonItineraire, drawable2, R.drawable.itineraire, 100, 120)
        setupButton(boutonHistorique, drawable, R.drawable.historique, 100, 110)

        boutonHistorique.setOnClickListener {
            if (userid == null) {
                Toast.makeText(this, "Utilisateur non défini", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = android.content.Intent(this, HistoriqueActivity::class.java)
            intent.putExtra("userid", userid)
            intent.putExtra("username", username)
            startActivity(intent)
        }

        boutonItineraire.setOnClickListener {
            if (userid == null) {
                Toast.makeText(this, "Utilisateur non défini", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            db.collection("users")
                .document(userid!!)
                .collection("position_actuelle")
                .document("dernier")
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val lat = document.getDouble("latitude")
                        val lng = document.getDouble("longitude")

                        if (lat != null && lng != null) {
                            val gmmIntentUri =
                                android.net.Uri.parse("google.navigation:q=$lat,$lng")
                            val mapIntent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                gmmIntentUri
                            )
                            mapIntent.setPackage("com.google.android.apps.maps")

                            if (mapIntent.resolveActivity(packageManager) != null) {
                                startActivity(mapIntent)
                            } else {
                                Toast.makeText(
                                    this,
                                    "Google Maps n'est pas installé",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(this, "Position invalide", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Pas de position enregistrée", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Erreur récupération position : ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            startLocationTracking()
            afficherDernierePosition()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_LOCATION
            )
        }
    }

    private fun startLocationTracking() {
        locationTracker.startLocationUpdates()
    }

    private fun afficherDernierePosition() {
        val uid = userid
        if (uid == null) {
            Log.w("Geolocalisation", "UID utilisateur non disponible")
            return
        }

        db.collection("users")
            .document(uid)
            .collection("position_actuelle")
            .document("dernier")
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val lat = documentSnapshot.getDouble("latitude")
                    val lng = documentSnapshot.getDouble("longitude")
                    if (lat != null && lng != null) {
                        val lastPosition = LatLng(lat, lng)
                        mMap.addMarker(
                            MarkerOptions()
                                .position(lastPosition)
                                .title("Dernière position enregistrée")
                        )
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastPosition, 15f))

                        // Vérification si en zone sécurisée
                        val inSafeZone = isInSafeZone(lat, lng)
                        if (!inSafeZone) {
                            showSOSNotification()
                        }
                    } else {
                        Toast.makeText(this, "Coordonnées non disponibles", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Toast.makeText(this, "Aucune position enregistrée", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Geolocalisation", "Erreur récupération position: ${e.message}")
                Toast.makeText(
                    this,
                    "Erreur lors de la récupération de la position",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun isInSafeZone(latitude: Double, longitude: Double): Boolean {
        if (safeZones.isEmpty()) return false

        for (zone in safeZones) {
            val dist = distance(latitude, longitude, zone.latitude, zone.longitude)
            if (dist <= zone.rayon) return true
        }
        return false
    }
    private fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // en mètres
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2).pow(2.0) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2).pow(2.0)

        val c = 2 * Math.atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    private fun showSOSNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notifications SOS",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications pour Alerte de sécurité"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logop)
            .setContentTitle("Enfant Hors Zone !")
            .setContentText("Un enfant est hors de la zone sécurisée !")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        Log.d("Geolocalisation", "Envoi notification SOS")

        notificationManager.notify(userid?.hashCode() ?: 0, builder.build())
    }


    private fun loadSafeZones() {
        userid?.let { uid ->
            db.collection("users").document(uid)
                .collection("securezone")
                .get()
                .addOnSuccessListener { snapshot ->
                    safeZones.clear()
                    for (doc in snapshot.documents) {
                        val adresse = doc.getString("adresse") ?: ""
                        val lat = doc.getDouble("latitude")
                        val lng = doc.getDouble("longitude")
                        val rayonDouble = doc.getDouble("rayon")

                        if (lat != null && lng != null && rayonDouble != null) {
                            // Convertir rayon en Int (en arrondissant)
                            val rayon = rayonDouble.toInt()
                            safeZones.add(SafeZone(adresse, lat, lng, rayon))
                        }
                    }
                    Log.d("Geolocalisation", "Zones sécurisées chargées: ${safeZones.size}")
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erreur chargement zones sécurisées", Toast.LENGTH_SHORT).show()
                }
        }
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    mMap.isMyLocationEnabled = true
                    startLocationTracking()
                    afficherDernierePosition()
                }
            } else {
                Toast.makeText(this, "Permission localisation refusée", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

