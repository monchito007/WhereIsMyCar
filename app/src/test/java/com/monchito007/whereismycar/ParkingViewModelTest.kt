package com.monchito007.whereismycar

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.ceil

class ParkingViewModelTest {

    private fun calculateWalkingTimeMinutes(distanceInMeters: Float): Int {
        if (distanceInMeters <= 0f) return 0
        return ceil(distanceInMeters / 80.0f).toInt()
    }

    @Test
    fun testWalkingTimeCalculation() {
        val distance = 400.0f // 400 meters
        val expectedMinutes = 5
        val minutes = calculateWalkingTimeMinutes(distance)
        assertEquals(expectedMinutes, minutes)
    }

    @Test
    fun testZeroDistanceWalkingTime() {
        val minutes = calculateWalkingTimeMinutes(0.0f)
        assertEquals(0, minutes)
    }
}
