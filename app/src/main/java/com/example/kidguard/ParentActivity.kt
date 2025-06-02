package com.example.kidguard

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ParentActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var childAdapter: ChildAdapter
    private val children = mutableListOf<Child>()
    private val childStatusMap = mutableMapOf<String, String>()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.parent_activity)

        db = FirebaseFirestore.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        val ajouterButton = findViewById<Button>(R.id.ajouterButton)
        val supButton = findViewById<Button>(R.id.suprimerButton)
        styleButton(ajouterButton)
        styleButton(supButton)

        recyclerView = findViewById(R.id.childRecyclerView)
        childAdapter = ChildAdapter(children) { child ->
            val intent = Intent(this, ChildDetailActivity::class.java)
            intent.putExtra("child_name", child.name)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = childAdapter
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        listenToChildrenRealtime()

        ajouterButton.setOnClickListener {
            val editText = EditText(this)
            editText.hint = "Entrez le nom de l'enfant"

            val titleView = TextView(this).apply {
                text = "Ajouter un enfant"
                setTextColor(ContextCompat.getColor(this@ParentActivity, R.color.colorPrimary))
                textSize = 20f
                setPadding(40, 30, 40, 20)
            }

            AlertDialog.Builder(this)
                .setCustomTitle(titleView)
                .setView(editText)
                .setPositiveButton("OK") { _, _ ->
                    val childName = editText.text.toString().trim()
                    val defaultStatus = "OK"
                    if (childName.isNotEmpty()) {
                        val newChild = Child(childName, defaultStatus)
                        children.add(newChild)
                        childAdapter.notifyItemInserted(children.size - 1)

                        val childMap = hashMapOf(
                            "name" to childName,
                            "status" to defaultStatus
                        )

                        db.collection("children").document(childName)
                            .set(childMap)
                            .addOnSuccessListener {
                                Log.d("FirestoreAdd", "$childName ajouté à Firestore")
                                Toast.makeText(this, "$childName ajouté à Firestore", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Log.e("FirestoreAdd", "Erreur d'ajout", e)
                                Toast.makeText(this, "Erreur d'ajout à Firestore", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Veuillez entrer un nom.", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Annuler", null)
                .create()
                .apply {
                    window?.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this@ParentActivity, R.color.gris)))
                }
                .show()
        }

        supButton.setOnClickListener {
            val childrenNames = children.map { it.name }.toTypedArray()

            val titleView = TextView(this).apply {
                text = "Supprimer un Enfant"
                setTextColor(ContextCompat.getColor(this@ParentActivity, R.color.colorPrimary))
                textSize = 20f
                setPadding(40, 30, 40, 20)
            }

            AlertDialog.Builder(this)
                .setCustomTitle(titleView)
                .setItems(childrenNames) { _, which ->
                    val selectedChild = children[which]
                    children.removeAt(which)
                    childAdapter.notifyItemRemoved(which)

                    db.collection("children").document(selectedChild.name)
                        .delete()
                        .addOnSuccessListener {
                            Log.d("FirestoreDelete", "${selectedChild.name} supprimé")
                            Toast.makeText(this, "${selectedChild.name} supprimé", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirestoreDelete", "Erreur de suppression", e)
                            Toast.makeText(this, "Erreur de suppression", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Annuler", null)
                .create()
                .apply {
                    window?.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this@ParentActivity, R.color.gris)))
                }
                .show()
        }

        val logoutIcon = findViewById<ImageView>(R.id.logoutIcon)
        logoutIcon.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(this, "Déconnecté avec succès", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun styleButton(button: Button) {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 10f
            setColor(Color.parseColor("#FFFFFF"))
        }
        button.background = drawable
        button.backgroundTintList = null
        button.elevation = 10f

        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> button.elevation = 12f
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> button.elevation = 8f
            }
            false
        }
    }

    private fun listenToChildrenRealtime() {
        db.collection("children")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("FirestoreListen", "Erreur d'écoute", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    children.clear()

                    for (doc in snapshots) {
                        val name = doc.getString("name") ?: continue
                        val status = doc.getString("status") ?: "OK"

                        val oldStatus = childStatusMap[name]
                        if (oldStatus != null && oldStatus != status && status == "SOS") {
                            saveNotificationToFirestore(name)
                        }

                        childStatusMap[name] = status
                        children.add(Child(name, status))
                    }

                    childAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun saveNotificationToFirestore(userId: String) {
        val notification = mapOf(
            "userId" to userId,
            "message" to "L'enfant $userId a déclenché un SOS",
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("notifications")
            .add(notification)
            .addOnSuccessListener {
                Log.d("SOSNotif", "Notification ajoutée pour $userId")
                showLocalNotification(userId)
            }
            .addOnFailureListener { e ->
                Log.e("SOSNotif", "Erreur d'ajout de la notification", e)
            }
    }

    private fun showLocalNotification(userId: String) {
        Log.d("NotifDebug", "Tentative d'affichage d'une notification pour $userId")

        val channelId = "sos_channel_id"
        val channelName = "Notifications SOS"

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications pour les alertes SOS"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logop)
            .setContentTitle("Alerte SOS")
            .setContentText("L'enfant $userId a déclenché un SOS")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(userId.hashCode(), builder.build())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("Permission", "Permission de notification accordée")
        }
    }
}
