package com.monchito007.whereismycar.location

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.monchito007.whereismycar.R

object NavigationIntents {
    fun openWalkingNavigation(context: Context, latitude: Double, longitude: Double): Boolean {
        val label = Uri.encode(context.getString(R.string.my_car))
        val geo = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($label)")
        val viewIntent = Intent(Intent.ACTION_VIEW, geo)

        val googleMaps = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("google.navigation:q=$latitude,$longitude&mode=w"),
        )
        val waze = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://waze.com/ul?ll=$latitude,$longitude&navigate=yes"),
        )
        val osmand = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("osmand.navigation:q=$latitude,$longitude"),
        )

        val chooser = Intent.createChooser(viewIntent, context.getString(R.string.navigate_with)).apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(googleMaps, waze, osmand))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(chooser)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
