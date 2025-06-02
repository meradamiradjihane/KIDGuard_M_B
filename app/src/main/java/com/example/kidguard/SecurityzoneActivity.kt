package com.example.kidguard

import android.graphics.Color
import android.location.Geocoder
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import java.util.*

class SecurityzoneActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var adresseInput: AutoCompleteTextView
    private lateinit var rayonInput: EditText
    private lateinit var addButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SecurezoneAdapter
    private val zones = mutableListOf<SafeZone>()

    private var userid: String? = null
    private var map: GoogleMap? = null
    private val db = FirebaseFirestore.getInstance()

    private lateinit var placesClient: PlacesClient
    private lateinit var autocompleteAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.securezoneactivity)

        adresseInput = findViewById(R.id.adresseAutoComplete)
        rayonInput = findViewById(R.id.rayonEditText)
        addButton = findViewById(R.id.addButton)
        recyclerView = findViewById(R.id.adresseRecyclerView)
        val userNameTextView = findViewById<TextView>(R.id.childNameTextView)
        userNameTextView.text = "Chargement..."


        Places.initialize(applicationContext, "AIzaSyBiC9L-5B_BAjnJMx08j45kUezjm_Tyqsc")
        placesClient = Places.createClient(this)


        autocompleteAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf<String>()
        )
        adresseInput.setAdapter(autocompleteAdapter)


        adresseInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let { query ->
                    if (query.isEmpty()) {
                        autocompleteAdapter.clear()
                        autocompleteAdapter.notifyDataSetChanged()
                        return
                    }
                    val request = FindAutocompletePredictionsRequest.builder()
                        .setTypeFilter(TypeFilter.ADDRESS)
                        .setQuery(query.toString())
                        .build()

                    placesClient.findAutocompletePredictions(request)
                        .addOnSuccessListener { response ->
                            val suggestions = response.autocompletePredictions
                                .map { it.getFullText(null).toString() }
                            autocompleteAdapter.clear()
                            autocompleteAdapter.addAll(suggestions)
                            autocompleteAdapter.notifyDataSetChanged()
                            adresseInput.showDropDown()
                        }

                }
            }
        })

        adapter = SecurezoneAdapter(zones,
            onItemClick = { zone -> afficherZoneSurCarte(zone) },
            onDeleteClick = { zone -> deleteZone(zone) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

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
                    userNameTextView.text = username
                    loadZonesFromdb()
                } else {
                    userNameTextView.text = username
                    Toast.makeText(this, "Utilisateur non trouvé", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                userNameTextView.text = username
                Toast.makeText(this, "Erreur lors de la récupération utilisateur", Toast.LENGTH_SHORT).show()
            }

        addButton.setOnClickListener {
            addZone()
        }
    }

    private fun addZone() {
        val adresse = adresseInput.text.toString()
        val rayon = rayonInput.text.toString().toIntOrNull() ?: 0

        if (adresse.isBlank() || rayon <= 0) {
            Toast.makeText(this, "Adresse ou rayon invalide", Toast.LENGTH_SHORT).show()
            return
        }

        val geocoder = Geocoder(this, Locale.getDefault())
        val result = geocoder.getFromLocationName(adresse, 1)
        if (result == null || result.isEmpty()) {
            Toast.makeText(this, "Adresse non trouvée", Toast.LENGTH_SHORT).show()
            return
        }

        val location = result[0]
        val zone = SafeZone(adresse, location.latitude, location.longitude, rayon)

        userid?.let { uid ->
            db.collection("users").document(uid)
                .collection("securezone")
                .add(zone)
                .addOnSuccessListener {
                    zones.add(zone)
                    adapter.notifyItemInserted(zones.size - 1)
                    adresseInput.text.clear()
                    rayonInput.text.clear()
                    afficherZoneSurCarte(zone)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erreur lors de l'ajout", Toast.LENGTH_SHORT).show()
                }
        } ?: Toast.makeText(this, "Utilisateur non défini", Toast.LENGTH_SHORT).show()
    }

    private fun deleteZone(zone: SafeZone) {
        userid?.let { uid ->
            db.collection("users").document(uid)
                .collection("securezone")
                .whereEqualTo("adresse", zone.adresse)
                .whereEqualTo("rayon", zone.rayon)
                .get()
                .addOnSuccessListener { snapshot ->
                    for (doc in snapshot) {
                        doc.reference.delete()
                    }
                    val index = zones.indexOf(zone)
                    if (index != -1) {
                        zones.removeAt(index)
                        adapter.notifyItemRemoved(index)
                    }
                    map?.clear()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erreur suppression", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadZonesFromdb() {
        userid?.let { uid ->
            db.collection("users").document(uid)
                .collection("securezone")
                .get()
                .addOnSuccessListener { snapshot ->
                    zones.clear()
                    zones.addAll(snapshot.toObjects(SafeZone::class.java))
                    adapter.notifyDataSetChanged()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erreur de chargement", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
    }

    private fun afficherZoneSurCarte(zone: SafeZone) {
        map?.clear()
        val position = LatLng(zone.latitude, zone.longitude)
        map?.addMarker(MarkerOptions().position(position).title(zone.adresse))
        map?.addCircle(
            CircleOptions()
                .center(position)
                .radius(zone.rayon.toDouble())
                .strokeColor(Color.GRAY )
                .fillColor(Color.LTGRAY)
                .strokeWidth(2f)
        )
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
    }
}
