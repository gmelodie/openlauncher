package com.openlauncher.app.ui.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.openlauncher.app.BuildConfig
import com.openlauncher.app.data.MAX_MAP_ZOOM
import com.openlauncher.app.data.MIN_MAP_ZOOM
import com.openlauncher.app.ui.theme.GruvLightBg0
import com.openlauncher.app.ui.theme.GruvLightBg2
import com.openlauncher.app.ui.theme.GruvLightFg0
import com.openlauncher.app.ui.theme.widgetInk
import com.openlauncher.app.ui.theme.widgetSubInk
import com.openlauncher.app.util.LocationData
import com.openlauncher.app.util.MAP_TILE_PX
import com.openlauncher.app.util.headingLabel
import com.openlauncher.app.util.osmTileUrl
import com.openlauncher.app.util.speedIn
import com.openlauncher.app.util.tilePlacements
import com.openlauncher.app.util.tileX
import com.openlauncher.app.util.tileY
import com.openlauncher.app.util.wrapTileX
import kotlin.math.floor

// The tile server policy asks for an application that identifies itself.
private const val TILE_USER_AGENT =
    "OpenLauncher/${BuildConfig.VERSION_NAME} (https://github.com/gmelodie/openlauncher)"

// A tile drawn at its own 256 physical pixels is too fine to read from the
// driver's seat, so each one covers a larger area of the screen.
private const val TILE_MAGNIFY = 1.5f

// Night keeps the daylight map and dims it. Inverting the colours turns the
// white road fill dark as well, which loses the roads.
private val NIGHT_TILES = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            0.45f, 0f, 0f, 0f, 0f,
            0f, 0.45f, 0f, 0f, 0f,
            0f, 0f, 0.45f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
)

@Composable
fun MapWidget(
    location: LocationData?,
    bearing: Float,
    zoom: Int,
    isMetric: Boolean,
    accent: Color,
    navLabel: String,
    isEditing: Boolean = false,
    isDayMode: Boolean = false,
    onOpenNav: () -> Unit = {},
    onZoomChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (location == null) {
        NavFallback(
            accent = accent,
            navLabel = navLabel,
            note = "WAITING FOR GPS",
            isEditing = isEditing,
            isDayMode = isDayMode,
            onOpenNav = onOpenNav,
            modifier = modifier
        )
        return
    }

    var tilesFailed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clipToBounds()
            .background(if (isDayMode) GruvLightBg2 else Color(0xFF101010))
            .clickable(enabled = !isEditing, onClick = onOpenNav)
    ) {
        TileGrid(
            location = location,
            zoom = zoom,
            isDayMode = isDayMode,
            onCentreTileState = { failed -> tilesFailed = failed }
        )
        HeadingMarker(bearing = bearing, accent = accent)
        MapOverlay(
            location = location,
            bearing = bearing,
            zoom = zoom,
            isMetric = isMetric,
            isDayMode = isDayMode,
            offline = tilesFailed,
            isEditing = isEditing,
            onZoomChange = onZoomChange
        )
    }
}

@Composable
private fun BoxScope.TileGrid(
    location: LocationData,
    zoom: Int,
    isDayMode: Boolean,
    onCentreTileState: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val tilePx = MAP_TILE_PX * TILE_MAGNIFY
    val tileSize = with(density) { tilePx.toDp() }

    BoxWithConstraints(modifier = Modifier.matchParentSize()) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val centreX = tileX(location.longitude, zoom)
        val centreY = tileY(location.latitude, zoom)

        for (tile in tilePlacements(centreX, centreY, widthPx, heightPx, tilePx, zoom)) {
            val isCentre = tile.column == floor(centreX).toInt() && tile.row == floor(centreY).toInt()
            val url = osmTileUrl(wrapTileX(tile.column, zoom), tile.row, zoom)
            key(zoom, tile.column, tile.row) {
                val request = remember(url) {
                    ImageRequest.Builder(context)
                        .data(url)
                        .setHeader("User-Agent", TILE_USER_AGENT)
                        .crossfade(false)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    colorFilter = if (isDayMode) null else NIGHT_TILES,
                    onError = { if (isCentre) onCentreTileState(true) },
                    onSuccess = { if (isCentre) onCentreTileState(false) },
                    modifier = Modifier
                        .absoluteOffset(
                            x = with(density) { tile.offsetX.toDp() },
                            y = with(density) { tile.offsetY.toDp() }
                        )
                        .size(tileSize)
                )
            }
        }
    }
}

@Composable
private fun BoxScope.HeadingMarker(bearing: Float, accent: Color) {
    Canvas(
        modifier = Modifier
            .align(Alignment.Center)
            .size(34.dp)
            .graphicsLayer { rotationZ = bearing }
    ) {
        val centreX = size.width / 2f
        val centreY = size.height / 2f
        drawCircle(color = accent.copy(alpha = 0.22f), radius = size.minDimension / 2f)
        val arrow = Path().apply {
            moveTo(centreX, centreY - size.minDimension * 0.36f)
            lineTo(centreX + size.minDimension * 0.24f, centreY + size.minDimension * 0.30f)
            lineTo(centreX, centreY + size.minDimension * 0.12f)
            lineTo(centreX - size.minDimension * 0.24f, centreY + size.minDimension * 0.30f)
            close()
        }
        drawPath(path = arrow, color = Color.White)
        drawPath(path = arrow, color = accent, style = Stroke(width = 2.dp.toPx()))
        drawCircle(color = accent, radius = 2.5.dp.toPx(), center = Offset(centreX, centreY))
    }
}

@Composable
private fun BoxScope.MapOverlay(
    location: LocationData,
    bearing: Float,
    zoom: Int,
    isMetric: Boolean,
    isDayMode: Boolean,
    offline: Boolean,
    isEditing: Boolean,
    onZoomChange: (Int) -> Unit
) {
    val ink = if (isDayMode) GruvLightFg0 else GruvLightBg0
    val scrim = if (isDayMode) GruvLightBg0.copy(alpha = 0.82f) else Color(0xFF101010).copy(alpha = 0.82f)
    val speed = location.speedMps.speedIn(isMetric)
    val unit = if (isMetric) "km/h" else "mph"

    Column(
        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
        horizontalAlignment = Alignment.End
    ) {
        ZoomButton(Icons.Default.Add, "Zoom in", ink, scrim, isEditing) {
            onZoomChange((zoom + 1).coerceAtMost(MAX_MAP_ZOOM))
        }
        Spacer(Modifier.height(4.dp))
        ZoomButton(Icons.Default.Remove, "Zoom out", ink, scrim, isEditing) {
            onZoomChange((zoom - 1).coerceAtLeast(MIN_MAP_ZOOM))
        }
    }

    Row(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .background(scrim)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = "%.0f".format(speed),
            color = ink,
            fontSize = 20.sp
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = unit,
            color = ink.copy(alpha = 0.8f),
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = if (offline) "MAP OFFLINE" else headingLabel(bearing),
            color = ink,
            fontSize = 13.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 2.dp)
        )
    }

    Text(
        text = "© OpenStreetMap",
        color = ink.copy(alpha = 0.75f),
        fontSize = 8.sp,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 6.dp, bottom = 28.dp)
    )
}

@Composable
private fun ZoomButton(
    icon: ImageVector,
    description: String,
    ink: Color,
    scrim: Color,
    isEditing: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = !isEditing,
        modifier = Modifier.size(30.dp).background(scrim)
    ) {
        Icon(icon, description, tint = ink, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun NavFallback(
    accent: Color,
    navLabel: String,
    note: String,
    isEditing: Boolean,
    isDayMode: Boolean,
    onOpenNav: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(enabled = !isEditing, onClick = onOpenNav).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Navigation,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(38.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = navLabel.uppercase(),
            color = widgetInk(isDayMode),
            fontSize = 14.sp,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = note,
            color = widgetSubInk(isDayMode),
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )
    }
}
