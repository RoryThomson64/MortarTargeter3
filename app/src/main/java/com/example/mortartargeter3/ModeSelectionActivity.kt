package com.example.mortartargeter3

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ModeSelectionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mode_selection)

        val btnMortarMode: Button = findViewById(R.id.btnMortarMode)
        val btnFieldMode: Button = findViewById(R.id.btnFieldMode)

        btnMortarMode.setOnClickListener {
            // Launch the main Mortar Targeter activity.
            startActivity(Intent(this, MainActivity::class.java))
        }

        btnFieldMode.setOnClickListener {
            // Launch the field activity. (You need to create FieldActivity.)
            startActivity(Intent(this, FieldActivity::class.java))
        }
    }
}
