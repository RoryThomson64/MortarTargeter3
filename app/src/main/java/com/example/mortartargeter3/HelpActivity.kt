package com.example.mortartargeter3

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class HelpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        val tvHelpContent: TextView = findViewById(R.id.tvHelpContent)
        tvHelpContent.movementMethod = ScrollingMovementMethod.getInstance()

        // Get the current local 24-hour time.
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val currentTime = sdf.format(Date())

        tvHelpContent.text = """
            Help - How to Use Mortar Targeter App
            
            1. Current Location:
               - Displays your current GPS coordinates (your launch point).
               - These coordinates are used as the starting point for all trajectory calculations.
               
            2. Wind Speed & Wind Direction:
               - These values are automatically retrieved from an online weather service.
               - Wind direction is measured in degrees from North.
               - Accurate wind data is critical for calculating an accurate firing solution.
               
            3. Shell Weight & Muzzle Velocity:
               - Enter the shell’s weight (in grams) and its launch velocity (m/s).
               - These parameters determine the projectile’s trajectory.
               
            4. Height Difference:
               - Adjust the slider to specify the vertical difference (in meters) between your launch point and the target.
               - Negative values indicate the target is below the launch point; positive values indicate above.
               - (Note: Extreme height differences can affect the trajectory calculation.)
               
            5. Targeting Mode:
               - Auto Mode: Directly input the target’s geographic coordinates.
               - Manual Mode: Enter the target’s horizontal distance and bearing.
               - In manual mode, if you select a location on the map, the “Targeting From” fields are updated automatically.
               
            6. Map Picker:
               - Tap "Select Target on Map" to open the map interface.
               - Tap on the map to drop a pin at the desired target location.
               - Use "Move Mortar" to update your mortar (launch) location to that pin if needed.
               
            7. Refresh Location:
               - Updates your current GPS location.
               - Use this if you have moved since the app started.
               
            8. Calculate Maximum Range:
               - Computes the maximum range achievable (under level ground and no wind conditions) based on your shell parameters.
               - This value is provided for reference.
               
            9. Settings:
               - Allows you to adjust drag parameters (frontal/side drag coefficients, effective areas, and air density).
               - Defaults are set for a Nerf Vortex but can be tuned for other projectiles.
               
            10. Calculation Results:
               - Target Distance: The horizontal distance from your mortar location to the target.
               - Target Bearing: The compass direction (degrees from North) from your launch point to the target.
               - Recommended Elevation: The firing elevation angle calculated to hit the target.
               - Recommended Bearing: The firing bearing adjusted for wind and drag.
               - Drag-Free Elevation (High-Angle Lob): The ideal elevation if no drag were present.
               - Time to Impact: Estimated flight time of the shell.
               - Impact Range: The horizontal distance the shell will travel before impact.
               - Impact Bearing: The actual compass direction of the impact point relative to your launch point.
               - Impact Location: The geographic coordinates (latitude, longitude) where the shell is predicted to land.
               - Shell Weight & Wind: Reiterates your input parameters for reference.
               
            11. Confirm Fire:
               - After reviewing the firing solution, tap the "Confirm Fire" button.
               - This saves the target’s location (where the shell will land) so you know where to recover your shells.
               
            12. View Fired Shots:
               - This button is always visible.
               - It opens a list of all confirmed shots.
               - You can tap a shot to view its location in Google Maps or long-press to delete a shot (with confirmation).
               
            Additional Information:
               - Local Time: $currentTime (24-hour format) – for your reference.
               
            Use these details to accurately set up your mortar. Tap the X button at the top-right to exit Help.
        """.trimIndent()

        val btnCloseHelp: ImageButton = findViewById(R.id.btnCloseHelp)
        btnCloseHelp.setOnClickListener { finish() }
    }
}
