package com.example.kidguard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class SuspectMessageActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SuspectMessageAdapter
    private val suspectMessages = mutableListOf<SuspectMessage>()
    private val suspectWords = listOf("frapper", "béte", "stupide")
    private val db = FirebaseFirestore.getInstance()
    private lateinit var perspectiveClient: PerspectiveClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_suspect_message)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = SuspectMessageAdapter(suspectMessages)
        recyclerView.adapter = adapter

        perspectiveClient = PerspectiveClient("Your_API_KEY")

        val username = intent.getStringExtra("username")?.trim()
        if (username.isNullOrEmpty()) {
            Toast.makeText(this, "Aucun nom d'utilisateur fourni", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val userId = snapshot.documents[0].getString("uid") ?: ""
                    loadConversationsForUser(userId)
                } else {
                    Toast.makeText(this, "Utilisateur non trouvé", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erreur lors de la récupération de l'utilisateur", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun loadConversationsForUser(userId: String) {
        db.collection("conversations")
            .whereArrayContains("participants", userId)
            .get()
            .addOnSuccessListener { conversations ->
                for (conv in conversations.documents) {
                    val conversationId = conv.id

                    db.collection("conversations")
                        .document(conversationId)
                        .collection("messages")
                        .get()
                        .addOnSuccessListener { messages ->
                            for (msg in messages.documents) {
                                val text = msg.getString("text") ?: ""
                                val sender = msg.getString("senderId") ?: ""

                                isSuspectCombined(text) { isSuspect ->
                                    if (isSuspect) {
                                        db.collection("users")
                                            .whereEqualTo("uid", sender)
                                            .get()
                                            .addOnSuccessListener { userSnapshot ->
                                                val senderName = if (!userSnapshot.isEmpty) {
                                                    userSnapshot.documents[0].getString("username") ?: sender
                                                } else {
                                                    sender
                                                }

                                                runOnUiThread {
                                                    suspectMessages.add(SuspectMessage(senderName, text))
                                                    adapter.notifyDataSetChanged()
                                                    saveNotificationToFirestore(userId)
                                                }
                                            }
                                            .addOnFailureListener {
                                                runOnUiThread {
                                                    suspectMessages.add(SuspectMessage(sender, text))
                                                    adapter.notifyDataSetChanged()
                                                    saveNotificationToFirestore(userId)
                                                }
                                            }
                                    }
                                }
                            }
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erreur lors du chargement des conversations", Toast.LENGTH_SHORT).show()
            }
    }

    private fun isSuspectCombined(text: String, callback: (Boolean) -> Unit) {

        if (suspectWords.any { text.contains(it, ignoreCase = true) }) {
            callback(true)
        } else {
            perspectiveClient.analyzeText(text, callback)
        }
    }

    private fun saveNotificationToFirestore(userId: String) {
        val notification = mapOf(
            "userId" to userId,
            "message" to "L'enfant a un message suspect",
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("notifications")
            .add(notification)
            .addOnSuccessListener {
                showLocalNotification(userId)
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
                description = "Notifications pour Message Suspect"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logop)
            .setContentTitle("Alerte Messages")
            .setContentText("Un enfant a un message suspect")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(userId.hashCode(), builder.build())
    }
}
