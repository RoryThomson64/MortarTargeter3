package com.example.mortartargeter3

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class ShotsListActivity : AppCompatActivity() {

    private lateinit var listViewShots: ListView
    private lateinit var btnCloseShots: ImageButton
    private lateinit var shotsList: MutableList<Shot>
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shots_list)

        listViewShots = findViewById(R.id.listViewShots)
        btnCloseShots = findViewById(R.id.btnCloseShots)
        btnCloseShots.setOnClickListener { finish() }

        loadShots()

        listViewShots.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val shot = shotsList[position]
                // Launch Google Maps to view the shot location.
                val uri = Uri.parse("geo:${shot.impactLat},${shot.impactLon}?q=${shot.impactLat},${shot.impactLon}(Shot)")
                val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                mapIntent.setPackage("com.google.android.apps.maps")
                startActivity(mapIntent)
            }

        listViewShots.onItemLongClickListener =
            AdapterView.OnItemLongClickListener { _, _, position, _ ->
                val shot = shotsList[position]
                AlertDialog.Builder(this)
                    .setTitle("Delete Shot")
                    .setMessage("Are you sure you want to delete this shot?")
                    .setPositiveButton("Yes") { _, _ ->
                        ShotsManager.deleteShot(this, shot.id)
                        loadShots()
                    }
                    .setNegativeButton("No", null)
                    .show()
                true
            }
    }

    private fun loadShots() {
        shotsList = ShotsManager.getShots(this)
        // Format the shot timestamp using local 24-hour time.
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val shotDescriptions = shotsList.map { shot ->
            "Shot at ${"%.6f".format(shot.impactLat)}, ${"%.6f".format(shot.impactLon)}\nTime: ${sdf.format(Date(shot.timestamp))}"
        }
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, shotDescriptions)
        listViewShots.adapter = adapter
    }
}
