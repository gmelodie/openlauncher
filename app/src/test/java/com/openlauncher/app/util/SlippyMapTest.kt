package com.openlauncher.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.floor

class SlippyMapTest {

    // Belo Horizonte, checked against the tile the OpenStreetMap server serves.
    private val latitude = -19.92
    private val longitude = -43.94

    @Test
    fun `a known position maps to its published tile`() {
        assertEquals(24768, floor(tileX(longitude, 16)).toInt())
        assertEquals(36469, floor(tileY(latitude, 16)).toInt())
    }

    @Test
    fun `one zoom step out halves the tile index`() {
        assertEquals(12384, floor(tileX(longitude, 15)).toInt())
        assertEquals(18234, floor(tileY(latitude, 15)).toInt())
    }

    @Test
    fun `the centre of the world sits at the middle of the grid`() {
        assertEquals(8.0, tileX(0.0, 4), 0.0001)
        assertEquals(8.0, tileY(0.0, 4), 0.0001)
    }

    @Test
    fun `columns wrap around the date line`() {
        assertEquals(15, wrapTileX(-1, 4))
        assertEquals(0, wrapTileX(16, 4))
        assertEquals(7, wrapTileX(7, 4))
    }

    @Test
    fun `rows past the poles have no tile`() {
        assertTrue(tileRowExists(0, 4))
        assertTrue(tileRowExists(15, 4))
        assertFalse(tileRowExists(-1, 4))
        assertFalse(tileRowExists(16, 4))
    }

    @Test
    fun `a latitude past the mercator limit stays inside the grid`() {
        val row = tileY(89.9, 4)
        assertTrue(row >= 0.0 && row < 16.0)
    }

    @Test
    fun `the heading label rounds to the nearest point`() {
        assertEquals("N", headingLabel(0f))
        assertEquals("N", headingLabel(359f))
        assertEquals("NE", headingLabel(44f))
        assertEquals("S", headingLabel(180f))
        assertEquals("NW", headingLabel(-45f))
    }

    @Test
    fun `the tiles cover every pixel of the cell`() {
        val width = 333f
        val height = 540f
        val tile = 384f
        val placements = tilePlacements(tileX(longitude, 16), tileY(latitude, 16), width, height, tile, 16)

        assertTrue(placements.isNotEmpty())
        assertTrue(placements.minOf { it.offsetX } <= 0f)
        assertTrue(placements.minOf { it.offsetY } <= 0f)
        assertTrue(placements.maxOf { it.offsetX + tile } >= width)
        assertTrue(placements.maxOf { it.offsetY + tile } >= height)
    }

    @Test
    fun `the vehicle sits at the centre of the cell`() {
        val width = 333f
        val height = 540f
        val tile = 384f
        val centreX = tileX(longitude, 16)
        val centreY = tileY(latitude, 16)
        val placements = tilePlacements(centreX, centreY, width, height, tile, 16)
        val centre = placements.first {
            it.column == floor(centreX).toInt() && it.row == floor(centreY).toInt()
        }

        val vehicleX = centre.offsetX + (centreX - floor(centreX)).toFloat() * tile
        val vehicleY = centre.offsetY + (centreY - floor(centreY)).toFloat() * tile
        assertEquals(width / 2f, vehicleX, 0.01f)
        assertEquals(height / 2f, vehicleY, 0.01f)
    }

    @Test
    fun `a cell over the north pole drops the rows that have no tile`() {
        val placements = tilePlacements(tileX(0.0, 2), tileY(85.0, 2), 800f, 800f, 384f, 2)
        assertTrue(placements.all { tileRowExists(it.row, 2) })
        assertTrue(placements.isNotEmpty())
    }

    @Test
    fun `the tile url carries zoom column and row in order`() {
        assertEquals("https://tile.openstreetmap.org/16/24768/36469.png", osmTileUrl(24768, 36469, 16))
    }
}
