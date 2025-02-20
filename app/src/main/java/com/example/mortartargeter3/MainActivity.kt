package com.example.mortartargeter3

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import okhttp3.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.*
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import com.google.android.gms.maps.MapsInitializer.Renderer
import com.google.android.gms.maps.model.LatLng
import java.io.IOException

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
    private lateinit var layoutAutoTargeting: LinearLayout
    private lateinit var layoutManualTargeting: LinearLayout
    private lateinit var etTargetLat: EditText
    private lateinit var etTargetLon: EditText
    private lateinit var etManualDistance: EditText
    private lateinit var etManualBearing: EditText
    private lateinit var etManualOriginLat: EditText
    private lateinit var etManualOriginLon: EditText
    private lateinit var btnCalculate: Button
    private lateinit var btnOpenMap: Button
    private lateinit var tvResult: TextView
    private lateinit var btnRefreshLocation: Button
    private lateinit var btnMaxRange: Button
    private lateinit var tvMaxRange: TextView
    private lateinit var btnSettings: Button
    private lateinit var btnHelp: ImageButton  // New Help button

    // Location client.
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: LatLng? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
        private const val MAP_PICK_REQUEST_CODE = 2000
        private const val TAG = "MortarTargeter"
        private const val GRAVITY = 9.81  // m/s²
        private const val EARTH_RADIUS = 6371000.0  // meters
    }

    // Helper: load drag settings from SharedPreferences.
    private fun loadDragSettings(): DragSettings {
        val prefs = getSharedPreferences("DragSettings", Context.MODE_PRIVATE)
        val frontalCd = prefs.getFloat("frontal_cd", 0.3f).toDouble()
        val sideCd = prefs.getFloat("side_cd", 1.2f).toDouble()
        val frontalArea = prefs.getFloat("frontal_area", 0.01f).toDouble()
        val sideArea = prefs.getFloat("side_area", 0.015f).toDouble()
        val airDensity = prefs.getFloat("air_density", 1.225f).toDouble()
        return DragSettings(frontalCd, sideCd, frontalArea, sideArea, airDensity)
    }

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

        // Initialize Google Maps.
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
        layoutAutoTargeting = findViewById(R.id.layoutAutoTargeting)
        layoutManualTargeting = findViewById(R.id.layoutManualTargeting)
        etTargetLat = findViewById(R.id.etTargetLat)
        etTargetLon = findViewById(R.id.etTargetLon)
        etManualDistance = findViewById(R.id.etManualDistance)
        etManualBearing = findViewById(R.id.etManualBearing)
        etManualOriginLat = findViewById(R.id.etManualOriginLat)
        etManualOriginLon = findViewById(R.id.etManualOriginLon)
        btnCalculate = findViewById(R.id.btnCalculate)
        btnOpenMap = findViewById(R.id.btnOpenMap)
        tvResult = findViewById(R.id.tvResult)
        tvResult.movementMethod = ScrollingMovementMethod.getInstance()
        btnRefreshLocation = findViewById(R.id.btnRefreshLocation)
        btnMaxRange = findViewById(R.id.btnMaxRange)
        tvMaxRange = findViewById(R.id.tvMaxRange)
        btnSettings = findViewById(R.id.btnSettings)
        btnHelp = findViewById(R.id.btnHelp) // Bind the Help button

        // Set Help button listener.
        btnHelp.setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestLocationPermission()

        seekBarHeightDiff.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val heightDiff = progress - 50
                tvHeightDifferenceLabel.text = "Height Difference (m): $heightDiff"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) { }
            override fun onStopTrackingTouch(seekBar: SeekBar?) { }
        })

        val rgTargetingMode = findViewById<RadioGroup>(R.id.rgTargetingMode)
        rgTargetingMode.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbAuto) {
                layoutAutoTargeting.visibility = LinearLayout.VISIBLE
                layoutManualTargeting.visibility = LinearLayout.GONE
            } else {
                layoutAutoTargeting.visibility = LinearLayout.GONE
                layoutManualTargeting.visibility = LinearLayout.VISIBLE
            }
        }

        btnCalculate.setOnClickListener { calculateFiringSolution() }
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


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MAP_PICK_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val updateType = data.getStringExtra("update_type")
            if (updateType == "mortar") {
                // Update mortar location in MainActivity.
                val newLat = data.getDoubleExtra("mortar_lat", 0.0)
                val newLon = data.getDoubleExtra("mortar_lon", 0.0)
                currentLocation = LatLng(newLat, newLon)
                tvCurrentLocation.text = "Current Location: $newLat, $newLon"
            } else if (updateType == "target") {
                // Update target coordinates.
                val targetLat = data.getDoubleExtra("selected_lat", 0.0)
                val targetLon = data.getDoubleExtra("selected_lon", 0.0)
                etTargetLat.setText(targetLat.toString())
                etTargetLon.setText(targetLon.toString())
            }
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
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
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
        val heightDiff = (seekBarHeightDiff.progress - 50).toDouble()

        var distance: Double
        var bearing: Double
        if (rbAuto.isChecked) {
            val targetLat = etTargetLat.text.toString().toDoubleOrNull() ?: run {
                Toast.makeText(this, "Enter target latitude", Toast.LENGTH_SHORT).show()
                return
            }
            val targetLon = etTargetLon.text.toString().toDoubleOrNull() ?: run {
                Toast.makeText(this, "Enter target longitude", Toast.LENGTH_SHORT).show()
                return
            }
            distance = calculateDistance(currentLocation!!.latitude, currentLocation!!.longitude, targetLat, targetLon)
            bearing = calculateBearing(currentLocation!!.latitude, currentLocation!!.longitude, targetLat, targetLon)
        } else {
            distance = etManualDistance.text.toString().toDoubleOrNull() ?: run {
                Toast.makeText(this, "Enter distance", Toast.LENGTH_SHORT).show()
                return
            }
            bearing = etManualBearing.text.toString().toDoubleOrNull() ?: run {
                Toast.makeText(this, "Enter bearing", Toast.LENGTH_SHORT).show()
                return
            }
            bearing = (bearing % 360 + 360) % 360
        }

        val windDirRad = Math.toRadians(windDirection)
        val wind_vx = windSpeed * sin(windDirRad)
        val wind_vy = windSpeed * cos(windDirRad)

        val toleranceRange = 0.5  // meters.
        val toleranceBearing = 0.5 // degrees.
        val maxBisectionIterations = 20
        val maxOuterIterations = 15

        var currentLaunchBearing = bearing

        fun findElevationForRange(launchBearing: Double): Double {
            val ratio = (distance * GRAVITY) / (muzzleVelocity * muzzleVelocity)
            if (ratio > 1) {
                Toast.makeText(this, "Target out of range", Toast.LENGTH_SHORT).show()
                return Double.NaN
            }
            val lowAngle = 0.5 * asin(ratio)
            val dragFreeHighAngle = (Math.PI / 2) - lowAngle + atan2(heightDiff, distance)
            val initialGuess = dragFreeHighAngle.coerceIn(0.7, Math.PI / 2 - 0.01)
            var low = (initialGuess - 0.2).coerceAtLeast(0.7)
            var high = (initialGuess + 0.2).coerceAtMost(Math.PI / 2 - 0.01)
            var mid = (low + high) / 2.0

            for (i in 0 until maxBisectionIterations) {
                val simResult = simulateProjectile(
                    muzzleVelocity,
                    mid,
                    launchBearing,
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
                if (heightDiff >= 0 && simResult.maxZ < heightDiff) {
                    low = mid
                    mid = (low + high) / 2.0
                    continue
                }
                val simRange = sqrt(simResult.impactX * simResult.impactX + simResult.impactY * simResult.impactY)
                val diff = simRange - distance
                if (abs(diff) < toleranceRange) return mid
                if (simRange > distance) low = mid else high = mid
                mid = (low + high) / 2.0
            }
            return mid
        }

        var currentElevation = findElevationForRange(currentLaunchBearing)
        for (i in 0 until maxOuterIterations) {
            val simResult = simulateProjectile(
                muzzleVelocity,
                currentElevation,
                currentLaunchBearing,
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
            val bearingRad = Math.toRadians(bearing)
            val targetX = distance * sin(bearingRad)
            val targetY = distance * cos(bearingRad)
            val errorX = targetX - simX
            val errorY = targetY - simY
            val errorCross = errorX * cos(bearingRad) - errorY * sin(bearingRad)
            val deltaBearingRad = atan2(errorCross, distance)
            val deltaBearingDeg = Math.toDegrees(deltaBearingRad)
            currentLaunchBearing += deltaBearingDeg
            currentLaunchBearing = (currentLaunchBearing % 360 + 360) % 360
            currentElevation = findElevationForRange(currentLaunchBearing)
            if (abs(deltaBearingDeg) < toleranceBearing) break
        }

        val finalSim = simulateProjectile(
            muzzleVelocity,
            currentElevation,
            currentLaunchBearing,
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

        val impactCoordinates = computeImpactCoordinates(
            currentLocation!!.latitude,
            currentLocation!!.longitude,
            impactDistance,
            impactBearing
        )
        val finalElevationDeg = Math.toDegrees(currentElevation)
        val ratio2 = (distance * GRAVITY) / (muzzleVelocity * muzzleVelocity)
        if (ratio2 > 1.0) {
            Toast.makeText(this, "Target out of range", Toast.LENGTH_SHORT).show()
            return
        }
        val lowAngle = 0.5 * asin(ratio2)
        val dragFreeHighAngle = (Math.PI / 2) - lowAngle + atan2(heightDiff, distance)
        val baseElevationDeg = Math.toDegrees(dragFreeHighAngle)

        Log.d(TAG, "Final simulation: maxZ=${finalSim.maxZ}, targetHeight=$heightDiff")

        tvResult.text = "Firing Solution with Drag & Side Drag:\n\n" +
                "Target Distance: ${"%.1f".format(distance)} m\n" +
                "Target Bearing: ${"%.1f".format(bearing)}°\n\n" +
                "Set Mortar to:\n" +
                "   Elevation: ${"%.1f".format(finalElevationDeg)}°\n" +
                "   Bearing:   ${"%.1f".format(currentLaunchBearing)}°\n\n" +
                "Additional Details:\n" +
                "   Drag-Free Elevation (High-Angle Lob): ${"%.1f".format(baseElevationDeg)}°\n" +
                "   Time to Impact: ${"%.2f".format(flightTime)} s\n" +
                "   Impact Range: ${"%.1f".format(impactDistance)} m\n" +
                "   Impact Bearing: ${"%.1f".format(impactBearing)}°\n" +
                "   Impact Location: ${"%.6f".format(impactCoordinates.first)}, ${"%.6f".format(impactCoordinates.second)}\n" +
                "   Shell Weight: ${"%.2f".format(shellWeight)} kg\n" +
                "   Wind: ${"%.2f".format(windSpeed)} m/s @ ${"%.1f".format(windDirection)}°"
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
        sideCd: Double,
        frontalArea: Double,
        sideArea: Double,
        rho: Double,
        targetHeight: Double
    ): SimulationResult {
        val launchBearingRad = Math.toRadians(launchBearing)
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

        // Compute fixed launch axis.
        val axisX = cos(elevation) * sin(launchBearingRad)
        val axisY = cos(elevation) * cos(launchBearingRad)
        val axisZ = sin(elevation)
        val axisMag = sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ)
        val axis = Triple(axisX / axisMag, axisY / axisMag, axisZ / axisMag)

        // Local function: compute derivatives of the state.
        fun derivatives(s: DoubleArray): DoubleArray {
            val vx = s[3]
            val vy = s[4]
            val vz = s[5]
            // Relative velocity (ignoring vertical wind)
            val relVx = vx - wind_vx
            val relVy = vy - wind_vy
            val relVz = vz
            val vRelMag = sqrt(relVx * relVx + relVy * relVy + relVz * relVz)
            // Decompose into components along the fixed axis and perpendicular.
            val vParallel = relVx * axis.first + relVy * axis.second + relVz * axis.third
            val parallelVx = vParallel * axis.first
            val parallelVy = vParallel * axis.second
            val parallelVz = vParallel * axis.third
            val perpVx = relVx - parallelVx
            val perpVy = relVy - parallelVy
            val perpVz = relVz - parallelVz
            val vPerpMag = sqrt(perpVx * perpVx + perpVy * perpVy + perpVz * perpVz)
            // Compute drag forces.
            val FdParallelMag = 0.5 * rho * (vParallel * vParallel) * frontalCd * frontalArea
            val FdParallelX = -FdParallelMag * sign(vParallel) * axis.first
            val FdParallelY = -FdParallelMag * sign(vParallel) * axis.second
            val FdParallelZ = -FdParallelMag * sign(vParallel) * axis.third
            val FdSideMag = if (vPerpMag > 0) 0.5 * rho * (vPerpMag * vPerpMag) * sideCd * sideArea else 0.0
            val FdSideX = if (vPerpMag > 0) -FdSideMag * (perpVx / vPerpMag) else 0.0
            val FdSideY = if (vPerpMag > 0) -FdSideMag * (perpVy / vPerpMag) else 0.0
            val FdSideZ = if (vPerpMag > 0) -FdSideMag * (perpVz / vPerpMag) else 0.0
            val FdX = FdParallelX + FdSideX
            val FdY = FdParallelY + FdSideY
            val FdZ = FdParallelZ + FdSideZ
            // Accelerations (adding gravity in -z).
            val ax = FdX / mass
            val ay = FdY / mass
            val az = FdZ / mass - 9.81
            return doubleArrayOf(vx, vy, vz, ax, ay, az)
        }

        // Local function: RK4 integration step.
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

            if (targetHeight < 0) {
                // For targets below launch altitude, detect crossing immediately.
                if ((prevState[2] - targetHeight) * (state[2] - targetHeight) <= 0.0) {
                    val fraction = if (abs(prevState[2] - targetHeight) > 1e-6)
                        (prevState[2] - targetHeight) / (prevState[2] - state[2])
                    else 1.0
                    val impactX = prevState[0] + (state[0] - prevState[0]) * fraction
                    val impactY = prevState[1] + (state[1] - prevState[1]) * fraction
                    val impactT = prevT + (t - prevT) * fraction
                    return SimulationResult(impactX, impactY, impactT, maxZ)
                }
            } else {
                // For targets at or above launch altitude, wait until after apex and descent.
                if (reachedApex && state[5] < 0 && (prevState[2] - targetHeight) * (state[2] - targetHeight) <= 0.0) {
                    val fraction = if (abs(prevState[2] - targetHeight) > 1e-6)
                        (prevState[2] - targetHeight) / (prevState[2] - state[2])
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
}
