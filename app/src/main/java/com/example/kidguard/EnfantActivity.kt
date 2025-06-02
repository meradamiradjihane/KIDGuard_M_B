package com.example.kidguard

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class EnfantActivity : AppCompatActivity() {

    private lateinit var sosButton: Button
    private lateinit var logoutButton: Button
    private lateinit var messageButton: ImageButton
    private lateinit var userNameTextView: TextView
    private lateinit var agendaButton: Button
    private lateinit var editTextView: TextView
    private lateinit var profileImageView: ImageView

    private lateinit var username: String
    private lateinit var auth: FirebaseAuth
    private lateinit var locationTracker: LocationTracker

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadImageAndSaveProfile(it) }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.enfant_activity)

        sosButton = findViewById(R.id.sosButton)
        messageButton = findViewById(R.id.messageButton)
        userNameTextView = findViewById(R.id.userNameTextView)
        agendaButton = findViewById(R.id.agendaButton)
        editTextView = findViewById(R.id.editTextView)
        profileImageView = findViewById(R.id.profileImageView)
        logoutButton = findViewById(R.id.logoutButton)


        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            startActivity(Intent(this, AuthentificationEnfantActivity::class.java))
            finish()
            return
        }

        val userId = currentUser.uid
        loadChildName(userId)
        loadChildProfile(userId)

        setupButtons()

        setupSOSButton()

        locationTracker = LocationTracker(this)
        if (checkPermissions()) locationTracker.startLocationUpdates()
        else requestPermissions()

        startService(Intent(this, ScreenBlockService::class.java))
       checkCameraOrEcouteForUser(userId)
      //startActivity(Intent(this, EnfantRecordingActivity::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        locationTracker.stopLocationUpdates()
    }

    private fun checkPermissions(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED && coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
            1001)
    }

    private fun loadChildName(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val name = document.getString("name") ?: document.getString("username") ?: "Enfant"
                userNameTextView.text = name
                username = document.getString("username") ?: "inconnu"

            }
            .addOnFailureListener {
                Toast.makeText(this, "Erreur lors de la récupération du nom", Toast.LENGTH_LONG).show()
            }
    }

    private fun checkCameraOrEcouteForUser(userid: String) {
        db.collection("users")
            .whereEqualTo("uid", userid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val found = querySnapshot.documents.any { doc ->
                    val camera = doc.getBoolean("camera") ?: false
                    val ecoute = doc.getBoolean("ecoute") ?: false
                    camera || ecoute
                }
                if (found) {
                    startActivity(Intent(this, EnfantRecordingActivity::class.java))
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erreur lors de la vérification des permissions", Toast.LENGTH_LONG).show()
            }
    }


    private fun loadChildProfile(userId: String) {
        val storageRef = storage.reference.child("profile_images/$userId.jpg")
        storageRef.downloadUrl.addOnSuccessListener { uri ->
            val imageUrlWithTimestamp = "${uri}?t=${System.currentTimeMillis()}"
            Glide.with(this)
                .load(imageUrlWithTimestamp)
                .transform(CircleCrop())
                .skipMemoryCache(true)
                .into(profileImageView)
        }
    }

    private fun uploadImageAndSaveProfile(uri: Uri) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid
        val storageRef = storage.reference.child("profile_images/$userId.jpg")

        storageRef.putFile(uri).addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                val imageUrl = "${downloadUri}?t=${System.currentTimeMillis()}"
                Glide.with(this)
                    .load(imageUrl)
                    .transform(CircleCrop())
                    .skipMemoryCache(true)
                    .into(profileImageView)

                Toast.makeText(this, "Photo de profil mise à jour", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Erreur d’upload: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupButtons() {
        agendaButton.apply {
            val calendarIcon = getDrawable(R.drawable.ic_calendar)
            calendarIcon?.setBounds(0, 0, 100, 120)
            setCompoundDrawables(calendarIcon, null, null, null)
            background = GradientDrawable().apply {
                cornerRadius = 10f
                setColor(Color.WHITE)
            }
            backgroundTintList = null
            elevation = 10f
            setOnClickListener {
                startActivity(Intent(this@EnfantActivity, AgendaActivity::class.java))
            }
        }

        logoutButton.apply {
            val logIcon = getDrawable(R.drawable.logout)
            logIcon?.setBounds(0, 0, 100, 120)
            setCompoundDrawables(logIcon, null, null, null)
            background = GradientDrawable().apply {
                cornerRadius = 10f
                setColor(Color.WHITE)
            }
            backgroundTintList = null
            elevation = 10f
            setOnClickListener {
                FirebaseAuth.getInstance().signOut()
                Toast.makeText(this@EnfantActivity, "Déconnecté avec succès", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@EnfantActivity, MainActivity::class.java))
                finish()
            }
        }

        messageButton.setOnClickListener {
            val intent = Intent(this, MessagesActivity::class.java)
            intent.putExtra("username", username)
            intent.putExtra("userid", auth.currentUser?.uid)
            startActivity(intent)
        }

        editTextView.setOnClickListener {
            val options = arrayOf("Modifier le nom", "Modifier la photo de profil")
            AlertDialog.Builder(this)
                .setTitle("Modifier le profil")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> modifierNom()
                        1 -> modifierPhoto()
                    }
                }
                .show()
        }
    }

    private fun setupSOSButton() {
        var isPressed = false
        var sosActivated = false
        val handler = Handler(Looper.getMainLooper())
        var actionRunnable: Runnable? = null

        sosButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isPressed = true
                    actionRunnable = Runnable {
                        if (isPressed) {
                            sosActivated = !sosActivated
                            sosButton.text = if (sosActivated) "SOS" else "OK"
                            if (sosActivated) launchSOS() else setOK()
                        }
                    }
                    handler.postDelayed(actionRunnable!!, 3000)
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isPressed = false
                    handler.removeCallbacks(actionRunnable!!)
                    true
                }

                else -> false
            }
        }
    }

    private fun launchSOS() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).update("status", "SOS")
        db.collection("children").document(username).update("status", "SOS")

        val sosLog = hashMapOf(
            "timestamp" to FieldValue.serverTimestamp(),
            "status" to "SOS"
        )

        db.collection("users").document(userId).collection("alerts").add(sosLog)
    }

    private fun setOK() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).update("status", "OK")
        db.collection("children").document(username).update("status", "OK")
    }

    private fun modifierNom() {
        val input = EditText(this).apply {
            setText(userNameTextView.text.toString())
        }

        AlertDialog.Builder(this)
            .setTitle("Nouveau nom")
            .setView(input)
            .setPositiveButton("Valider") { _, _ ->
                val nouveauNom = input.text.toString().trim()
                if (nouveauNom.isNotEmpty()) {
                    val currentUser = auth.currentUser ?: return@setPositiveButton
                    val userId = currentUser.uid
                    userNameTextView.text = nouveauNom
                    db.collection("users").document(userId).update("name", nouveauNom)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Nom mis à jour", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Erreur: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(this, "Nom invalide", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun modifierPhoto() {
        imagePicker.launch("image/*")
    }
}
