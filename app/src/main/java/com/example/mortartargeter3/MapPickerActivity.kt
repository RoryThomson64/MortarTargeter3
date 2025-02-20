package com.example.mortartargeter3

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlin.math.*

class MapPickerActivity : FragmentActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var btnClose: ImageButton
    private lateinit var btnToggleSatellite: ImageButton
    private lateinit var btnMyLocation: ImageButton
    private lateinit var tvDistance: TextView
    private lateinit var btnConfirm: Button  // For confirming target selection
    private lateinit var btnMoveMortar: Button // New button to update mortar location

    private var myLocation: LatLng? = null
    private var selectedLatLng: LatLng? = null

    companion object {
        private const val EARTH_RADIUS = 6371000.0  // in meters
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_picker)

        // Bind views
        btnClose = findViewById(R.id.btnClose)
        btnToggleSatellite = findViewById(R.id.btnToggleSatellite)
        btnMyLocation = findViewById(R.id.btnMyLocation)
        tvDistance = findViewById(R.id.tvDistance)
        btnConfirm = findViewById(R.id.btnConfirm)
        btnMoveMortar = findViewById(R.id.btnMoveMortar)

        // Set up button listeners
        btnClose.setOnClickListener { finish() }

        btnToggleSatellite.setOnClickListener {
            if (::mMap.isInitialized) {
                mMap.mapType = if (mMap.mapType != GoogleMap.MAP_TYPE_SATELLITE)
                    GoogleMap.MAP_TYPE_SATELLITE else GoogleMap.MAP_TYPE_NORMAL
            }
        }

        btnMyLocation.setOnClickListener {
            if (::mMap.isInitialized) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                mMap.isMyLocationEnabled = true
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val currentLatLng = LatLng(it.latitude, it.longitude)
                        myLocation = currentLatLng
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                        // Add marker for My Location (green)
                        mMap.addMarker(
                            MarkerOptions()
                                .position(currentLatLng)
                                .title("My Location")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                        )
                    }
                }
            }
        }

        btnConfirm.setOnClickListener {
            if (selectedLatLng == null) {
                Toast.makeText(this, "Please select a target location", Toast.LENGTH_SHORT).show()
            } else {
                // Return target coordinates.
                val distance = myLocation?.let { origin ->
                    computeDistance(origin.latitude, origin.longitude, selectedLatLng!!.latitude, selectedLatLng!!.longitude)
                } ?: 0.0
                val resultIntent = Intent().apply {
                    putExtra("update_type", "target")
                    putExtra("selected_lat", selectedLatLng!!.latitude)
                    putExtra("selected_lon", selectedLatLng!!.longitude)
                    putExtra("distance", distance)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }

        btnMoveMortar.setOnClickListener {
            if (selectedLatLng == null) {
                Toast.makeText(this, "Please drop a pin to set mortar location", Toast.LENGTH_SHORT).show()
            } else {
                myLocation = selectedLatLng
                // Clear the map and re-add the marker for the new mortar location.
                mMap.clear()
                mMap.addMarker(
                    MarkerOptions()
                        .position(myLocation!!)
                        .title("My Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                )
                Toast.makeText(this, "Mortar location updated", Toast.LENGTH_SHORT).show()
                // Return mortar location via intent.
                val resultIntent = Intent().apply {
                    putExtra("update_type", "mortar")
                    putExtra("mortar_lat", myLocation!!.latitude)
                    putExtra("mortar_lon", myLocation!!.longitude)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Enable basic UI controls
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = true

        // Check if current location was passed from MainActivity
        val currentLat = intent.getDoubleExtra("current_lat", 0.0)
        val currentLon = intent.getDoubleExtra("current_lon", 0.0)
        if (currentLat != 0.0 || currentLon != 0.0) {
            myLocation = LatLng(currentLat, currentLon)
            // Add marker for My Location (green)
            mMap.addMarker(
                MarkerOptions()
                    .position(myLocation!!)
                    .title("My Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation!!, 15f))
        } else {
            val defaultLocation = LatLng(0.0, 0.0)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 2f))
        }

        // Set a listener for map clicks to select target location
        mMap.setOnMapClickListener { latLng ->
            mMap.clear()
            // Re-add My Location marker if available
            myLocation?.let {
                mMap.addMarker(
                    MarkerOptions()
                        .position(it)
                        .title("My Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                )
            }
            // Add Target marker (blue)
            mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Target")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
            selectedLatLng = latLng

            // Update the distance text if myLocation is available
            myLocation?.let { origin ->
                val distance = computeDistance(origin.latitude, origin.longitude, latLng.latitude, latLng.longitude)
                tvDistance.text = "Distance: ${"%.1f".format(distance)} m"
            }
        }
    }

    // Compute distance between two points using the Haversine formula.
    private fun computeDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS * c
    }
}
