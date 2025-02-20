package com.example.mortartargeter3

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
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
import com.google.openlocationcode.OpenLocationCode
import kotlin.math.*

class FieldMapPickerActivity : FragmentActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var btnClose: ImageButton
    private lateinit var btnToggleSatellite: ImageButton
    private lateinit var btnMyLocation: ImageButton
    private lateinit var btnConfirm: Button
    private lateinit var tvPlusCode: TextView
    private lateinit var tvDistance: TextView

    private var myLocation: LatLng? = null
    private var selectedLatLng: LatLng? = null

    companion object {
        private const val EARTH_RADIUS = 6371000.0  // meters
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_field_map_picker)

        // Bind UI elements.
        btnClose = findViewById(R.id.btnClose)
        btnToggleSatellite = findViewById(R.id.btnToggleSatellite)
        btnMyLocation = findViewById(R.id.btnMyLocation)
        btnConfirm = findViewById(R.id.btnConfirm)
        tvDistance = findViewById(R.id.tvDistance)

        btnClose.setOnClickListener { finish() }
        btnToggleSatellite.setOnClickListener {
            if (::mMap.isInitialized) {
                mMap.mapType = if (mMap.mapType != GoogleMap.MAP_TYPE_SATELLITE)
                    GoogleMap.MAP_TYPE_SATELLITE else GoogleMap.MAP_TYPE_NORMAL
            }
        }
        btnMyLocation.setOnClickListener { centerOnMyLocation() }

        // Confirm button: generate Plus Code and return result.
        btnConfirm.setOnClickListener {
            if (selectedLatLng == null) {
                Toast.makeText(this, "Please select a target location", Toast.LENGTH_SHORT).show()
            } else {
                val plusCode = OpenLocationCode.encode(selectedLatLng!!.latitude, selectedLatLng!!.longitude, 10)
                Toast.makeText(this, "Plus Code: $plusCode", Toast.LENGTH_LONG).show()
                val resultIntent = Intent().apply {
                    putExtra("plus_code", plusCode)
                    putExtra("selected_lat", selectedLatLng!!.latitude)
                    putExtra("selected_lon", selectedLatLng!!.longitude)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }

        // Set up the map fragment.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Disable the default My Location button.
        mMap.uiSettings.isMyLocationButtonEnabled = false

        // Enable basic map UI controls.
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = true

        // Center the map on the user's location.
        centerOnMyLocation()

        // Set a map click listener for selecting target location.
        mMap.setOnMapClickListener { latLng ->
            mMap.clear()
            // Re-add My Location marker if available.
            myLocation?.let {
                mMap.addMarker(
                    MarkerOptions()
                        .position(it)
                        .title("My Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                )
            }
            // Add a marker for the target (blue).
            mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Target")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
            selectedLatLng = latLng

            // Update distance bar if myLocation is available.
            myLocation?.let { origin ->
                val distance = computeDistance(origin.latitude, origin.longitude, latLng.latitude, latLng.longitude)
                tvDistance.text = "Distance: ${"%.1f".format(distance)} m"
            }
        }
    }

    private fun centerOnMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
            return
        }
        mMap.isMyLocationEnabled = true
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                myLocation = LatLng(it.latitude, it.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation!!, 15f))
                mMap.addMarker(
                    MarkerOptions()
                        .position(myLocation!!)
                        .title("My Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                )
            }
        }
    }

    // Computes distance (in meters) between two lat/lon points using the Haversine formula.
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
