package com.example.mortartargeter3

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class SettingsActivity : AppCompatActivity() {

    private lateinit var etFrontalCd: EditText
    private lateinit var etSideCd: EditText
    private lateinit var etFrontalArea: EditText
    private lateinit var etSideArea: EditText
    private lateinit var etAirDensity: EditText
    private lateinit var etShellWeight: EditText  // New field for shell weight

    // Preset UI elements.
    private lateinit var etPresetName: EditText
    private lateinit var btnSavePreset: Button
    private lateinit var btnLoadPreset: Button
    private lateinit var btnDeletePreset: Button

    // Active settings buttons
    private lateinit var btnSave: Button
    private lateinit var btnResetDefaults: Button
    private lateinit var btnCloseSettings: ImageButton

    // Default values for drag settings and shell weight.
    private val DEFAULT_FRONTAL_CD = 0.3f
    private val DEFAULT_SIDE_CD = 1.2f
    private val DEFAULT_FRONTAL_AREA = 0.01f
    private val DEFAULT_SIDE_AREA = 0.015f
    private val DEFAULT_AIR_DENSITY = 1.225f
    private val DEFAULT_SHELL_WEIGHT = 0.0f  // Default shell weight in grams

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Bind active settings UI elements.
        etFrontalCd = findViewById(R.id.etFrontalCd)
        etSideCd = findViewById(R.id.etSideCd)
        etFrontalArea = findViewById(R.id.etFrontalArea)
        etSideArea = findViewById(R.id.etSideArea)
        etAirDensity = findViewById(R.id.etAirDensity)
        etShellWeight = findViewById(R.id.etShellWeight)  // Bind the shell weight field

        // Bind preset UI elements.
        etPresetName = findViewById(R.id.etPresetName)
        btnSavePreset = findViewById(R.id.btnSavePreset)
        btnLoadPreset = findViewById(R.id.btnLoadPreset)
        btnDeletePreset = findViewById(R.id.btnDeletePreset)

        // Bind other buttons.
        btnSave = findViewById(R.id.btnSaveSettings)
        btnResetDefaults = findViewById(R.id.btnResetDefaults)
        btnCloseSettings = findViewById(R.id.btnCloseSettings)

        // Load current active settings.
        val prefs = getSharedPreferences("DragSettings", Context.MODE_PRIVATE)
        etFrontalCd.setText(prefs.getFloat("frontal_cd", DEFAULT_FRONTAL_CD).toString())
        etSideCd.setText(prefs.getFloat("side_cd", DEFAULT_SIDE_CD).toString())
        etFrontalArea.setText(prefs.getFloat("frontal_area", DEFAULT_FRONTAL_AREA).toString())
        etSideArea.setText(prefs.getFloat("side_area", DEFAULT_SIDE_AREA).toString())
        etAirDensity.setText(prefs.getFloat("air_density", DEFAULT_AIR_DENSITY).toString())
        etShellWeight.setText(prefs.getFloat("shell_weight", DEFAULT_SHELL_WEIGHT).toString())

        // Save active settings button.
        btnSave.setOnClickListener {
            val newFrontalCd = etFrontalCd.text.toString().toFloatOrNull() ?: DEFAULT_FRONTAL_CD
            val newSideCd = etSideCd.text.toString().toFloatOrNull() ?: DEFAULT_SIDE_CD
            val newFrontalArea = etFrontalArea.text.toString().toFloatOrNull() ?: DEFAULT_FRONTAL_AREA
            val newSideArea = etSideArea.text.toString().toFloatOrNull() ?: DEFAULT_SIDE_AREA
            val newAirDensity = etAirDensity.text.toString().toFloatOrNull() ?: DEFAULT_AIR_DENSITY
            val newShellWeight = etShellWeight.text.toString().toFloatOrNull() ?: DEFAULT_SHELL_WEIGHT

            prefs.edit().apply {
                putFloat("frontal_cd", newFrontalCd)
                putFloat("side_cd", newSideCd)
                putFloat("frontal_area", newFrontalArea)
                putFloat("side_area", newSideArea)
                putFloat("air_density", newAirDensity)
                putFloat("shell_weight", newShellWeight)  // Save shell weight
                apply()
            }
            Toast.makeText(this, "Active settings saved.", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Reset active settings to defaults.
        btnResetDefaults.setOnClickListener {
            prefs.edit().apply {
                putFloat("frontal_cd", DEFAULT_FRONTAL_CD)
                putFloat("side_cd", DEFAULT_SIDE_CD)
                putFloat("frontal_area", DEFAULT_FRONTAL_AREA)
                putFloat("side_area", DEFAULT_SIDE_AREA)
                putFloat("air_density", DEFAULT_AIR_DENSITY)
                putFloat("shell_weight", DEFAULT_SHELL_WEIGHT)
                apply()
            }
            etFrontalCd.setText(DEFAULT_FRONTAL_CD.toString())
            etSideCd.setText(DEFAULT_SIDE_CD.toString())
            etFrontalArea.setText(DEFAULT_FRONTAL_AREA.toString())
            etSideArea.setText(DEFAULT_SIDE_AREA.toString())
            etAirDensity.setText(DEFAULT_AIR_DENSITY.toString())
            etShellWeight.setText(DEFAULT_SHELL_WEIGHT.toString())
            Toast.makeText(this, "Settings reset to default", Toast.LENGTH_SHORT).show()
        }

        // Preset management (save/load/delete) remains unchanged, but include the shell weight value.
        btnSavePreset.setOnClickListener {
            val presetName = etPresetName.text.toString().trim()
            if (presetName.isEmpty()) {
                Toast.makeText(this, "Enter a preset name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val presetsPrefs = getSharedPreferences("SettingsPresets", Context.MODE_PRIVATE)
            val presetsStr = presetsPrefs.getString("presets", "{}")
            val presetsJson = JSONObject(presetsStr)
            if (presetsJson.has(presetName)) {
                AlertDialog.Builder(this)
                    .setTitle("Overwrite Preset?")
                    .setMessage("A preset with the name '$presetName' already exists. Overwrite it?")
                    .setPositiveButton("Yes") { _, _ ->
                        savePreset(presetName, presetsJson, presetsPrefs)
                    }
                    .setNegativeButton("No", null)
                    .show()
            } else {
                savePreset(presetName, presetsJson, presetsPrefs)
            }
        }

        btnLoadPreset.setOnClickListener {
            val presetsPrefs = getSharedPreferences("SettingsPresets", Context.MODE_PRIVATE)
            val presetsStr = presetsPrefs.getString("presets", "{}")
            val presetsJson = JSONObject(presetsStr)
            val presetNames = mutableListOf<String>()
            val keys = presetsJson.keys()
            while (keys.hasNext()) {
                presetNames.add(keys.next())
            }
            if (presetNames.isEmpty()) {
                Toast.makeText(this, "No presets saved.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AlertDialog.Builder(this)
                .setTitle("Select a Preset")
                .setItems(presetNames.toTypedArray()) { _, which ->
                    val selectedPreset = presetNames[which]
                    val presetObj = presetsJson.getJSONObject(selectedPreset)
                    etFrontalCd.setText(presetObj.getDouble("frontal_cd").toString())
                    etSideCd.setText(presetObj.getDouble("side_cd").toString())
                    etFrontalArea.setText(presetObj.getDouble("frontal_area").toString())
                    etSideArea.setText(presetObj.getDouble("side_area").toString())
                    etAirDensity.setText(presetObj.getDouble("air_density").toString())
                    if (presetObj.has("shell_weight")) {
                        etShellWeight.setText(presetObj.getDouble("shell_weight").toString())
                    } else {
                        etShellWeight.setText(DEFAULT_SHELL_WEIGHT.toString())
                    }
                    Toast.makeText(this, "Preset '$selectedPreset' loaded.", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        btnDeletePreset.setOnClickListener {
            val presetsPrefs = getSharedPreferences("SettingsPresets", Context.MODE_PRIVATE)
            val presetsStr = presetsPrefs.getString("presets", "{}")
            val presetsJson = JSONObject(presetsStr)
            val presetNames = mutableListOf<String>()
            val keys = presetsJson.keys()
            while (keys.hasNext()) {
                presetNames.add(keys.next())
            }
            if (presetNames.isEmpty()) {
                Toast.makeText(this, "No presets saved.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AlertDialog.Builder(this)
                .setTitle("Select a Preset to Delete")
                .setItems(presetNames.toTypedArray()) { _, which ->
                    val selectedPreset = presetNames[which]
                    AlertDialog.Builder(this)
                        .setTitle("Delete Preset")
                        .setMessage("Are you sure you want to delete the preset '$selectedPreset'?")
                        .setPositiveButton("Yes") { _, _ ->
                            presetsJson.remove(selectedPreset)
                            presetsPrefs.edit().putString("presets", presetsJson.toString()).apply()
                            Toast.makeText(this, "Preset '$selectedPreset' deleted.", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("No", null)
                        .show()
                }
                .show()
        }

        btnCloseSettings.setOnClickListener {
            finish()
        }
    }

    private fun savePreset(presetName: String, presetsJson: JSONObject, presetsPrefs: android.content.SharedPreferences) {
        val presetObj = JSONObject().apply {
            put("frontal_cd", etFrontalCd.text.toString().toFloatOrNull() ?: DEFAULT_FRONTAL_CD)
            put("side_cd", etSideCd.text.toString().toFloatOrNull() ?: DEFAULT_SIDE_CD)
            put("frontal_area", etFrontalArea.text.toString().toFloatOrNull() ?: DEFAULT_FRONTAL_AREA)
            put("side_area", etSideArea.text.toString().toFloatOrNull() ?: DEFAULT_SIDE_AREA)
            put("air_density", etAirDensity.text.toString().toFloatOrNull() ?: DEFAULT_AIR_DENSITY)
            put("shell_weight", etShellWeight.text.toString().toFloatOrNull() ?: DEFAULT_SHELL_WEIGHT)
        }
        presetsJson.put(presetName, presetObj)
        presetsPrefs.edit().putString("presets", presetsJson.toString()).apply()
        Toast.makeText(this, "Preset '$presetName' saved.", Toast.LENGTH_SHORT).show()
    }
}
