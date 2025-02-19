package com.example.mortartargeter3

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var etFrontalCd: EditText
    private lateinit var etSideCd: EditText
    private lateinit var etFrontalArea: EditText
    private lateinit var etSideArea: EditText
    private lateinit var etAirDensity: EditText
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        etFrontalCd = findViewById(R.id.etFrontalCd)
        etSideCd = findViewById(R.id.etSideCd)
        etFrontalArea = findViewById(R.id.etFrontalArea)
        etSideArea = findViewById(R.id.etSideArea)
        etAirDensity = findViewById(R.id.etAirDensity)
        btnSave = findViewById(R.id.btnSaveSettings)

        // Load current settings.
        val prefs = getSharedPreferences("DragSettings", Context.MODE_PRIVATE)
        etFrontalCd.setText(prefs.getFloat("frontal_cd", 0.3f).toString())
        etSideCd.setText(prefs.getFloat("side_cd", 1.2f).toString())
        etFrontalArea.setText(prefs.getFloat("frontal_area", 0.01f).toString())
        etSideArea.setText(prefs.getFloat("side_area", 0.015f).toString())
        etAirDensity.setText(prefs.getFloat("air_density", 1.225f).toString())

        btnSave.setOnClickListener {
            val newFrontalCd = etFrontalCd.text.toString().toFloatOrNull() ?: 0.3f
            val newSideCd = etSideCd.text.toString().toFloatOrNull() ?: 1.2f
            val newFrontalArea = etFrontalArea.text.toString().toFloatOrNull() ?: 0.01f
            val newSideArea = etSideArea.text.toString().toFloatOrNull() ?: 0.015f
            val newAirDensity = etAirDensity.text.toString().toFloatOrNull() ?: 1.225f

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
    }
}
