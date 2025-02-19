package com.example.mortartargeter3

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import okhttp3.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import kotlin.math.*
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import com.google.android.gms.maps.MapsInitializer.Renderer

class MainActivity : AppCompatActivity(), OnMapsSdkInitializedCallback {

    // UI elements
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

    // Location client
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null

    companion object {
        // Permission and map request codes.
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
        private const val MAP_PICK_REQUEST_CODE = 2000

        // Physical constants.
        private const val GRAVITY = 9.81           // m/s²
        private const val EARTH_RADIUS = 6371000.0 // meters

        // Drag parameters for a non-spherical projectile.
        // These values are adjustable.
        private const val FRONTAL_DRAG_COEFFICIENT = 0.3
        private const val SIDE_DRAG_COEFFICIENT = 1.2
        private const val FRONTAL_AREA = 0.01    // m² (effective frontal area)
        private const val SIDE_AREA = 0.015       // m² (effective side area)
        private const val AIR_DENSITY = 1.225    // kg/m³

        private const val TAG = "MortarTargeter"
    }

    // Extended simulation result including maximum altitude reached.
    data class SimulationResult(
        val impactX: Double,
        val impactY: Double,
        val time: Double,
        val maxZ: Double
    )

    fun getApiKey(context: Context): String {
        return try {
            val inputStream = context.assets.open("apikey.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.readLine().trim() // Read and trim any whitespace
        } catch (e: Exception) {
            e.printStackTrace()
            "API_KEY_NOT_FOUND" // Default value if file is missing
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val apiKey = getApiKey(this)

        // Initialize Google Maps with the API key
        MapsInitializer.initialize(this, Renderer.LATEST, this)

        // Bind UI elements
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

        // Initialize location provider
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestLocationPermission()

        // Update height difference label (maps SeekBar progress to -50…+50)
        seekBarHeightDiff.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val heightDiff = progress - 50
                tvHeightDifferenceLabel.text = "Height Difference (m): $heightDiff"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) { }
            override fun onStopTrackingTouch(seekBar: SeekBar?) { }
        })

        // Toggle targeting modes
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

        btnCalculate.setOnClickListener {
            calculateFiringSolution()
        }

        btnOpenMap.setOnClickListener {
            val intent = Intent(this, MapPickerActivity::class.java)
            currentLocation?.let { location ->
                intent.putExtra("current_lat", location.latitude)
                intent.putExtra("current_lon", location.longitude)
            }
            startActivityForResult(intent, MAP_PICK_REQUEST_CODE)
        }
    }

    // Handle map picker result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MAP_PICK_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val lat = data.getDoubleExtra("selected_lat", 0.0)
            val lon = data.getDoubleExtra("selected_lon", 0.0)
            etTargetLat.setText(lat.toString())
            etTargetLon.setText(lon.toString())
        }
    }

    // Request location permissions
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

    // Handle permission result
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

    // Get device location
    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLocation = location
                    tvCurrentLocation.text = "Current Location: ${location.latitude}, ${location.longitude}"
                    fetchWindData(location.latitude, location.longitude)
                } else {
                    tvCurrentLocation.text = "Current Location: Unknown"
                }
            }
        }
    }

    // Fetch wind data from wttr.in
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
    // 1. Compute a drag-free high-angle initial guess.
    // 2. Restrict the search interval to ±0.2 rad around that guess.
    // 3. Use bisection to find the high-angle elevation that (in simulation with drag, side effects, and wind)
    //    produces the target horizontal range when the projectile crosses the target altitude.
    //    (If the projectile never reaches the target altitude, force a higher angle.)
    // 4. Adjust the launch bearing based on the lateral (cross-range) error.
    private fun calculateFiringSolution() {
        if (currentLocation == null) {
            Toast.makeText(this, "Current location not available", Toast.LENGTH_SHORT).show()
            return
        }

        // Read inputs
        val windSpeed = etWindSpeed.text.toString().toDoubleOrNull() ?: 0.0
        val windDirection = etWindDirection.text.toString().toDoubleOrNull() ?: 0.0
        val shellWeightGrams = etShellWeight.text.toString().toDoubleOrNull() ?: 0.0
        val shellWeight = shellWeightGrams / 1000.0  // kg
        val muzzleVelocity = etMuzzleVelocity.text.toString().toDoubleOrNull() ?: 0.0
        // heightDiff: positive means target is above launch altitude.
        val heightDiff = (seekBarHeightDiff.progress - 50).toDouble()

        // Determine target horizontal distance and bearing.
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
        }

        // Compute wind vector (windDirection is degrees from north)
        val windDirRad = Math.toRadians(windDirection)
        val wind_vx = windSpeed * sin(windDirRad)
        val wind_vy = windSpeed * cos(windDirRad)

        // Tolerances and iteration parameters
        val toleranceRange = 0.5   // meters
        val toleranceBearing = 0.5 // degrees
        val maxBisectionIterations = 20
        val maxOuterIterations = 15

        // Use the target bearing as the initial guess for launch bearing.
        var currentLaunchBearing = bearing

        // Inner function: for a fixed launch bearing, find the high-angle elevation (in radians) that yields the desired horizontal range.
        fun findElevationForRange(launchBearing: Double): Double {
            // Compute a drag-free high-angle solution as a starting guess.
            val ratio = (distance * GRAVITY) / (muzzleVelocity * muzzleVelocity)
            if (ratio > 1) {
                Toast.makeText(this, "Target out of range", Toast.LENGTH_SHORT).show()
                return Double.NaN
            }
            val lowAngle = 0.5 * asin(ratio)
            val dragFreeHighAngle = (Math.PI / 2) - lowAngle + atan2(heightDiff, distance)
            val initialGuess = dragFreeHighAngle.coerceIn(0.7, Math.PI / 2 - 0.01)

            // Define a narrow search interval around the initial guess.
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
                    FRONTAL_DRAG_COEFFICIENT,
                    SIDE_DRAG_COEFFICIENT,
                    AIR_DENSITY,
                    heightDiff
                )
                // If the projectile never reaches the target altitude (or reaches it only before apex), force a higher angle.
                if (heightDiff >= 0 && simResult.maxZ < heightDiff) {
                    low = mid
                    mid = (low + high) / 2.0
                    continue
                }
                val simRange = sqrt(simResult.impactX * simResult.impactX + simResult.impactY * simResult.impactY)
                val diff = simRange - distance
                if (abs(diff) < toleranceRange) {
                    return mid
                }
                // For high-angle solutions, horizontal range decreases as elevation increases.
                if (simRange > distance) {
                    low = mid
                } else {
                    high = mid
                }
                mid = (low + high) / 2.0
            }
            return mid
        }

        // Outer loop: adjust launch bearing based on cross-range (lateral) error.
        var currentElevation = findElevationForRange(currentLaunchBearing)
        for (i in 0 until maxOuterIterations) {
            val simResult = simulateProjectile(
                muzzleVelocity,
                currentElevation,
                currentLaunchBearing,
                wind_vx,
                wind_vy,
                shellWeight,
                FRONTAL_DRAG_COEFFICIENT,
                SIDE_DRAG_COEFFICIENT,
                AIR_DENSITY,
                heightDiff
            )
            val simX = simResult.impactX
            val simY = simResult.impactY

            // Compute target local coordinates (x east, y north)
            val bearingRad = Math.toRadians(bearing)
            val targetX = distance * sin(bearingRad)
            val targetY = distance * cos(bearingRad)

            val errorX = targetX - simX
            val errorY = targetY - simY
            // Lateral error is the projection on the unit vector perpendicular to the target direction.
            val errorCross = errorX * cos(bearingRad) - errorY * sin(bearingRad)
            // Use arctan to compute an angular correction.
            val deltaBearingRad = atan2(errorCross, distance)
            val deltaBearingDeg = Math.toDegrees(deltaBearingRad)
            currentLaunchBearing += deltaBearingDeg

            // Recompute the required elevation for the new bearing.
            currentElevation = findElevationForRange(currentLaunchBearing)

            if (abs(deltaBearingDeg) < toleranceBearing) break
        }

        // Final simulation with converged parameters.
        val finalSim = simulateProjectile(
            muzzleVelocity,
            currentElevation,
            currentLaunchBearing,
            wind_vx,
            wind_vy,
            shellWeight,
            FRONTAL_DRAG_COEFFICIENT,
            SIDE_DRAG_COEFFICIENT,
            AIR_DENSITY,
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
        // Compute a drag-free high-angle solution for reference.
        val ratio = (distance * GRAVITY) / (muzzleVelocity * muzzleVelocity)
        if (ratio > 1) {
            Toast.makeText(this, "Target out of range", Toast.LENGTH_SHORT).show()
            return
        }
        val lowAngle = 0.5 * asin(ratio)
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

    // --- Simulation of Projectile Flight with Side Drag ---
    // This function integrates the projectile's equations (with drag separated into frontal and side components)
    // until the projectile crosses the target altitude.
    // For targetHeight < 0, it stops at the first crossing.
    // For targetHeight >= 0, it waits until after the apex (vz < 0) and then on the descent.
    // The timestep (dt) is set to 0.001 for high resolution.
    private fun simulateProjectile(
        muzzleVelocity: Double,
        elevation: Double,
        launchBearing: Double,
        wind_vx: Double,
        wind_vy: Double,
        mass: Double,
        frontalCd: Double,
        sideCd: Double,
        rho: Double,
        targetHeight: Double
    ): SimulationResult {
        val launchBearingRad = Math.toRadians(launchBearing)
        var vx = muzzleVelocity * cos(elevation) * sin(launchBearingRad)
        var vy = muzzleVelocity * cos(elevation) * cos(launchBearingRad)
        var vz = muzzleVelocity * sin(elevation)
        var x = 0.0
        var y = 0.0
        var z = 0.0
        var t = 0.0
        val dt = 0.001
        var maxZ = z

        // Compute the projectile's fixed axis (based on initial firing direction)
        val axisX = cos(elevation) * sin(launchBearingRad)
        val axisY = cos(elevation) * cos(launchBearingRad)
        val axisZ = sin(elevation)
        val axisMag = sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ)
        val axis = Triple(axisX / axisMag, axisY / axisMag, axisZ / axisMag)

        var prevX = x
        var prevY = y
        var prevZ = z
        var prevT = t
        var reachedApex = false

        while (t < 100) {
            t += dt
            prevX = x; prevY = y; prevZ = z; prevT = t - dt

            // Compute relative velocity (projectile relative to wind)
            val relVx = vx - wind_vx
            val relVy = vy - wind_vy
            val relVz = vz  // vertical wind is ignored
            // Decompose relative velocity into components along the projectile's fixed axis and perpendicular.
            val vRelMag = sqrt(relVx * relVx + relVy * relVy + relVz * relVz)
            val vParallel = relVx * axis.first + relVy * axis.second + relVz * axis.third
            val parallelVx = vParallel * axis.first
            val parallelVy = vParallel * axis.second
            val parallelVz = vParallel * axis.third
            val perpVx = relVx - parallelVx
            val perpVy = relVy - parallelVy
            val perpVz = relVz - parallelVz
            val vPerpMag = sqrt(perpVx * perpVx + perpVy * perpVy + perpVz * perpVz)

            // Frontal (parallel) drag force
            val FdParallelMag = 0.5 * rho * (vParallel * vParallel) * frontalCd * FRONTAL_AREA
            val FdParallelX = -FdParallelMag * sign(vParallel) * axis.first
            val FdParallelY = -FdParallelMag * sign(vParallel) * axis.second
            val FdParallelZ = -FdParallelMag * sign(vParallel) * axis.third

            // Side (perpendicular) drag force
            val FdSideMag = if (vPerpMag > 0) 0.5 * rho * (vPerpMag * vPerpMag) * sideCd * SIDE_AREA else 0.0
            val FdSideX = if (vPerpMag > 0) -FdSideMag * (perpVx / vPerpMag) else 0.0
            val FdSideY = if (vPerpMag > 0) -FdSideMag * (perpVy / vPerpMag) else 0.0
            val FdSideZ = if (vPerpMag > 0) -FdSideMag * (perpVz / vPerpMag) else 0.0

            // Total drag force components.
            val FdX = FdParallelX + FdSideX
            val FdY = FdParallelY + FdSideY
            val FdZ = FdParallelZ + FdSideZ

            // Accelerations due to drag.
            val ax = FdX / mass
            val ay = FdY / mass
            val az = FdZ / mass

            // Total acceleration (adding gravity in the -z direction)
            val totalAx = ax
            val totalAy = ay
            val totalAz = az - GRAVITY

            vx += totalAx * dt
            vy += totalAy * dt
            vz += totalAz * dt

            x += vx * dt
            y += vy * dt
            z += vz * dt
            maxZ = max(maxZ, z)

            if (vz < 0) reachedApex = true

            if (targetHeight < 0) {
                if ((prevZ - targetHeight) * (z - targetHeight) <= 0) {
                    val fraction = if ((prevZ - targetHeight) != 0.0) (prevZ - targetHeight) / (prevZ - z) else 1.0
                    val impactX = prevX + (x - prevX) * fraction
                    val impactY = prevY + (y - prevY) * fraction
                    val impactT = prevT + (t - prevT) * fraction
                    return SimulationResult(impactX, impactY, impactT, maxZ)
                }
            } else {
                if (reachedApex && (prevZ - targetHeight) * (z - targetHeight) <= 0 && vz < 0) {
                    val fraction = if ((prevZ - targetHeight) != 0.0) (prevZ - targetHeight) / (prevZ - z) else 1.0
                    val impactX = prevX + (x - prevX) * fraction
                    val impactY = prevY + (y - prevY) * fraction
                    val impactT = prevT + (t - prevT) * fraction
                    return SimulationResult(impactX, impactY, impactT, maxZ)
                }
            }
        }
        return SimulationResult(x, y, t, maxZ)
    }

    // Haversine formula: calculates the great-circle distance between two coordinates.
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS * c
    }

    // Calculates the initial bearing (azimuth) between two coordinates.
    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val dLonRad = Math.toRadians(lon2 - lon1)
        val y = sin(dLonRad) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLonRad)
        val brng = Math.toDegrees(atan2(y, x))
        return (brng + 360) % 360
    }

    // Computes geographic impact coordinates using great-circle navigation.
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
        val impactLat = Math.toDegrees(impactLatRad)
        val impactLon = Math.toDegrees(impactLonRad)
        return Pair(impactLat, impactLon)
    }

    override fun onMapsSdkInitialized(renderer: Renderer) {
        when (renderer) {
            Renderer.LATEST -> println("Using latest Google Maps renderer")
            Renderer.LEGACY -> println("Using legacy Google Maps renderer")
        }
    }
}
