package com.example.kidguard

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MessagesActivity : AppCompatActivity() {

    private lateinit var conversationRecyclerView: RecyclerView
    private lateinit var btnAddMessage: ImageButton
    private lateinit var adapter: ConversationAdapter
    private val conversations = mutableListOf<ConversationData>()
    private lateinit var userTextView: TextView
    private lateinit var db: FirebaseFirestore
    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        conversationRecyclerView = findViewById(R.id.conversationRecyclerView)
        btnAddMessage = findViewById(R.id.btnAddMessage)
        userTextView = findViewById(R.id.titleTextView)

        val username = intent.getStringExtra("username")
        userTextView.text = "Messages de $username"

        db = FirebaseFirestore.getInstance()
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        adapter = ConversationAdapter(
            conversationList = conversations,
            onClick = { contactName ->
                val conversation = conversations.find { it.contactName == contactName }
                conversation?.let {
                    getOrCreateConversation(currentUserId, it.contactUserId) { conversationId ->
                        val intent = Intent(this, ChatActivity::class.java)
                        intent.putExtra("conversationId", conversationId)
                        intent.putExtra("contactName", it.contactName)
                        startActivity(intent)
                    }
                }
            },
            onLongClick = { position ->
                val conversation = conversations[position]
                AlertDialog.Builder(this@MessagesActivity)
                    .setTitle("Supprimer la conversation")
                    .setMessage("Voulez-vous vraiment supprimer la conversation avec ${conversation.contactName} ?")
                    .setPositiveButton("Oui") { _, _ ->
                        db.collection("users").document(currentUserId)
                            .collection("conversations").document(conversation.id)
                            .delete()
                            .addOnSuccessListener {
                                conversations.removeAt(position)
                                adapter.notifyItemRemoved(position)
                            }
                    }
                    .setNegativeButton("Non", null)
                    .show()
            }
        )

        conversationRecyclerView.layoutManager = LinearLayoutManager(this)
        conversationRecyclerView.adapter = adapter

        btnAddMessage.setOnClickListener {
            showAddContactDialog()
        }

        listenToUserConversations()
    }

    private fun listenToUserConversations() {
        db.collection("users").document(currentUserId)
            .collection("conversations")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                conversations.clear()
                for (doc in snapshot.documents) {
                    val conversationId = doc.id
                    val contactName = doc.getString("contactName") ?: continue
                    val contactUserId = doc.getString("contactUserId") ?: continue
                    val lastMessage = doc.getString("lastMessage") ?: ""
                    val lastTimestamp = doc.getLong("lastTimestamp") ?: 0L

                    conversations.add(
                        ConversationData(
                            id = conversationId,
                            contactName = contactName,
                            contactUserId = contactUserId,
                            lastMessage = lastMessage,
                            lastTimestamp = lastTimestamp
                        )
                    )
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun showAddContactDialog() {
        val input = EditText(this)
        input.hint = "Nom du contact exact"

        AlertDialog.Builder(this@MessagesActivity)
            .setTitle("Ajouter un contact")
            .setView(input)
            .setPositiveButton("Ajouter") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    searchUserByName(name)
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun searchUserByName(name: String) {
        db.collection("users")
            .whereEqualTo("username", name)
            .get()
            .addOnSuccessListener { documents ->
                val doc = documents.firstOrNull()
                if (doc != null) {
                    val userId = doc.id
                    if (userId == currentUserId) {
                        AlertDialog.Builder(this@MessagesActivity)
                            .setTitle("Erreur")
                            .setMessage("Vous ne pouvez pas vous ajouter vous-même.")
                            .setPositiveButton("OK", null)
                            .show()
                        return@addOnSuccessListener
                    }
                    getOrCreateConversation(currentUserId, userId) {}
                } else {
                    AlertDialog.Builder(this@MessagesActivity)
                        .setTitle("Erreur")
                        .setMessage("Aucun utilisateur trouvé avec ce nom d'utilisateur.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
            .addOnFailureListener { exception ->
                AlertDialog.Builder(this@MessagesActivity)
                    .setTitle("Erreur")
                    .setMessage("Erreur lors de la recherche de l'utilisateur : ${exception.localizedMessage}")
                    .setPositiveButton("OK", null)
                    .show()
            }
    }

    private fun getOrCreateConversation(currentUserId: String, otherUserId: String, onComplete: (String) -> Unit) {
        val conversationsRef = db.collection("conversations")

        conversationsRef
            .whereArrayContains("participants", currentUserId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val existingConversationId = querySnapshot.documents.firstOrNull { doc ->
                    val participants = doc.get("participants") as? List<*>
                    participants?.size == 2 && participants.contains(otherUserId)
                }?.id

                if (existingConversationId != null) {
                    addConversationReferenceForUsers(existingConversationId, currentUserId, otherUserId, onComplete)
                } else {
                    val newConversation = mapOf(
                        "participants" to listOf(currentUserId, otherUserId),
                        "createdAt" to System.currentTimeMillis()
                    )
                    conversationsRef.add(newConversation)
                        .addOnSuccessListener { docRef ->
                            addConversationReferenceForUsers(docRef.id, currentUserId, otherUserId, onComplete)
                        }
                        .addOnFailureListener {
                            AlertDialog.Builder(this@MessagesActivity)
                                .setTitle("Erreur")
                                .setMessage("Impossible de créer une conversation.")
                                .setPositiveButton("OK", null)
                                .show()
                        }
                }
            }
            .addOnFailureListener {
                AlertDialog.Builder(this@MessagesActivity)
                    .setTitle("Erreur")
                    .setMessage("Erreur lors de la récupération des conversations.")
                    .setPositiveButton("OK", null)
                    .show()
            }
    }

    private fun addConversationReferenceForUsers(
        conversationId: String,
        currentUserId: String,
        otherUserId: String,
        onComplete: (String) -> Unit
    ) {
        db.collection("users").document(otherUserId).get()
            .addOnSuccessListener { otherUserDoc ->
                val otherUserName = otherUserDoc.getString("name") ?: "Inconnu"

                val currentUserConvRef = db.collection("users").document(currentUserId)
                    .collection("conversations").document(conversationId)

                currentUserConvRef.set(
                    mapOf(
                        "contactName" to otherUserName,
                        "contactUserId" to otherUserId,
                        "lastMessage" to "",
                        "lastTimestamp" to 0L
                    )
                )

                db.collection("users").document(currentUserId).get()
                    .addOnSuccessListener { currentUserDoc ->
                        val currentUserName = currentUserDoc.getString("name") ?: "Inconnu"

                        val otherUserConvRef = db.collection("users").document(otherUserId)
                            .collection("conversations").document(conversationId)

                        otherUserConvRef.set(
                            mapOf(
                                "contactName" to currentUserName,
                                "contactUserId" to currentUserId,
                                "lastMessage" to "",
                                "lastTimestamp" to 0L
                            )
                        ).addOnCompleteListener {
                            onComplete(conversationId)
                        }
                    }
            }
    }
}

// ConversationData.kt
