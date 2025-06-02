package com.example.kidguard

import android.widget.TextView
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity



class ChildDetailActivity : AppCompatActivity() {
    @SuppressLint("ClickableViewAccessibility", "SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_child_detail)

        val name = intent.getStringExtra("child_name")

        val nameTextView = findViewById<TextView>(R.id.childNameTextView)
        nameTextView.text = " $name "
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 10f
            setColor(Color.parseColor("#FFFFFF"))
        }

        fun setupButton(button: Button, iconResId: Int, x: Int,y:Int) {
            button.background = drawable
            button.backgroundTintList = null
            button.elevation = 10f

            val logo = resources.getDrawable(iconResId, theme)
            logo.setBounds(-20, 0, x, y)
            button.setCompoundDrawables(logo, null, null, null)
            button.setCompoundDrawablePadding(-130)

            button.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        button.background = drawable
                        button.elevation = 12f
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        button.background = drawable
                        button.elevation = 8f
                    }
                }
                false
            }
        }

        setupButton(findViewById(R.id.AgendaButton),R.drawable.agenda,120,140)
        setupButton(findViewById(R.id.GeoButton), R.drawable.geol,120,120)
        setupButton(findViewById(R.id.ZonesButton), R.drawable.zones,130,150)
        setupButton(findViewById(R.id.MsgButton), R.drawable.msgs,150,170)
        setupButton(findViewById(R.id.TCButton), R.drawable.tc,130,140)
        setupButton(findViewById(R.id.ModeEButton), R.drawable.ecoute,150,150)
        val geoButton = findViewById<Button>(R.id.GeoButton)
            geoButton.setOnClickListener {
                val intent = Intent(this, GeolocalisationActivity::class.java)
                intent.putExtra("username", name)
                startActivity(intent)
            }
        val SzButton = findViewById<Button>(R.id.ZonesButton)
        SzButton.setOnClickListener {
            val intent = Intent(this, SecurityzoneActivity::class.java)
            intent.putExtra("username", name)
            startActivity(intent)
        }
        val agendaButton = findViewById<Button>(R.id.AgendaButton)
        agendaButton.setOnClickListener {
            val intent = Intent(this, AgendaActivity::class.java)
            intent.putExtra("username", name)
            startActivity(intent)
        }
        val tempsecranButton= findViewById<Button>(R.id.TCButton)
        tempsecranButton.setOnClickListener {
            val intent = Intent(this, TempsEcranActivity::class.java)
            intent.putExtra("username", name)
            startActivity(intent)
        }

        val EcouteButton= findViewById<Button>(R.id.ModeEButton)
        EcouteButton.setOnClickListener {
        val intent = Intent(this, EcouteActivity::class.java)
        intent.putExtra("username", name)
        startActivity(intent)
         }
        val SuspectMButton= findViewById<Button>(R.id.MsgButton)
        SuspectMButton.setOnClickListener {
            val intent = Intent(this, SuspectMessageActivity::class.java)
            intent.putExtra("username", name)
            startActivity(intent)
        }
        val NotifIcon = findViewById<ImageView>(R.id.logo)
        NotifIcon .setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }


}





}






