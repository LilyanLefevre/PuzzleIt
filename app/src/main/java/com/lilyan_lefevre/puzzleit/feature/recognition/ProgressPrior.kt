package com.lilyan_lefevre.puzzleit.feature.recognition

import kotlin.math.max
import kotlin.math.sqrt

/**
 * Which cells of the box are already placed, read from a progress photo rectified to the box's frame (same corners).
 * A cell is placed when the photo's pattern there correlates with the box's (4 x 4 colour blocks, each channel's mean removed
 * per cell so exposure, white balance and side light do not matter, a quarter-cell shift allowed for the framing). A plain cell
 * (sky) is never called placed: the bare table would look the same. Conservative on purpose: a cell wrongly called placed
 * pushes the right answer down, a cell missed only loses the help.
 */
object ProgressPrior {
    private const val B = 4                 // colour blocks per cell side
    var MIN_CORR = 0.7f                     // correlation photo / box above which the cell is placed
    var MIN_TEXTURE = 2f                    // Lab spread inside the box's cell below which we cannot tell
    var SHIFT = 1                           // blocks (quarter cells) of framing error searched each way

    /** Per grid cell (row-major), 1 = placed, 0 = not (or cannot tell). */
    fun estimate(box: Raster, photo: Raster, grid: Grid): FloatArray {
        val nx = grid.cols * B; val ny = grid.rows * B
        val bx = blocks(box.fit(max(nx, ny) * 4), nx, ny)
        val ph = blocks(photo.fit(max(nx, ny) * 4), nx, ny)
        return FloatArray(grid.count) { cell ->
            val own = centred(bx, cellBlocks(cell, grid, 0, 0, nx, ny)!!)
            if (sqrt(own.sumOf { (it * it).toDouble() } / own.size) < MIN_TEXTURE) return@FloatArray 0f
            val p = centred(ph, cellBlocks(cell, grid, 0, 0, nx, ny)!!)
            var best = -1f
            for (dy in -SHIFT..SHIFT) for (dx in -SHIFT..SHIFT) {
                val b = cellBlocks(cell, grid, dx, dy, nx, ny)?.let { centred(bx, it) } ?: continue
                best = max(best, corr(p, b))
            }
            if (best > MIN_CORR) 1f else 0f
        }
    }

    /** Mean Lab of each of nx x ny blocks. */
    private fun blocks(r: Raster, nx: Int, ny: Int): FloatArray {
        val lab = LabImage.from(r).lab
        val sum = FloatArray(nx * ny * 3); val n = IntArray(nx * ny)
        for (y in 0 until r.h) for (x in 0 until r.w) {
            val b = (y * ny / r.h) * nx + x * nx / r.w; val i = (y * r.w + x) * 3
            for (c in 0..2) sum[b * 3 + c] += lab[i + c]
            n[b]++
        }
        for (b in n.indices) for (c in 0..2) sum[b * 3 + c] /= n[b].coerceAtLeast(1)
        return sum
    }

    /** The cell's blocks, shifted by (dx, dy) blocks; null when the shift leaves the box. */
    private fun cellBlocks(cell: Int, grid: Grid, dx: Int, dy: Int, nx: Int, ny: Int): IntArray? {
        val x0 = cell % grid.cols * B + dx; val y0 = cell / grid.cols * B + dy
        if (x0 < 0 || y0 < 0 || x0 + B > nx || y0 + B > ny) return null
        return IntArray(B * B) { (y0 + it / B) * nx + x0 + it % B }
    }

    /** The blocks' Lab values with each channel's mean over them removed. */
    private fun centred(v: FloatArray, idx: IntArray): FloatArray {
        val out = FloatArray(idx.size * 3)
        for (c in 0..2) {
            val m = idx.sumOf { v[it * 3 + c].toDouble() }.toFloat() / idx.size
            for (k in idx.indices) out[k * 3 + c] = v[idx[k] * 3 + c] - m
        }
        return out
    }

    private fun corr(a: FloatArray, b: FloatArray): Float {
        var ab = 0.0; var aa = 0.0; var bb = 0.0
        for (i in a.indices) { ab += a[i] * b[i]; aa += a[i] * a[i]; bb += b[i] * b[i] }
        return if (aa < 1e-6 || bb < 1e-6) 0f else (ab / sqrt(aa * bb)).toFloat()
    }
}
