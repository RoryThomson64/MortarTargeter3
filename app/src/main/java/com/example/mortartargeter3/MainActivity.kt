package com.example.mortartargeter3

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.MapsInitializer.Renderer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import com.google.android.gms.maps.model.LatLng
import okhttp3.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.IOException
import kotlin.math.*
import com.google.openlocationcode.OpenLocationCode



// Top-level SimulationResult for unambiguous access.
data class SimulationResult(
    val impactX: Double,
    val impactY: Double,
    val time: Double,
    val maxZ: Double
)

// Data class for drag settings.
data class DragSettings(
    val frontalCd: Double,
    val sideCd: Double,
    val frontalArea: Double,
    val sideArea: Double,
    val airDensity: Double
)

// Data class for a fired shot.
data class Shot(
    val id: Long,
    val impactLat: Double,
    val impactLon: Double,
    val timestamp: Long
)

class MainActivity : AppCompatActivity(), OnMapsSdkInitializedCallback {

    // UI elements.
    private lateinit var tvCurrentLocation: TextView
    private lateinit var etWindSpeed: EditText
    private lateinit var etWindDirection: EditText
    private lateinit var etShellWeight: EditText
    private lateinit var etMuzzleVelocity: EditText
    private lateinit var tvHeightDifferenceLabel: TextView
    private lateinit var seekBarHeightDiff: SeekBar
    private lateinit var rbAuto: RadioButton
    private lateinit var rbManual: RadioButton
    private lateinit var rbPlusCode: RadioButton
    private lateinit var layoutAutoTargeting: LinearLayout
    private lateinit var layoutManualTargeting: LinearLayout
    private lateinit var layoutPlusCodeTargeting: LinearLayout
    private lateinit var etTargetLat: EditText
    private lateinit var etTargetLon: EditText
    private lateinit var etManualDistance: EditText
    private lateinit var etManualBearing: EditText
    private lateinit var etManualOriginLat: EditText
    private lateinit var etManualOriginLon: EditText
    private lateinit var etPlusCode: EditText
    private lateinit var btnCalculate: Button
    private lateinit var btnOpenMap: Button
    private lateinit var tvResult: TextView
    private lateinit var btnRefreshLocation: Button
    private lateinit var btnMaxRange: Button
    private lateinit var tvMaxRange: TextView
    private lateinit var btnSettings: Button
    private lateinit var btnHelp: ImageButton
    private lateinit var btnConfirmFire: Button
    private lateinit var btnViewShots: Button

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Location is stored as LatLng.
    private var currentLocation: LatLng? = null

    // The most recent calculated shot.
    private var lastShot: Shot? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
        private const val MAP_PICK_REQUEST_CODE = 2000
        private const val TAG = "MortarTargeter"
        private const val GRAVITY = 9.81  // m/s²
        private const val EARTH_RADIUS = 6371000.0  // meters
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data);
        if ( resultCode == Activity.RESULT_OK) {
            data?.let {

                val selectedLat = it.getDoubleExtra("selected_lat", 0.0);
                val selectedLon = it.getDoubleExtra("selected_lon", 0.0);

                val mortarLat = it.getDoubleExtra("mortar_lat", currentLocation?.latitude?:0.0);
                val mortarLon = it.getDoubleExtra("mortar_lon", currentLocation?.longitude?:0.0);


                etTargetLat.setText(selectedLat.toString());
                etTargetLon.setText(selectedLon.toString());

                currentLocation = LatLng(mortarLat,mortarLon);
                tvCurrentLocation.text = "Current Location: ${currentLocation!!.latitude.toString()}, ${currentLocation!!.longitude.toString()}"
            }
        }
    }
    // Helper: load drag settings from SharedPreferences.
    private fun loadDragSettings(): DragSettings {
        val prefs = getSharedPreferences("DragSettings", MODE_PRIVATE)
        val frontalCd = prefs.getFloat("frontal_cd", 0.3f).toDouble()
        val sideCd = prefs.getFloat("side_cd", 1.2f).toDouble()
        val frontalArea = prefs.getFloat("frontal_area", 0.01f).toDouble()
        val sideArea = prefs.getFloat("side_area", 0.015f).toDouble()
        val airDensity = prefs.getFloat("air_density", 1.225f).toDouble()
        return DragSettings(frontalCd, sideCd, frontalArea, sideArea, airDensity)
    }

    // (Optional) Get API key method remains here for other purposes.
    fun getApiKey(context: Context): String {
        return try {
            val inputStream = context.assets.open("apikey.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.readLine().trim()
        } catch (e: Exception) {
            e.printStackTrace()
            "API_KEY_NOT_FOUND"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MapsInitializer.initialize(this, Renderer.LATEST, this)

        // Bind UI elements.
        tvCurrentLocation = findViewById(R.id.tvCurrentLocation)
        etWindSpeed = findViewById(R.id.etWindSpeed)
        etWindDirection = findViewById(R.id.etWindDirection)
        etShellWeight = findViewById(R.id.etShellWeight)
        etMuzzleVelocity = findViewById(R.id.etMuzzleVelocity)
        tvHeightDifferenceLabel = findViewById(R.id.tvHeightDifferenceLabel)
        seekBarHeightDiff = findViewById(R.id.seekBarHeightDiff)
        rbAuto = findViewById(R.id.rbAuto)
        rbManual = findViewById(R.id.rbManual)
        rbPlusCode = findViewById(R.id.rbPlusCode)
        layoutAutoTargeting = findViewById(R.id.layoutAutoTargeting)
        layoutManualTargeting = findViewById(R.id.layoutManualTargeting)
        layoutPlusCodeTargeting = findViewById(R.id.layoutPlusCodeTargeting)
        etTargetLat = findViewById(R.id.etTargetLat)
        etTargetLon = findViewById(R.id.etTargetLon)
        etManualDistance = findViewById(R.id.etManualDistance)
        etManualBearing = findViewById(R.id.etManualBearing)
        etManualOriginLat = findViewById(R.id.etManualOriginLat)
        etManualOriginLon = findViewById(R.id.etManualOriginLon)
        etPlusCode = findViewById(R.id.etPlusCode)
        btnCalculate = findViewById(R.id.btnCalculate)
        btnOpenMap = findViewById(R.id.btnOpenMap)
        tvResult = findViewById(R.id.tvResult)
        tvResult.movementMethod = ScrollingMovementMethod.getInstance()
        btnRefreshLocation = findViewById(R.id.btnRefreshLocation)
        btnMaxRange = findViewById(R.id.btnMaxRange)
        tvMaxRange = findViewById(R.id.tvMaxRange)
        btnSettings = findViewById(R.id.btnSettings)
        btnHelp = findViewById(R.id.btnHelp)
        btnConfirmFire = findViewById(R.id.btnConfirmFire)
        btnViewShots = findViewById(R.id.btnViewShots)

        // Set up the radio group listener to toggle targeting layouts.
        val rgTargetingMode = findViewById<RadioGroup>(R.id.rgTargetingMode)
        rgTargetingMode.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbAuto -> {
                    layoutAutoTargeting.visibility = View.VISIBLE
                    layoutManualTargeting.visibility = View.GONE
                    layoutPlusCodeTargeting.visibility = View.GONE
                }
                R.id.rbManual -> {
                    layoutAutoTargeting.visibility = View.GONE
                    layoutManualTargeting.visibility = View.VISIBLE
                    layoutPlusCodeTargeting.visibility = View.GONE
                }
                R.id.rbPlusCode -> {
                    layoutAutoTargeting.visibility = View.GONE
                    layoutManualTargeting.visibility = View.GONE
                    layoutPlusCodeTargeting.visibility = View.VISIBLE
                }
            }
        }

        // Set Help button listener.
        btnHelp.setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestLocationPermission()

        // SeekBar listener (maps progress to height difference)
        seekBarHeightDiff.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Here progress (0-100) is used directly; adjust as needed.
                tvHeightDifferenceLabel.text = "Height Difference (m): $progress"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) { }
            override fun onStopTrackingTouch(seekBar: SeekBar?) { }
        })

        btnCalculate.setOnClickListener {
            calculateFiringSolution()
            btnConfirmFire.visibility = View.VISIBLE
        }

        btnOpenMap.setOnClickListener {
            val intent = Intent(this, MapPickerActivity::class.java)
            currentLocation?.let { location ->
                intent.putExtra("current_lat", location.latitude)
                intent.putExtra("current_lon", location.longitude)
            }
            startActivityForResult(intent, MAP_PICK_REQUEST_CODE)
        }
        btnRefreshLocation.setOnClickListener { getLastLocation() }
        btnMaxRange.setOnClickListener {
            val shellWeightGrams = etShellWeight.text.toString().toDoubleOrNull()
            val muzzleVelocity = etMuzzleVelocity.text.toString().toDoubleOrNull()
            if (shellWeightGrams == null || muzzleVelocity == null) {
                Toast.makeText(this, "Enter valid shell weight and muzzle velocity", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val mass = shellWeightGrams / 1000.0
            val settings = loadDragSettings()
            val maxRange = calculateMaxRange(muzzleVelocity, mass, settings)
            tvMaxRange.text = "Maximum Range: ${"%.1f".format(maxRange)} m"
        }
        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnConfirmFire.setOnClickListener {
            if (rbAuto.isChecked) {
                val targetLat = etTargetLat.text.toString().toDoubleOrNull()
                val targetLon = etTargetLon.text.toString().toDoubleOrNull()
                if (targetLat == null || targetLon == null) {
                    Toast.makeText(this, "Target coordinates missing.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                lastShot = Shot(
                    id = System.currentTimeMillis(),
                    impactLat = targetLat,
                    impactLon = targetLon,
                    timestamp = System.currentTimeMillis()
                )
            } else {
                if (lastShot == null) {
                    Toast.makeText(this, "No firing solution available.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
            ShotsManager.addShot(this, lastShot!!)
            Toast.makeText(this, "Shot confirmed and saved.", Toast.LENGTH_SHORT).show()
            btnConfirmFire.visibility = View.GONE
        }

        btnViewShots.setOnClickListener {
            startActivity(Intent(this, ShotsListActivity::class.java))
        }
    }

    private fun calculateMaxRange(muzzleVelocity: Double, mass: Double, settings: DragSettings): Double {
        var bestRange = 0.0
        var bestElevation = 0.0
        val step = 0.01  // Radian increment.
        var angle = 0.1
        // Use a small negative targetHeight to force detection of ground impact.
        val simulatedTargetHeight = -0.001
        while (angle < (Math.PI / 2 - 0.01)) {
            val simResult = simulateProjectile(
                muzzleVelocity,
                angle,
                launchBearing = 0.0,  // Firing due north.
                wind_vx = 0.0,
                wind_vy = 0.0,
                mass,
                settings.frontalCd,
                settings.sideCd,
                settings.frontalArea,
                settings.sideArea,
                settings.airDensity,
                targetHeight = simulatedTargetHeight
            )
            val range = sqrt(simResult.impactX * simResult.impactX + simResult.impactY * simResult.impactY)
            if (range > bestRange) {
                bestRange = range
                bestElevation = angle
            }
            angle += step
        }
        Log.d(TAG, "Max range achieved at elevation: ${Math.toDegrees(bestElevation)}°")
        return bestRange
    }

    // --- Plus Code Decoding ---
    // Decodes a Plus Code string into latitude/longitude coordinates.
    private fun decodePlusCode(plusCode: String): Pair<Double, Double>? {
        return try {
            val codeArea = OpenLocationCode.decode(plusCode)
            Pair(codeArea.centerLatitude, codeArea.centerLongitude)
        } catch (e: Exception) {
            null
        }
    }



    // --- Firing Solution Solver ---
    private fun calculateFiringSolution() {
        if (currentLocation == null) {
            Toast.makeText(this, "Current location not available", Toast.LENGTH_SHORT).show()
            return
        }
        val settings = loadDragSettings()
        val windSpeed = etWindSpeed.text.toString().toDoubleOrNull() ?: 0.0
        val windDirection = etWindDirection.text.toString().toDoubleOrNull() ?: 0.0
        val shellWeightGrams = etShellWeight.text.toString().toDoubleOrNull() ?: 0.0
        val shellWeight = shellWeightGrams / 1000.0
        val muzzleVelocity = etMuzzleVelocity.text.toString().toDoubleOrNull() ?: 0.0
        // Map seekBar progress (assumed 0-100) to a height difference value.
        val heightDiff = (seekBarHeightDiff.progress - 50).toDouble()

        when {
            rbPlusCode.isChecked -> {
                // Use Plus Code input.
                val plusCodeInput = etPlusCode.text.toString().trim()
                if (plusCodeInput.isEmpty()) {
                    Toast.makeText(this, "Enter a Plus Code", Toast.LENGTH_SHORT).show()
                    return
                }
                val coordinates = decodePlusCode(plusCodeInput)
                if (coordinates == null) {
                    Toast.makeText(this, "Invalid Plus Code", Toast.LENGTH_SHORT).show()
                    return
                }
                proceedWithFiringSolution(coordinates.first, coordinates.second, settings, windSpeed, windDirection, shellWeight, muzzleVelocity, heightDiff)
            }
            rbAuto.isChecked -> {
                val targetLat = etTargetLat.text.toString().toDoubleOrNull() ?: run {
                    Toast.makeText(this, "Enter target latitude", Toast.LENGTH_SHORT).show()
                    return
                }
                val targetLon = etTargetLon.text.toString().toDoubleOrNull() ?: run {
                    Toast.makeText(this, "Enter target longitude", Toast.LENGTH_SHORT).show()
                    return
                }
                proceedWithFiringSolution(targetLat, targetLon, settings, windSpeed, windDirection, shellWeight, muzzleVelocity, heightDiff)
            }
            else -> { // Manual mode.
                val distance = etManualDistance.text.toString().toDoubleOrNull() ?: run {
                    Toast.makeText(this, "Enter distance", Toast.LENGTH_SHORT).show()
                    return
                }
                val bearing = etManualBearing.text.toString().toDoubleOrNull() ?: run {
                    Toast.makeText(this, "Enter bearing", Toast.LENGTH_SHORT).show()
                    return
                }
                val computedCoordinates = computeImpactCoordinates(currentLocation!!.latitude, currentLocation!!.longitude, distance, bearing)
                proceedWithFiringSolution(computedCoordinates.first, computedCoordinates.second, settings, windSpeed, windDirection, shellWeight, muzzleVelocity, heightDiff)
            }
        }
    }

    private fun proceedWithFiringSolution(
        targetLat: Double,
        targetLon: Double,
        settings: DragSettings,
        windSpeed: Double,
        windDirection: Double,
        shellWeight: Double,
        muzzleVelocity: Double,
        heightDiff: Double
    ) {
        // Calculate distance and target bearing from current location.
        val distance = calculateDistance(
            currentLocation!!.latitude,
            currentLocation!!.longitude,
            targetLat,
            targetLon
        )
        val targetBearing = calculateBearing(
            currentLocation!!.latitude,
            currentLocation!!.longitude,
            targetLat,
            targetLon
        )

        // Convert wind direction (from which the wind is blowing) into a wind vector.
        val windDirRad = Math.toRadians(windDirection)
        val wind_vx = -windSpeed * sin(windDirRad)
        val wind_vy = -windSpeed * cos(windDirRad)

        // Use the full 2D iterative solver for a high-angle lob solution.
        val (finalElevation, finalBearing) = solveFiringSolutionHighAngle(
            distance,
            targetBearing,
            muzzleVelocity,
            wind_vx,
            wind_vy,
            shellWeight,
            settings,
            heightDiff
        )

        // Run one final simulation using the computed solution.
        val finalSim = simulateProjectile(
            muzzleVelocity,
            finalElevation,
            finalBearing,
            wind_vx,
            wind_vy,
            shellWeight,
            settings.frontalCd,
            settings.sideCd,
            settings.frontalArea,
            settings.sideArea,
            settings.airDensity,
            heightDiff
        )
        val impactX = finalSim.impactX
        val impactY = finalSim.impactY
        val flightTime = finalSim.time
        val impactDistance = sqrt(impactX * impactX + impactY * impactY)
        val impactBearingRad = atan2(impactX, impactY)
        val impactBearing = (Math.toDegrees(impactBearingRad) + 360) % 360

        // Compute impact coordinates from the current location.
        val impactCoordinates = computeImpactCoordinates(
            currentLocation!!.latitude,
            currentLocation!!.longitude,
            impactDistance,
            impactBearing
        )

        // For reference, compute a drag-free high-angle solution.
        val ratio = (distance * GRAVITY) / (muzzleVelocity * muzzleVelocity)
        if (ratio > 1.0) {
            Toast.makeText(this, "Target out of range", Toast.LENGTH_SHORT).show()
            return
        }
        val lowAngle = 0.5 * asin(ratio)
        val dragFreeHighAngle = (Math.PI / 2) - lowAngle + atan2(heightDiff, distance)
        val baseElevationDeg = Math.toDegrees(dragFreeHighAngle)
        val finalElevationDeg = Math.toDegrees(finalElevation)

        // Save the computed shot.
        lastShot = Shot(
            id = System.currentTimeMillis(),
            impactLat = if (rbAuto.isChecked || rbPlusCode.isChecked)
                etTargetLat.text.toString().toDoubleOrNull() ?: impactCoordinates.first
            else
                impactCoordinates.first,
            impactLon = if (rbAuto.isChecked || rbPlusCode.isChecked)
                etTargetLon.text.toString().toDoubleOrNull() ?: impactCoordinates.second
            else
                impactCoordinates.second,
            timestamp = System.currentTimeMillis()
        )

        tvResult.text = "Firing Solution with Drag & Side Drag:\n\n" +
                "Target Distance: ${"%.1f".format(distance)} m\n" +
                "Target Bearing: ${"%.1f".format(targetBearing)}°\n\n" +
                "Set Mortar to:\n" +
                "   Elevation: ${"%.1f".format(finalElevationDeg)}°\n" +
                "   Bearing:   ${"%.1f".format(finalBearing)}°\n\n" +
                "Additional Details:\n" +
                "   Drag-Free Elevation (High-Angle Lob): ${"%.1f".format(baseElevationDeg)}°\n" +
                "   Time to Impact: ${"%.2f".format(flightTime)} s\n" +
                "   Impact Range: ${"%.1f".format(impactDistance)} m\n" +
                "   Impact Bearing: ${"%.1f".format(impactBearing)}°\n" +
                "   Impact Location: ${"%.6f".format(impactCoordinates.first)}, ${"%.6f".format(impactCoordinates.second)}\n" +
                "   Shell Weight: ${"%.2f".format(shellWeight)} kg\n" +
                "   Wind: ${"%.2f".format(windSpeed)} m/s @ ${"%.1f".format(windDirection)}°"
    }

    /**
     * Two-dimensional iterative solver for a high-angle lob solution.
     * Adjusts both the elevation and bearing until the simulated impact point converges to the target.
     *
     * @return Pair(elevation in radians, bearing in degrees)
     */
    private fun solveFiringSolutionHighAngle(
        distance: Double,
        targetBearing: Double,
        muzzleVelocity: Double,
        wind_vx: Double,
        wind_vy: Double,
        shellWeight: Double,
        settings: DragSettings,
        heightDiff: Double
    ): Pair<Double, Double> {
        // Compute the no-drag solution ratio.
        val ratio = (distance * GRAVITY) / (muzzleVelocity * muzzleVelocity)
        // For a high-angle lob, we use the complementary solution and add height adjustment.
        var elevation = if (ratio <= 1)
            (Math.PI / 2) - 0.5 * asin(ratio) + atan2(heightDiff, distance)
        else
            (Math.PI / 2) - 0.1

        // Constrain the initial guess within 40° to 90°.
        val minAngle = Math.toRadians(40.0)
        val maxAngle = Math.toRadians(90.0)
        elevation = elevation.coerceIn(minAngle, maxAngle)
        var bearing = targetBearing

        // Tolerance and iteration parameters.
        val tolerance = 0.1  // meters acceptable error in impact position.
        val maxIterations = 100
        val kElev = 0.03   // Gain for elevation correction.
        val kBearing = 0.1 // Gain for bearing correction.

        // Convert target bearing into local X-Y coordinates.
        val targetBearingRad = Math.toRadians(targetBearing)
        val targetX = distance * sin(targetBearingRad)
        val targetY = distance * cos(targetBearingRad)

        for (i in 0 until maxIterations) {
            // Simulate the projectile trajectory for the current guess.
            val simResult = simulateProjectile(
                muzzleVelocity,
                elevation,
                bearing,
                wind_vx,
                wind_vy,
                shellWeight,
                settings.frontalCd,
                settings.sideCd,
                settings.frontalArea,
                settings.sideArea,
                settings.airDensity,
                heightDiff
            )
            val simX = simResult.impactX
            val simY = simResult.impactY

            // Compute error between the simulated impact and the target.
            val errorX = targetX - simX
            val errorY = targetY - simY
            val errorDistance = sqrt(errorX * errorX + errorY * errorY)

            if (errorDistance < tolerance) break

            // Calculate the simulated range.
            val simRange = sqrt(simX * simX + simY * simY)
            // Compute a range error.
            val rangeError = simRange - distance
            // Use an adaptive gain: the larger the error, the more you adjust.
            // For example, multiply by (1 + abs(rangeError)/distance)
            val adaptiveFactor = 1.0 + abs(rangeError) / distance
            // Increase the elevation gain if overshooting (rangeError positive).
            val dElev = kElev * rangeError * adaptiveFactor / distance
            elevation += dElev
            // Clamp the elevation.
            elevation = elevation.coerceIn(minAngle, maxAngle)

            // Update bearing as before.
            val desiredBearing = (Math.toDegrees(atan2(errorX, errorY)) + 360) % 360
            val dBearing = kBearing * (((desiredBearing - bearing + 540) % 360) - 180)
            bearing = (bearing + dBearing + 360) % 360
        }
        return Pair(elevation, bearing)
    }



    /**
     * Two-dimensional iterative solver that adjusts both the elevation and bearing
     * until the simulated impact point converges to the target.
     *
     * @return Pair(elevation in radians, bearing in degrees)
     */
    private fun solveFiringSolution(
        distance: Double,
        targetBearing: Double,
        muzzleVelocity: Double,
        wind_vx: Double,
        wind_vy: Double,
        shellWeight: Double,
        settings: DragSettings,
        heightDiff: Double
    ): Pair<Double, Double> {
        // Initial guess from the no-drag ballistic solution (choose lower-angle solution).
        val ratio = (distance * GRAVITY) / (muzzleVelocity * muzzleVelocity)
        var elevation = if (ratio <= 1) 0.5 * asin(ratio) else 0.1
        var bearing = targetBearing

        // Tolerance and iteration parameters.
        val tolerance = 0.5  // meters (acceptable error in impact position).
        val maxIterations = 50
        val kElev = 0.05   // Gain for elevation correction.
        val kBearing = 0.1 // Gain for bearing correction.

        // Define a minimum elevation (in radians) to avoid negative launch angles.
        val minElevation = 0.1  // ~5.7 degrees

        // Convert target bearing into local X-Y coordinates.
        val targetBearingRad = Math.toRadians(targetBearing)
        val targetX = distance * sin(targetBearingRad)
        val targetY = distance * cos(targetBearingRad)

        for (i in 0 until maxIterations) {
            // Simulate the projectile trajectory for the current guess.
            val simResult = simulateProjectile(
                muzzleVelocity,
                elevation,
                bearing,
                wind_vx,
                wind_vy,
                shellWeight,
                settings.frontalCd,
                settings.sideCd,
                settings.frontalArea,
                settings.sideArea,
                settings.airDensity,
                heightDiff
            )
            val simX = simResult.impactX
            val simY = simResult.impactY

            // Compute error between the simulated impact and the target position.
            val errorX = targetX - simX
            val errorY = targetY - simY
            val errorDistance = sqrt(errorX * errorX + errorY * errorY)

            if (errorDistance < tolerance) break

            // Update elevation based on range error.
            val simRange = sqrt(simX * simX + simY * simY)
            val dElev = kElev * (distance - simRange) / distance
            elevation += dElev
            // Enforce a lower bound to avoid negative elevation.
            elevation = max(elevation, minElevation)

            // Update bearing based on lateral error.
            // Determine the desired bearing from the error vector.
            val desiredBearing = (Math.toDegrees(atan2(errorX, errorY)) + 360) % 360
            // Compute a small bearing correction (handling wrap-around properly).
            val dBearing = kBearing * (((desiredBearing - bearing + 540) % 360) - 180)
            bearing = (bearing + dBearing + 360) % 360
        }
        return Pair(elevation, bearing)
    }


    // --- Simulation using RK4 Integration & Drag Settings ---
    private fun simulateProjectile(
        muzzleVelocity: Double,
        elevation: Double,
        launchBearing: Double,
        wind_vx: Double,
        wind_vy: Double,
        mass: Double,
        frontalCd: Double,
        sideCd: Double,      // (Unused in this revised model)
        frontalArea: Double,
        sideArea: Double,    // (Unused in this revised model)
        rho: Double,
        targetHeight: Double
    ): SimulationResult {
        val launchBearingRad = Math.toRadians(launchBearing)
        // Initial velocity components.
        val initialVx = muzzleVelocity * cos(elevation) * sin(launchBearingRad)
        val initialVy = muzzleVelocity * cos(elevation) * cos(launchBearingRad)
        val initialVz = muzzleVelocity * sin(elevation)
        var state = doubleArrayOf(0.0, 0.0, 0.0, initialVx, initialVy, initialVz)
        var t = 0.0
        val dt = 0.001
        var maxZ = 0.0
        var reachedApex = false
        var prevState = state.copyOf()
        var prevT = t

        // Adjust target altitude slightly if >= 0 to ensure proper detection on descent.
        val effectiveTarget = if (targetHeight >= 0) targetHeight - 0.001 else targetHeight

        fun derivatives(s: DoubleArray): DoubleArray {
            val vx = s[3]
            val vy = s[4]
            val vz = s[5]
            // Compute the relative velocity (projectile velocity minus wind).
            val relVx = vx - wind_vx
            val relVy = vy - wind_vy
            val relVz = vz
            val speed = sqrt(relVx * relVx + relVy * relVy + relVz * relVz)
            // Drag magnitude using the frontal drag coefficient and area.
            val dragMagnitude = 0.5 * rho * speed * speed * frontalCd * frontalArea
            // Apply drag opposite to the relative velocity direction.
            val dragX = if (speed != 0.0) -dragMagnitude * (relVx / speed) else 0.0
            val dragY = if (speed != 0.0) -dragMagnitude * (relVy / speed) else 0.0
            val dragZ = if (speed != 0.0) -dragMagnitude * (relVz / speed) else 0.0
            val ax = dragX / mass
            val ay = dragY / mass
            val az = dragZ / mass - 9.81
            return doubleArrayOf(vx, vy, vz, ax, ay, az)
        }

        fun rk4Step(s: DoubleArray, dt: Double): DoubleArray {
            val k1 = derivatives(s)
            val s2 = DoubleArray(6) { i -> s[i] + 0.5 * dt * k1[i] }
            val k2 = derivatives(s2)
            val s3 = DoubleArray(6) { i -> s[i] + 0.5 * dt * k2[i] }
            val k3 = derivatives(s3)
            val s4 = DoubleArray(6) { i -> s[i] + dt * k3[i] }
            val k4 = derivatives(s4)
            return DoubleArray(6) { i -> s[i] + dt / 6.0 * (k1[i] + 2 * k2[i] + 2 * k3[i] + k4[i]) }
        }

        while (t < 100) {
            if (state[2] > maxZ) maxZ = state[2]
            if (state[5] < 0) reachedApex = true

            if (effectiveTarget < 0) {
                if ((prevState[2] - effectiveTarget) * (state[2] - effectiveTarget) <= 0.0) {
                    val fraction = if (abs(prevState[2] - effectiveTarget) > 1e-6)
                        (prevState[2] - effectiveTarget) / (prevState[2] - state[2])
                    else 1.0
                    val impactX = prevState[0] + (state[0] - prevState[0]) * fraction
                    val impactY = prevState[1] + (state[1] - prevState[1]) * fraction
                    val impactT = prevT + (t - prevT) * fraction
                    return SimulationResult(impactX, impactY, impactT, maxZ)
                }
            } else {
                if (reachedApex && state[5] < 0 && (prevState[2] - effectiveTarget) * (state[2] - effectiveTarget) <= 0.0) {
                    val fraction = if (abs(prevState[2] - effectiveTarget) > 1e-6)
                        (prevState[2] - effectiveTarget) / (prevState[2] - state[2])
                    else 1.0
                    val impactX = prevState[0] + (state[0] - prevState[0]) * fraction
                    val impactY = prevState[1] + (state[1] - prevState[1]) * fraction
                    val impactT = prevT + (t - prevT) * fraction
                    return SimulationResult(impactX, impactY, impactT, maxZ)
                }
            }
            prevState = state.copyOf()
            prevT = t
            state = rk4Step(state, dt)
            t += dt
        }
        return SimulationResult(state[0], state[1], t, maxZ)
    }


    // Haversine formula.
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS * c
    }

    // Bearing calculation.
    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val dLonRad = Math.toRadians(lon2 - lon1)
        val y = sin(dLonRad) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLonRad)
        val brng = Math.toDegrees(atan2(y, x))
        return (brng + 360) % 360
    }

    // Compute impact coordinates.
    private fun computeImpactCoordinates(lat: Double, lon: Double, distance: Double, bearing: Double): Pair<Double, Double> {
        val latRad = Math.toRadians(lat)
        val lonRad = Math.toRadians(lon)
        val bearingRad = Math.toRadians(bearing)
        val angularDistance = distance / EARTH_RADIUS
        val impactLatRad = asin(sin(latRad) * cos(angularDistance) +
                cos(latRad) * sin(angularDistance) * cos(bearingRad))
        val impactLonRad = lonRad + atan2(
            sin(bearingRad) * sin(angularDistance) * cos(latRad),
            cos(angularDistance) - sin(latRad) * sin(impactLatRad)
        )
        return Pair(Math.toDegrees(impactLatRad), Math.toDegrees(impactLonRad))
    }

    override fun onMapsSdkInitialized(renderer: Renderer) {
        when (renderer) {
            Renderer.LATEST -> println("Using latest Google Maps renderer")
            Renderer.LEGACY -> println("Using legacy Google Maps renderer")
        }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            getLastLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLastLocation()
        } else {
            Toast.makeText(this, "Location permission required!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            LocationServices.getFusedLocationProviderClient(this).lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = LatLng(location.latitude, location.longitude)
                    tvCurrentLocation.text = "Current Location: ${location.latitude}, ${location.longitude}"
                    fetchWindData(location.latitude, location.longitude)
                } else {
                    tvCurrentLocation.text = "Current Location: Unknown"
                }
            }
        }
    }

    private fun fetchWindData(lat: Double, lon: Double) {
        val url = "https://wttr.in/$lat,$lon?format=j1"
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to fetch weather data", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { jsonString ->
                    try {
                        val jsonObject = JSONObject(jsonString)
                        val currentCondition = jsonObject.getJSONArray("current_condition").getJSONObject(0)
                        val windSpeedKmph = currentCondition.getString("windspeedKmph").toDouble()
                        val windSpeed = windSpeedKmph / 3.6
                        val windDirDegree = currentCondition.getString("winddirDegree").toDouble()
                        runOnUiThread {
                            etWindSpeed.setText(windSpeed.toString())
                            etWindDirection.setText(windDirDegree.toString())
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Error parsing weather data", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }
}
