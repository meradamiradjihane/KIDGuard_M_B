package com.example.kidguard

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AgendaActivity : AppCompatActivity() {

    private lateinit var calendarView: CalendarView
    private lateinit var taskContainer: LinearLayout
    private lateinit var btnAddTask: ImageButton

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var selectedDate: String = dateFormat.format(Date())

    private val db = FirebaseFirestore.getInstance()
    private var username: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agenda)


        val userNameTextView = findViewById<TextView>(R.id.childNameTextView)
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        username = intent.getStringExtra("username")

        if (username == null && userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        username = document.getString("username")
                        val displayName = username ?: "Inconnu"

                        userNameTextView.text = displayName
                        if (username != null) {
                            afficherTaches()
                        }
                    } else {
                        userNameTextView.text = "Inconnu"
                        Toast.makeText(this, "Utilisateur introuvable", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    userNameTextView.text = "Inconnu"
                    Toast.makeText(this, "Erreur lors de la récupération : ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else if (username != null) {
             db.collection("users").whereEqualTo("username", username).get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val document = querySnapshot.documents[0]
                        val name = document.getString("name")
                        val displayName = name ?: username ?: "Inconnu"
                        userNameTextView.text = displayName
                    } else {
                        userNameTextView.text = username
                    }
                    afficherTaches()
                }
                .addOnFailureListener {
                    userNameTextView.text = username
                    afficherTaches()
                }
        } else {
            userNameTextView.text = "Inconnu"
            Toast.makeText(this, "Impossible de récupérer l'utilisateur", Toast.LENGTH_SHORT).show()
            finish()
        }

        calendarView = findViewById(R.id.calendarView)
        taskContainer = findViewById(R.id.taskContainer)
        btnAddTask = findViewById(R.id.btnAddTask)

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            selectedDate = dateFormat.format(calendar.time)
            afficherTaches()
        }

        btnAddTask.setOnClickListener {
            afficherPopupAjoutTache()
        }


    }

        private fun afficherTaches() {
        taskContainer.removeAllViews()

        val titreView = TextView(this).apply {
            text = "Tâches pour le $selectedDate"
            textSize = 18f
            setPadding(16, 16, 16, 16)
        }
        taskContainer.addView(titreView)

        if (username == null) return

        db.collection("agenda")
            .document(username!!)
            .collection(selectedDate)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val nom = document.getString("nom") ?: ""
                    val heure = document.getString("heure") ?: ""

                    val taskView = LayoutInflater.from(this)
                        .inflate(R.layout.item_task, taskContainer, false)

                    val taskNameView = taskView.findViewById<TextView>(R.id.taskName)
                    val taskTimeView = taskView.findViewById<TextView>(R.id.taskTime)

                    taskNameView.text = nom
                    taskTimeView.text = heure

                    taskView.setOnLongClickListener {
                        AlertDialog.Builder(this)
                            .setTitle("Supprimer cette tâche ?")
                            .setMessage("$nom - $heure")
                            .setPositiveButton("Supprimer") { _, _ ->
                                db.collection("agenda").document(username!!)
                                    .collection(selectedDate).document(document.id)
                                    .delete()
                                    .addOnSuccessListener { afficherTaches() }
                            }
                            .setNegativeButton("Annuler", null)
                            .show()
                        true
                    }

                    taskContainer.addView(taskView)
                }
            }
    }

    private fun afficherPopupAjoutTache() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val inputNomTache = dialogView.findViewById<EditText>(R.id.editTextNomTache)
        val inputHeureTache = dialogView.findViewById<EditText>(R.id.editTextHeureTache)

        AlertDialog.Builder(this)
            .setTitle("Ajouter une tâche")
            .setView(dialogView)
            .setPositiveButton("Ajouter") { _, _ ->
                val nom = inputNomTache.text.toString().trim()
                val heure = inputHeureTache.text.toString().trim()
                if (nom.isNotEmpty() && heure.isNotEmpty()) {
                    val tache = hashMapOf(
                        "nom" to nom,
                        "heure" to heure
                    )
                    if (username != null) {
                        db.collection("agenda")
                            .document(username!!)
                            .collection(selectedDate)
                            .add(tache)
                            .addOnSuccessListener {
                                afficherTaches()
                            }
                    }
                } else {
                    Toast.makeText(this, "Veuillez remplir les deux champs", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
}
