package com.monchito007.whereismycar.bluetooth

enum class CarConnectionEvent { CONNECTED, DISCONNECTED }

enum class AutoParkAction {
    /** El coche se ha desconectado: obtener ubicación y guardar tras el debounce. */
    START,
    /** El coche se ha vuelto a conectar: cancelar un guardado pendiente (microcorte). */
    CANCEL,
    IGNORE
}

/**
 * Decide qué hacer ante un evento Bluetooth. Lógica pura para poder testearla
 * sin dependencias de Android.
 *
 * [phoneBluetoothOn] indica si el Bluetooth del móvil seguía encendido al recibir el evento.
 * Al apagarlo (o activar el modo avión) el sistema también envía ACL_DISCONNECTED, pero eso no
 * significa que hayas aparcado, así que esa desconexión se ignora.
 */
fun decideAutoParkAction(
    event: CarConnectionEvent,
    eventDeviceAddress: String?,
    config: CarBluetoothConfig,
    phoneBluetoothOn: Boolean = true,
): AutoParkAction {
    if (!config.isActive || eventDeviceAddress == null) return AutoParkAction.IGNORE
    if (!eventDeviceAddress.equals(config.deviceAddress, ignoreCase = true)) return AutoParkAction.IGNORE
    return when (event) {
        CarConnectionEvent.DISCONNECTED ->
            if (phoneBluetoothOn) AutoParkAction.START else AutoParkAction.IGNORE
        CarConnectionEvent.CONNECTED -> AutoParkAction.CANCEL
    }
}
