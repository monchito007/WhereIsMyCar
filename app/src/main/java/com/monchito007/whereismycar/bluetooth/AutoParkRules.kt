package com.monchito007.whereismycar.bluetooth

import com.monchito007.whereismycar.location.Geo

/**
 * Reglas puras del aparcamiento automático (sin dependencias de Android, para poder testearlas).
 */
object AutoParkRules {

    /**
     * Antigüedad máxima de una posición respecto a la desconexión. Antes de apagar el coche
     * sueles estar parado (maniobra), pero una posición más antigua puede ser de cuando aún
     * circulabas, a cientos de metros.
     */
    const val MAX_FIX_BEFORE_DISCONNECT_MS = 60_000L

    /**
     * Retraso máximo de una posición respecto a la desconexión. Si el trabajo se ejecuta tarde
     * (Android lo puede retrasar), la posición sería la de donde estás andando, no la del coche.
     */
    const val MAX_FIX_AFTER_DISCONNECT_MS = 120_000L

    /** Un aparcamiento guardado a mano poco antes de la desconexión se considera "este mismo". */
    const val RECENT_MANUAL_WINDOW_MS = 10 * 60_000L

    /** Distancia por debajo de la cual el aparcamiento existente es el mismo sitio. */
    const val SAME_PLACE_METERS = 100.0

    enum class FixCheck { OK, TOO_OLD, TOO_LATE }

    /** Comprueba si una posición tomada en [fixTimeMillis] sirve como ubicación del coche. */
    fun checkFixTime(fixTimeMillis: Long, disconnectedAtMillis: Long): FixCheck = when {
        fixTimeMillis < disconnectedAtMillis - MAX_FIX_BEFORE_DISCONNECT_MS -> FixCheck.TOO_OLD
        fixTimeMillis > disconnectedAtMillis + MAX_FIX_AFTER_DISCONNECT_MS -> FixCheck.TOO_LATE
        else -> FixCheck.OK
    }

    enum class ExistingParking {
        /** No hay aparcamiento o es de otro momento/sitio: se guarda el automático. */
        REPLACE,
        /** Se guardó a mano después de la desconexión: el usuario manda, no se toca. */
        KEEP_SAVED_AFTER_DISCONNECT,
        /** Hay uno de hace pocos minutos en el mismo sitio: no se toca (ni sus fotos ni su nota). */
        KEEP_SAME_PLACE,
    }

    /**
     * Decide si el guardado automático debe sustituir el aparcamiento actual. Sustituirlo lo pasa
     * al historial y borra sus fotos, así que nunca se hace con uno que el usuario acaba de guardar.
     */
    fun checkExistingParking(
        existingSavedAtMillis: Long?,
        existingLatitude: Double?,
        existingLongitude: Double?,
        disconnectedAtMillis: Long,
        newLatitude: Double,
        newLongitude: Double,
    ): ExistingParking {
        if (existingSavedAtMillis == null || existingLatitude == null || existingLongitude == null) {
            return ExistingParking.REPLACE
        }
        if (existingSavedAtMillis >= disconnectedAtMillis) {
            return ExistingParking.KEEP_SAVED_AFTER_DISCONNECT
        }
        val recent = existingSavedAtMillis >= disconnectedAtMillis - RECENT_MANUAL_WINDOW_MS
        if (recent) {
            val distance = Geo.haversineMeters(existingLatitude, existingLongitude, newLatitude, newLongitude)
            if (distance <= SAME_PLACE_METERS) return ExistingParking.KEEP_SAME_PLACE
        }
        return ExistingParking.REPLACE
    }
}
