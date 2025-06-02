package com.example.kidguard

import android.app.TimePickerDialog
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class TempsEcranActivity : AppCompatActivity() {

    private lateinit var editTextHeure: EditText
    private lateinit var editTextCreneau: EditText
    private lateinit var checkboxLimiteHeures: CheckBox
    private lateinit var checkboxCreneau: CheckBox
    private lateinit var UserTextView:TextView
    private lateinit var btnModeVacance: Button
    private val db = FirebaseFirestore.getInstance()
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temps_ecran)

        editTextHeure = findViewById(R.id.editTextHeure)
        editTextCreneau = findViewById(R.id.editTextCreneau)
        checkboxLimiteHeures = findViewById(R.id.checkboxLimiteHeures)
        checkboxCreneau = findViewById(R.id.checkboxCreneau)
        btnModeVacance=findViewById(R.id.btnModeVacance)
        UserTextView = findViewById(R.id.childNameTextView)
        val username = intent.getStringExtra("username")
        if (username == null) {
            Toast.makeText(this, "Aucun nom d'utilisateur fourni", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        UserTextView.text=username

        db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val document = snapshot.documents[0]
                    userId = document.getString("uid")
                    loadPreferencesFromFirestore()
                } else {
                    Toast.makeText(this, "Utilisateur non trouvé", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erreur lors de la récupération de l'utilisateur", Toast.LENGTH_SHORT).show()
                finish()
            }

        checkboxLimiteHeures.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkboxCreneau.isChecked = false
                editTextCreneau.isEnabled = false
                editTextCreneau.setText("00:00")
            } else {
                editTextCreneau.isEnabled = true
            }
        }

        checkboxCreneau.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkboxLimiteHeures.isChecked = false
                editTextHeure.isEnabled = false
                editTextHeure.setText("00:00")
            } else {
                editTextHeure.isEnabled = true
            }
        }

        editTextHeure.setOnClickListener {
            showTimePicker { time ->
                editTextHeure.setText(time)
                if (checkboxLimiteHeures.isChecked) {
                    saveToFirestore("heure", time)
                    saveToFirestore("mode", "heure")
                }
            }
        }

        editTextCreneau.setOnClickListener {
            showTimePicker { duree ->
                editTextCreneau.setText(duree)

                val currentTime = Calendar.getInstance()
                val parts = duree.split(":")
                if (parts.size == 2) {
                    val hoursToAdd = parts[0].toInt()
                    val minutesToAdd = parts[1].toInt()

                    currentTime.add(Calendar.HOUR_OF_DAY, hoursToAdd)
                    currentTime.add(Calendar.MINUTE, minutesToAdd)

                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val endTime = sdf.format(currentTime.time)

                    if (checkboxCreneau.isChecked) {
                        saveToFirestore("heure", endTime)
                        saveToFirestore("mode", "creneau")
                        saveToFirestore("creneau", duree)
                    }
                }
            }
        }
        val drawable= GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 10f
        }


        btnModeVacance.background = drawable
        btnModeVacance.backgroundTintList = null
        btnModeVacance.elevation = 10f
        btnModeVacance.setOnClickListener {
            userId?.let { uid ->
                db.collection("users").document(uid)
                    .collection("tempsEcran")
                    .document("settings")
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Temps d’écran désactivé (Mode Vacance)", Toast.LENGTH_SHORT).show()
                        editTextHeure.setText("")
                        editTextCreneau.setText("")
                        checkboxLimiteHeures.isChecked = false
                        checkboxCreneau.isChecked = false
                        editTextHeure.isEnabled = true
                        editTextCreneau.isEnabled = true
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Erreur lors de la désactivation du temps d’écran", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun loadPreferencesFromFirestore() {
        userId?.let { uid ->
            db.collection("users").document(uid).collection("tempsEcran").document("settings")
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val mode = doc.getString("mode")
                        if (mode == "heure") {
                            checkboxLimiteHeures.isChecked = true
                            editTextHeure.setText(doc.getString("heure") ?: "00:00")
                        } else if (mode == "creneau") {
                            checkboxCreneau.isChecked = true
                            editTextCreneau.setText(doc.getString("creneau") ?: "00:00")
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erreur de chargement des préférences", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveToFirestore(field: String, value: Any) {
        userId?.let { uid ->
            val data = mapOf(field to value)
            db.collection("users").document(uid).collection("tempsEcran").document("settings")
                .set(data, com.google.firebase.firestore.SetOptions.merge())
        }
    }

    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
            onTimeSelected(formattedTime)
        }, hour, minute, true).show()
    }
}
