package com.nioro.spectrumcolor

import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Vista previa de las esferas de la Tienda sin Android (Premium.kt se pinta igual en la JVM).
 * Uso: java -cp <jar> com.nioro.spectrumcolor.PremiumPreviewKt <carpeta> [tamaño] [esferas] [tiempos]
 *  - grid.png: las 35 en un mosaico; tira_K.png: la esfera K en varios momentos; además mide la velocidad.
 */
fun main(args: Array<String>) {
    val dir = File(args.getOrElse(0) { "." })
    dir.mkdirs()
    val n = args.getOrElse(1) { "128" }.toInt()
    val sel = args.getOrNull(2)?.split(",")?.mapNotNull { it.toIntOrNull() } ?: (0 until Premium.COUNT).toList()
    val times = args.getOrNull(3)?.split(",")?.mapNotNull { it.toFloatOrNull() } ?: listOf(0.4f, 1.3f, 2.2f, 3.1f)
    val cols = intArrayOf(0xFF33E0FF.toInt(), 0xFFFF4F9A.toInt(), 0xFFFFD84D.toInt(), 0xFF8C5CFF.toInt())
    val p = Premium.Painter()
    val buf = IntArray(n * n)
    val bg = Color(14, 18, 38)

    fun put(img: BufferedImage, ox: Int, oy: Int, k: Int, col: Int, t: Float) {
        p.paint(k, buf, n, col, t)
        for (j in 0 until n) for (i in 0 until n) {
            val c = buf[j * n + i]
            val a = (c ushr 24) / 255f
            val base = img.getRGB(ox + i, oy + j)
            val r = (((c shr 16) and 255) * a + ((base shr 16) and 255) * (1 - a)).toInt()
            val g = (((c shr 8) and 255) * a + ((base shr 8) and 255) * (1 - a)).toInt()
            val b = ((c and 255) * a + (base and 255) * (1 - a)).toInt()
            img.setRGB(ox + i, oy + j, (0xFF shl 24) or (r shl 16) or (g shl 8) or b)
        }
    }
    fun blank(w: Int, h: Int): BufferedImage {
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.color = bg
        g.fillRect(0, 0, w, h)
        g.dispose()
        return img
    }
    fun label(img: BufferedImage, s: String, x: Int, y: Int) {
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.color = Color(230, 235, 255)
        g.font = g.font.deriveFont(12f)
        g.drawString(s, x, y)
        g.dispose()
    }

    // mosaico de todas (o las elegidas)
    val per = 7
    val rows = (sel.size + per - 1) / per
    val grid = blank(per * n, rows * (n + 16))
    for ((idx, k) in sel.withIndex()) {
        val ox = (idx % per) * n
        val oy = (idx / per) * (n + 16)
        put(grid, ox, oy, k, cols[k % 4], 1.7f + k * 0.13f)
        label(grid, "$k ${Premium.names[k]}", ox + 4, oy + n + 12)
    }
    ImageIO.write(grid, "png", File(dir, "grid.png"))

    // tiras en el tiempo (con los 4 colores de juego)
    if (sel.size <= 8) for (k in sel) {
        val img = blank(times.size * n, 4 * n)
        for ((ti, t) in times.withIndex()) for (ci in 0 until 4) put(img, ti * n, ci * n, k, cols[ci], t)
        ImageIO.write(img, "png", File(dir, "tira_$k.png"))
    }

    // velocidad: cada esfera pintada 60 veces
    val reps = 60
    val sb = StringBuilder()
    var total = 0.0
    for (k in sel) {
        for (w in 0 until 5) p.paint(k, buf, n, cols[0], w * 0.1f)
        val t0 = System.nanoTime()
        for (r in 0 until reps) p.paint(k, buf, n, cols[0], r / 60f)
        val ms = (System.nanoTime() - t0) / 1e6 / reps
        total += ms
        sb.append(String.format("%2d %-22s %.2f ms\n", k, Premium.names[k], ms))
    }
    sb.append(String.format("promedio %.2f ms por esfera de %d px\n", total / sel.size, n))
    File(dir, "velocidad.txt").writeText(sb.toString())
    print(sb)
}
