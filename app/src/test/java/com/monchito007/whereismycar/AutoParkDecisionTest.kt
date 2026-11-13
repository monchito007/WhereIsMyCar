package com.monchito007.whereismycar

import com.monchito007.whereismycar.bluetooth.AutoParkAction
import com.monchito007.whereismycar.bluetooth.CarBluetoothConfig
import com.monchito007.whereismycar.bluetooth.CarConnectionEvent
import com.monchito007.whereismycar.bluetooth.decideAutoParkAction
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoParkDecisionTest {

    private val car = "AA:BB:CC:DD:EE:FF"
    private val activeConfig = CarBluetoothConfig(enabled = true, deviceAddress = car, deviceName = "Mi coche")

    @Test
    fun `disconnect of selected car starts auto park`() {
        assertEquals(AutoParkAction.START, decideAutoParkAction(CarConnectionEvent.DISCONNECTED, car, activeConfig))
    }

    @Test
    fun `reconnect of selected car cancels pending auto park`() {
        assertEquals(AutoParkAction.CANCEL, decideAutoParkAction(CarConnectionEvent.CONNECTED, car, activeConfig))
    }

    @Test
    fun `address comparison is case insensitive`() {
        assertEquals(
            AutoParkAction.START,
            decideAutoParkAction(CarConnectionEvent.DISCONNECTED, car.lowercase(), activeConfig)
        )
    }

    @Test
    fun `other devices are ignored`() {
        assertEquals(
            AutoParkAction.IGNORE,
            decideAutoParkAction(CarConnectionEvent.DISCONNECTED, "11:22:33:44:55:66", activeConfig)
        )
    }

    @Test
    fun `nothing happens when disabled`() {
        val disabled = activeConfig.copy(enabled = false)
        assertEquals(AutoParkAction.IGNORE, decideAutoParkAction(CarConnectionEvent.DISCONNECTED, car, disabled))
    }

    @Test
    fun `nothing happens when no car selected`() {
        val noCar = CarBluetoothConfig(enabled = true)
        assertEquals(AutoParkAction.IGNORE, decideAutoParkAction(CarConnectionEvent.DISCONNECTED, car, noCar))
    }

    @Test
    fun `null device address is ignored`() {
        assertEquals(AutoParkAction.IGNORE, decideAutoParkAction(CarConnectionEvent.DISCONNECTED, null, activeConfig))
    }

    @Test
    fun `disconnect caused by turning off phone bluetooth is ignored`() {
        assertEquals(
            AutoParkAction.IGNORE,
            decideAutoParkAction(CarConnectionEvent.DISCONNECTED, car, activeConfig, phoneBluetoothOn = false)
        )
    }
}
