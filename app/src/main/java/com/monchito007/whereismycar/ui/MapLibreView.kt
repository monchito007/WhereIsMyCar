package com.monchito007.whereismycar.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.monchito007.whereismycar.R
import com.monchito007.whereismycar.data.ParkedLocation
import com.monchito007.whereismycar.data.ParkingHistoryEntry
import com.monchito007.whereismycar.location.UserLocation
import kotlin.math.roundToInt
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.maps.widgets.CompassView
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconAnchor
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconOffset
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

enum class MapStyle(val url: String, @param:StringRes val labelRes: Int) {
    LIBERTY("https://tiles.openfreemap.org/styles/liberty", R.string.map_style_liberty),
    BRIGHT("https://tiles.openfreemap.org/styles/bright", R.string.map_style_bright),
    POSITRON("https://tiles.openfreemap.org/styles/positron", R.string.map_style_positron),
    DARK("https://tiles.openfreemap.org/styles/dark", R.string.map_style_dark),
    FIORD("https://tiles.openfreemap.org/styles/fiord", R.string.map_style_fiord),
}

private const val USER_SOURCE = "user-source"
private const val USER_LAYER = "user-layer"
private const val CAR_SOURCE = "car-source"
private const val CAR_LAYER = "car-layer"
private const val CAR_ICON = "car-icon"
private const val CAR_ICON_SIZE_DP = 42
private const val HISTORY_SOURCE = "history-source"
private const val HISTORY_LAYER = "history-layer"

private class MapHandle {
    var map: MapLibreMap? = null
    var style: Style? = null
    var userLocation: UserLocation? = null
    var parkedLocation: ParkedLocation? = null
    var didCenterOnUser = false
    var desiredStyle = MapStyle.LIBERTY
    var requestedStyle: MapStyle? = null
    var historyLocation: ParkingHistoryEntry? = null
    var centeredHistoryId: Long? = null
    var compassPosition: CompassPosition? = null
}

/**
 * Dónde colocar la brújula, en píxeles relativos al mapa: [topPx] es el borde superior
 * y [centerFromEndPx] la distancia desde el borde derecho hasta el centro de la brújula.
 */
data class CompassPosition(val topPx: Int, val centerFromEndPx: Int)

private fun applyCompassPosition(map: MapLibreMap, mapView: MapView, position: CompassPosition?) {
    position ?: return
    // La vista de la brújula es más grande que su imagen: usar su ancho real si ya está medida.
    val compassView = mapView.findCompassView()
    val compassWidth = compassView?.width?.takeIf { it > 0 }
        ?: map.uiSettings.compassImage?.intrinsicWidth
        ?: 0
    val endMargin = (position.centerFromEndPx - compassWidth / 2).coerceAtLeast(0)
    map.uiSettings.compassGravity = Gravity.TOP or Gravity.END
    map.uiSettings.setCompassMargins(0, position.topPx, endMargin, 0)
    if (compassView != null && compassView.width == 0) {
        // Aún no medida: recolocar cuando lo esté.
        compassView.post { if (compassView.width > 0) applyCompassPosition(map, mapView, position) }
    }
}

private fun View.findCompassView(): CompassView? {
    if (this is CompassView) return this
    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            getChildAt(i).findCompassView()?.let { return it }
        }
    }
    return null
}

@Composable
fun ParkingMap(
    mapStyle: MapStyle,
    userLocation: UserLocation?,
    parkedLocation: ParkedLocation?,
    historyLocation: ParkingHistoryEntry?,
    cameraCommand: CameraCommand?,
    onCameraCommandConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    compassPosition: CompassPosition? = null,
) {
    val context = LocalContext.current
    val handle = remember { MapHandle() }
    val mapView = remember {
        MapView(context).apply { onCreate(null) }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(mapView, lifecycle) {
        var started = false
        var resumed = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    mapView.onStart()
                    started = true
                }
                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
                    resumed = true
                }
                Lifecycle.Event.ON_PAUSE -> {
                    mapView.onPause()
                    resumed = false
                }
                Lifecycle.Event.ON_STOP -> {
                    mapView.onStop()
                    started = false
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (resumed) mapView.onPause()
            if (started) mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = {
            handle.desiredStyle = mapStyle
            mapView.getMapAsync { map ->
                handle.map = map
                map.uiSettings.isAttributionEnabled = true
                map.uiSettings.isLogoEnabled = true
                applyCompassPosition(map, mapView, handle.compassPosition)
                loadMapStyle(handle, map, handle.desiredStyle, mapView.context)
            }
            mapView
        },
        modifier = modifier,
        update = {
            handle.desiredStyle = mapStyle
            handle.userLocation = userLocation
            handle.parkedLocation = parkedLocation
            handle.historyLocation = historyLocation
            applyLocations(handle)
            val map = handle.map
            if (compassPosition != handle.compassPosition) {
                handle.compassPosition = compassPosition
                map?.let { applyCompassPosition(it, mapView, compassPosition) }
            }
            if (map != null && handle.requestedStyle != mapStyle) {
                loadMapStyle(handle, map, mapStyle, mapView.context)
            }
            if (historyLocation == null) {
                handle.centeredHistoryId = null
            } else if (map != null && handle.centeredHistoryId != historyLocation.id) {
                handle.centeredHistoryId = historyLocation.id
                handle.didCenterOnUser = true
                map.easeCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(historyLocation.latitude, historyLocation.longitude),
                        17.0,
                    ),
                )
            }
            if (map != null && cameraCommand != null) {
                when (cameraCommand) {
                    CameraCommand.CenterOnUser -> userLocation?.let {
                        map.easeCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(it.latitude, it.longitude),
                                17.0,
                            ),
                        )
                    }
                    CameraCommand.FitBoth -> {
                        val user = userLocation
                        val parked = parkedLocation
                        if (user != null && parked != null) {
                            val bounds = LatLngBounds.Builder()
                                .include(LatLng(user.latitude, user.longitude))
                                .include(LatLng(parked.latitude, parked.longitude))
                                .build()
                            map.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 140))
                        } else if (parked != null) {
                            map.easeCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(parked.latitude, parked.longitude),
                                    17.0,
                                ),
                            )
                        }
                    }
                }
                onCameraCommandConsumed()
            } else if (!handle.didCenterOnUser && userLocation != null && map != null) {
                handle.didCenterOnUser = true
                map.easeCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(userLocation.latitude, userLocation.longitude),
                        16.0,
                    ),
                )
            }
        },
    )
}

enum class CameraCommand {
    CenterOnUser,
    FitBoth,
}

private fun loadMapStyle(
    handle: MapHandle,
    map: MapLibreMap,
    mapStyle: MapStyle,
    context: android.content.Context,
) {
    handle.requestedStyle = mapStyle
    handle.style = null
    map.setStyle(Style.Builder().fromUri(mapStyle.url)) { style ->
        if (handle.requestedStyle != mapStyle) return@setStyle
        style.addImage(CAR_ICON, vectorBitmap(context, R.drawable.ic_car_pin, CAR_ICON_SIZE_DP))
        style.addSource(GeoJsonSource(USER_SOURCE, FeatureCollection.fromFeatures(emptyArray())))
        style.addSource(GeoJsonSource(CAR_SOURCE, FeatureCollection.fromFeatures(emptyArray())))
        style.addSource(GeoJsonSource(HISTORY_SOURCE, FeatureCollection.fromFeatures(emptyArray())))
        style.addLayer(
            CircleLayer(USER_LAYER, USER_SOURCE).withProperties(
                circleColor("#1565C0"),
                circleRadius(7.5f),
                circleStrokeColor("#FFFFFF"),
                circleStrokeWidth(3f),
            ),
        )
        style.addLayer(
            SymbolLayer(CAR_LAYER, CAR_SOURCE).withProperties(
                iconImage(CAR_ICON),
                iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                iconAllowOverlap(true),
                iconOffset(arrayOf(0f, 0f)),
            ),
        )
        style.addLayer(
            CircleLayer(HISTORY_LAYER, HISTORY_SOURCE).withProperties(
                circleColor("#EF6C00"),
                circleRadius(9f),
                circleStrokeColor("#FFFFFF"),
                circleStrokeWidth(3f),
            ),
        )
        handle.style = style
        applyLocations(handle)
    }
}

private fun applyLocations(handle: MapHandle) {
    val style = handle.style ?: return
    (style.getSource(USER_SOURCE) as? GeoJsonSource)?.setGeoJson(
        pointCollection(handle.userLocation?.longitude, handle.userLocation?.latitude),
    )
    (style.getSource(CAR_SOURCE) as? GeoJsonSource)?.setGeoJson(
        pointCollection(handle.parkedLocation?.longitude, handle.parkedLocation?.latitude),
    )
    (style.getSource(HISTORY_SOURCE) as? GeoJsonSource)?.setGeoJson(
        pointCollection(handle.historyLocation?.longitude, handle.historyLocation?.latitude),
    )
}

private fun pointCollection(longitude: Double?, latitude: Double?): FeatureCollection {
    if (longitude == null || latitude == null) {
        return FeatureCollection.fromFeatures(emptyArray())
    }
    return FeatureCollection.fromFeatures(
        arrayOf(Feature.fromGeometry(Point.fromLngLat(longitude, latitude))),
    )
}

private fun vectorBitmap(context: android.content.Context, @DrawableRes id: Int, sizeDp: Int): Bitmap {
    val size = (sizeDp * context.resources.displayMetrics.density).roundToInt()
    val drawable = ContextCompat.getDrawable(context, id) ?: error("Missing drawable $id")
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
