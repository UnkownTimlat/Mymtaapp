package com.example.data.model

/**
 * GTA:SA MTA coordinate conversion calibration.
 * Standard GTA San Andreas world coordinates span:
 * X: -3000.0 (West) to +3000.0 (East)
 * Y: -3000.0 (South) to +3000.0 (North)
 *
 * Configurable so that server administrators or custom radar bounds can be calibrated.
 */
data class MapCalibrationConfig(
    val minX: Float = -3000f,
    val maxX: Float = 3000f,
    val minY: Float = -3000f,
    val maxY: Float = 3000f
) {
    /**
     * Converts GTA MTA world coordinates (x, y) to normalized map coordinates (0.0 to 1.0).
     * u = 0.0 (left edge), u = 1.0 (right edge)
     * v = 0.0 (top edge / North), v = 1.0 (bottom edge / South)
     */
    fun worldToNormalized(worldX: Float, worldY: Float): Pair<Float, Float> {
        val width = (maxX - minX).takeIf { it > 0f } ?: 6000f
        val height = (maxY - minY).takeIf { it > 0f } ?: 6000f

        val u = ((worldX - minX) / width).coerceIn(0f, 1f)
        // MTA Y coordinate is inverted relative to standard screen coordinate system:
        // +Y is North (top of map / v=0.0), -Y is South (bottom of map / v=1.0)
        val v = ((maxY - worldY) / height).coerceIn(0f, 1f)

        return Pair(u, v)
    }

    /**
     * Converts normalized (u, v) back to GTA MTA world coordinates (x, y).
     */
    fun normalizedToWorld(u: Float, v: Float): Pair<Float, Float> {
        val width = maxX - minX
        val height = maxY - minY
        val x = minX + (u * width)
        val y = maxY - (v * height)
        return Pair(x, y)
    }
}
