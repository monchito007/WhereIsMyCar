package com.monchito007.whereismycar

import com.monchito007.whereismycar.location.Geo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun haversine_knownDistance() {
        val meters = Geo.haversineMeters(40.4168, -3.7038, 40.4178, -3.7038)
        assertTrue(meters in 100.0..130.0)
    }

    @Test
    fun walkingMinutes_scalesWithDistance() {
        assertEquals(0, Geo.walkingMinutes(0.0))
        assertTrue(Geo.walkingMinutes(400.0) >= 1)
    }
}
