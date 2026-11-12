package com.monchito007.whereismycar.location

import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
)

object Geo {
    private const val EARTH_RADIUS_METERS = 6_371_000.0
    private const val WALKING_METERS_PER_MINUTE = 5_000.0 / 60.0
    private const val PATH_FACTOR = 1.25

    fun haversineMeters(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
    ): Double {
        val dLat = Math.toRadians(endLat - startLat)
        val dLon = Math.toRadians(endLon - startLon)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(startLat)) *
            cos(Math.toRadians(endLat)) *
            sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(a))
    }

    fun walkingMinutes(distanceMeters: Double): Int {
        if (distanceMeters <= 0.0) return 0
        val adjusted = distanceMeters * PATH_FACTOR
        return (adjusted / WALKING_METERS_PER_MINUTE).roundToInt().coerceAtLeast(1)
    }

    fun formatDistance(distanceMeters: Double): String {
        return if (distanceMeters < 1000) {
            "${distanceMeters.roundToInt()} m"
        } else {
            String.format(Locale.US, "%.1f km", distanceMeters / 1000.0)
        }
    }

    fun formatDuration(minutes: Int): String {
        if (minutes < 24 * 60) return formatWalkingTime(minutes)
        val days = minutes / (24 * 60)
        return if (days == 1) "1 día" else "$days días"
    }

    fun formatWalkingTime(minutes: Int): String {
        if (minutes < 1) return "< 1 min"
        if (minutes < 60) return "$minutes min"
        val hours = minutes / 60
        val rest = minutes % 60
        return if (rest == 0) "${hours} h" else "${hours} h ${rest} min"
    }
}
