package com.openlauncher.app.util

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.tan

// Web Mercator, the projection every raster tile server uses. Both functions
// return a fractional tile index: the whole part picks the tile and the fraction
// gives the position inside it.
const val MAP_TILE_PX = 256

// A hair inside the true Mercator limit of 85.0511287798, so the projection
// of a clamped latitude cannot round to just below the first tile row.
private const val MAX_MERCATOR_LAT = 85.0511

fun tileX(longitude: Double, zoom: Int): Double = (longitude + 180.0) / 360.0 * (1 shl zoom)

fun tileY(latitude: Double, zoom: Int): Double {
    val radians = latitude.coerceIn(-MAX_MERCATOR_LAT, MAX_MERCATOR_LAT) * PI / 180.0
    return (1.0 - ln(tan(radians) + 1.0 / cos(radians)) / PI) / 2.0 * (1 shl zoom)
}

// Columns wrap around the date line. Rows do not — the poles have no tile.
fun wrapTileX(x: Int, zoom: Int): Int {
    val span = 1 shl zoom
    return ((x % span) + span) % span
}

fun tileRowExists(y: Int, zoom: Int): Boolean = y in 0 until (1 shl zoom)

fun osmTileUrl(x: Int, y: Int, zoom: Int): String = "https://tile.openstreetmap.org/$zoom/$x/$y.png"

// Where one tile lands inside the widget. The offsets are the top-left corner in
// pixels, measured from the top-left corner of the widget.
data class TilePlacement(val column: Int, val row: Int, val offsetX: Float, val offsetY: Float)

// Covers a widget of [widthPx] by [heightPx] with tiles of [tilePx], holding the
// position at ([centreX], [centreY]) in the middle of the widget.
fun tilePlacements(
    centreX: Double,
    centreY: Double,
    widthPx: Float,
    heightPx: Float,
    tilePx: Float,
    zoom: Int
): List<TilePlacement> {
    val baseColumn = floor(centreX).toInt()
    val baseRow = floor(centreY).toInt()
    val halfColumns = ceil(widthPx / 2f / tilePx).toInt()
    val halfRows = ceil(heightPx / 2f / tilePx).toInt()
    val placements = mutableListOf<TilePlacement>()
    for (column in baseColumn - halfColumns..baseColumn + halfColumns) {
        for (row in baseRow - halfRows..baseRow + halfRows) {
            if (!tileRowExists(row, zoom)) continue
            placements.add(
                TilePlacement(
                    column = column,
                    row = row,
                    offsetX = widthPx / 2f - ((centreX - column) * tilePx).toFloat(),
                    offsetY = heightPx / 2f - ((centreY - row) * tilePx).toFloat()
                )
            )
        }
    }
    return placements
}

private val COMPASS_POINTS =
    listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

fun headingLabel(bearing: Float): String {
    val normalized = ((bearing % 360f) + 360f) % 360f
    return COMPASS_POINTS[(normalized / 45f).roundToInt() % COMPASS_POINTS.size]
}
