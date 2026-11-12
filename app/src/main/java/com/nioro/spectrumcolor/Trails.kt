package com.nioro.spectrumcolor

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** Estelas (rastro) de la bola. 0 = clásica; `need` = estrellas totales para usarla. */
object Trails {
    val names = arrayOf(
        "Clásica", "Chispas", "Láser", "Doble láser", "Arcoíris", "Cinta", "Neón", "Estrellas", "Corazones",
        "Burbujas", "Diamantes", "Fuego", "Humo", "Relámpago", "Píxeles", "Anillos", "Cometa", "Ondas",
        "Galaxia", "Hilos de luz", "Confeti",
        // nuevas
        "Nieve", "Hojas", "Notas musicales", "Pétalos", "Electricidad", "Tinta", "Aurora boreal", "Código", "Ascuas",
        "Cristales", "Espiral", "Polvo de hadas", "Plasma", "Huellas", "Píxel arcoíris", "Cadena", "Meteoros", "Sombras",
        "Prisma", "Supernova",
        // especiales de la Tienda (41..50): se compran con monedas
        "Cola de dragón", "Río de estrellas", "Rayo cósmico", "Alas de luz", "Fuego fénix",
        "Nebulosa", "Arcoíris líquido", "Burbujas mágicas", "Pétalos de sakura", "Estela infinita"
    )
    /** Estelas especiales de la Tienda: índices PREMIUM_FIRST.. y su precio en monedas. */
    const val PREMIUM_FIRST = 41
    val price = intArrayOf(200, 300, 400, 550, 700, 900, 1100, 1400, 1800, 2400)
    fun premium(k: Int): Boolean = k >= PREMIUM_FIRST && k < names.size
    val need = intArrayOf(0, 5, 12, 20, 30, 40, 52, 64, 78, 92, 108, 124, 142, 160, 180, 200, 222, 244, 268, 292, 318,
        16, 26, 36, 48, 58, 70, 84, 98, 114, 130, 150, 170, 190, 212, 234, 256, 280, 304, 330, 352,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    val life = floatArrayOf(
        0.35f, 0.38f, 0.40f, 0.40f, 0.50f, 0.45f, 0.40f, 0.45f, 0.50f, 0.55f, 0.45f,
        0.50f, 0.60f, 0.25f, 0.40f, 0.45f, 0.55f, 0.50f, 0.50f, 0.50f, 0.60f,
        0.60f, 0.60f, 0.60f, 0.60f, 0.30f, 0.50f, 0.55f, 0.50f, 0.60f, 0.45f,
        0.50f, 0.60f, 0.40f, 0.55f, 0.45f, 0.45f, 0.40f, 0.45f, 0.40f, 0.50f,
        0.55f, 0.60f, 0.45f, 0.55f, 0.60f, 0.62f, 0.50f, 0.60f, 0.60f, 0.60f
    )
    /** Tiempo máximo que se conserva un punto del rastro (el mayor de `life`). */
    const val MAX_LIFE = 0.62f
    val COUNT get() = names.size

    private val path = Path()
    private var nx = 0f
    private var ny = 0f

    private fun h(i: Int, k: Int): Float {
        val v = sin(i * 12.9898f + k * 78.233f) * 43758.547f
        return v - floor(v)
    }

    private fun seg(c: Canvas, x0: Float, y0: Float, x1: Float, y1: Float, w: Float, col: Int) {
        val pp = Gl.p()
        pp.style = Paint.Style.STROKE
        pp.strokeCap = Paint.Cap.ROUND
        pp.strokeWidth = max(0.8f, w)
        pp.color = col
        c.drawLine(x0, y0, x1, y1, pp)
    }

    private fun dot(c: Canvas, x: Float, y: Float, r: Float, col: Int) {
        if (r <= 0.2f) return
        val pp = Gl.p()
        pp.color = col
        c.drawCircle(x, y, r, pp)
    }

    private fun ring(c: Canvas, x: Float, y: Float, r: Float, w: Float, col: Int) {
        val pp = Gl.p()
        pp.style = Paint.Style.STROKE
        pp.strokeWidth = w
        pp.color = col
        c.drawCircle(x, y, r, pp)
    }

    private fun normal(p: List<FloatArray>, i: Int) {
        val a = p[max(0, i - 1)]
        val b = p[minOf(p.size - 1, i + 1)]
        var dx = b[0] - a[0]
        var dy = b[1] - a[1]
        val l = sqrt(dx * dx + dy * dy)
        if (l < 0.001f) { nx = 1f; ny = 0f; return }
        dx /= l; dy /= l
        nx = -dy; ny = dx
    }

    private fun poly(c: Canvas, x: FloatArray, y: FloatArray, n: Int, w: Float, col: Int) {
        if (n < 2) return
        path.reset()
        path.moveTo(x[0], y[0])
        for (i in 1 until n) path.lineTo(x[i], y[i])
        val pp = Gl.p()
        pp.style = Paint.Style.STROKE
        pp.strokeCap = Paint.Cap.ROUND
        pp.strokeJoin = Paint.Join.ROUND
        pp.strokeWidth = max(0.8f, w)
        pp.color = col
        c.drawPath(path, pp)
    }

    private fun spark(c: Canvas, x: Float, y: Float, r: Float, col: Int, a: Int) {
        seg(c, x - r, y, x + r, y, r * 0.22f, Gl.alpha(col, a))
        seg(c, x, y - r, x, y + r, r * 0.22f, Gl.alpha(col, a))
        dot(c, x, y, r * 0.3f, Gl.white(a))
    }

    private fun star(c: Canvas, x: Float, y: Float, r: Float, rot: Float, fill: Int, stroke: Int) {
        path.reset()
        for (k in 0 until 10) {
            val ang = rot + k * 3.14159f / 5f
            val rr = if (k % 2 == 0) r else r * 0.45f
            val px = x + cos(ang) * rr
            val py = y + sin(ang) * rr
            if (k == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        var pp = Gl.p()
        pp.color = fill
        c.drawPath(path, pp)
        pp = Gl.p()
        pp.style = Paint.Style.STROKE
        pp.strokeWidth = 1.6f
        pp.color = stroke
        c.drawPath(path, pp)
    }

    private fun heart(c: Canvas, x: Float, y: Float, s: Float, fill: Int, stroke: Int) {
        path.reset()
        path.moveTo(x, y + s * 0.9f)
        path.cubicTo(x - s * 1.6f, y - s * 0.2f, x - s * 0.7f, y - s * 1.3f, x, y - s * 0.4f)
        path.cubicTo(x + s * 0.7f, y - s * 1.3f, x + s * 1.6f, y - s * 0.2f, x, y + s * 0.9f)
        path.close()
        var pp = Gl.p()
        pp.color = fill
        c.drawPath(path, pp)
        pp = Gl.p()
        pp.style = Paint.Style.STROKE
        pp.strokeWidth = 1.5f
        pp.color = stroke
        c.drawPath(path, pp)
    }

    private fun quad(c: Canvas, x: Float, y: Float, a: Float, b: Float, rot: Float, fill: Int, stroke: Int) {
        c.save()
        c.translate(x, y)
        c.rotate(rot)
        var pp = Gl.p()
        pp.color = fill
        c.drawRect(-a, -b, a, b, pp)
        if (stroke != 0) {
            pp = Gl.p()
            pp.style = Paint.Style.STROKE
            pp.strokeWidth = 1.5f
            pp.color = stroke
            c.drawRect(-a, -b, a, b, pp)
        }
        c.restore()
    }

    private val bx = FloatArray(300)
    private val by = FloatArray(300)

    /** Dibuja el rastro `st`. p: puntos (x, y, edad en segundos), del más viejo al más nuevo. R: radio de la bola. */
    fun draw(c: Canvas, st: Int, p: List<FloatArray>, col: Int, pal: IntArray, R: Float, tm: Float) {
        if (p.isEmpty()) return
        val L = life[st.coerceIn(0, life.size - 1)]
        val n0 = p.size
        when (st) {
            0 -> for (t in p) {
                val f = 1f - t[2] / L
                if (f > 0f) dot(c, t[0], t[1], R * (0.3f + 0.55f * f), Gl.alpha(col, (f * 70f).toInt()))
            }
            1 -> for (i in 0 until n0) {
                val t = p[i]; val f = 1f - t[2] / L
                if (f <= 0f || i % 2 != 0) continue
                spark(c, t[0] + (h(i, 1) - 0.5f) * R * 1.5f, t[1] + (h(i, 2) - 0.5f) * R * 1.5f, R * (0.25f + 0.4f * f), if (i % 4 == 1) Color.WHITE else col, (f * 255f).toInt())
            }
            2, 3, 6 -> {
                for (i in 1 until n0) {
                    val a = p[i - 1]; val b = p[i]; val f = 1f - b[2] / L
                    if (f <= 0f) continue
                    when (st) {
                        2 -> {
                            seg(c, a[0], a[1], b[0], b[1], R * 0.7f * f, Gl.alpha(col, (f * 80f).toInt()))
                            seg(c, a[0], a[1], b[0], b[1], R * 0.2f * f + 1f, Gl.white((f * 250f).toInt()))
                        }
                        3 -> {
                            normal(p, i)
                            val o = R * 0.5f * (0.4f + 0.6f * f)
                            seg(c, a[0] + nx * o, a[1] + ny * o, b[0] + nx * o, b[1] + ny * o, R * 0.2f * f + 1f, Gl.alpha(col, (f * 255f).toInt()))
                            seg(c, a[0] - nx * o, a[1] - ny * o, b[0] - nx * o, b[1] - ny * o, R * 0.2f * f + 1f, Gl.white((f * 255f).toInt()))
                        }
                        else -> {
                            seg(c, a[0], a[1], b[0], b[1], R * 1.4f * f, Gl.alpha(col, (f * 40f).toInt()))
                            seg(c, a[0], a[1], b[0], b[1], R * 0.8f * f, Gl.alpha(col, (f * 100f).toInt()))
                            seg(c, a[0], a[1], b[0], b[1], R * 0.22f * f + 1f, Gl.white((f * 255f).toInt()))
                        }
                    }
                }
            }
            4 -> for (i in 1 until n0) {
                val a = p[i - 1]; val b = p[i]; val f = 1f - b[2] / L
                if (f <= 0f) continue
                val hc = Color.HSVToColor(floatArrayOf((i * 22f + tm * 90f) % 360f, 0.85f, 1f))
                seg(c, a[0], a[1], b[0], b[1], R * 0.85f * (0.35f + 0.65f * f), Gl.alpha(hc, (f * 235f).toInt()))
            }
            5 -> for (i in 1 until n0) {
                val a = p[i - 1]; val b = p[i]; val f = 1f - b[2] / L
                if (f <= 0f) continue
                seg(c, a[0], a[1], b[0], b[1], R * 1.5f * f, Gl.alpha(col, (f * 190f).toInt()))
                seg(c, a[0], a[1], b[0], b[1], R * 0.35f * f, Gl.white((f * 200f).toInt()))
            }
            7 -> for (i in 0 until n0) {
                val t = p[i]; val f = 1f - t[2] / L
                if (f <= 0f || i % 2 != 0) continue
                star(c, t[0], t[1], R * 0.6f * f + 2f, tm * 3f + i, Gl.alpha(col, (f * 220f).toInt()), Gl.white((f * 230f).toInt()))
            }
            8 -> for (i in 0 until n0) {
                val t = p[i]; val f = 1f - t[2] / L
                if (f <= 0f || i % 3 != 0) continue
                heart(c, t[0] + (h(i, 3) - 0.5f) * R, t[1] - t[2] * R * 2f, R * 0.5f * f + 2f, Gl.alpha(if (i % 2 == 0) Color.rgb(255, 90, 140) else col, (f * 230f).toInt()), Gl.white((f * 200f).toInt()))
            }
            9 -> for (i in 0 until n0) {
                val t = p[i]; val f = 1f - t[2] / L
                if (f <= 0f || i % 2 != 0) continue
                val r = R * (0.2f + 0.45f * h(i, 4))
                val x = t[0] + (h(i, 5) - 0.5f) * R * 1.2f
                val y = t[1] - t[2] * R * 3f
                dot(c, x, y, r, Gl.alpha(col, (f * 45f).toInt()))
                ring(c, x, y, r, 2f, Gl.white((f * 210f).toInt()))
                dot(c, x - r * 0.35f, y - r * 0.35f, r * 0.2f, Gl.white((f * 230f).toInt()))
            }
            10 -> for (i in 0 until n0) {
                val t = p[i]; val f = 1f - t[2] / L
                if (f <= 0f || i % 2 != 0) continue
                val s = R * 0.42f * f + 1.5f
                quad(c, t[0], t[1], s, s, 45f + t[2] * 200f, Gl.alpha(col, (f * 210f).toInt()), Gl.white((f * 230f).toInt()))
            }
            11 -> for (i in 0 until n0) {
                val t = p[i]; val f = 1f - t[2] / L
                if (f <= 0f) continue
                val fc = if (f > 0.66f) Color.rgb(255, 235, 110) else if (f > 0.33f) Color.rgb(255, 150, 40) else Color.rgb(230, 50, 30)
                dot(c, t[0] + (h(i, 6) - 0.5f) * R * 0.6f, t[1] - t[2] * R * 4f, R * 0.75f * f, Gl.alpha(fc, (f * 170f).toInt()))
            }
            12 -> for (i in 0 until n0) {
                val t = p[i]; val f = 1f - t[2] / L
                if (f <= 0f) continue
                dot(c, t[0] + (h(i, 7) - 0.5f) * R * 0.8f, t[1] - t[2] * R * 2.2f, R * (0.35f + (1f - f) * 0.9f), Gl.alpha(Color.rgb(170, 176, 190), (f * 85f).toInt()))
            }
            13 -> {
                val seed = (tm * 28f).toInt()
                var m = 0
                for (i in 0 until n0) {
                    val t = p[i]
                    if (1f - t[2] / L <= 0f || m >= 299) continue
                    bx[m] = t[0] + (h(i + seed, 8) - 0.5f) * R * 1.3f
                    by[m] = t[1] + (h(i + seed, 9) - 0.5f) * R * 1.3f
                    m++
                }
                poly(c, bx, by, m, R * 0.7f, Gl.alpha(col, 70))
                poly(c, bx, by, m, R * 0.22f, Gl.white(245))
            }
            14 -> {
                val g = R * 0.5f
                for (i in 0 until n0) {
                    val t = p[i]; val f = 1f - t[2] / L
                    if (f <= 0f || i % 2 != 0) continue
                    val x = floor(t[0] / g) * g
                    val y = floor(t[1] / g) * g
                    val cc = if (i % 6 == 0) Color.WHITE else col
                    c.drawRect(x, y, x + g * 0.88f, y + g * 0.88f, Gl.p().also { it.color = Gl.alpha(cc, (f * 235f).toInt()) })
                }
            }
            15 -> for (i in 0 until n0) {
                val t = p[i]; val f = 1f - t[2] / L
                if (f <= 0f || i % 3 != 0) continue
                ring(c, t[0], t[1], R * (0.3f + (1f - f) * 1.3f), 2.5f + 2f * f, Gl.alpha(col, (f * 210f).toInt()))
            }
            16 -> for (i in 1 until n0) {
                val a = p[i - 1]; val b = p[i]
                for (k in 0 until 3) {
                    val u = k / 3f
                    val age = a[2] + (b[2] - a[2]) * u
                    val f = 1f - age / L
                    if (f <= 0f) continue
                    val x = a[0] + (b[0] - a[0]) * u
                    val y = a[1] + (b[1] - a[1]) * u
                    val cc = Color.rgb(
                        (255 * f + Color.red(col) * (1f - f)).toInt().coerceIn(0, 255),
                        (255 * f + Color.green(col) * (1f - f)).toInt().coerceIn(0, 255),
                        (255 * f + Color.blue(col) * (1f - f)).toInt().coerceIn(0, 255)
                    )
                    dot(c, x, y, R * 0.95f * f, Gl.alpha(cc, (f * 150f).toInt()))
                }
            }
            17 -> for (s in -1..1 step 2) {
                var m = 0
                for (i in 0 until n0) {
                    val t = p[i]; val f = 1f - t[2] / L
                    if (f <= 0f || m >= 299) continue
                    normal(p, i)
                    val o = sin(t[2] * 20f + tm * 5f) * R * 0.7f * s
                    bx[m] = t[0] + nx * o; by[m] = t[1] + ny * o; m++
                }
                poly(c, bx, by, m, R * 0.3f, Gl.alpha(if (s > 0) col else Color.WHITE, 210))
            }
            18 -> for (i in 0 until n0) {
                val t = p[i]; val f = 1f - t[2] / L
                if (f <= 0f) continue
                val tw = 0.55f + 0.45f * sin(tm * 9f + i * 1.7f)
                dot(c, t[0] + (h(i, 10) - 0.5f) * R * 2f, t[1] + (h(i, 11) - 0.5f) * R * 2f, R * 0.28f * f * (0.6f + tw) + 1f, Gl.alpha(pal[i % 4], (f * 255f * tw).toInt()))
            }
            19 -> for (s in 0 until 3) {
                var m = 0
                for (i in 0 until n0) {
                    val t = p[i]; val f = 1f - t[2] / L
                    if (f <= 0f || m >= 299) continue
                    normal(p, i)
                    val o = sin(t[2] * 16f + s * 2.094f + tm * 3f) * R * 0.8f
                    bx[m] = t[0] + nx * o; by[m] = t[1] + ny * o; m++
                }
                poly(c, bx, by, m, R * 0.22f, Gl.alpha(pal[s % 4], 230))
            }
            20 -> for (i in 0 until n0) {
                val t = p[i]; val f = 1f - t[2] / L
                if (f <= 0f || i % 2 != 0) continue
                quad(c, t[0] + (h(i, 12) - 0.5f) * R * 1.4f, t[1] + t[2] * R * 3f, R * 0.28f, R * 0.14f, i * 47f + t[2] * 500f, Gl.alpha(pal[i % 4], (f * 240f).toInt()), 0)
            }
            in 21..40 -> drawNew(c, st, p, col, pal, R, tm, L)
            in PREMIUM_FIRST until names.size -> drawPremium(c, st, p, col, pal, R, tm, L)
            else -> {}
        }
    }

    private fun hsv(h: Float, s: Float = 0.85f, v: Float = 1f): Int = Color.HSVToColor(floatArrayOf(((h % 360f) + 360f) % 360f, s, v))

    /** Estelas 21..40. */
    private fun drawNew(c: Canvas, st: Int, p: List<FloatArray>, col: Int, pal: IntArray, R: Float, tm: Float, L: Float) {
        val n0 = p.size
        when (st) {
            21 -> for (i in 0 until n0) { // nieve: copos que caen y giran
                val t = p[i]; val f = 1f - t[2] / L
                if (f <= 0f || i % 2 != 0) continue
                val x = t[0] + (h(i, 21) - 0.5f) * R * 1.6f + sin(t[2] * 6f + i) * R * 0.3f
                val y = t[1] + t[2] * R * 2.2f
                val s = R * (0.18f + 0.22f * h(i, 22)) * (0.5f + 0.5f * f)
                for (k in 0 until 3) {
                    val an = k * 1.0472f + t[2] * 3f + i
                    seg(c, x - cos(an) * s, y - sin(an) * s, x + cos(an) * s, y + sin(an) * s, 1.6f, Gl.white((f * 235f).toInt()))
                }
            }
            22 -> for (i in 0 until n0) { // hojas que caen balanceándose
                val t = p[i]; val f = 1f - t[2] / L
                if (f <= 0f || i % 3 != 0) continue
                val x = t[0] + sin(t[2] * 7f + i) * R * 0.8f
                val y = t[1] + t[2] * R * 2.5f
                val lc = if (i % 2 == 0) Color.rgb(120, 200, 80) else Color.rgb(240, 150, 50)
                c.save(); c.translate(x, y); c.rotate(sin(t[2] * 6f + i) * 50f + i * 30f)
                path.reset(); path.moveTo(0f, -R * 0.45f); path.quadTo(R * 0.32f, 0f, 0f, R * 0.45f); path.quadTo(-R * 0.32f, 0f, 0f, -R * 0.45f)
                val pp = Gl.p(); pp.color = Gl.alpha(lc, (f * 235f).toInt()); c.drawPath(path, pp)
                seg(c, 0f, -R * 0.4f, 0f, R * 0.4f, 1.2f, Gl.alpha(Color.rgb(60, 90, 30), (f * 200f).toInt()))
                c.restore()
            }
            23 -> for (i in 0 until n0) { // notas musicales que suben
                val t = p[i]; val f = 1f - t[2] / L
                if (f <= 0f || i % 4 != 0) continue
                val x = t[0] + (h(i, 23) - 0.5f) * R * 1.5f + sin(t[2] * 8f) * R * 0.3f
                val y = t[1] - t[2] * R * 2f
                val nc = Gl.alpha(if (i % 8 == 0) Color.WHITE else col, (f * 240f).toInt())
                dot(c, x, y, R * 0.2f, nc)
                seg(c, x + R * 0.18f, y, x + R * 0.18f, y - R * 0.7f, 2.2f, nc)
                seg(c, x + R * 0.18f, y - R * 0.7f, x + R * 0.45f, y - R * 0.5f, 2.2f, nc)
            }
            24 -> for (i in 0 until n0) { // pétalos rosados
                val t = p[i]; val f = 1f - t[2] / L
                if (f <= 0f || i % 2 != 0) continue
                val x = t[0] + (h(i, 24) - 0.5f) * R * 1.4f
                val y = t[1] + t[2] * R * 1.6f
                val pc = if (i % 4 == 0) Color.rgb(255, 200, 220) else Color.rgb(255, 140, 180)
                quad(c, x, y, R * 0.22f * f + 1.5f, R * 0.12f * f + 1f, i * 40f + t[2] * 300f, Gl.alpha(pc, (f * 230f).toInt()), 0)
            }
            25 -> { // electricidad: zigzag que tiembla entre los puntos
                for (i in 1 until n0) {
                    val a = p[i - 1]; val b = p[i]; val f = 1f - b[2] / L
                    if (f <= 0f) continue
                    val j = (h(i, (tm * 30f).toInt()) - 0.5f) * R * 0.9f * f
                    normal(p, i)
                    seg(c, a[0], a[1], b[0] + nx * j, b[1] + ny * j, R * 0.35f * f + 1f, Gl.alpha(col, (f * 90f).toInt()))
                    seg(c, a[0], a[1], b[0] + nx * j, b[1] + ny * j, 2f, Gl.white((f * 255f).toInt()))
                }
            }
            26 -> for (i in 1 until n0) { // tinta: trazo grueso que se seca en gotas
                val a = p[i - 1]; val b = p[i]; val f = 1f - b[2] / L
                if (f <= 0f) continue
                seg(c, a[0], a[1], b[0], b[1], R * 1.1f * f, Gl.alpha(Gl.darken(col, 0.35f), (f * 220f).toInt()))
                if (i % 5 == 0) dot(c, b[0] + (h(i, 26) - 0.5f) * R * 2f, b[1] + (h(i, 27) - 0.5f) * R * 2f, R * 0.18f * f, Gl.alpha(Gl.darken(col, 0.35f), (f * 220f).toInt()))
            }
            27 -> for (s in 0 until 3) { // aurora boreal: bandas onduladas verdes y violetas
                for (i in 1 until n0) {
                    val a = p[i - 1]; val b = p[i]; val f = 1f - b[2] / L
                    if (f <= 0f) continue
                    normal(p, i)
                    val o = (s - 1) * R * 0.5f + sin(b[2] * 10f + tm * 2f + s) * R * 0.3f
                    val ac = if (s == 1) Color.rgb(120, 255, 170) else if (s == 0) Color.rgb(80, 200, 255) else Color.rgb(190, 120, 255)
                    seg(c, a[0] + nx * o, a[1] + ny * o, b[0] + nx * o, b[1] + ny * o, R * 0.5f * f, Gl.alpha(ac, (f * 110f).toInt()))
                }
            }
            28 -> for (i in 0 until n0) { // código: bloquecitos verdes que caen como en una pantalla
                val t = p[i]; val f = 1f - t[2] / L
                if (f <= 0f) continue
                val x = t[0] + (floor(h(i, 28) * 5f) - 2f) * R * 0.32f
                val y = t[1] + t[2] * R * 3.5f
                val a = if ((i + (tm * 12f).toInt()) % 3 == 0) 255 else 140
                quad(c, x, y, R * 0.1f, R * 0.16f, 0f, Gl.alpha(Color.rgb(90, 255, 120), (f * a).toInt()), 0)
            }
            29 -> for (i in 0 until n0) { // ascuas que suben y parpadean
                val t = p[i]; val f = 1f - t[2] / L
                if (f <= 0f) continue
                val x = t[0] + (h(i, 29) - 0.5f) * R * 1.3f + sin(t[2] * 9f + i) * R * 0.25f
                val y = t[1] - t[2] * R * 3f
                val fl = 0.6f + 0.4f * sin(tm * 20f + i)
                spark(c, x, y, R * 0.14f * f + 1f, if (f > 0.5f) Color.rgb(255, 210, 90) else Color.rgb(255, 110, 40), (f * 255f * fl).toInt())
            }
            30 -> for (i in 0 until n0) { // cristales: triángulos brillantes
                val t = p[i]; val f = 1f - t[2] / L
                if (f <= 0f || i % 2 != 0) continue
                val s = R * 0.45f * f + 2f
                c.save(); c.translate(t[0] + (h(i, 30) - 0.5f) * R, t[1]); c.rotate(i * 37f + t[2] * 160f)
                path.reset(); path.moveTo(0f, -s); path.lineTo(s * 0.87f, s * 0.5f); path.lineTo(-s * 0.87f, s * 0.5f); path.close()
                var pp = Gl.p(); pp.color = Gl.alpha(pal[i % 4], (f * 150f).toInt()); c.drawPath(path, pp)
                pp = Gl.p(); pp.style = Paint.Style.STROKE; pp.strokeWidth = 1.5f; pp.color = Gl.white((f * 230f).toInt()); c.drawPath(path, pp)
                c.restore()
            }
            31 -> for (i in 0 until n0) { // espiral: dos hélices de puntos
                val t = p[i]; val f = 1f - t[2] / L
                if (f <= 0f) continue
                normal(p, i)
                for (k in 0..1) {
                    val o = sin(t[2] * 18f + tm * 4f + k * 3.1416f) * R * 0.75f
                    dot(c, t[0] + nx * o, t[1] + ny * o, R * 0.14f * (0.4f + 0.6f * f), Gl.alpha(if (k == 0) col else Color.WHITE, (f * 230f).toInt()))
                }
            }
            32 -> for (i in 0 until n0) { // polvo de hadas: chispitas doradas que caen
                val t = p[i]; val f = 1f - t[2] / L
                if (f <= 0f) continue
                val x = t[0] + (h(i, 32) - 0.5f) * R * 1.8f
                val y = t[1] + t[2] * t[2] * R * 6f
                val tw = 0.5f + 0.5f * sin(tm * 14f + i * 2.3f)
                spark(c, x, y, R * 0.12f * f + 0.8f, if (i % 3 == 0) Color.WHITE else Color.rgb(255, 215, 110), (f * 255f * tw).toInt())
            }
            33 -> for (i in 1 until n0) { // plasma: tubo grueso con los colores del mundo
                val a = p[i - 1]; val b = p[i]; val f = 1f - b[2] / L
                if (f <= 0f) continue
                val pc = pal[((i / 3) + (tm * 6f).toInt()) % 4]
                seg(c, a[0], a[1], b[0], b[1], R * 1.2f * f, Gl.alpha(pc, (f * 70f).toInt()))
                seg(c, a[0], a[1], b[0], b[1], R * 0.55f * f, Gl.alpha(pc, (f * 200f).toInt()))
                seg(c, a[0], a[1], b[0], b[1], R * 0.15f * f + 1f, Gl.white((f * 240f).toInt()))
            }
            34 -> for (i in 0 until n0) { // huellas de gatito, alternadas a cada lado
                val t = p[i]; val f = 1f - t[2] / L
                if (f <= 0f || i % 5 != 0) continue
                normal(p, i)
                val sd = if ((i / 5) % 2 == 0) 1f else -1f
                val x = t[0] + nx * R * 0.45f * sd
                val y = t[1] + ny * R * 0.45f * sd
                val pc = Gl.alpha(col, (f * 230f).toInt())
                dot(c, x, y + R * 0.08f, R * 0.22f, pc)
                for (k in -1..1) dot(c, x + k * R * 0.17f, y - R * 0.2f - (if (k == 0) R * 0.06f else 0f), R * 0.09f, pc)
            }
            35 -> for (i in 0 until n0) { // píxel arcoíris: cuadraditos de todos los colores
                val t = p[i]; val f = 1f - t[2] / L
                if (f <= 0f || i % 2 != 0) continue
                val q = R * 0.32f
                val x = floor((t[0] + (h(i, 35) - 0.5f) * R) / q) * q
                val y = floor(t[1] / q) * q
                quad(c, x, y, q * 0.45f * f + 1f, q * 0.45f * f + 1f, 0f, Gl.alpha(hsv(i * 25f + tm * 120f), (f * 240f).toInt()), 0)
            }
            36 -> for (i in 0 until n0) { // cadena: eslabones alternados
                val t = p[i]; val f = 1f - t[2] / L
                if (f <= 0f || i % 3 != 0) continue
                c.save(); c.translate(t[0], t[1]); c.rotate(if ((i / 3) % 2 == 0) 0f else 90f)
                val pp = Gl.p(); pp.style = Paint.Style.STROKE; pp.strokeWidth = R * 0.12f + 1f; pp.color = Gl.alpha(Color.rgb(200, 205, 220), (f * 240f).toInt())
                Gl.rect.set(-R * 0.3f, -R * 0.18f, R * 0.3f, R * 0.18f); c.drawRoundRect(Gl.rect, R * 0.18f, R * 0.18f, pp)
                c.restore()
            }
            37 -> for (i in 0 until n0) { // meteoros: rayas diagonales que caen hacia atrás
                val t = p[i]; val f = 1f - t[2] / L
                if (f <= 0f || i % 3 != 0) continue
                val x = t[0] + (h(i, 37) - 0.5f) * R * 2f
                val y = t[1] + t[2] * R * 2f
                seg(c, x, y, x - R * 0.9f * f, y + R * 0.9f * f, R * 0.18f * f + 1f, Gl.alpha(Color.rgb(255, 170, 80), (f * 200f).toInt()))
                dot(c, x, y, R * 0.14f * f + 1f, Gl.white((f * 255f).toInt()))
            }
            38 -> for (i in 0 until n0) { // sombras: copias huecas de la bola que se desvanecen
                val t = p[i]; val f = 1f - t[2] / L
                if (f <= 0f || i % 4 != 0) continue
                ring(c, t[0], t[1], R * (0.95f - 0.3f * (1f - f)), 2.5f, Gl.alpha(col, (f * 160f).toInt()))
            }
            39 -> for (i in 1 until n0) { // prisma: rojo, verde y azul separados
                val a = p[i - 1]; val b = p[i]; val f = 1f - b[2] / L
                if (f <= 0f) continue
                normal(p, i)
                val o = R * 0.35f * (1f - f * 0.5f)
                seg(c, a[0] + nx * o, a[1] + ny * o, b[0] + nx * o, b[1] + ny * o, R * 0.2f * f + 1f, Gl.alpha(Color.rgb(255, 70, 90), (f * 230f).toInt()))
                seg(c, a[0], a[1], b[0], b[1], R * 0.2f * f + 1f, Gl.alpha(Color.rgb(80, 255, 130), (f * 230f).toInt()))
                seg(c, a[0] - nx * o, a[1] - ny * o, b[0] - nx * o, b[1] - ny * o, R * 0.2f * f + 1f, Gl.alpha(Color.rgb(80, 150, 255), (f * 230f).toInt()))
            }
            40 -> for (i in 0 until n0) { // supernova: anillos que se expanden
                val t = p[i]; val f = 1f - t[2] / L
                if (f <= 0f || i % 6 != 0) continue
                val rr = R * (0.2f + 1.3f * (1f - f))
                ring(c, t[0], t[1], rr, R * 0.12f * f + 1f, Gl.alpha(if ((i / 6) % 2 == 0) col else Color.WHITE, (f * 220f).toInt()))
            }
        }
    }

    /** Resplandor con transparencia redondeada (pocas variantes: Gl.glow guarda un degradado por color y opacidad). */
    private fun glowQ(c: Canvas, x: Float, y: Float, r: Float, col: Int, a: Int) {
        val q = (a / 24) * 24
        if (q > 0) Gl.glow(c, x, y, r, col, q)
    }

    private fun mixc(a: Int, b: Int, k: Float): Int {
        val u = k.coerceIn(0f, 1f)
        val r = ((a shr 16) and 255) + ((((b shr 16) and 255) - ((a shr 16) and 255)) * u).toInt()
        val g = ((a shr 8) and 255) + ((((b shr 8) and 255) - ((a shr 8) and 255)) * u).toInt()
        val bl = (a and 255) + (((b and 255) - (a and 255)) * u).toInt()
        return Color.rgb(r, g, bl)
    }

    /** Estelas especiales de la Tienda (41..50): varias capas, brillo y movimiento propio. */
    private fun drawPremium(c: Canvas, st: Int, p: List<FloatArray>, col: Int, pal: IntArray, R: Float, tm: Float, L: Float) {
        val n0 = p.size
        when (st) {
            41 -> { // cola de dragón: cuerpo que ondula, escamas y chispas de fuego
                for (i in 1 until n0) {
                    val a = p[i - 1]; val b = p[i]; val f = 1f - b[2] / L
                    if (f <= 0f) continue
                    normal(p, i)
                    val wv = sin(i * 0.45f - tm * 9f) * R * 0.55f * (1f - f)
                    val wa = sin((i - 1) * 0.45f - tm * 9f) * R * 0.55f * (1f - (1f - a[2] / L))
                    val x0 = a[0] + nx * wa; val y0 = a[1] + ny * wa
                    val x1 = b[0] + nx * wv; val y1 = b[1] + ny * wv
                    seg(c, x0, y0, x1, y1, R * 1.25f * f + 2f, Gl.alpha(Gl.darken(col, 0.45f), (f * 235f).toInt()))
                    seg(c, x0, y0, x1, y1, R * 0.8f * f + 1.5f, Gl.alpha(col, (f * 240f).toInt()))
                    if (i % 3 == 0) {
                        val s2 = R * 0.55f * f
                        seg(c, x1 - nx * s2, y1 - ny * s2, x1 + (b[0] - a[0]) * 0.8f, y1 + (b[1] - a[1]) * 0.8f, 2f, Gl.white((f * 170f).toInt()))
                        seg(c, x1 + nx * s2, y1 + ny * s2, x1 + (b[0] - a[0]) * 0.8f, y1 + (b[1] - a[1]) * 0.8f, 2f, Gl.white((f * 170f).toInt()))
                    }
                    seg(c, x0, y0, x1, y1, R * 0.16f * f + 1f, Gl.white((f * 230f).toInt()))
                    if (i % 4 == 0 && f < 0.55f) {
                        val ex = x1 + (h(i, 41) - 0.5f) * R * 1.4f
                        val ey = y1 + b[2] * R * 1.5f
                        dot(c, ex, ey, R * 0.16f * (0.4f + f), Gl.alpha(0xFFFFB040.toInt(), (f * 2f * 255f).toInt().coerceAtMost(255)))
                    }
                }
            }
            42 -> { // río de estrellas: dos hebras en hélice con estrellitas que titilan
                for (sgn in 0..1) for (i in 0 until n0) {
                    val t = p[i]; val f = 1f - t[2] / L
                    if (f <= 0f) continue
                    normal(p, i)
                    val o = sin(i * 0.5f + tm * 6f + sgn * 3.1416f) * R * 0.85f * (0.4f + 0.6f * (1f - f))
                    val x = t[0] + nx * o; val y = t[1] + ny * o
                    val front = cos(i * 0.5f + tm * 6f + sgn * 3.1416f) > 0f
                    dot(c, x, y, R * (if (front) 0.2f else 0.12f) * (0.4f + 0.6f * f), Gl.alpha(if (sgn == 0) col else Color.WHITE, (f * (if (front) 255f else 140f)).toInt()))
                    if (i % 5 == sgn) {
                        val tw = 0.5f + 0.5f * sin(tm * 12f + i)
                        glowQ(c, x, y, R * 1.1f * f, col, (f * 140f).toInt())
                        star(c, x, y, R * (0.25f + 0.25f * tw) * f + 1f, tm * 3f + i, Gl.white((f * 255f).toInt()), Gl.alpha(col, (f * 255f).toInt()))
                    }
                }
            }
            43 -> { // rayo cósmico: haz grueso con núcleo blanco y arcos eléctricos
                for (i in 1 until n0) {
                    val a = p[i - 1]; val b = p[i]; val f = 1f - b[2] / L
                    if (f <= 0f) continue
                    seg(c, a[0], a[1], b[0], b[1], R * 2.1f * f, Gl.alpha(col, (f * 45f).toInt()))
                    seg(c, a[0], a[1], b[0], b[1], R * 1.1f * f, Gl.alpha(col, (f * 130f).toInt()))
                    seg(c, a[0], a[1], b[0], b[1], R * 0.38f * f + 1f, Gl.white((f * 255f).toInt()))
                }
                val fr2 = (tm * 24f).toInt()
                for (k in 0..1) {
                    var px = 0f; var py = 0f
                    for (i in 0 until n0) {
                        val t = p[i]; val f = 1f - t[2] / L
                        if (f <= 0f) { px = 0f; continue }
                        normal(p, i)
                        val o = (h(i + fr2 * 7, k + 43) - 0.5f) * R * 1.8f * f
                        val x = t[0] + nx * o; val y = t[1] + ny * o
                        if (px != 0f && i % 2 == 0) seg(c, px, py, x, y, 2.2f, Gl.alpha(mixc(col, Color.WHITE, 0.6f), (f * 230f).toInt()))
                        if (i % 2 == 0) { px = x; py = y }
                    }
                }
            }
            44 -> { // alas de luz: plumas que se abren a cada lado y se desvanecen
                for (i in 0 until n0) {
                    val t = p[i]; val f = 1f - t[2] / L
                    if (f <= 0f || i % 3 != 0) continue
                    normal(p, i)
                    val side = if ((i / 3) % 2 == 0) 1f else -1f
                    val open = (1f - f) * 0.9f + 0.1f
                    val len = R * (1.4f + 0.6f * h(i, 44)) * (0.5f + 0.5f * f)
                    val bx = t[0]; val by = t[1]
                    val tx = bx + nx * side * len * open + (h(i, 45) - 0.5f) * R * 0.3f
                    val ty = by + ny * side * len * open + len * 0.45f
                    path.reset()
                    path.moveTo(bx, by)
                    path.quadTo((bx + tx) / 2f + nx * side * R * 0.35f, (by + ty) / 2f - R * 0.5f, tx, ty)
                    path.quadTo((bx + tx) / 2f - nx * side * R * 0.1f, (by + ty) / 2f + R * 0.25f, bx, by)
                    val pp = Gl.p()
                    pp.color = Gl.alpha(mixc(col, Color.WHITE, 0.35f), (f * 190f).toInt())
                    c.drawPath(path, pp)
                    val ps = Gl.p()
                    ps.style = Paint.Style.STROKE
                    ps.strokeWidth = 1.6f
                    ps.color = Gl.white((f * 230f).toInt())
                    c.drawPath(path, ps)
                    glowQ(c, tx, ty, R * 0.7f * f, col, (f * 160f).toInt())
                }
            }
            45 -> { // fuego fénix: bolas de fuego que suben y pasan de blanco a rojo
                for (i in 0 until n0) {
                    val t = p[i]; val f = 1f - t[2] / L
                    if (f <= 0f || i % 2 != 0) continue
                    val age = 1f - f
                    val x = t[0] + sin(i * 1.7f + tm * 7f) * R * 0.35f * age
                    val y = t[1] - age * R * 1.2f
                    val cc = if (age < 0.25f) mixc(Color.WHITE, col, age / 0.25f) else if (age < 0.6f) mixc(col, 0xFFFF8A20.toInt(), (age - 0.25f) / 0.35f) else mixc(0xFFFF8A20.toInt(), 0xFFB01818.toInt(), (age - 0.6f) / 0.4f)
                    glowQ(c, x, y, R * (0.9f + age * 1.4f), cc, (f * 200f).toInt())
                    dot(c, x, y, R * 0.35f * f + 1f, Gl.alpha(mixc(cc, Color.WHITE, 0.5f), (f * 255f).toInt()))
                    if (i % 6 == 0 && age > 0.3f) dot(c, x + (h(i, 46) - 0.5f) * R * 2f, y - age * R * 2f, R * 0.12f, Gl.alpha(0xFFFFD060.toInt(), (f * 255f).toInt()))
                }
            }
            46 -> { // nebulosa: nubes de colores que se expanden, con estrellas
                for (i in 0 until n0) {
                    val t = p[i]; val f = 1f - t[2] / L
                    if (f <= 0f || i % 3 != 0) continue
                    val age = 1f - f
                    val cc = pal[(i / 3) % 4]
                    val ox = (h(i, 47) - 0.5f) * R * 1.6f * age
                    val oy = (h(i, 48) - 0.5f) * R * 1.6f * age
                    glowQ(c, t[0] + ox, t[1] + oy, R * (1.2f + age * 2.4f), cc, (f * 150f).toInt())
                    if (i % 6 == 0) {
                        val tw = 0.5f + 0.5f * sin(tm * 10f + i * 2f)
                        star(c, t[0] - ox * 1.5f, t[1] - oy * 1.5f, R * 0.22f * (0.5f + tw) + 1f, tm + i, Gl.white((f * 255f).toInt()), Gl.white((f * 120f).toInt()))
                    }
                }
            }
            47 -> { // arcoíris líquido: cinta gruesa de bandas que fluyen, con brillo
                for (i in 1 until n0) {
                    val a = p[i - 1]; val b = p[i]; val f = 1f - b[2] / L
                    if (f <= 0f) continue
                    normal(p, i)
                    val w = R * 1.5f * (0.35f + 0.65f * f)
                    for (k in 0 until 5) {
                        val o = (k - 2f) / 5f * w
                        val hc = Color.HSVToColor(floatArrayOf(((k * 60f + i * 9f - tm * 160f) % 360f + 360f) % 360f, 0.75f, 1f))
                        seg(c, a[0] + nx * o, a[1] + ny * o, b[0] + nx * o, b[1] + ny * o, w / 5f + 1.5f, Gl.alpha(hc, (f * 230f).toInt()))
                    }
                    seg(c, a[0] - nx * w * 0.25f, a[1] - ny * w * 0.25f, b[0] - nx * w * 0.25f, b[1] - ny * w * 0.25f, w * 0.12f + 1f, Gl.white((f * 200f).toInt()))
                }
            }
            48 -> { // burbujas mágicas: tornasoladas, crecen, flotan y revientan
                for (i in 0 until n0) {
                    val t = p[i]; val f = 1f - t[2] / L
                    if (f <= 0f || i % 3 != 0) continue
                    val age = 1f - f
                    val x = t[0] + (h(i, 49) - 0.5f) * R * 2f + sin(tm * 3f + i) * R * 0.2f
                    val y = t[1] - age * R * 1.5f
                    val rr = R * (0.25f + 0.55f * h(i, 50)) * (0.5f + age)
                    if (f < 0.12f) { ring(c, x, y, rr * (1.6f - f * 4f), 1.5f, Gl.white((f * 8f * 200f).toInt().coerceAtMost(200))); continue }
                    val hc = Color.HSVToColor(floatArrayOf((i * 37f + tm * 120f) % 360f, 0.55f, 1f))
                    dot(c, x, y, rr, Gl.alpha(col, (f * 50f).toInt()))
                    ring(c, x, y, rr, 2.2f, Gl.alpha(hc, (f * 230f).toInt()))
                    ring(c, x, y, rr * 0.82f, 1.2f, Gl.alpha(mixc(hc, col, 0.5f), (f * 120f).toInt()))
                    dot(c, x - rr * 0.35f, y - rr * 0.35f, rr * 0.2f, Gl.white((f * 230f).toInt()))
                }
            }
            49 -> { // pétalos de sakura: giran y caen
                for (i in 0 until n0) {
                    val t = p[i]; val f = 1f - t[2] / L
                    if (f <= 0f || i % 2 != 0) continue
                    val age = 1f - f
                    val x = t[0] + (h(i, 51) - 0.5f) * R * 2.2f + sin(tm * 2.5f + i) * R * 0.5f * age
                    val y = t[1] + age * R * 1.8f
                    val s2 = R * (0.3f + 0.15f * h(i, 52)) * (0.6f + 0.4f * f)
                    c.save()
                    c.translate(x, y)
                    c.rotate(i * 40f + tm * 140f * (if (i % 4 == 0) -1f else 1f))
                    c.scale(1f, 0.55f + 0.45f * sin(tm * 5f + i))
                    path.reset()
                    path.moveTo(0f, s2)
                    path.cubicTo(-s2 * 1.1f, s2 * 0.2f, -s2 * 0.6f, -s2 * 0.9f, -s2 * 0.12f, -s2 * 0.7f)
                    path.lineTo(0f, -s2 * 0.45f)
                    path.lineTo(s2 * 0.12f, -s2 * 0.7f)
                    path.cubicTo(s2 * 0.6f, -s2 * 0.9f, s2 * 1.1f, s2 * 0.2f, 0f, s2)
                    path.close()
                    val pp = Gl.p()
                    pp.color = Gl.alpha(mixc(0xFFFFA8C8.toInt(), col, 0.3f), (f * 240f).toInt())
                    c.drawPath(path, pp)
                    val pi = Gl.p()
                    pi.color = Gl.white((f * 120f).toInt())
                    c.drawCircle(0f, s2 * 0.1f, s2 * 0.28f, pi)
                    c.restore()
                }
            }
            50 -> { // estela infinita: tres hebras arcoíris en hélice, núcleo de luz y destellos
                for (i in 1 until n0) {
                    val a = p[i - 1]; val b = p[i]; val f = 1f - b[2] / L
                    if (f <= 0f) continue
                    seg(c, a[0], a[1], b[0], b[1], R * 0.9f * f, Gl.alpha(col, (f * 90f).toInt()))
                    seg(c, a[0], a[1], b[0], b[1], R * 0.25f * f + 1f, Gl.white((f * 240f).toInt()))
                }
                for (k in 0 until 3) {
                    var px = 0f; var py = 0f
                    for (i in 0 until n0) {
                        val t = p[i]; val f = 1f - t[2] / L
                        if (f <= 0f) { px = 0f; continue }
                        normal(p, i)
                        val o = sin(i * 0.42f - tm * 7f + k * 2.094f) * R * 1.0f * (0.35f + 0.65f * (1f - f))
                        val x = t[0] + nx * o; val y = t[1] + ny * o
                        if (px != 0f) {
                            val hc = Color.HSVToColor(floatArrayOf(((k * 120f + i * 8f - tm * 200f) % 360f + 360f) % 360f, 0.8f, 1f))
                            seg(c, px, py, x, y, R * 0.22f * f + 1.2f, Gl.alpha(hc, (f * 240f).toInt()))
                        }
                        px = x; py = y
                    }
                }
                for (i in 0 until n0) {
                    val t = p[i]; val f = 1f - t[2] / L
                    if (f <= 0f || i % 7 != 0) continue
                    val tw = 0.5f + 0.5f * sin(tm * 14f + i)
                    spark(c, t[0] + (h(i, 53) - 0.5f) * R * 2f, t[1] + (h(i, 54) - 0.5f) * R * 2f, R * (0.3f + 0.3f * tw) * f + 1f, Color.WHITE, (f * 255f).toInt())
                }
            }
        }
    }
}
