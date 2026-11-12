package com.nioro.spectrumcolor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

/**
 * Muestra en pantalla las esferas de la Tienda (Premium.kt). Cada combinación esfera + tamaño + color tiene su propia
 * imagen, que se vuelve a pintar varias veces por segundo; el trabajo de cada imagen se reparte entre varios núcleos
 * del procesador. Solo se usa desde el hilo de la interfaz.
 */
object PremiumDraw {
    /** Píxeles reales por unidad de diseño del lienzo actual (lo fija quien dibuja: GameView o la Aventura). */
    @JvmField var pxPerUnit = 1f

    private class Img(val n: Int) {
        val bmp: Bitmap = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
        val px = IntArray(n * n)
        var last = -100f
        var used = 0f
    }

    private val cache = HashMap<Long, Img>()
    private val workers = max(0, min(3, Runtime.getRuntime().availableProcessors() - 1))
    private val pool = if (workers > 0) Executors.newFixedThreadPool(workers) { r ->
        Thread(r, "esferas").also { it.isDaemon = true }
    } else null
    private val painters = Array(workers + 1) { Premium.Painter() }
    private val bp = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val rf = RectF()
    private var lastSweep = 0f

    /**
     * Dibuja la esfera k centrada en x,y con radio r (unidades del lienzo). cap = tamaño máximo de la imagen en
     * píxeles; fps = cuántas veces por segundo se vuelve a pintar.
     */
    fun draw(c: Canvas, x: Float, y: Float, r: Float, col: Int, k: Int, tm: Float, cap: Int, fps: Float, alpha: Int = 255) {
        if (k !in 0 until Premium.COUNT || r <= 0.5f) return
        var n = (2f * Premium.M * r * pxPerUnit).toInt()
        n = ((n + 15) / 16 * 16).coerceIn(32, cap)
        val key = (k.toLong() shl 48) or (n.toLong() shl 32) or (col.toLong() and 0xFFFFFFFFL)
        var img = cache[key]
        if (img == null) {
            if (cache.size >= 64) sweep(tm, true)
            img = Img(n)
            cache[key] = img
        }
        if (tm - img.last >= 1f / fps || tm < img.last) {
            render(k, img.px, n, col, tm)
            img.bmp.setPixels(img.px, 0, n, 0, 0, n, n)
            img.last = tm
        }
        img.used = tm
        val h = Premium.M * r
        rf.set(x - h, y - h, x + h, y + h)
        bp.alpha = alpha
        c.drawBitmap(img.bmp, null, rf, bp)
        if (tm - lastSweep > 3f) { lastSweep = tm; sweep(tm, false) }
    }

    /** Suelta las imágenes que no se usaron hace rato. */
    private fun sweep(tm: Float, force: Boolean) {
        val it = cache.values.iterator()
        while (it.hasNext()) {
            val im = it.next()
            if (tm - im.used > (if (force) 0.5f else 4f) || tm < im.used - 1f) it.remove()
        }
    }

    private fun render(k: Int, px: IntArray, n: Int, col: Int, tm: Float) {
        val ex = pool
        if (ex == null || n < 64) {
            painters[0].paint(k, px, n, col, tm)
            return
        }
        val parts = workers + 1
        val latch = CountDownLatch(workers)
        for (w in 1..workers) {
            ex.execute {
                try { painters[w].paint(k, px, n, col, tm, w, parts) } finally { latch.countDown() }
            }
        }
        painters[0].paint(k, px, n, col, tm, 0, parts)
        try { latch.await() } catch (_: InterruptedException) {}
    }
}
