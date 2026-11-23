package com.lilyan_lefevre.puzzleit.feature.recognition

import androidx.test.platform.app.InstrumentationRegistry
import java.util.Random
import kotlin.math.abs
import kotlin.math.sqrt
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks the two ONNX networks react to their input on the device (a constant output would mean the app only uses colour). */
class OnnxNetworkDeviceTest {
    private val assets = InstrumentationRegistry.getInstrumentation().targetContext.assets
    private fun model(name: String) = assets.open(name).use { it.readBytes() }

    private fun image(size: Int, kind: Int): FloatArray {
        val rnd = Random(kind.toLong())
        return FloatArray(3 * size * size) { i ->
            val x = (i % size) / size.toFloat(); val y = ((i / size) % size) / size.toFloat(); val c = i / (size * size)
            when (kind) {
                0 -> -1.5f
                1 -> 1.5f
                2 -> rnd.nextGaussian().toFloat()
                3 -> (x * 2 - 1) * (c + 1) / 2f
                else -> if (((x * 8).toInt() + (y * 8).toInt()) % 2 == 0) 1.5f else -1.5f
            }
        }
    }

    private fun cos(a: FloatArray, b: FloatArray): Double {
        var d = 0.0; var na = 0.0; var nb = 0.0
        for (i in a.indices) { d += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i] }
        return d / (sqrt(na) * sqrt(nb))
    }

    private fun meanAbsDiff(a: FloatArray, b: FloatArray) = a.indices.sumOf { abs(a[it] - b[it]).toDouble() } / a.size

    @Test
    fun rerankerEmbeddingsDependOnTheImage() {
        val net = OnnxEmbedder(model("reranker.onnx"))
        val imgs = (0..4).map { image(PieceReranker.SIZE, it) }
        val emb = net.embed(imgs)
        val again = net.embed(listOf(imgs[2]))[0]
        val sims = ArrayList<Double>()
        for (i in 0..4) for (j in i + 1..4) sims += cos(emb[i], emb[j]).also { println("ONNXCHECK reranker cos($i,$j)=%.4f".format(it)) }
        val stab = cos(emb[2], again)
        println("ONNXCHECK reranker dim=${emb[0].size} minCos=%.4f maxCos=%.4f stability=%.6f".format(sims.min(), sims.max(), stab))
        assertTrue("embeddings are constant: min cosine ${sims.min()}", sims.min() < 0.99)
        assertTrue("same image, different embedding: $stab", stab > 0.999)
    }

    /** A table-coloured picture with a coloured, textured square object centred at ([cx],[cy]) (fractions), or none when [cx] < 0. */
    private fun scene(cx: Float, cy: Float): FloatArray {
        val s = PieceSegmenter.SIZE; val rnd = Random(5)
        val mean = floatArrayOf(0.485f, 0.456f, 0.406f); val std = floatArrayOf(0.229f, 0.224f, 0.225f)
        return FloatArray(3 * s * s) { i ->
            val x = (i % s) / s.toFloat(); val y = ((i / s) % s) / s.toFloat(); val c = i / (s * s)
            val inside = cx >= 0 && abs(x - cx) < 0.18f && abs(y - cy) < 0.18f
            val v = if (inside) floatArrayOf(0.85f, 0.2f, 0.15f)[c] + 0.15f * rnd.nextFloat() else floatArrayOf(0.8f, 0.76f, 0.72f)[c] + 0.03f * rnd.nextFloat()
            (v - mean[c]) / std[c]
        }
    }

    private fun norm(m: FloatArray): FloatArray { val lo = m.min(); val sp = maxOf(1e-6f, m.max() - lo); return FloatArray(m.size) { (m[it] - lo) / sp } }

    @Test
    fun segmenterMasksDependOnTheImage() {
        val net = OnnxSegmenter(model("segmenter.onnx"))
        val s = PieceSegmenter.SIZE
        val centres = listOf(0.5f to 0.5f, 0.25f to 0.3f, 0.75f to 0.7f, -1f to -1f)
        val raw = centres.map { net.run(scene(it.first, it.second)) }
        val masks = raw.map(::norm)
        fun at(m: FloatArray, fx: Float, fy: Float) = m[(fy * s).toInt() * s + (fx * s).toInt()]
        for ((k, c) in centres.withIndex()) println("ONNXCHECK segmenter scene$k raw[min=%.4f max=%.4f] onObject=%.3f farCorner=%.3f".format(raw[k].min(), raw[k].max(), at(masks[k], maxOf(c.first, 0.5f), maxOf(c.second, 0.5f)), at(masks[k], if (c.first < 0.5f) 0.95f else 0.05f, 0.95f)))
        val diffs = ArrayList<Double>()
        for (i in 0..3) for (j in i + 1..3) diffs += meanAbsDiff(masks[i], masks[j])
        val stab = meanAbsDiff(masks[0], norm(net.run(scene(0.5f, 0.5f))))
        println("ONNXCHECK segmenter normalised meanAbsDiff min=%.4f max=%.4f stability=%.8f".format(diffs.min(), diffs.max(), stab))
        assertTrue("masks do not follow the object: min diff ${diffs.min()}", diffs.min() > 0.02)
        assertTrue("object not brighter than background: ${at(masks[0], 0.5f, 0.5f)}", at(masks[0], 0.5f, 0.5f) > at(masks[0], 0.05f, 0.95f) + 0.3f)
        assertTrue("same image, different mask: $stab", stab < 1e-4)
    }
}
