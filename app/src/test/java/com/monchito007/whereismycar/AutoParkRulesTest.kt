package com.monchito007.whereismycar

import com.monchito007.whereismycar.bluetooth.AutoParkRules
import com.monchito007.whereismycar.bluetooth.AutoParkRules.ExistingParking
import com.monchito007.whereismycar.bluetooth.AutoParkRules.FixCheck
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoParkRulesTest {

    private val disconnectedAt = 1_000_000_000L
    private val lat = 40.4168
    private val lon = -3.7038

    // --- Antigüedad de la posición ---

    @Test
    fun `fix taken right at disconnect is ok`() {
        assertEquals(FixCheck.OK, AutoParkRules.checkFixTime(disconnectedAt + 5_000, disconnectedAt))
    }

    @Test
    fun `fix taken while maneuvering shortly before disconnect is ok`() {
        assertEquals(FixCheck.OK, AutoParkRules.checkFixTime(disconnectedAt - 30_000, disconnectedAt))
    }

    @Test
    fun `stale last known fix is rejected`() {
        assertEquals(FixCheck.TOO_OLD, AutoParkRules.checkFixTime(disconnectedAt - 10 * 60_000, disconnectedAt))
    }

    @Test
    fun `fix obtained long after disconnect is rejected`() {
        assertEquals(FixCheck.TOO_LATE, AutoParkRules.checkFixTime(disconnectedAt + 5 * 60_000, disconnectedAt))
    }

    // --- Aparcamiento existente ---

    private fun check(savedAt: Long?, existingLat: Double? = lat, existingLon: Double? = lon, newLat: Double = lat) =
        AutoParkRules.checkExistingParking(
            existingSavedAtMillis = savedAt,
            existingLatitude = existingLat,
            existingLongitude = existingLon,
            disconnectedAtMillis = disconnectedAt,
            newLatitude = newLat,
            newLongitude = lon,
        )

    @Test
    fun `no existing parking is replaced`() {
        assertEquals(ExistingParking.REPLACE, check(savedAt = null, existingLat = null, existingLon = null))
    }

    @Test
    fun `parking saved manually after disconnect is kept`() {
        assertEquals(ExistingParking.KEEP_SAVED_AFTER_DISCONNECT, check(savedAt = disconnectedAt + 20_000))
    }

    @Test
    fun `parking saved after disconnect is kept even if far away`() {
        assertEquals(
            ExistingParking.KEEP_SAVED_AFTER_DISCONNECT,
            check(savedAt = disconnectedAt + 20_000, newLat = lat + 0.01),
        )
    }

    @Test
    fun `parking saved manually just before in the same place is kept`() {
        assertEquals(ExistingParking.KEEP_SAME_PLACE, check(savedAt = disconnectedAt - 2 * 60_000))
    }

    @Test
    fun `recent parking in another place is replaced`() {
        // ~1,1 km al norte
        assertEquals(ExistingParking.REPLACE, check(savedAt = disconnectedAt - 2 * 60_000, newLat = lat + 0.01))
    }

    @Test
    fun `old parking in the same place is replaced`() {
        assertEquals(ExistingParking.REPLACE, check(savedAt = disconnectedAt - 24 * 60 * 60_000L))
    }
}
