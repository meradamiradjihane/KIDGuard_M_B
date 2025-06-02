package com.example.kidguard

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AuthentificationEnfantActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_authenf)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            startActivity(Intent(this, EnfantActivity::class.java))
            finish()
            return
        }

        val drawableNormal = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 10f
            setColor(Color.parseColor("#FFFFFF"))
        }

        val suivantButton = findViewById<Button>(R.id.nextButton)
        suivantButton.background = drawableNormal
        suivantButton.backgroundTintList = null
        suivantButton.elevation = 10f

        val usernameField = findViewById<EditText>(R.id.usernameField)
        val passwordField = findViewById<EditText>(R.id.passwordField)

        suivantButton.setOnClickListener {
            val username = usernameField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            db.collection("children").document(username).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val fakeEmail = "$username@kidguard.fake"

                        auth.signInWithEmailAndPassword(fakeEmail, password)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Connexion réussie", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, EnfantActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener {
                                auth.createUserWithEmailAndPassword(fakeEmail, password)
                                    .addOnSuccessListener { result ->
                                        val userId = result.user?.uid ?: return@addOnSuccessListener
                                        val userMap = hashMapOf(
                                            "uid" to userId,
                                            "status" to "OK",
                                            "username" to username,
                                            "role" to "enfant",
                                            "createdAt" to FieldValue.serverTimestamp()
                                        )
                                        db.collection("users").document(userId).set(userMap)
                                            .addOnSuccessListener {
                                                Toast.makeText(this, "Compte créé", Toast.LENGTH_SHORT).show()
                                                startActivity(Intent(this, EnfantActivity::class.java))
                                                finish()
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(this, "Erreur Firestore : ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                    }

                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Erreur création compte : ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                    } else {
                        Toast.makeText(this, "Nom d'enfant introuvable", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erreur lors de la vérification : ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}
