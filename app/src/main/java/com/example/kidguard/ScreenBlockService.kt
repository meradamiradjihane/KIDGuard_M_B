package com.example.kidguard

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ScreenBlockService : Service() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val handler = Handler()
    private val checkIntervalMs = 60 * 1000L // Vérifier toutes les minutes

    private val runnable = object : Runnable {
        override fun run() {
            checkScreenBlockTime()
            handler.postDelayed(this, checkIntervalMs)
        }
    }

    override fun onCreate() {
        super.onCreate()
        handler.post(runnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }

    private fun checkScreenBlockTime() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // On récupère le role ET le temps de blocage dans Firestore
        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                if (userDoc.exists()) {
                    val role = userDoc.getString("role")
                    if (role == "enfant") {
                        // Si c'est un enfant, on va chercher l'heure de blocage
                        db.collection("users").document(userId)
                            .collection("tempsEcran").document("settings")
                            .get()
                            .addOnSuccessListener { settingsDoc ->
                                if (settingsDoc.exists()) {
                                    val heureBlocage = settingsDoc.getString("heure")

                                    if (!heureBlocage.isNullOrEmpty()) {
                                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                                        val currentTime = sdf.format(Date())

                                        Log.d("ScreenBlockService", "CurrentTime: $currentTime, HeureBlocage: $heureBlocage")

                                        if (currentTime == heureBlocage) {
                                            val intent = Intent(this, ScreenBlockActivity::class.java)
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            startActivity(intent)
                                        }
                                    }
                                } else {
                                    Log.d("ScreenBlockService", "Pas de réglages temps écran pour cet utilisateur")
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("ScreenBlockService", "Erreur récupération temps écran: ${e.message}")
                            }
                    }
                }
            }
            .addOnFailureListener {
                Log.e("ScreenBlockService", "Erreur récupération user data Firestore: ${it.message}")
            }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
