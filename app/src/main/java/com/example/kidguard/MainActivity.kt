package com.example.kidguard

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class MainActivity : AppCompatActivity() {

    @SuppressLint("ClickableViewAccessibility", "ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.choose_activity)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 10f
            setColor(Color.parseColor("#FFFFFF"))
        }
        val ParentButton = findViewById<Button>(R.id.parentButton)
        ParentButton.background = drawable
        ParentButton.backgroundTintList = null
        ParentButton.elevation = 10f
        ParentButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> ParentButton.elevation = 12f
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> ParentButton.elevation = 8f
            }
            false
        }

        val parentLogo = resources.getDrawable(R.drawable.parent, theme)
        parentLogo.setBounds(-10, 0, 150, 150)
        ParentButton.setCompoundDrawables(parentLogo, null, null, null)
        ParentButton.setCompoundDrawablePadding(-130)

        ParentButton.setOnClickListener {
            val intent = Intent(this, AuthentificationParentActivity::class.java)
            startActivity(intent)
        }

        val kidButton = findViewById<Button>(R.id.kidButton)
        kidButton.background = drawable
        kidButton.backgroundTintList = null
        kidButton.elevation = 10f

        kidButton.setOnTouchListener { _, event ->

            when (event.action) {

                MotionEvent.ACTION_DOWN -> kidButton.elevation = 12f
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> kidButton.elevation = 8f

            }

            false
        }

        val kidLogo = resources.getDrawable(R.drawable.kids, theme)
        kidLogo.setBounds(-20, 0, 180, 150)
        kidButton.setCompoundDrawables(kidLogo, null, null, null)
        kidButton.setCompoundDrawablePadding(-130)

        kidButton.setOnClickListener {
            val intent = Intent(this, AuthentificationEnfantActivity::class.java)
            startActivity(intent)
        }


    }
}


