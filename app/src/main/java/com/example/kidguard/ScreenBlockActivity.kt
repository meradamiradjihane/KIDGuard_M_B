package com.example.kidguard

import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ScreenBlockActivity : AppCompatActivity() {

    private val codeParental = "1234"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

          window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setFinishOnTouchOutside(false)

        setContentView(R.layout.activity_screen_block)

        val editTextPin = findViewById<EditText>(R.id.editTextPin)
        val buttonUnlock = findViewById<Button>(R.id.buttonUnlock)

        buttonUnlock.setOnClickListener {
            val input = editTextPin.text.toString()
            if (input == codeParental) {
                finish() // Débloque l’écran
            } else {
                Toast.makeText(this, "Code incorrect", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {
        // Empêche le bouton retour de fermer l’activité
    }
}
