package com.example.mortartargeter3

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HelpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        val tvHelpContent: TextView = findViewById(R.id.tvHelpContent)
        tvHelpContent.movementMethod = ScrollingMovementMethod.getInstance()
        tvHelpContent.text = """
            Help - How to Use Mortar Targeter App
            
            1. Current Location:
               - Displays your current GPS coordinates.
               
            2. Wind Speed & Wind Direction:
               - Fetched automatically from a weather service.
               - Wind direction is given in degrees (from North).
               
            3. Shell Weight & Muzzle Velocity:
               - Enter the shell’s weight (in grams) and its launch velocity (m/s).
               
            4. Height Difference:
               - Adjust the slider to specify the elevation difference between your location and the target.
               
            5. Targeting Mode:
               - Auto Mode: Enter target coordinates.
               - Manual Mode: Enter distance and bearing.
               - In manual mode, selecting a target on the map updates the “Targeting From” coordinates.
               
            6. Map Picker:
               - Use "Select Target on Map" to pick a target location.
               - In Map Picker, you can also use "Move Mortar" to update your mortar’s location to the dropped pin.
               
            7. Refresh Location:
               - Updates your current GPS location.
               
            8. Calculate Maximum Range:
               - Computes the maximum range achievable based on shell parameters.
               
            9. Settings:
               - Adjust drag parameters (frontal/side drag coefficients, areas, air density).
               - Defaults are set for a Nerf Vortex.
               
            10. Calculation Results:
               - Displays the computed firing solution including:
                 • Target Distance & Bearing.
                 • Recommended Elevation & Bearing for the mortar.
                 • Drag-Free Elevation (ideal without drag).
                 • Time to Impact, Impact Range, and Impact Location.
            
            Use these details to adjust your mortar for best accuracy.
            Tap the X button at the top-right to exit.
        """.trimIndent()

        val btnCloseHelp: ImageButton = findViewById(R.id.btnCloseHelp)
        btnCloseHelp.setOnClickListener { finish() }
    }
}
