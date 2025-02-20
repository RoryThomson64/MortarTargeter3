package com.example.mortartargeter3

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ShotsManager {
    private const val PREFS_NAME = "ShotsPrefs"
    private const val KEY_SHOTS = "shots_list"

    fun getShots(context: Context): MutableList<Shot> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val shotsJson = prefs.getString(KEY_SHOTS, "[]")
        val shotsArray = JSONArray(shotsJson)
        val shotsList = mutableListOf<Shot>()
        for (i in 0 until shotsArray.length()) {
            val obj = shotsArray.getJSONObject(i)
            val shot = Shot(
                id = obj.getLong("id"),
                impactLat = obj.getDouble("impactLat"),
                impactLon = obj.getDouble("impactLon"),
                timestamp = obj.getLong("timestamp")
            )
            shotsList.add(shot)
        }
        return shotsList
    }

    fun saveShots(context: Context, shots: List<Shot>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        for (shot in shots) {
            val obj = JSONObject()
            obj.put("id", shot.id)
            obj.put("impactLat", shot.impactLat)
            obj.put("impactLon", shot.impactLon)
            obj.put("timestamp", shot.timestamp)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_SHOTS, jsonArray.toString()).apply()
    }

    fun addShot(context: Context, shot: Shot) {
        val shots = getShots(context)
        shots.add(shot)
        saveShots(context, shots)
    }

    fun deleteShot(context: Context, shotId: Long) {
        val shots = getShots(context)
        val filtered = shots.filter { it.id != shotId }
        saveShots(context, filtered)
    }
}
