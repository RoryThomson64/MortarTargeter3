package com.example.mortartargeter3

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var etFrontalCd: EditText
    private lateinit var etSideCd: EditText
    private lateinit var etFrontalArea: EditText
    private lateinit var etSideArea: EditText
    private lateinit var etAirDensity: EditText
    private lateinit var btnSave: Button
    private lateinit var btnResetDefaults: Button

    // Default values for a Nerf Vortex (or your chosen defaults)
    private val DEFAULT_FRONTAL_CD = 0.3f
    private val DEFAULT_SIDE_CD = 1.2f
    private val DEFAULT_FRONTAL_AREA = 0.01f
    private val DEFAULT_SIDE_AREA = 0.015f
    private val DEFAULT_AIR_DENSITY = 1.225f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        etFrontalCd = findViewById(R.id.etFrontalCd)
        etSideCd = findViewById(R.id.etSideCd)
        etFrontalArea = findViewById(R.id.etFrontalArea)
        etSideArea = findViewById(R.id.etSideArea)
        etAirDensity = findViewById(R.id.etAirDensity)
        btnSave = findViewById(R.id.btnSaveSettings)
        btnResetDefaults = findViewById(R.id.btnResetDefaults)

        // Load current settings.
        val prefs = getSharedPreferences("DragSettings", Context.MODE_PRIVATE)
        etFrontalCd.setText(prefs.getFloat("frontal_cd", DEFAULT_FRONTAL_CD).toString())
        etSideCd.setText(prefs.getFloat("side_cd", DEFAULT_SIDE_CD).toString())
        etFrontalArea.setText(prefs.getFloat("frontal_area", DEFAULT_FRONTAL_AREA).toString())
        etSideArea.setText(prefs.getFloat("side_area", DEFAULT_SIDE_AREA).toString())
        etAirDensity.setText(prefs.getFloat("air_density", DEFAULT_AIR_DENSITY).toString())

        btnSave.setOnClickListener {
            val newFrontalCd = etFrontalCd.text.toString().toFloatOrNull() ?: DEFAULT_FRONTAL_CD
            val newSideCd = etSideCd.text.toString().toFloatOrNull() ?: DEFAULT_SIDE_CD
            val newFrontalArea = etFrontalArea.text.toString().toFloatOrNull() ?: DEFAULT_FRONTAL_AREA
            val newSideArea = etSideArea.text.toString().toFloatOrNull() ?: DEFAULT_SIDE_AREA
            val newAirDensity = etAirDensity.text.toString().toFloatOrNull() ?: DEFAULT_AIR_DENSITY

            prefs.edit().apply {
                putFloat("frontal_cd", newFrontalCd)
                putFloat("side_cd", newSideCd)
                putFloat("frontal_area", newFrontalArea)
                putFloat("side_area", newSideArea)
                putFloat("air_density", newAirDensity)
                apply()
            }
            finish()
        }

        btnResetDefaults.setOnClickListener {
            // Reset settings to default values.
            prefs.edit().apply {
                putFloat("frontal_cd", DEFAULT_FRONTAL_CD)
                putFloat("side_cd", DEFAULT_SIDE_CD)
                putFloat("frontal_area", DEFAULT_FRONTAL_AREA)
                putFloat("side_area", DEFAULT_SIDE_AREA)
                putFloat("air_density", DEFAULT_AIR_DENSITY)
                apply()
            }
            // Update the EditText fields with default values.
            etFrontalCd.setText(DEFAULT_FRONTAL_CD.toString())
            etSideCd.setText(DEFAULT_SIDE_CD.toString())
            etFrontalArea.setText(DEFAULT_FRONTAL_AREA.toString())
            etSideArea.setText(DEFAULT_SIDE_AREA.toString())
            etAirDensity.setText(DEFAULT_AIR_DENSITY.toString())
            Toast.makeText(this, "Settings reset to default", Toast.LENGTH_SHORT).show()
        }
    }
}
