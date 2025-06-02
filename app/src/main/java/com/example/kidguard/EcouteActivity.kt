package com.example.kidguard

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class EcouteActivity : AppCompatActivity() {
    private lateinit var UserTextView: TextView
    private val db = FirebaseFirestore.getInstance()
    private var userId: String? = null
    private val storage = FirebaseStorage.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ecoute)

        val btnCamera = findViewById<ImageButton>(R.id.btnCamera)
        val btnAudio = findViewById<ImageButton>(R.id.btnAudio)
        UserTextView = findViewById(R.id.childNameTextView)

        val username = intent.getStringExtra("username")
        if (username == null) {
            Toast.makeText(this, "Aucun nom d'utilisateur fourni", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        UserTextView.text = username

        db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val document = snapshot.documents[0]
                    userId = document.getString("uid")
                } else {
                    Toast.makeText(this, "Utilisateur non trouvé", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erreur lors de la récupération de l'utilisateur", Toast.LENGTH_SHORT).show()
                finish()
            }


        btnCamera.setOnClickListener {
            if (userId == null) {
                Toast.makeText(this, "ID utilisateur introuvable", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userRef = db.collection("users").document(userId!!)
            userRef.update("camera", true)
                .addOnSuccessListener {
                    Toast.makeText(this, "Partage de caméra activé pour 30 secondes", Toast.LENGTH_SHORT).show()
                    Handler(Looper.getMainLooper()).postDelayed({
                        userRef.update("camera", false)
                    }, 1000)

                    Handler(Looper.getMainLooper()).postDelayed({
                        playLastVideoFromStorage(userId!!)
                    }, 32000)
                }

        }

        // Audio
        btnAudio.setOnClickListener {
            if (userId == null) {
                Toast.makeText(this, "ID utilisateur introuvable", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userRef = db.collection("users").document(userId!!)
            userRef.update("ecoute", true)
                .addOnSuccessListener {
                    Toast.makeText(this, "Écoute activée pour 30 secondes", Toast.LENGTH_SHORT).show()
                    Handler(Looper.getMainLooper()).postDelayed({
                        userRef.update("ecoute", false)

                    }, 30000)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erreur lors de l'activation de l'écoute", Toast.LENGTH_SHORT).show()
                }
            Handler(Looper.getMainLooper()).postDelayed({
            playLastAudioFromStorage(userId!!) }, 32000)
        }
    }


    private fun playLastAudioFromStorage(userId: String) {
        val folderRef = storage.reference.child("recordings/audio/")
        folderRef.listAll()
            .addOnSuccessListener { listResult ->
                val userFiles = listResult.items.filter { it.name.startsWith(userId) }
                if (userFiles.isEmpty()) {
                    Toast.makeText(this, "Aucun enregistrement trouvé", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val lastFile = userFiles.maxByOrNull { it.name.substringAfter("_").substringBefore(".").toLong() }

                lastFile?.downloadUrl?.addOnSuccessListener { uri ->
                    val mediaPlayer = MediaPlayer().apply {
                        setDataSource(uri.toString())
                        prepare()
                        start()
                    }

                    Toast.makeText(this, "Lecture audio...", Toast.LENGTH_SHORT).show()

                    mediaPlayer.setOnCompletionListener {
                        mediaPlayer.release()
                        Toast.makeText(this, "Lecture terminée", Toast.LENGTH_SHORT).show()
                    }
                }?.addOnFailureListener {
                    Toast.makeText(this, "Erreur de lecture", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erreur d'accès au stockage", Toast.LENGTH_SHORT).show()
            }
    }
    private fun playLastVideoFromStorage(userId: String) {
        val folderRef = storage.reference.child("recordings/video/")
        folderRef.listAll()
            .addOnSuccessListener { listResult ->
                // Filtrer les fichiers appartenant à l'utilisateur
                val userFiles = listResult.items.filter { it.name.startsWith(userId) }
                if (userFiles.isEmpty()) {
                    Toast.makeText(this, "Aucune vidéo trouvée", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val lastFile = userFiles.maxByOrNull { it.name.substringAfter("_").substringBefore(".").toLong() }

                lastFile?.downloadUrl?.addOnSuccessListener { uri ->
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "video/mp4")
                        flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    startActivity(intent)

                }?.addOnFailureListener {
                    Toast.makeText(this, "Erreur de lecture vidéo", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erreur d'accès au stockage vidéo", Toast.LENGTH_SHORT).show()
            }
    }

}
