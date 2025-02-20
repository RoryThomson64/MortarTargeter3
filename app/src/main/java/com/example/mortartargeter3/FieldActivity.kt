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
    private lateinit var tvArtilleryCallout: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_field)

        val btnSelectTarget: Button = findViewById(R.id.btnSelectTargetOnMap)
        tvPlusCodeResult = findViewById(R.id.tvPlusCodeResult)
        tvArtilleryCallout = findViewById(R.id.tvArtilleryCallout)
        val btnDone: Button = findViewById(R.id.btnDone)

        btnSelectTarget.setOnClickListener {
            // Launch FieldMapPickerActivity for selecting a target.
            val intent = Intent(this, FieldMapPickerActivity::class.java)
            startActivityForResult(intent, MAP_PICK_REQUEST_CODE)
        }

        btnDone.setOnClickListener {
            // When "Done" is clicked, exit Field Mode.
            Toast.makeText(this, "Exiting Field Mode", Toast.LENGTH_SHORT).show()
            finish()
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
                // Display the Plus Code.
                tvPlusCodeResult.text = plusCode

                // Automatically generate the artillery call using the NATO phonetic alphabet.
                val natoCall = generateNatoCall(plusCode)
                tvArtilleryCallout.text = "Artillery Artillery:\n$natoCall \nConfirm"

                Toast.makeText(this, "Target: $lat, $lon\nPlus Code: $plusCode", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Error retrieving Plus Code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Converts the given Plus Code into a radio callout using the NATO phonetic alphabet.
     */
    private fun generateNatoCall(plusCode: String): String {
        val natoMap = mapOf(
            'A' to "Alpha",   'B' to "Bravo",   'C' to "Charlie", 'D' to "Delta",
            'E' to "Echo",    'F' to "Foxtrot", 'G' to "Golf",    'H' to "Hotel",
            'I' to "India",   'J' to "Juliet",  'K' to "Kilo",    'L' to "Lima",
            'M' to "Mike",    'N' to "November",'O' to "Oscar",   'P' to "Papa",
            'Q' to "Quebec",  'R' to "Romeo",   'S' to "Sierra",  'T' to "Tango",
            'U' to "Uniform", 'V' to "Victor",  'W' to "Whiskey", 'X' to "X-ray",
            'Y' to "Yankee",  'Z' to "Zulu",
            '0' to "Zero",    '1' to "One",     '2' to "Two",     '3' to "Three",
            '4' to "Four",    '5' to "Five",    '6' to "Six",     '7' to "Seven",
            '8' to "Eight",   '9' to "Niner",   '+' to "Plus"
        )
        return plusCode.toUpperCase().map { char ->
            natoMap[char] ?: char.toString()
        }.joinToString(" ")
    }
}
