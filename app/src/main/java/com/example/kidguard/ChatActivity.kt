package com.example.kidguard

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatActivity : AppCompatActivity() {
    private lateinit var chatLayout: LinearLayout
    private lateinit var inputEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var contactNameTextView: TextView

    private lateinit var db: FirebaseFirestore
    private lateinit var currentUserId: String
    private var contactId: String = ""
    private lateinit var conversationId: String
    private lateinit var contactName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatLayout = findViewById(R.id.chatLayout)
        inputEditText = findViewById(R.id.inputEditText)
        sendButton = findViewById(R.id.sendButton)
        contactNameTextView = findViewById(R.id.contactName)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        conversationId = intent.getStringExtra("conversationId") ?: return
        contactName = intent.getStringExtra("contactName") ?: "Contact"

        contactNameTextView.text = contactName
        db = FirebaseFirestore.getInstance()

        fetchContactIdFromConversation()

        listenForMessages()

        sendButton.setOnClickListener {
            val messageText = inputEditText.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                inputEditText.text.clear()
            }
        }
    }

    private fun fetchContactIdFromConversation() {
        db.collection("users").document(currentUserId)
            .collection("conversations").document(conversationId)
            .get()
            .addOnSuccessListener { document ->
                contactId = document.getString("contactUserId") ?: ""
            }
    }

    private fun sendMessage(text: String) {
        val timestamp = System.currentTimeMillis()
        val message = hashMapOf(
            "senderId" to currentUserId,
            "text" to text,
            "timestamp" to timestamp
        )

        db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener {
                updateLastMessage(text, timestamp)
            }
    }

    private fun updateLastMessage(text: String, timestamp: Long) {
        val updates = mapOf(
            "lastMessage" to text,
            "lastTimestamp" to timestamp
        )

        db.collection("users").document(currentUserId)
            .collection("conversations").document(conversationId)
            .update(updates)

        if (contactId.isNotEmpty()) {
            db.collection("users").document(contactId)
                .collection("conversations").document(conversationId)
                .update(updates)
        }
    }

    private fun listenForMessages() {
        db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, _ ->
                chatLayout.removeAllViews()
                for (doc in snapshots ?: return@addSnapshotListener) {
                    val text = doc.getString("text") ?: ""
                    val senderId = doc.getString("senderId") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    val timeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

                    if (senderId == currentUserId) {
                        addSentMessage(text, timeFormatted)
                    } else {
                        addReceivedMessage(text, timeFormatted)
                    }
                }
            }
    }

    private fun addSentMessage(message: String, time: String) {
        val messageView = layoutInflater.inflate(R.layout.item_message_sent, chatLayout, false)
        messageView.findViewById<TextView>(R.id.sentMessageTextView).text = message
        messageView.findViewById<TextView?>(R.id.sentTimeTextView)?.text = time
        chatLayout.addView(messageView)
    }

    private fun addReceivedMessage(message: String, time: String) {
        val messageView = layoutInflater.inflate(R.layout.item_message_received, chatLayout, false)
        messageView.findViewById<TextView>(R.id.receivedMessageTextView).text = message
        messageView.findViewById<TextView?>(R.id.receivedTimeTextView)?.text = time
        chatLayout.addView(messageView)
    }


}
