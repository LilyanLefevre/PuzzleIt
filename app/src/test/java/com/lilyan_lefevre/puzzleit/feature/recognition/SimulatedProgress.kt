package com.lilyan_lefevre.puzzleit.feature.recognition

import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A puzzle in progress for the benchmarks: which cells are on the table (the border first, then islands, like a person builds it)
 * and a progress photo of it already framed to the box's corners, with what the framing and the camera leave: each corner
 * dragged up to [FRAMING] cell off, white balance, exposure, side light, grain and the bare table elsewhere.
 * PUZZLE_PROGRESS = comma list of the shares of cells placed to measure next to the baseline, "o" suffix = the matcher gets
 * the true cells instead of the estimate (e.g. "0.3,0.6,0.6o"); PUZZLE_PLACED_COST tunes the matcher's cost.
 */
class SimulatedProgress(val fraction: Float, val oracle: Boolean) {

    /** Estimator quality: placed cells found, bare cells wrongly called placed, pieces in hand called placed. */
    private var placedCells = 0; private var placedFound = 0; private var bareCells = 0; private var bareWrong = 0; private var inHand = 0; private var inHandWrong = 0

    override fun toString() = "progress ${(fraction * 100).roundToInt()}%${if (oracle) " (truth)" else ""}".padEnd(20)
    fun report() = "$this cost ${PieceMatcher.PLACED_COST}: placed found ${pct(placedFound, placedCells)}, bare called placed ${pct(bareWrong, bareCells)}, piece in hand called placed $inHandWrong/$inHand"
    private fun pct(a: Int, b: Int) = "${100 * a / b.coerceAtLeast(1)}%"

    /** The prior handed to the matcher (the estimate from a simulated photo, or the truth), counting the estimator's errors. */
    fun prior(box: Raster, grid: Grid, loose: Set<Int>, seed: Long): FloatArray {
        val rnd = Random(seed)
        val on = placed(grid, fraction, loose, rnd)
        val est = if (oracle) FloatArray(on.size) { if (on[it]) 1f else 0f } else ProgressPrior.estimate(box, photo(box, grid, on, rnd), grid)
        for (c in on.indices) {
            if (on[c]) { placedCells++; if (est[c] > 0.5f) placedFound++ } else { bareCells++; if (est[c] > 0.5f) bareWrong++ }
            if (c in loose) { inHand++; if (est[c] > 0.5f) inHandWrong++ }
        }
        return est
    }

    companion object {
        /** null = the baseline (no progress photo), then the variants asked by PUZZLE_PROGRESS. */
        val variants: List<SimulatedProgress?> = listOf<SimulatedProgress?>(null) +
            System.getenv("PUZZLE_PROGRESS").orEmpty().split(',').filter { it.isNotBlank() }.map { SimulatedProgress(it.trim().removeSuffix("o").toFloat(), it.trim().endsWith("o")) }

        init {
            System.getenv("PUZZLE_PLACED_COST")?.toDoubleOrNull()?.let { PieceMatcher.PLACED_COST = it }
            System.getenv("PUZZLE_PROGRESS_CORR")?.toFloatOrNull()?.let { ProgressPrior.MIN_CORR = it }
            System.getenv("PUZZLE_PROGRESS_TEXTURE")?.toFloatOrNull()?.let { ProgressPrior.MIN_TEXTURE = it }
        }

        /** How far (in cells) each framed corner may land from the puzzle's real corner. */
        val FRAMING = System.getenv("PUZZLE_PROGRESS_FRAMING")?.toDoubleOrNull() ?: 0.3

        private val tables =listOf(intArrayOf(120, 86, 60), intArrayOf(48, 36, 30), intArrayOf(205, 195, 185), intArrayOf(70, 72, 78), intArrayOf(40, 90, 50))

        /** The cells on the table: border first, then islands grown from random seeds, never the [loose] ones. */
        fun placed(grid: Grid, frac: Float, loose: Set<Int>, rnd: Random): BooleanArray {
            val on = BooleanArray(grid.count)
            val want = (frac * grid.count).roundToInt()
            var n = 0
            val border = (0 until grid.count).filter { val x = it % grid.cols; val y = it / grid.cols; x == 0 || y == 0 || x == grid.cols - 1 || y == grid.rows - 1 }
            for (c in border) if (n < want && c !in loose) { on[c] = true; n++ }
            var guard = 0
            while (n < want && guard++ < 1_000_000) {
                // Grow next to what is placed (islands attach to the frame or to each other), sometimes a new island.
                val c = rnd.nextInt(grid.count)
                if (on[c] || c in loose) continue
                val x = c % grid.cols; val y = c / grid.cols
                val touches = (x > 0 && on[c - 1]) || (x < grid.cols - 1 && on[c + 1]) || (y > 0 && on[c - grid.cols]) || (y < grid.rows - 1 && on[c + grid.cols])
                if (touches || rnd.nextInt(40) == 0) { on[c] = true; n++ }
            }
            return on
        }

        /** The framed progress photo, [cellPx] pixels per cell. */
        fun photo(box: Raster, grid: Grid, on: BooleanArray, rnd: Random, cellPx: Int = 24): Raster {
            val w = grid.cols * cellPx; val h = grid.rows * cellPx
            val src = box.fit(maxOf(w, h))
            // The person drags the 4 corners: each lands up to FRAMING cell off, the error in between is interpolated.
            val off = DoubleArray(8) { (rnd.nextDouble() * 2 - 1) * FRAMING * cellPx }
            val table = tables[rnd.nextInt(tables.size)]
            val gains = FloatArray(3) { 0.85f + rnd.nextFloat() * 0.3f }; val exposure = 0.8f + rnd.nextFloat() * 0.35f
            val light = rnd.nextFloat() * 0.4f; val la = rnd.nextDouble() * 2 * PI
            val px = IntArray(w * h)
            for (y in 0 until h) for (x in 0 until w) {
                // Where this pixel of the framed photo really is on the puzzle (the framing is a little off).
                val fx = x.toDouble() / w; val fy = y.toDouble() / h
                fun lerp(k: Int) = (off[k] * (1 - fx) + off[k + 2] * fx) * (1 - fy) + (off[k + 4] * (1 - fx) + off[k + 6] * fx) * fy
                val u = x + lerp(0); val v = y + lerp(1)
                val cx = (u / cellPx).toInt(); val cy = (v / cellPx).toInt()
                val inside = u >= 0 && v >= 0 && cx < grid.cols && cy < grid.rows
                var rgb = if (inside && on[cy * grid.cols + cx]) {
                    val p = src.px[(v * src.h / h).toInt().coerceIn(0, src.h - 1) * src.w + (u * src.w / w).toInt().coerceIn(0, src.w - 1)]
                    intArrayOf(p shr 16 and 255, p shr 8 and 255, p and 255)
                } else {
                    val grain = (sin(y * 0.35 + sin(x * 0.05) * 3) * 8).toInt()
                    IntArray(3) { table[it] + grain }
                }
                val ramp = 1 + light * ((x.toDouble() / w - 0.5) * cos(la) + (y.toDouble() / h - 0.5) * sin(la))
                rgb = IntArray(3) { c -> (rgb[c] * gains[c] * exposure * ramp + rnd.nextGaussian() * 4).roundToInt().coerceIn(0, 255) }
                px[y * w + x] = (255 shl 24) or (rgb[0] shl 16) or (rgb[1] shl 8) or rgb[2]
            }
            return Raster(w, h, px)
        }
    }
}
