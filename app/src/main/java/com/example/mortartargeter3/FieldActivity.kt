package com.example.mortartargeter3

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class FieldActivity : AppCompatActivity() {

    companion object {
        const val MAP_PICK_REQUEST_CODE = 3000 // Unique request code for map picker
    }

    private lateinit var tvPlusCodeResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_field)

        val btnSelectTarget: Button = findViewById(R.id.btnSelectTargetOnMap)
        tvPlusCodeResult = findViewById(R.id.tvPlusCodeResult)
        val btnDone: Button = findViewById(R.id.btnDone)

        btnSelectTarget.setOnClickListener {
            // Launch FieldMapPickerActivity for selecting a target.
            val intent = Intent(this, FieldMapPickerActivity::class.java)
            startActivityForResult(intent, MAP_PICK_REQUEST_CODE)
        }

        btnDone.setOnClickListener {
            // Keep the activity open or perform any final actions.
            Toast.makeText(this, "Field mode completed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MAP_PICK_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            // Retrieve the Plus Code from the returned intent.
            val plusCode = data.getStringExtra("plus_code")
            val lat = data.getDoubleExtra("selected_lat", 0.0)
            val lon = data.getDoubleExtra("selected_lon", 0.0)

            if (plusCode != null) {
                tvPlusCodeResult.text = "Plus Code: $plusCode"
                Toast.makeText(this, "Target: $lat, $lon\nPlus Code: $plusCode", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Error retrieving Plus Code", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
