package com.example.kidguard

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class HistoriqueActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val historiqueList = mutableListOf<String>()
    private val db = FirebaseFirestore.getInstance()
    private var userid: String? = null
    private lateinit var userTextView:TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historique)

        listView = findViewById(R.id.historiqueListView)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, historiqueList)
        listView.adapter = adapter

        userid = intent.getStringExtra("userid") // récupère userid
        val username = intent.getStringExtra("username")
        val userNameTextView = findViewById<TextView>(R.id.childNameTextView)
        userNameTextView.text = username

        if (userid != null) {
            chargerHistorique(userid!!)
        } else {
            Toast.makeText(this, "Utilisateur non défini", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun chargerHistorique(uid: String) {
        db.collection("users")
            .document(uid)
            .collection("historique")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                historiqueList.clear()
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRANCE)
                for (doc in documents) {
                    val lat = doc.getDouble("latitude")
                    val lon = doc.getDouble("longitude")
                    val timestamp = doc.getTimestamp("timestamp")
                    val dateStr = timestamp?.let { sdf.format(it.toDate()) } ?: "Heure inconnue"

                    if (lat != null && lon != null) {
                        historiqueList.add("📍 $lat, $lon\n🕒 $dateStr")
                    } else {
                        Log.w("HistoriqueActivity", "Coordonnées manquantes dans doc ${doc.id}")
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("HistoriqueActivity", "Erreur chargement historique : ${e.message}")
                Toast.makeText(this, "Erreur de chargement", Toast.LENGTH_SHORT).show()
            }
    }
}
