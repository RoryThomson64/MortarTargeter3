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
               • Displays your current GPS coordinates (your launch point).
               • These coordinates are used as the starting point for all trajectory calculations.

            2. Wind Speed & Wind Direction:
               • Automatically retrieved from an online weather service.
               • Wind direction is measured in degrees from North.
               • Accurate wind data is critical for computing the correct firing solution.

            3. Shell Weight & Muzzle Velocity:
               • Enter the shell’s weight (in grams) and its launch velocity (m/s).
               • These parameters determine the projectile’s trajectory.

            4. Height Difference:
               • Adjust the slider to specify the vertical difference (in meters) between your launch point and the target.
               • Negative values indicate the target is below the launch point; positive values indicate above.
               • Extreme differences can affect trajectory calculations.

            5. Targeting Mode:
               • Coordinates (Auto Mode): Directly enter the target’s latitude and longitude.
               • Manual Mode: Enter the target’s horizontal distance and bearing.
               • Plus Code Mode (Open Location Plus):
                 - Plus Codes are an open-source, offline geocoding system.
                 - Instead of full coordinates, you can share a short Plus Code (e.g. "7FG9V2W8+Q2").
                 - This code represents the center of a geographic area.
                 - To use this mode, select the "Plus Code" option and enter your Plus Code in the provided field.
                 - The app decodes the Plus Code offline to determine the target's coordinates.
                 - Plus Codes are especially useful over radio, as they are shorter and easier to communicate than numeric coordinates.

            6. Map Picker:
               • Tap "Select Target on Map" to open the map interface.
               • Drop a pin on the desired target location.
               • If needed, update your mortar (launch) location by tapping the pin.

            7. Refresh Location:
               • Updates your current GPS location. Use this if you have moved since launching the app.

            8. Calculate Maximum Range:
               • Computes the maximum achievable range (assuming level ground and no wind) based on your shell parameters.
               • This value is provided as a reference.

            9. Settings:
               • Adjust drag parameters such as frontal and side drag coefficients, effective areas, and air density.
               • Defaults are provided but can be tuned for different projectiles.

           10. Calculation Results:
               • Displays target distance, target bearing, recommended firing elevation and adjusted bearing.
               • Shows additional details such as drag-free elevation, estimated flight time, impact range, and impact location.
               • Reiterates input parameters (shell weight and wind conditions).

           11. Confirm Fire:
               • Once you review the firing solution, tap "Confirm Fire" to save the target's location.
               • This saved shot can later be reviewed in the "View Fired Shots" section.

           12. View Fired Shots:
               • Displays a list of all confirmed shots.
               • Tap a shot to view its location in Google Maps or long-press to delete it (with confirmation).
               
            Open Location Plus (Plus Codes):
               • Open Location Plus is our implementation of Plus Codes – an open-source, offline geocoding system.
               • It converts a short code (e.g. "7FG9V2W8+Q2") into a set of coordinates representing the center of that area.
               • This method is ideal for sharing locations over radio or in areas with limited connectivity.
               • Simply select the "Plus Code" targeting mode and enter your code.
               
            Use these details to accurately set up your mortar. Tap the X button at the top-right to exit Help.
        """.trimIndent()

        val btnCloseHelp: ImageButton = findViewById(R.id.btnCloseHelp)
        btnCloseHelp.setOnClickListener { finish() }
    }
}
