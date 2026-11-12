package com.nioro.spectrumcolor

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Esferas de la Tienda. No se dibujan con figuras: se "pintan" píxel por píxel, como hace un shader en una tarjeta
 * gráfica, en una imagen pequeña que luego se muestra escalada (PremiumDraw.kt). Así tienen volumen, luz, plasma,
 * fuego y niebla de verdad. Sin Android: se prueba y se previsualiza en la JVM (tools/PremiumPreview.kt).
 *
 * La imagen cubre un cuadrado de lado 2·M (la esfera mide 1 de radio), así que los efectos pueden salir de la bola
 * (alas, llamas, discos, rayos). La bola conserva el color de juego: todo se tiñe con él.
 */
object Premium {
    const val COUNT = 35
    /** Radio de la imagen / radio de la esfera. */
    const val M = 1.75f

    val names = arrayOf(
        // raras
        "Núcleo de plasma", "Metal líquido", "Océano abisal", "Tinta viva", "Holograma", "Cristal prismático",
        "Pulso vital", "Aurora cuántica",
        // épicas
        "Corazón de lava", "Tormenta eléctrica", "Fuego espectral", "Galaxia espiral", "Reactor", "Geoda",
        "Atardecer neón", "Vórtice carmesí", "Nebulosa viva", "Cometa helado",
        // legendarias
        "Ojo del dragón", "Portal dimensional", "Corona solar", "Bobina de Tesla", "Orbitales", "Fractal",
        "Sakura", "Fuegos artificiales", "Prisma de luz", "Hipernova",
        // míticas
        "Singularidad", "Fénix", "Mariposa de luz", "Púlsar", "Eclipse de diamante", "Génesis", "Infinito"
    )

    /** 0 rara, 1 épica, 2 legendaria, 3 mítica. */
    val tier = IntArray(COUNT) { if (it < 8) 0 else if (it < 18) 1 else if (it < 28) 2 else 3 }
    val price = intArrayOf(
        100, 130, 160, 190, 220, 260, 300, 350,
        400, 450, 500, 560, 620, 680, 750, 820, 900, 980,
        1100, 1200, 1300, 1400, 1500, 1650, 1800, 1950, 2100, 2250,
        2500, 2800, 3100, 3400, 3800, 4200, 5000
    )
    val tierNames = arrayOf("Rara", "Épica", "Legendaria", "Mítica")
    val tierColor = intArrayOf(0xFF4FB2FF.toInt(), 0xFFC064FF.toInt(), 0xFFFFC23D.toInt(), 0xFFFF5FD2.toInt())

    // ------------------------------------------------------------------ tablas rápidas
    private const val TN = 4096
    private const val TAU = 6.2831855f
    private val sinT = FloatArray(TN + 1) { kotlin.math.sin(it * TAU / TN) }
    private val perm = IntArray(512)
    private val rv = FloatArray(256)

    init {
        val r = java.util.Random(1337)
        val p = IntArray(256) { it }
        for (i in 255 downTo 1) { val j = r.nextInt(i + 1); val t = p[i]; p[i] = p[j]; p[j] = t }
        for (i in 0 until 512) perm[i] = p[i and 255]
        for (i in 0 until 256) rv[i] = r.nextFloat()
    }

    fun ff(v: Float): Int { val i = v.toInt(); return if (v < i) i - 1 else i }
    fun fr(v: Float): Float = v - ff(v)

    /** Seno por tabla (mucho más rápido que Math.sin). */
    fun s(x: Float): Float {
        val u = x * (TN / TAU)
        val i0 = ff(u)
        val f = u - i0
        val i = i0 and (TN - 1)
        val a = sinT[i]
        return a + (sinT[i + 1] - a) * f
    }
    fun c(x: Float): Float = s(x + 1.5707964f)

    fun hash(ix: Int, iy: Int): Float = rv[perm[(perm[ix and 255] + (iy and 255)) and 511] and 255]

    /** Ruido suave 0..1. */
    fun noise(x: Float, y: Float): Float {
        val xi = ff(x)
        val yi = ff(y)
        val fx = x - xi
        val fy = y - yi
        val ux = fx * fx * (3f - 2f * fx)
        val uy = fy * fy * (3f - 2f * fy)
        val x0 = xi and 255
        val y0 = yi and 255
        val a = perm[x0]
        val b = perm[x0 + 1]
        val v00 = rv[perm[a + y0] and 255]
        val v01 = rv[perm[a + y0 + 1] and 255]
        val v10 = rv[perm[b + y0] and 255]
        val v11 = rv[perm[b + y0 + 1] and 255]
        val l0 = v00 + (v10 - v00) * ux
        val l1 = v01 + (v11 - v01) * ux
        return l0 + (l1 - l0) * uy
    }

    /** Ruido fractal (varias capas de ruido, cada una el doble de fina y girada). */
    fun fbm(x: Float, y: Float, oct: Int): Float {
        var sum = 0f
        var amp = 0.5f
        var norm = 0f
        var xx = x
        var yy = y
        for (o in 0 until oct) {
            sum += amp * noise(xx, yy)
            norm += amp
            val nx = xx * 1.6f - yy * 1.2f + 1.7f
            val ny = xx * 1.2f + yy * 1.6f + 9.2f
            xx = nx; yy = ny
            amp *= 0.5f
        }
        return sum / norm
    }

    fun sat(v: Float): Float = if (v < 0f) 0f else if (v > 1f) 1f else v

    fun at2(y: Float, x: Float): Float {
        val ax = abs(x)
        val ay = abs(y)
        val mx = max(ax, ay)
        if (mx < 1e-9f) return 0f
        val a = min(ax, ay) / mx
        val s2 = a * a
        var r = ((-0.0464964749f * s2 + 0.15931422f) * s2 - 0.327622764f) * s2 * a + a
        if (ay > ax) r = 1.5707964f - r
        if (x < 0f) r = 3.1415927f - r
        if (y < 0f) r = -r
        return r
    }

    /** Geometría de la esfera para una imagen de n×n: se calcula una sola vez por tamaño. */
    class Geo(val n: Int) {
        val x = FloatArray(n * n); val y = FloatArray(n * n); val d = FloatArray(n * n); val z = FloatArray(n * n)
        val ang = FloatArray(n * n); val lon = FloatArray(n * n); val lat = FloatArray(n * n); val lnd = FloatArray(n * n)
        val dif = FloatArray(n * n); val spc = FloatArray(n * n); val fre = FloatArray(n * n)
        val halo = FloatArray(n * n); val cov = FloatArray(n * n)

        init {
            var lx = -0.45f; var ly = -0.58f; var lz = 0.68f
            var l = sqrt(lx * lx + ly * ly + lz * lz); lx /= l; ly /= l; lz /= l
            var hx = lx; var hy = ly; var hz = lz + 1f
            l = sqrt(hx * hx + hy * hy + hz * hz); hx /= l; hy /= l; hz /= l
            val pxs = 2f * M / n
            for (j in 0 until n) for (i in 0 until n) {
                val k = j * n + i
                val xx = ((i + 0.5f) / n * 2f - 1f) * M
                val yy = ((j + 0.5f) / n * 2f - 1f) * M
                val dd = sqrt(xx * xx + yy * yy)
                val zz = if (dd < 1f) sqrt(1f - dd * dd) else 0f
                x[k] = xx; y[k] = yy; d[k] = dd; z[k] = zz
                ang[k] = atan2(yy, xx)
                lon[k] = atan2(xx, max(zz, 0.001f))
                lat[k] = asin(yy.coerceIn(-1f, 1f))
                lnd[k] = ln(dd + 0.08f)
                if (dd < 1f) {
                    dif[k] = max(0f, xx * lx + yy * ly + zz * lz)
                    spc[k] = max(0f, xx * hx + yy * hy + zz * hz).pow(46f)
                    fre[k] = (1f - zz).pow(2.4f)
                }
                val e = (dd - 1f).coerceAtLeast(0f)
                val edge = sat((M - dd) / (M - 1f))
                halo[k] = exp(-e * 4.6f) * edge * edge
                cov[k] = sat((1f - dd) / pxs + 0.5f)
            }
        }
    }

    private val geos = HashMap<Int, Geo>()
    fun geo(n: Int): Geo = synchronized(geos) { geos.getOrPut(n) { Geo(n) } }

    /**
     * Pinta la esfera k en `out` (n×n, ARGB sin premultiplicar) con el color de juego col, en el segundo t.
     * Cada hilo debe usar su propio Painter.
     */
    class Painter {
        // píxel actual
        private var x = 0f; private var y = 0f; private var d = 0f; private var z = 0f
        private var ang = 0f; private var lon = 0f; private var lat = 0f; private var lnd = 0f; private var t = 0f
        // color base (el de juego) y derivados
        private var br = 0f; private var bg = 0f; private var bb = 0f
        private var lr = 0f; private var lg = 0f; private var lb = 0f
        // salida del estilo: albedo (se ilumina), emisión (brilla sola), fuera de la bola
        private var ar = 0f; private var ag = 0f; private var ab = 0f
        private var er = 0f; private var eg = 0f; private var eb = 0f
        private var or = 0f; private var og = 0f; private var ob = 0f
        private var ain = 1f
        // ajustes del estilo
        private var rimR = 1f; private var rimG = 1f; private var rimB = 1f
        private var rimK = 0.6f; private var haloK = 0.5f; private var specK = 1f; private var amb = 0.35f; private var alphaIn = 1f
        // por cuadro (calculado una vez fuera del bucle)
        private val q = FloatArray(16)
        // paleta tipo arcoíris
        private var hr = 0f; private var hg = 0f; private var hb = 0f

        private fun rainbow(h: Float) {
            hr = 0.5f + 0.5f * c(TAU * h)
            hg = 0.5f + 0.5f * c(TAU * (h - 0.333f))
            hb = 0.5f + 0.5f * c(TAU * (h - 0.667f))
        }

        private fun stars(sx: Float, sy: Float, scale: Float, th: Float): Float {
            val gx = sx * scale
            val gy = sy * scale
            val ix = ff(gx)
            val iy = ff(gy)
            val h = hash(ix, iy)
            if (h < th) return 0f
            val cx = ix + 0.25f + 0.5f * hash(ix + 17, iy + 3)
            val cy = iy + 0.25f + 0.5f * hash(ix + 5, iy + 29)
            val dx = gx - cx
            val dy = gy - cy
            val v = 1f - (dx * dx + dy * dy) * 11f
            if (v <= 0f) return 0f
            return v * v * (0.55f + 0.45f * s(t * (2f + h * 3f) + h * 40f))
        }

        private fun tm(v: Float): Int {
            val o = if (v <= 0.78f) (if (v < 0f) 0f else v) else 0.78f + 0.22f * (1f - 1f / (1f + (v - 0.78f) * 3.2f))
            return (o * 255f + 0.5f).toInt().coerceIn(0, 255)
        }

        fun paint(k: Int, out: IntArray, n: Int, col: Int, time: Float) = paint(k, out, n, col, time, 0, 1)

        /** Pinta solo las filas first, first+step, first+2·step…: varios Painter pueden repartirse una imagen. */
        fun paint(k: Int, out: IntArray, n: Int, col: Int, time: Float, first: Int, step: Int) {
            val g = geo(n)
            t = time
            br = ((col shr 16) and 255) / 255f
            bg = ((col shr 8) and 255) / 255f
            bb = (col and 255) / 255f
            lr = 0.55f * br + 0.45f; lg = 0.55f * bg + 0.45f; lb = 0.55f * bb + 0.45f
            setup(k)
            frame(k)
            val gx = g.x; val gy = g.y; val gd = g.d; val gz = g.z; val ga = g.ang; val glo = g.lon; val gla = g.lat
            val gln = g.lnd; val gdf = g.dif; val gsp = g.spc; val gfr = g.fre; val gha = g.halo; val gcv = g.cov
            var row = first
            while (row < n) {
            val rowEnd = row * n + n
            for (i in row * n until rowEnd) {
                val dd = gd[i]
                if (dd >= M) { out[i] = 0; continue }
                x = gx[i]; y = gy[i]; d = dd; z = gz[i]; ang = ga[i]; lon = glo[i]; lat = gla[i]; lnd = gln[i]
                val cv = gcv[i]
                var cr = 0f; var cg = 0f; var cb = 0f; var ca = 0f
                if (cv > 0f) {
                    ar = 0f; ag = 0f; ab = 0f; er = 0f; eg = 0f; eb = 0f; ain = alphaIn
                    inside(k)
                    val lit = amb + (1f - amb) * gdf[i]
                    val sp = gsp[i] * specK
                    val fz = gfr[i] * rimK
                    cr = ar * lit + er + sp + fz * rimR
                    cg = ag * lit + eg + sp + fz * rimG
                    cb = ab * lit + eb + sp + fz * rimB
                    ca = ain
                }
                if (cv < 1f) {
                    or = 0f; og = 0f; ob = 0f
                    outside(k)
                    val h = haloK * gha[i]
                    val qr = rimR * h + or
                    val qg = rimG * h + og
                    val qb = rimB * h + ob
                    val mx = max(qr, max(qg, qb))
                    val oa = if (mx > 1f) 1f else mx
                    if (cv <= 0f) {
                        if (oa <= 0.004f) { out[i] = 0; continue }
                        val inv = 1f / oa
                        cr = qr * inv; cg = qg * inv; cb = qb * inv; ca = oa
                    } else {
                        // borde: mezcla (premultiplicada) de la bola y lo de afuera
                        val a1 = ca * cv
                        val a2 = oa * (1f - cv)
                        val inv2 = if (oa > 0f) 1f / oa else 0f
                        val aa = a1 + a2
                        if (aa <= 0.004f) { out[i] = 0; continue }
                        cr = (cr * a1 + qr * inv2 * a2) / aa
                        cg = (cg * a1 + qg * inv2 * a2) / aa
                        cb = (cb * a1 + qb * inv2 * a2) / aa
                        ca = aa
                    }
                }
                val a8 = (ca * 255f + 0.5f).toInt().coerceIn(0, 255)
                out[i] = (a8 shl 24) or (tm(cr) shl 16) or (tm(cg) shl 8) or tm(cb)
            }
            row += step
            }
        }

        private fun setup(k: Int) {
            rimR = lr; rimG = lg; rimB = lb
            rimK = 0.55f; haloK = 0.45f + 0.12f * tier[k]; specK = 1f; amb = 0.35f; alphaIn = 1f
            when (k) {
                1 -> { specK = 1.6f; rimR = 1f; rimG = 1f; rimB = 1f; rimK = 0.3f; haloK = 0.35f }
                3 -> { specK = 1.3f; rimK = 0.35f; haloK = 0.3f; amb = 0.55f }
                4 -> { alphaIn = 0.86f; rimK = 1.1f; rimR = 0.5f * br + 0.3f; rimG = 0.5f * bg + 0.5f; rimB = 1f }
                5 -> { specK = 1.5f; amb = 0.45f }
                7, 8, 11, 16 -> rimK = 0.7f
                13 -> { specK = 0.6f; amb = 0.5f; haloK = 0.4f }
                17 -> { specK = 1.4f; rimR = 0.85f; rimG = 0.95f; rimB = 1f }
                20 -> { specK = 0.2f; rimR = 1f; rimG = 0.75f * lg + 0.2f; rimB = 0.3f * lb; rimK = 0.2f; haloK = 0.9f }
                26 -> { alphaIn = 0.93f; specK = 1.4f; rimR = 1f; rimG = 1f; rimB = 1f; rimK = 0.8f }
                28 -> { specK = 0.5f; rimK = 0.3f; haloK = 0.25f }
                32 -> { specK = 0.15f; rimK = 0.05f; haloK = 0.1f; rimR = 1f; rimG = 1f; rimB = 1f }
                34 -> { rimK = 0.9f; specK = 1.5f }
            }
        }

        /** Cálculos que valen para todo el cuadro (posiciones de cosas que se mueven). */
        private fun frame(k: Int) {
            when (k) {
                9 -> { // tormenta: rayo activo, semilla y destello
                    val cyc = ff(t * 1.3f)
                    val ph = fr(t * 1.3f)
                    q[0] = cyc * 2.7f
                    q[1] = if (ph < 0.5f && hash(cyc, 7) > 0.12f) (1f - ph / 0.5f) * (0.65f + 0.35f * s(t * 90f)) else 0f
                    q[2] = (hash(cyc, 11) - 0.5f) * 0.6f
                }
                18 -> { // ojo: hacia dónde mira y parpadeo
                    q[0] = 0.2f * s(t * 0.7f) + 0.07f * s(t * 2.3f)
                    q[1] = 0.12f * s(t * 0.9f + 1f)
                    val b = fr(t / 4.6f)
                    q[2] = if (b < 0.06f) s(b / 0.06f * 3.1416f) else 0f
                    q[3] = 0.75f + 0.25f * s(t * 1.3f)
                }
                21 -> for (j in 0 until 6) q[j] = j * 1.0472f + 0.55f * s(t * 0.8f + j * 1.3f) + t * 0.25f
                22 -> for (j in 0 until 3) {
                    val phi = j * 1.0472f + t * 0.35f
                    q[j * 4] = c(phi); q[j * 4 + 1] = s(phi)
                    q[j * 4 + 2] = t * (2.2f + j * 0.45f) + j * 2f
                }
                31 -> { q[0] = c(t * 4.2f); q[1] = s(t * 4.2f) }
                32 -> { val a = -0.85f + 0.35f * s(t * 0.35f); q[0] = c(a); q[1] = s(a); q[2] = 0.65f + 0.35f * s(t * 2.2f) }
                34 -> { // corredor del infinito
                    val tau = t * 1.6f
                    val st = s(tau); val ct = c(tau)
                    val den = 1f + st * st
                    q[0] = 0.72f * ct / den
                    q[1] = 0.72f * st * ct / den
                }
            }
        }

        // ================================================================== dentro de la bola
        private fun inside(k: Int) {
            when (k) {
                0 -> { // núcleo de plasma
                    val qx = x * 1.2f; val qy = y * 1.2f
                    val v = fbm(qx * 1.8f + t * 0.35f, qy * 1.8f - t * 0.3f, 3)
                    val w = abs(s(v * 9f + ang * 2f + t * 1.6f))
                    var f1 = sat(1f - w * 4f); f1 *= f1 * f1
                    val w2 = abs(s(v * 7f - ang * 3f - t * 1.1f + 1.7f))
                    var f2 = sat(1f - w2 * 5f); f2 = f2 * f2 * f2 * 0.6f
                    val core = 1f / (1f + d * d * 14f) * (0.85f + 0.15f * s(t * 5f))
                    val f = f1 + f2
                    ar = br * 0.1f; ag = bg * 0.1f; ab = bb * 0.12f + 0.02f
                    er = br * f * 1.5f + f1 * f1 * 0.6f + core * (br * 1.3f + 0.7f)
                    eg = bg * f * 1.5f + f1 * f1 * 0.6f + core * (bg * 1.3f + 0.7f)
                    eb = bb * f * 1.5f + f1 * f1 * 0.6f + core * (bb * 1.3f + 0.7f)
                }
                1 -> { // metal líquido
                    val rip = s(d * 22f - t * 5f) * 0.05f * (1f - d) + (fbm(x * 2.5f + t * 0.5f, y * 2.5f - t * 0.2f, 2) - 0.5f) * 0.35f
                    val rvv = y * (1.3f - 0.4f * z) + rip
                    var env = if (rvv < -0.5f) 0.95f else if (rvv < 0.05f) 0.95f - (rvv + 0.5f) * 1.7f else if (rvv < 0.25f) 0.05f + (rvv - 0.05f) * 0.8f else 0.21f + (rvv - 0.25f) * 0.3f
                    val win = sat(1f - abs(rvv + 0.62f) * 12f)
                    env = env * (0.85f + 0.15f * s(x * 3f + t)) + win * 0.9f
                    val e4 = env * env * env * env
                    er = br * env * 1.05f + e4 * 0.7f
                    eg = bg * env * 1.05f + e4 * 0.7f
                    eb = bb * env * 1.05f + e4 * 0.7f
                }
                2 -> { // océano abisal
                    val depth = sat((y + 1f) * 0.5f)
                    val dk = 1f - depth * 0.75f
                    ar = (br * 0.25f + 0.02f) * dk; ag = (bg * 0.25f + 0.1f) * dk; ab = (bb * 0.25f + 0.22f) * dk
                    val n1 = noise(x * 3.2f + t * 0.45f, y * 3.2f + t * 0.3f)
                    val n2 = noise(x * 3.2f - t * 0.35f + 5f, y * 3.2f + t * 0.2f)
                    var cau = sat(1f - abs(n1 - n2) * 7f); cau *= cau; cau *= cau * (1.3f - depth)
                    var ray = sat(0.5f + 0.5f * s(x * 6f + y * 1.5f + t * 0.6f)); ray *= ray; ray *= ray * sat(0.4f - y) * 0.4f
                    val dots = stars(x + s(t * 0.3f) * 0.1f, y - t * 0.12f, 9f, 0.88f) * (0.4f + depth)
                    val bub = bubbles(x, y, t, 5f)
                    er = (br * 0.5f + 0.25f) * cau * 1.4f + ray * lr + dots * 0.2f + bub
                    eg = (bg * 0.5f + 0.55f) * cau * 1.4f + ray * lg + dots * 1.0f + bub
                    eb = (bb * 0.5f + 0.6f) * cau * 1.4f + ray * lb + dots * 1.0f + bub
                }
                3 -> { // tinta viva en leche
                    val q1 = fbm(x * 2f + t * 0.15f, y * 2f - t * 0.1f, 3)
                    val w = fbm(x * 2f + q1 * 3f, y * 2f + q1 * 3f + t * 0.2f, 3)
                    val ink = sat((w - 0.47f) * 7f)
                    val edge = sat(1f - abs(w - 0.47f) * 28f)
                    val mr = 0.93f; val mg = 0.93f; val mb = 0.96f
                    ar = mr + (br * 0.85f - mr) * ink - edge * 0.25f
                    ag = mg + (bg * 0.85f - mg) * ink - edge * 0.25f
                    ab = mb + (bb * 0.85f - mb) * ink - edge * 0.25f
                    er = edge * br * 0.25f; eg = edge * bg * 0.25f; eb = edge * bb * 0.25f
                }
                4 -> { // holograma
                    var u = lon * 2f + t * 0.6f
                    val row = ff((y + 1f) * 9f)
                    if (hash(row, ff(t * 6f)) > 0.86f) u += 0.35f
                    val v = lat * 2f
                    val gu = fr(u * 3f); val gv = fr(v * 3f + 0.5f * ff(u * 3f))
                    val grid = sat(1f - min(min(gu, 1f - gu), min(gv, 1f - gv)) * 14f)
                    val scan = 0.55f + 0.45f * s(y * 120f + t * 12f)
                    val sweep = sat(1f - abs(y - (fr(t * 0.45f) * 2.6f - 1.3f)) * 6f)
                    val f = grid * 0.9f * scan + 0.12f * scan + sweep * 0.7f
                    ar = 0.02f; ag = 0.05f; ab = 0.08f
                    er = (br * 0.5f + 0.1f) * f; eg = (bg * 0.5f + 0.5f) * f; eb = (bb * 0.4f + 0.6f) * f
                    ain = 0.6f + 0.4f * sat(f + (1f - z))
                }
                5 -> { // cristal prismático (talla brillante)
                    val ring = if (d < 0.42f) 0 else if (d < 0.74f) 1 else 2
                    val nsec = if (ring == 2) 16f else 8f
                    val sec = (ang + 3.1416f) / TAU * nsec + (if (ring == 1) 0.5f else 0f) + t * 0.05f
                    val si = ff(sec)
                    val fa = fr(sec)
                    val h = hash(si * 3 + ring, 3)
                    val shim = 0.5f + 0.5f * s(t * 2.6f + h * 20f)
                    var b = if (ring == 0) 0.55f + 0.25f * shim else (0.2f + 0.8f * h * (0.55f + 0.45f * shim)) * (0.75f + 0.5f * abs(fa - 0.5f))
                    val rdist = if (ring == 0) abs(d - 0.42f) else min(abs(d - 0.42f), abs(d - 0.74f))
                    val edge = sat(1f - min(if (ring == 0) 9f else min(fa, 1f - fa) * d * 30f, rdist * 45f))
                    val sw = fr((ang + 3.1416f) / TAU - t * 0.22f)
                    val sweep = sat(1f - min(sw, 1f - sw) * 10f) * (0.2f + 0.8f * h)
                    val fire = if (h > 0.72f) sat((b - 0.5f) * 3f) else 0f
                    rainbow(h * 3f + t * 0.3f)
                    b *= b
                    ar = br * (0.15f + 0.85f * b); ag = bg * (0.15f + 0.85f * b); ab = bb * (0.15f + 0.85f * b)
                    er = hr * fire * 0.8f + edge * 0.45f + sweep * 0.7f + br * b * 0.3f
                    eg = hg * fire * 0.8f + edge * 0.45f + sweep * 0.7f + bg * b * 0.3f
                    eb = hb * fire * 0.8f + edge * 0.45f + sweep * 0.7f + bb * b * 0.3f
                }
                6 -> { // pulso vital (electrocardiograma)
                    val ph = fr((x + 1.2f) * 0.42f - t * 0.75f)
                    val ecg = ecg(ph)
                    val yl = -ecg * 0.62f + 0.05f
                    val dist = abs(y - yl)
                    var line = sat(1f - dist * 20f); line *= line
                    var glow = sat(1f - dist * 7f); glow *= glow * 0.45f
                    val head = sat(1f - ph * 2.2f)
                    val gx = fr(x * 5f + 0.5f); val gy = fr(y * 5f + 0.5f)
                    val grid = sat(1f - min(min(gx, 1f - gx), min(gy, 1f - gy)) * 30f) * 0.12f
                    val beat = sat(1f - fr(t * 0.75f) * 5f)
                    val f = (line + glow) * (0.35f + 0.65f * head)
                    ar = 0.03f; ag = 0.04f; ab = 0.05f
                    er = br * (f * 1.6f + grid + beat * 0.35f) + line * head * 0.6f
                    eg = bg * (f * 1.6f + grid + beat * 0.35f) + line * head * 0.6f
                    eb = bb * (f * 1.6f + grid + beat * 0.35f) + line * head * 0.6f
                }
                7 -> { // aurora cuántica
                    ar = 0.02f; ag = 0.03f; ab = 0.07f
                    var acc = 0f; var top = 0f
                    for (j in 0 until 2) {
                        val yc = -0.12f + 0.2f * s(x * 2.2f + t * 0.6f + j * 2f) + 0.3f * (fbm(x * 1.4f + t * 0.22f + j * 7f, j * 3f, 2) - 0.5f) + j * 0.22f
                        val dist = y - yc
                        val band = if (dist < 0f) sat(1f + dist * 2.4f) else sat(1f - dist * 9f)
                        val rays = 0.35f + 0.65f * fbm(x * 15f + j * 5f, t * 0.7f + j, 2)
                        val v = band * band * rays
                        acc += v
                        top += v * sat(-dist * 2.5f)
                    }
                    val st = stars(x, y, 11f, 0.9f) * 0.8f
                    er = acc * (br * 0.4f + 0.1f) + top * 0.7f + st
                    eg = acc * (bg * 0.4f + 0.7f) + top * 0.2f + st
                    eb = acc * (bb * 0.4f + 0.35f) + top * 0.9f + st
                }
                8 -> { // corazón de lava
                    val u = lon * 1.1f + t * 0.22f
                    val v = lat * 1.1f
                    val n1 = fbm(u * 2.2f, v * 2.2f, 3)
                    val cr = abs(fbm(u * 3.1f + 5f, v * 3.1f + 2f, 3) - 0.5f)
                    var seam = sat(1f - cr * 13f); seam *= seam
                    val heat = seam * (0.75f + 0.25f * s(t * 3f + n1 * 10f))
                    val rock = 0.06f + n1 * 0.12f
                    ar = rock * 1.1f + br * 0.04f; ag = rock * 0.9f + bg * 0.03f; ab = rock * 0.8f + bb * 0.03f
                    val hr2 = br * 0.45f + 0.6f; val hg2 = bg * 0.45f + 0.28f; val hb2 = bb * 0.45f + 0.02f
                    val rimIn = (1f - z) * (1f - z) * 0.4f
                    val h3 = heat * heat * heat
                    er = hr2 * (heat * 2f + rimIn) + h3 * 0.7f
                    eg = hg2 * (heat * 2f + rimIn) + h3 * 0.6f
                    eb = hb2 * (heat * 2f + rimIn) + h3 * 0.3f
                }
                9 -> { // tormenta eléctrica
                    val cl = fbm(x * 2.4f + t * 0.4f, y * 2.4f + t * 0.15f, 4)
                    val fl = q[1]
                    val seed = q[0]
                    val bx = q[2] + 0.28f * s(y * 5f + seed) + 0.1f * s(y * 13f + seed * 2f) + 0.05f * s(y * 31f + seed * 3f)
                    val dist = abs(x - bx)
                    var bolt = sat(1f - dist * 30f); bolt *= bolt
                    // rama
                    val by2 = y - 0.1f
                    val bx2 = bx + (if (by2 > 0f) by2 * 0.9f + 0.06f * s(by2 * 20f + seed) else 99f)
                    var br2 = if (by2 > 0f && by2 < 0.7f) sat(1f - abs(x - bx2) * 34f) else 0f
                    br2 *= br2 * 0.7f
                    var glow = sat(1f - dist * 5f); glow *= glow
                    val b = (bolt + br2) * (fl + 0.12f)
                    val lit = cl * (0.35f + fl * 0.9f)
                    val inner = 1f / (1f + d * d * 6f) * (0.35f + 0.15f * s(t * 7f)) * cl
                    var crk = abs(s(fbm(x * 3f + t * 2f, y * 3f, 2) * 14f + t * 4f)); crk = sat(1f - crk * 7f); crk *= crk * 0.5f
                    ar = 0.1f + lit * 0.45f + br * 0.08f; ag = 0.11f + lit * 0.45f + bg * 0.08f; ab = 0.15f + lit * 0.5f + bb * 0.08f
                    er = b * (0.8f + br) + (glow * fl + inner + crk) * br * 0.8f + crk * 0.3f
                    eg = b * (0.8f + bg) + (glow * fl + inner + crk) * bg * 0.8f + crk * 0.3f
                    eb = b * (0.9f + bb) + (glow * fl + inner + crk) * bb * 0.8f + crk * 0.35f
                }
                10 -> { // fuego espectral
                    val v = flame(x, y, 0.02f) * (0.75f + 0.25f * z)
                    fireRamp(v, 0.35f, 0.9f, 1f)
                    ar = br * 0.08f; ag = bg * 0.08f; ab = bb * 0.12f
                }
                11 -> { // galaxia espiral
                    val a = ang - t * 0.35f
                    val arm0 = 0.5f + 0.5f * c(2f * (a + lnd * 2.3f))
                    var arm = arm0 * arm0; arm *= arm
                    val dust = fbm(x * 4f + t * 0.1f, y * 4f, 3)
                    val dens = arm * sat(1.05f - d) * (0.55f + 0.7f * dust)
                    val bulge = 1f / (1f + d * d * 30f)
                    val st = stars(x, y, 13f, 0.88f)
                    val pinkR = 1f; val pinkG = 0.45f; val pinkB = 0.8f
                    val mixk = sat(d * 1.3f)
                    ar = 0.02f; ag = 0.02f; ab = 0.06f
                    er = (br + (pinkR - br) * mixk * 0.5f) * dens * 1.5f + bulge * 1.2f + st
                    eg = (bg + (pinkG - bg) * mixk * 0.5f) * dens * 1.5f + bulge * 1.05f + st
                    eb = (bb + (pinkB - bb) * mixk * 0.5f) * dens * 1.5f + bulge * 0.8f + st
                }
                12 -> { // reactor
                    val rr = d * 6f
                    val ri = ff(rr)
                    val frr = rr - ri
                    val band = sat(1f - abs(frr - 0.5f) * 6f)
                    val dir = if (ri % 2 == 0) 1f else -1f
                    val rot = t * (0.6f + ri * 0.35f) * dir
                    val seg = (ang + rot + 3.1416f) / TAU * (6 + ri * 4)
                    val segf = fr(seg)
                    val gap = sat(min(segf, 1f - segf) * 14f)
                    val on = if (hash(ri, ff(seg)) > 0.3f) 1f else 0.15f
                    val glow = band * gap * on * (0.7f + 0.3f * s(t * 4f + ri))
                    val core = 1f / (1f + d * d * 45f) * (1.1f + 0.4f * s(t * 6f))
                    val hx = fr(x * 9f); val hy = fr(y * 9f)
                    val metal = 0.05f + 0.04f * sat(1f - min(min(hx, 1f - hx), min(hy, 1f - hy)) * 10f)
                    ar = metal; ag = metal; ab = metal * 1.2f
                    val g2 = glow * glow
                    er = br * glow * 1.4f + g2 * 0.5f + core * (br + 0.8f)
                    eg = bg * glow * 1.4f + g2 * 0.5f + core * (bg + 0.8f)
                    eb = bb * glow * 1.4f + g2 * 0.5f + core * (bb + 0.8f)
                }
                13 -> { // geoda
                    if (d > 0.84f) {
                        val rock = 0.6f + fbm(x * 9f, y * 9f, 2) * 0.6f
                        ar = 0.3f * rock + br * 0.05f; ag = 0.24f * rock + bg * 0.05f; ab = 0.2f * rock + bb * 0.05f
                        val inner = sat(1f - (d - 0.84f) * 25f)
                        er = br * inner * 0.5f; eg = bg * inner * 0.5f; eb = bb * inner * 0.5f
                    } else {
                        val sec = (ang + 3.1416f) / TAU * 26f
                        val si = ff(sec)
                        val h = hash(si, 9)
                        val len = 0.84f - (0.25f + h * 0.4f)
                        val facet = fr(sec)
                        if (d > len + abs(facet - 0.5f) * 0.22f) {
                            val shade = if (facet < 0.5f) 0.55f + facet else 1.3f - facet
                            val tip = sat(1f - (d - len) * 12f)
                            var gl = sat(s(t * 3f + h * 30f)); gl *= gl; gl *= gl; gl *= gl
                            ar = lr * shade * 0.9f; ag = lg * shade * 0.9f; ab = lb * shade * 0.9f
                            er = br * 0.25f + tip * gl * 1.4f; eg = bg * 0.25f + tip * gl * 1.4f; eb = bb * 0.25f + tip * gl * 1.4f
                        } else {
                            val gw = 1f - d / max(len, 0.05f)
                            val st = stars(x, y, 16f, 0.8f)
                            ar = 0.04f; ag = 0.04f; ab = 0.05f
                            er = br * gw * 1.1f + st * 0.6f; eg = bg * gw * 1.1f + st * 0.6f; eb = bb * gw * 1.1f + st * 0.6f
                        }
                    }
                }
                14 -> { // atardecer neón (synthwave)
                    if (y > 0.06f) {
                        val dep = 0.32f / (y + 0.01f)
                        val gx = x * dep * 1.4f
                        val gz = dep + t * 1.6f
                        val fx = fr(gx); val fz = fr(gz)
                        val w = 0.06f * dep.coerceAtMost(4f)
                        val lx = sat(1f - min(fx, 1f - fx) / w)
                        val lz = sat(1f - min(fz, 1f - fz) / (w * 0.8f))
                        val line = max(lx, lz) * sat(1.4f - dep * 0.18f)
                        val hz = sat(1f - (y - 0.06f) * 5f)
                        ar = 0.05f; ag = 0.01f; ab = 0.08f
                        er = (br * 0.5f + 0.55f) * line * 1.3f + hz * 0.6f
                        eg = (bg * 0.4f + 0.12f) * line * 1.3f + hz * 0.2f
                        eb = (bb * 0.5f + 0.5f) * line * 1.3f + hz * 0.5f
                    } else {
                        val sky = sat((y + 1f) * 0.9f)
                        ar = 0.05f + sky * 0.35f; ag = 0.02f + sky * 0.05f; ab = 0.12f + sky * 0.2f
                        val sx = x; val sy = y + 0.1f
                        val sd = sqrt(sx * sx + sy * sy)
                        if (sd < 0.5f) {
                            val cut = sy > -0.22f && fr((sy + t * 0.05f) * 13f) < 0.35f + sy * 0.6f
                            if (!cut) {
                                val gy = sat((sy + 0.5f) / 1f)
                                er = 1f; eg = 0.85f - gy * 0.55f + bg * 0.2f; eb = 0.3f - gy * 0.1f + bb * 0.4f
                                er = er * 0.6f + br * 0.4f + 0.3f
                            }
                        } else {
                            val sg = sat(1f - (sd - 0.5f) * 5f)
                            er = sg * 0.6f * (0.5f + br); eg = sg * 0.2f; eb = sg * 0.4f * (0.5f + bb)
                            val st = stars(x, y, 12f, 0.9f) * sat(-y * 2f)
                            er += st; eg += st; eb += st
                        }
                    }
                }
                15 -> { // vórtice carmesí
                    val sw = ang * 2f - d * 5f + t * 3f
                    val bands = 0.5f + 0.5f * s(sw)
                    val streak = fbm(sw * 0.6f, d * 6f - t * 2f, 3)
                    val eye = sat(d * 4.5f)
                    var b2 = bands * bands
                    val b8 = b2 * b2 * b2 * b2
                    b2 *= (0.35f + streak) * eye
                    val cr2 = br * 0.6f + 0.4f; val cg2 = bg * 0.6f + 0.04f; val cb2 = bb * 0.6f + 0.1f
                    ar = 0.08f; ag = 0.02f; ab = 0.03f
                    er = cr2 * b2 * 1.6f + b8 * 0.45f * eye; eg = cg2 * b2 * 1.6f + b8 * 0.45f * eye; eb = cb2 * b2 * 1.6f + b8 * 0.45f * eye
                }
                16 -> { // nebulosa viva
                    val q1 = fbm(x * 1.6f + t * 0.08f, y * 1.6f, 3)
                    val q2 = fbm(x * 1.6f + 3.1f, y * 1.6f - t * 0.06f, 3)
                    val r1 = fbm(x * 1.6f + q1 * 2.5f + t * 0.12f, y * 1.6f + q2 * 2.5f, 3)
                    val v = r1 * r1 * 1.9f
                    val st = stars(x, y, 14f, 0.9f)
                    ar = 0.02f; ag = 0.02f; ab = 0.05f
                    er = v * (br * 0.6f + q1 * 0.7f) + st; eg = v * (bg * 0.6f + q2 * 0.25f) + st; eb = v * (bb * 0.6f + (1f - q1) * 0.6f) + st
                }
                17 -> { // cometa helado
                    val cr = abs(fbm(x * 4f, y * 4f, 3) - 0.5f)
                    var vein = sat(1f - cr * 22f); vein *= vein
                    val mist = fbm(x * 2f + t * 0.2f, y * 2f - t * 0.1f, 2)
                    val gl = stars(x, y, 10f, 0.9f) * 1.6f
                    ar = 0.5f + br * 0.3f; ag = 0.6f + bg * 0.25f; ab = 0.72f + bb * 0.2f
                    er = vein * 0.5f + mist * br * 0.3f + gl; eg = vein * 0.6f + mist * bg * 0.3f + gl; eb = vein * 0.75f + mist * bb * 0.3f + gl
                }
                18 -> { // ojo del dragón
                    val ex = x - q[0]; val ey = y - q[1]
                    val ed = sqrt(ex * ex + ey * ey)
                    val ir = 0.62f
                    if (ed < ir) {
                        val ea = at2(ey, ex)
                        val fib = 0.5f + 0.5f * s(ea * 38f + fbm(ea * 3f, ed * 4f, 2) * 6f)
                        val u = ed / ir
                        val ring = sat((u - 0.82f) * 6f)
                        val gold = 1f - u
                        var irR = (br * u + 1f * gold) * (0.55f + 0.45f * fib) * (1f - ring * 0.8f)
                        var irG = (bg * u + 0.72f * gold) * (0.55f + 0.45f * fib) * (1f - ring * 0.8f)
                        var irB = (bb * u + 0.15f * gold) * (0.55f + 0.45f * fib) * (1f - ring * 0.8f)
                        val pw = 0.07f + 0.05f * q[3]
                        val pxv = ex / pw; val pyv = ey / 0.5f
                        val pu = pxv * pxv + pyv * pyv
                        val pupil = sat((1.1f - pu) * 3f)
                        irR *= 1f - pupil; irG *= 1f - pupil; irB *= 1f - pupil
                        ar = irR * 0.5f; ag = irG * 0.5f; ab = irB * 0.5f
                        er = irR * 0.9f; eg = irG * 0.9f; eb = irB * 0.9f
                    } else {
                        val hx = x * 7f; val hy = y * 7f + 0.5f * ff(x * 7f)
                        val sx = fr(hx); val sy = fr(hy)
                        val edge = sat(1f - min(min(sx, 1f - sx), min(sy, 1f - sy)) * 7f)
                        val sh = 0.12f + 0.1f * hash(ff(hx), ff(hy))
                        ar = br * sh + 0.03f; ag = bg * sh + 0.02f; ab = bb * sh + 0.02f
                        er = br * edge * 0.35f; eg = bg * edge * 0.35f; eb = bb * edge * 0.35f
                    }
                    val lid = q[2]
                    if (abs(y) > 1f - lid * 1.05f) { ar = br * 0.15f + 0.04f; ag = bg * 0.15f + 0.03f; ab = bb * 0.15f + 0.03f; er = 0f; eg = 0f; eb = 0f }
                }
                19 -> { // portal dimensional
                    val sw = ang + 2.2f / (d + 0.18f) - t * 2.2f
                    val sp = 0.5f + 0.5f * s(sw * 3f)
                    val streak = fbm(sw * 0.8f, d * 3f - t, 2)
                    val sp3 = sp * sp * sp
                    val core = 1f / (1f + d * d * 25f) * 1.5f
                    ar = 0.05f; ag = 0.02f; ab = 0.08f
                    er = br * sp3 * 1.6f * (0.3f + d) + 0.55f * streak * 0.5f + core
                    eg = bg * sp3 * 1.6f * (0.3f + d) + 0.15f * streak * 0.5f + core
                    eb = bb * sp3 * 1.6f * (0.3f + d) + 0.9f * streak * 0.5f + core
                }
                20 -> { // corona solar (superficie)
                    val gran = fbm(lon * 5f + t * 0.15f, lat * 5f, 3)
                    val ld = 0.5f + 0.5f * z
                    val spot = sat((fbm(lon * 1.5f + t * 0.05f + 9f, lat * 1.5f, 2) - 0.66f) * 8f)
                    val k2 = (0.8f + gran * 0.65f) * ld * (1f - spot * 0.75f) * 1.35f
                    er = (0.65f + br * 0.4f) * k2; eg = (0.42f + bg * 0.35f) * k2; eb = (0.08f + bb * 0.3f) * k2
                }
                21 -> { // bobina de Tesla (lámpara de plasma)
                    var fil = 0f
                    for (j in 0 until 6) {
                        val aw = q[j] + 0.25f * s(d * 7f - t * 4f + j)
                        var df = ang - aw
                        df -= TAU * ff(df / TAU + 0.5f)
                        val v = sat(1f - abs(df) * d * 22f)
                        if (v > fil) fil = v
                    }
                    fil = fil * fil * sat(d * 2.2f - 0.25f) * (1f + sat((d - 0.8f) * 6f))
                    val core = if (d < 0.17f) 1.2f else 1f / (1f + (d - 0.17f) * 40f) * 0.7f
                    ar = 0.05f; ag = 0.02f; ab = 0.08f
                    val vr = br * 0.6f + 0.4f; val vg = bg * 0.6f + 0.1f; val vb = bb * 0.6f + 0.45f
                    er = vr * fil * 1.6f + fil * fil * 0.7f + core * (br + 0.6f)
                    eg = vg * fil * 1.6f + fil * fil * 0.7f + core * (bg + 0.6f)
                    eb = vb * fil * 1.6f + fil * fil * 0.7f + core * (bb + 0.6f)
                }
                22 -> { // orbitales: núcleo, nube y órbitas por delante
                    val nb = fbm(x * 9f + t * 0.2f, y * 9f, 2)
                    val nuc = sat((0.3f - d) * 12f)
                    val cloud = fbm(x * 3f + t * 0.3f, y * 3f, 2) * 0.35f
                    ar = br * (0.12f + nuc * nb * 0.9f); ag = bg * (0.12f + nuc * nb * 0.9f); ab = bb * (0.12f + nuc * nb * 0.9f) + 0.03f
                    er = br * cloud + nuc * nb * 0.4f; eg = bg * cloud + nuc * nb * 0.4f; eb = bb * cloud + nuc * nb * 0.4f
                    orbits(true)
                    er += or; eg += og; eb += ob
                }
                23 -> { // fractal (caleidoscopio)
                    val a = ang + t * 0.25f
                    val seg = 0.7853982f
                    var a2 = a - 2f * seg * ff(a / (2f * seg))
                    a2 = abs(a2 - seg)
                    var px = c(a2) * d; var py = s(a2) * d
                    for (it in 0 until 3) { px = abs(px * 1.6f - 0.5f); py = abs(py * 1.6f - 0.3f) }
                    val v = s(px * 6f + t) * s(py * 6f - t * 0.7f)
                    rainbow(v * 0.5f + d * 0.6f + t * 0.1f)
                    val f = 0.3f + 0.9f * sat(v + 0.2f)
                    ar = 0.05f; ag = 0.05f; ab = 0.06f
                    er = (hr * 0.5f + br * 0.5f) * f; eg = (hg * 0.5f + bg * 0.5f) * f; eb = (hb * 0.5f + bb * 0.5f) * f
                }
                24 -> { // sakura
                    ar = br * 0.55f + 0.42f; ag = bg * 0.55f + 0.3f; ab = bb * 0.55f + 0.36f
                    val bx = x * 4f + t * 0.15f; val by = y * 4f - t * 0.25f
                    val ix = ff(bx); val iy = ff(by)
                    val h = hash(ix, iy)
                    val cx = ix + 0.5f + (hash(ix + 3, iy) - 0.5f) * 0.4f; val cy = iy + 0.5f + (hash(ix, iy + 3) - 0.5f) * 0.4f
                    val dd = sqrt((bx - cx) * (bx - cx) + (by - cy) * (by - cy))
                    val bok = if (h > 0.62f) sat((0.36f - dd) * 6f) * (0.35f + 0.35f * s(t * 1.5f + h * 20f)) else 0f
                    val pet = petals(true)
                    er = bok * 0.4f + pet * (0.75f + br * 0.3f); eg = bok * 0.32f + pet * (0.45f + bg * 0.2f); eb = bok * 0.38f + pet * (0.55f + bb * 0.3f)
                }
                25 -> { // fuegos artificiales
                    ar = 0.02f + br * 0.03f; ag = 0.02f + bg * 0.03f; ab = 0.05f + bb * 0.03f
                    var accR = stars(x, y, 12f, 0.93f) * 0.5f; var accG = accR; var accB = accR
                    for (j in 0 until 4) {
                        val pt = t / 2.2f + j / 4f
                        val ph = fr(pt)
                        val cyc = ff(pt)
                        val cx = (hash(cyc, j) - 0.5f) * 0.9f
                        val cy = (hash(j + 9, cyc) - 0.5f) * 0.8f
                        val u = 1f - ph
                        val rr = 0.75f * (1f - u * u * u)
                        val dx = x - cx; val dy = y - cy
                        val dist = sqrt(dx * dx + dy * dy)
                        if (dist > rr + 0.1f) continue
                        val a = at2(dy, dx)
                        val rf = fr(a * 3.82f + hash(j, cyc) * 7f)
                        val ray = sat(1f - abs(rf - 0.5f) * 3.2f)
                        val ringv = sat(1f - abs(dist - rr) * 11f) * ray
                        val tr = if (dist < rr) ray * (dist / max(rr, 0.01f)).let { it * it * it } * 0.6f else 0f
                        val fade = u * sqrt(u)
                        val v = (ringv * 1.9f + tr * 1.3f) * fade + (if (ph < 0.08f) sat(1f - dist * 6f) * (1f - ph / 0.08f) else 0f)
                        val kind = ff(hash(cyc + 5, j) * 3f)
                        val cr = if (kind == 0) br else if (kind == 1) 1f else bg * 0.3f + 0.7f
                        val cg = if (kind == 0) bg else if (kind == 1) 0.8f else bb * 0.3f + 0.3f
                        val cb = if (kind == 0) bb else if (kind == 1) 0.3f else br * 0.3f + 0.8f
                        accR += v * (cr + 0.3f); accG += v * (cg + 0.3f); accB += v * (cb + 0.3f)
                    }
                    er = accR; eg = accG; eb = accB
                }
                26 -> { // prisma de luz
                    ar = br * 0.35f + 0.05f; ag = bg * 0.35f + 0.05f; ab = bb * 0.35f + 0.07f
                    prism()
                    er += or; eg += og; eb += ob
                }
                27 -> { // hipernova
                    val ph = fr(t / 3.2f)
                    val rr = ph * 1.6f
                    val ringv = sat(1f - abs(d - rr - (fbm(ang * 3f, t, 2) - 0.5f) * 0.2f) * 8f)
                    val rv2 = ringv * ringv * (1f - ph)
                    val core = (1f - ph * 0.8f) / (1f + d * d * (40f - ph * 30f))
                    val flash = sat(1f - ph * 12f)
                    val rem = if (d < rr) fbm(x * 3f, y * 3f + ph, 3) * 0.45f * (1f - ph * 0.5f) else 0f
                    ar = 0.03f; ag = 0.02f; ab = 0.05f
                    er = rv2 * (0.6f + br * 1.2f) + core * (1f + br) + rem * (br * 0.7f + 0.5f) + flash * 0.6f
                    eg = rv2 * (0.6f + bg * 1.2f) + core * (1f + bg) + rem * (bg * 0.7f + 0.1f) + flash * 0.6f
                    eb = rv2 * (0.6f + bb * 1.2f) + core * (1f + bb) + rem * (bb * 0.7f + 0.6f) + flash * 0.6f
                }
                28 -> { // singularidad
                    disk(true)
                }
                29 -> { // fénix (fuego en remolino)
                    val v = sat(flame(x, y, 0.05f) + (1f - d) * 0.12f) * (0.8f + 0.2f * z)
                    fireRamp(v, 1f, 0.55f, 0.12f)
                    ar = 0.05f; ag = 0.02f; ab = 0.01f
                }
                30 -> { // mariposa de luz (interior)
                    val gw = 1f - d * 0.7f
                    val st = stars(x, y + t * 0.2f, 9f, 0.8f) * 1.2f
                    ar = br * 0.35f; ag = bg * 0.35f; ab = bb * 0.35f
                    er = br * gw * 0.6f + 0.25f * gw * gw + st; eg = bg * gw * 0.6f + 0.25f * gw * gw + st; eb = bb * gw * 0.6f + 0.25f * gw * gw + st
                }
                31 -> { // púlsar
                    val core = 1f / (1f + d * d * 120f) * 3f
                    val st = abs(x) / max(d, 0.01f)
                    val lv = d / (st * st + 0.02f)
                    val f = fr(lv * 2.2f)
                    val field = sat(1f - abs(f - 0.5f) * 10f) * 0.35f * sat(1f - lv * 0.25f)
                    val flow = sat(1f - abs(fr(lv * 2.2f - t * 0.8f) - 0.5f) * 3f) * field
                    val beam = beam()
                    ar = 0.02f; ag = 0.02f; ab = 0.04f
                    er = core * 0.9f + (field + flow) * br + beam * (0.6f + br * 0.6f)
                    eg = core * 0.95f + (field + flow) * bg + beam * (0.6f + bg * 0.6f)
                    eb = core * 1.1f + (field + flow) * bb + beam * (0.7f + bb * 0.6f)
                }
                32 -> { // eclipse: la luna tapa el sol
                    val sh = fbm(x * 3f, y * 3f, 2) * 0.06f
                    ar = br * sh; ag = bg * sh; ab = bb * sh
                    val rimIn = sat((d - 0.94f) * 16f)
                    val dx = x - q[0]; val dy = y - q[1]
                    val fl = 1f / (1f + (dx * dx + dy * dy) * 900f) * q[2]
                    er = rimIn * (0.6f + br * 0.5f) + fl * 2f; eg = rimIn * (0.6f + bg * 0.5f) + fl * 2f; eb = rimIn * (0.65f + bb * 0.5f) + fl * 2f
                }
                33 -> { // génesis
                    val pt = t / 4f
                    val ph = fr(pt)
                    val cyc = ff(pt)
                    val e = ph * 1.5f
                    val sec = ff((ang + 3.1416f) / TAU * 48f)
                    val rp = fr(d * 3f - ph * 4f + hash(sec, cyc))
                    val sp = if (d < e) sat(1f - rp * 7f) * (1f - ph) else 0f
                    val core = 1f / (1f + d * d * (60f * (1f - ph) + 4f)) * (1.4f - ph)
                    val web = sat(1f - abs(fbm(x * 4f, y * 4f, 3) - 0.5f) * 18f) * 0.25f
                    val hot = sat(1f - ph * 2.5f)
                    ar = 0.03f; ag = 0.02f; ab = 0.05f
                    er = sp * (br + hot) * 1.4f + core * (1f + br) + web * br
                    eg = sp * (bg + hot) * 1.4f + core * (0.9f + bg) + web * bg
                    eb = sp * (bb + hot) * 1.4f + core * (0.9f + bb) + web * (bb + 0.3f)
                }
                34 -> { // infinito: película iridiscente + lemniscata de luz
                    rainbow(z * 1.3f + fbm(x * 2f + t * 0.2f, y * 2f, 2) * 0.8f + t * 0.08f)
                    ar = hr * 0.35f + br * 0.35f; ag = hg * 0.35f + bg * 0.35f; ab = hb * 0.35f + bb * 0.35f
                    val a2 = 0.72f * 0.72f
                    val r2 = x * x + y * y
                    val f = r2 * r2 - a2 * (x * x - y * y)
                    val gx = 4f * x * r2 - 2f * a2 * x
                    val gy = 4f * y * r2 + 2f * a2 * y
                    val dist = abs(f) / (sqrt(gx * gx + gy * gy) + 0.05f)
                    var line = sat(1f - dist * 22f); line *= line
                    var glow = sat(1f - dist * 6f); glow *= glow * 0.4f
                    val pulse = 0.6f + 0.4f * s(ang * 2f + (if (x > 0f) 0f else 3.14f) - t * 4f)
                    val dx = x - q[0]; val dy = y - q[1]
                    val run = 1f / (1f + (dx * dx + dy * dy) * 160f) * 1.6f
                    rainbow(ang / TAU + t * 0.3f)
                    er = (line * pulse + glow) * (hr * 0.6f + 0.5f) + run
                    eg = (line * pulse + glow) * (hg * 0.6f + 0.5f) + run
                    eb = (line * pulse + glow) * (hb * 0.6f + 0.5f) + run
                }
            }
        }

        // ================================================================== fuera de la bola
        private fun outside(k: Int) {
            when (k) {
                0 -> { // chispazos de plasma que se escapan
                    if (d > 1.35f) return
                    val a = abs(s(fbm(ang * 3f + t, d * 4f, 2) * 10f + t * 3f))
                    var v = sat(1f - a * 6f); v *= v
                    v *= sat(1f - (d - 1f) * 3.2f) * (0.5f + 0.5f * s(t * 7f + ang * 2f))
                    or = v * lr * 1.3f; og = v * lg * 1.3f; ob = v * lb * 1.3f
                }
                2 -> { // burbujas que suben
                    val v = bubbles(x, y, t, 4f) * sat(1f - (d - 1f) * 1.4f)
                    or = v * lr; og = v * lg; ob = v * lb
                }
                4 -> { // aberración cromática del borde
                    val r1 = sat(1f - abs(d - 1.03f) * 50f) * 0.5f
                    val b1 = sat(1f - abs(d - 1.07f) * 50f) * 0.5f
                    or = r1 * 0.9f; og = b1 * 0.5f; ob = b1
                }
                6 -> { // ondas del latido
                    val ph = fr(t * 0.75f)
                    val rr = 1f + ph * 0.7f
                    val v = sat(1f - abs(d - rr) * 18f) * (1f - ph) * 0.8f
                    or = v * lr; og = v * lg; ob = v * lb
                }
                8 -> { // brasas que suben
                    val em = embers(x, y, t, 7f, 0.55f) * sat(1f - (d - 1f) * 1.6f)
                    or = em * (0.6f + br * 0.4f) * 1.4f; og = em * (0.3f + bg * 0.3f) * 1.4f; ob = em * 0.1f
                }
                9 -> { // relámpagos que saltan fuera
                    val fl = q[1]
                    if (fl <= 0f || d > 1.45f) return
                    val a = abs(s(fbm(ang * 4f + q[0], d * 5f, 2) * 12f))
                    var v = sat(1f - a * 8f); v *= v * fl * sat(1f - (d - 1f) * 2.4f)
                    or = v * (0.7f + br * 0.5f); og = v * (0.7f + bg * 0.5f); ob = v * (0.8f + bb * 0.5f)
                }
                10 -> { // lenguas de fuego encima
                    if (y > 0.2f) return
                    val fo = flame(x, y, 0f)
                    val v = sat(fo * 1.5f - (d - 1f) * 2.2f) * sat(0.3f - y * 1.5f)
                    fireRamp(v, 0.35f, 0.9f, 1f)
                    or = er * v; og = eg * v; ob = eb * v
                }
                12 -> { // anillo exterior con marcas
                    val band = sat(1f - abs(d - 1.16f) * 40f)
                    val seg = fr((ang + t * 0.8f) / TAU * 24f)
                    val v = band * (if (seg < 0.6f) 1f else 0.15f) * 0.9f
                    or = v * lr; og = v * lg; ob = v * lb
                }
                13 -> { // destellos de gema alrededor
                    val st = stars(x + 3f, y, 7f, 0.86f) * sat(1f - (d - 1f) * 2f) * 1.2f
                    or = st * lr; og = st * lg; ob = st * lb
                }
                14 -> { // resplandor del horizonte
                    val v = sat(1f - abs(y - 0.06f) * 9f) * sat(1f - (d - 1f) * 2.5f) * 0.9f
                    or = v * (0.5f + br * 0.5f); og = v * 0.2f; ob = v * (0.4f + bb * 0.5f)
                }
                15 -> { // remolino que sale
                    val sw = ang * 2f - d * 5f + t * 3f
                    var b = 0.5f + 0.5f * s(sw); b *= b * b
                    val v = b * sat(1f - (d - 1f) * 2.2f) * 0.7f
                    or = v * (br * 0.6f + 0.4f); og = v * bg * 0.5f; ob = v * (bb * 0.6f + 0.1f)
                }
                16 -> { // polvo estelar
                    val st = stars(x, y, 10f, 0.9f) * sat(1f - (d - 1f) * 1.6f)
                    val neb = (fbm(x * 1.6f + t * 0.08f, y * 1.6f, 2) - 0.45f).coerceAtLeast(0f) * sat(1f - (d - 1f) * 2.5f) * 0.8f
                    or = st + neb * br; og = st + neb * bg * 0.5f; ob = st + neb * bb
                }
                17 -> { // cola de cometa y nieve
                    val dx = -0.6f; val dy = 0.8f
                    val along = x * dx + y * dy
                    val perp = x * dy - y * dx
                    var v = 0f
                    if (along > 0.4f) {
                        val wdt = 0.35f + (along - 0.4f) * 0.4f
                        v = sat(1f - abs(perp) / wdt) * sat(1f - (along - 1f) * 1.3f)
                        v *= 0.4f + 0.6f * fbm(along * 4f - t * 3f, perp * 6f, 2)
                    }
                    val sn = stars(x - t * 0.3f, y + t * 0.4f, 8f, 0.9f) * sat(1f - (d - 1f) * 1.5f)
                    or = v * (0.6f + br * 0.4f) + sn; og = v * (0.75f + bg * 0.3f) + sn; ob = v * (0.95f + bb * 0.2f) + sn
                }
                19 -> { // anillo de runas
                    val band = sat(1f - abs(d - 1.22f) * 14f)
                    val cell = (ang + t * 0.4f + 3.1416f) / TAU * 18f
                    val gl = fr(cell * 2f)
                    val mark = if (hash(ff(cell), 4) > 0.3f) sat(1f - abs(gl - 0.5f) * 5f) else 0f
                    val bord = sat(1f - abs(d - 1.12f) * 60f) + sat(1f - abs(d - 1.32f) * 60f)
                    val v = (band * mark + bord * 0.6f) * 0.9f
                    or = v * lr; og = v * lg; ob = v * lb
                }
                20 -> { // rayos de la corona y protuberancias
                    val cr = fbm(ang * 5f + 3f, t * 0.3f, 3)
                    val rays = sat(cr * 1.7f - 0.45f) * sat(1f - (d - 1f) * 1.5f)
                    val lp = sat(1f - abs(d - 1.1f - 0.07f * s(ang * 7f + t)) * 22f) * sat(s(ang * 3f + t * 0.3f))
                    val v = rays * 0.9f + lp * 0.9f
                    or = v * (0.7f + br * 0.4f); og = v * (0.4f + bg * 0.3f); ob = v * (0.1f + bb * 0.3f)
                }
                21 -> { // chispas donde los rayos tocan el vidrio
                    var best = 0f
                    for (j in 0 until 6) {
                        var df = ang - q[j]
                        df -= TAU * ff(df / TAU + 0.5f)
                        val v = sat(1f - abs(df) * 9f)
                        if (v > best) best = v
                    }
                    val v = best * best * sat(1f - (d - 1f) * 6f) * 0.8f
                    or = v * (br * 0.6f + 0.4f); og = v * (bg * 0.6f + 0.2f); ob = v * (bb * 0.6f + 0.5f)
                }
                22 -> orbits(false)
                24 -> { // pétalos girando alrededor
                    val v = petals(false)
                    or = v * (0.75f + br * 0.3f); og = v * (0.45f + bg * 0.2f); ob = v * (0.55f + bb * 0.3f)
                }
                26 -> prism()
                27 -> { // la onda sigue fuera
                    val ph = fr(t / 3.2f)
                    val rr = ph * 1.6f
                    val ringv = sat(1f - abs(d - rr - (fbm(ang * 3f, t, 2) - 0.5f) * 0.2f) * 8f)
                    val v = ringv * ringv * (1f - ph)
                    or = v * (0.6f + br); og = v * (0.6f + bg); ob = v * (0.6f + bb)
                }
                28 -> disk(false)
                29 -> wings(true)
                30 -> wings(false)
                31 -> {
                    val b = beam() * sat(1f - (d - 1f) * 1.4f)
                    or = b * (0.6f + br * 0.6f); og = b * (0.6f + bg * 0.6f); ob = b * (0.7f + bb * 0.6f)
                }
                32 -> { // corona del eclipse y el diamante
                    val cr = fbm(ang * 4f, d * 1.5f - t * 0.2f, 3)
                    val fall = sat(1f - (d - 1f) * 1.25f)
                    val inner = sat(1f - (d - 1f) * 9f)
                    val v = (sat(cr * 1.8f - 0.55f) * fall * 0.9f + inner * 0.9f)
                    val dx = x - q[0]; val dy = y - q[1]
                    val fl = (1f / (1f + (dx * dx + dy * dy) * 300f) * 2f + sat(1f - abs(dy) * 70f) * sat(1f - abs(dx) * 2f) + sat(1f - abs(dx) * 70f) * sat(1f - abs(dy) * 2f)) * q[2]
                    or = v * (0.75f + br * 0.4f) + fl; og = v * (0.75f + bg * 0.4f) + fl; ob = v * (0.8f + bb * 0.4f) + fl
                }
                33 -> { // chispas que salen disparadas
                    val pt = t / 4f
                    val ph = fr(pt)
                    val cyc = ff(pt)
                    val e = ph * 1.5f
                    if (d > e) return
                    val sec = ff((ang + 3.1416f) / TAU * 48f)
                    val rp = fr(d * 3f - ph * 4f + hash(sec, cyc))
                    val v = sat(1f - rp * 7f) * (1f - ph) * sat(1f - (d - 1f) * 1.5f)
                    or = v * (br + 0.5f); og = v * (bg + 0.5f); ob = v * (bb + 0.5f)
                }
                34 -> { // halo arcoíris que gira
                    rainbow(ang / TAU + t * 0.4f)
                    val v = sat(1f - abs(d - 1.08f) * 14f) * 0.9f + stars(x, y, 9f, 0.88f) * sat(1f - (d - 1f) * 1.5f)
                    or = v * hr; og = v * hg; ob = v * hb
                }
            }
        }

        // ------------------------------------------------------------------ piezas compartidas
        private fun ecg(p: Float): Float {
            fun bump(c0: Float, w: Float, h: Float): Float { val u = (p - c0) / w; return if (abs(u) < 1f) h * (1f - u * u) * (1f - u * u) else 0f }
            return bump(0.14f, 0.05f, 0.12f) - bump(0.285f, 0.015f, 0.18f) + bump(0.31f, 0.02f, 0.85f) - bump(0.34f, 0.02f, 0.32f) + bump(0.56f, 0.07f, 0.22f)
        }

        /** Rampa de fuego: oscuro → color de juego → (acento) → blanco. */
        private fun fireRamp(v: Float, kr: Float, kg: Float, kb: Float) {
            if (v < 0.35f) {
                val u = v / 0.35f
                er = br * u * 0.7f; eg = bg * u * 0.7f; eb = bb * u * 0.7f
            } else if (v < 0.75f) {
                val u = (v - 0.35f) / 0.4f
                er = br * 0.7f + (kr - br * 0.7f) * u; eg = bg * 0.7f + (kg - bg * 0.7f) * u; eb = bb * 0.7f + (kb - bb * 0.7f) * u
                er *= 1.2f; eg *= 1.2f; eb *= 1.2f
            } else {
                val u = (v - 0.75f) / 0.25f
                val u2 = u * u
                er = kr * 1.2f + (1.45f - kr * 1.2f) * u2; eg = kg * 1.2f + (1.35f - kg * 1.2f) * u2; eb = kb * 1.2f + (1.25f - kb * 1.2f) * u2
            }
        }

        /** Llama que sube: 0..1 (más fuerte abajo, con lenguas que se cortan arriba). */
        private fun flame(px: Float, py: Float, bias: Float): Float {
            val w = noise(px * 3f, py * 2f + t * 2.6f)
            val n = fbm(px * 4.2f + (w - 0.5f) * 1.4f, py * 2f + t * 2.9f, 3)
            val v = (n - 0.5f) * 2.1f + py * 0.8f + 0.2f + bias
            return sat(v)
        }

        /** Burbujas (aros con brillo) que suben. */
        private fun bubbles(px: Float, py: Float, tt: Float, scale: Float): Float {
            val gx = px * scale + s(py * 3f + tt) * 0.15f
            val gy = py * scale + tt * 1.6f
            val ix = ff(gx); val iy = ff(gy)
            val h = hash(ix, iy)
            if (h < 0.72f) return 0f
            val rr = 0.12f + 0.16f * hash(ix + 7, iy)
            val cx = ix + 0.5f; val cy = iy + 0.5f
            val dx = gx - cx; val dy = gy - cy
            val dd = sqrt(dx * dx + dy * dy)
            val ring = sat(1f - abs(dd - rr) * 22f) * 0.7f
            val hi = sat(1f - sqrt((dx + rr * 0.4f) * (dx + rr * 0.4f) + (dy + rr * 0.4f) * (dy + rr * 0.4f)) * 30f)
            return ring + hi
        }

        /** Pétalos de cerezo girando: dentro de la bola van por delante; fuera, alrededor. */
        private fun petals(front: Boolean): Float {
            var v = 0f
            for (j in 0 until 6) {
                val a = t * 0.6f + j * 1.0472f
                val rr = (if (front) 0.55f else 1.32f) + 0.08f * s(t * 1.3f + j)
                val px = c(a) * rr; val py = s(a) * rr * 0.8f + (if (front) 0.15f * s(t + j) else 0f)
                if (front && j % 2 == 1) continue
                val rot = a * 2f + t + j
                val cr = c(rot); val sr = s(rot)
                val lx = (x - px) * cr + (y - py) * sr
                val ly = -(x - px) * sr + (y - py) * cr
                val sz = if (front) 0.8f else 1f
                val ex = lx / (0.2f * sz); val ey = ly / (0.12f * sz)
                val e = ex * ex + ey * ey
                if (e < 1f) {
                    val notch = if (ex > 0.55f && abs(ey) < 0.22f) 0.15f else 1f
                    val vv = sat((1f - e) * 3f) * notch * (0.75f + 0.25f * (1f - ex))
                    if (vv > v) v = vv
                }
            }
            return v
        }

        private fun embers(px: Float, py: Float, tt: Float, scale: Float, th: Float): Float {
            val gx = px * scale + s(py * 2f + tt) * 0.3f
            val gy = py * scale + tt * 4f
            val ix = ff(gx); val iy = ff(gy)
            val h = hash(ix, iy)
            if (h < th) return 0f
            val cx = ix + 0.5f; val cy = iy + 0.5f
            val dx = gx - cx; val dy = gy - cy
            val v = 1f - (dx * dx + dy * dy) * 14f
            return if (v > 0f) v * v * (0.5f + 0.5f * s(tt * 9f + h * 30f)) else 0f
        }

        /** Disco de acreción del agujero negro (dentro: sombra, anillo de fotones y disco por delante). */
        private fun disk(inner: Boolean) {
            or = 0f; og = 0f; ob = 0f
            val dy = y * 3.4f
            val de = sqrt(x * x + dy * dy)
            var v = 0f
            if (de > 0.48f && de < 1.75f) {
                val band = 0.55f + 0.45f * s(de * 16f - t * 5f + (fbm(ang * 2f, de * 3f, 2) - 0.5f) * 6f)
                val fall = sat((de - 0.48f) * 5f) * sat((1.75f - de) * 1.4f)
                val dop = 1f + 0.7f * (-x / max(de, 0.05f))
                v = band * fall * dop
            }
            val front = if (inner) (if (d < 0.38f) (if (y > 0f) 1f else 0f) else 1f) else 1f
            var vv = v * front
            // arco del disco de atrás, curvado por la gravedad sobre la sombra
            if (y < 0f) {
                val ld = sqrt(x * x + ((y + 0.05f) * 1.25f) * ((y + 0.05f) * 1.25f))
                vv += sat(1f - abs(ld - 0.52f) * 9f) * sat(-y * 6f) * 0.9f
            }
            val pr = sat(1f - abs(d - 0.40f) * 32f)
            val hot = sat(1.3f - de * 0.7f)
            val cr = vv * (br * 0.8f + 0.5f * hot + 0.3f) + pr * 1.4f
            val cg = vv * (bg * 0.8f + 0.35f * hot + 0.1f) + pr * 1.3f
            val cb = vv * (bb * 0.8f + 0.2f * hot) + pr * 1.2f
            if (inner) {
                val shadow = if (d < 0.38f) 0f else 1f
                ar = 0.02f * shadow; ag = 0.02f * shadow; ab = 0.03f * shadow
                er = cr; eg = cg; eb = cb
            } else {
                or = cr; og = cg; ob = cb
            }
        }

        /** Tres órbitas inclinadas con electrones; la mitad de atrás queda tapada por la bola. */
        private fun orbits(front: Boolean) {
            or = 0f; og = 0f; ob = 0f
            var acc = 0f; var hot = 0f
            for (j in 0 until 3) {
                val cp = q[j * 4]; val sp = q[j * 4 + 1]
                val u = x * cp + y * sp
                val v = -x * sp + y * cp
                if (front && v < 0f) continue
                if (!front && v >= 0f && d < 1f) continue
                val vs = v * 3.4f
                val e = sqrt(u * u + vs * vs)
                val ring = sat(1f - abs(e - 1.28f) * 36f)
                val th = q[j * 4 + 2]
                val ex = 1.28f * c(th); val ey = 1.28f / 3.4f * s(th)
                val dx = u - ex; val dy = v - ey
                val el = 1f / (1f + (dx * dx + dy * dy) * 260f) * 1.6f
                val pa = at2(vs, u)
                var df = th - pa
                df -= TAU * ff(df / TAU)
                val trail = if (df < 2.2f) (1f - df / 2.2f) else 0f
                acc += ring * (0.25f + 0.85f * trail * trail)
                hot += el
            }
            or = acc * lr + hot * (0.6f + br); og = acc * lg + hot * (0.6f + bg); ob = acc * lb + hot * (0.6f + bb)
        }

        /** Haz blanco que entra por la izquierda y sale abierto en arcoíris por la derecha. */
        private fun prism() {
            or = 0f; og = 0f; ob = 0f
            val tilt = 0.12f * s(t * 0.5f)
            if (x < 0f) {
                val yl = x * (0.28f + tilt)
                val v = sat(1f - abs(y - yl) * 22f) * (if (d < 1f) 0.5f else 1f)
                val sp = stars(x - t * 0.6f, y, 18f, 0.7f) * 0.6f * sat(1f - abs(y - yl) * 12f)
                or = v + sp; og = v + sp; ob = v + sp
            } else {
                val a = y / max(x, 0.02f)
                val lo = -0.05f + tilt; val hi = 0.5f + tilt
                if (a > lo && a < hi) {
                    val u = (a - lo) / (hi - lo)
                    rainbow(u * 0.8f)
                    val edge = sat(min(u, 1f - u) * 8f)
                    val v = edge * sat(x * 3f) * (if (d < 1f) 0.8f else sat(1f - (d - 1f) * 1.1f))
                    or = hr * v * 1.3f; og = hg * v * 1.3f; ob = hb * v * 1.3f
                }
            }
        }

        /** Haz del púlsar: gira muy rápido y atraviesa la bola. */
        private fun beam(): Float {
            val dist = abs(x * q[1] - y * q[0])
            var b = sat(1f - dist * (4f + d * 2f)); b *= b
            return b * sat(d * 4f - 0.6f)
        }

        /** Alas: de fuego (fénix) o de luz (mariposa). */
        private fun wings(fire: Boolean) {
            or = 0f; og = 0f; ob = 0f
            val ax = abs(x)
            if (ax < 0.55f) return
            val flap = if (fire) 0.85f + 0.15f * s(t * 3.5f) else 0.7f + 0.3f * s(t * 5f)
            val wx = (ax - 0.55f) / flap
            if (wx > 1.2f) return
            var mask: Float
            if (fire) {
                val lift = 0.3f * s(t * 3.5f)
                val top = 0.2f - wx * (1.25f + lift) * (1f - 0.25f * wx)
                val fing = 0.1f * abs(s(wx * 13f - t * 3f))
                val bot = 0.32f - wx * (0.35f + lift * 0.6f) + fing
                if (y < top || y > bot) return
                val hgt = bot - top
                val u = (y - top) / max(hgt, 0.01f)
                mask = sat(u * 6f) * sat((1f - u) * 5f) * sat((1.15f - wx) * 3.5f)
                val feath = 0.6f + 0.4f * s(u * 9f + wx * 4f)
                val fl = fbm(x * 3f, y * 3f + t * 2.5f, 2)
                val v = sat(mask * feath * (0.55f + 0.7f * fl) * (1.25f - wx * 0.6f))
                fireRamp(v, 1f, 0.55f, 0.12f)
                or = er * v; og = eg * v; ob = eb * v
            } else {
                val wyU = (y + 0.3f + wx * 0.25f) / 0.62f; val wxU = (wx - 0.62f) / 0.6f
                val up = wxU * wxU + wyU * wyU
                val wyL = (y - 0.42f) / 0.42f; val wxL = (wx - 0.42f) / 0.45f
                val lo = wxL * wxL + wyL * wyL
                val m0 = max(sat((1f - up) * 2.5f), sat((1f - lo) * 2.5f))
                if (m0 <= 0f) return
                val rimU = sat(1f - abs(1f - up) * 5f); val rimL = sat(1f - abs(1f - lo) * 5f)
                val va = y / (wx + 0.1f)
                val vein = sat(1f - abs(fr(va * 2.2f) - 0.5f) * 7f) * 0.5f
                val ed = sqrt((wx - 0.75f) * (wx - 0.75f) + (y + 0.42f) * (y + 0.42f))
                val eye = sat(1f - ed * 7f)
                rainbow(wx * 0.5f + y * 0.3f + t * 0.15f)
                val m = m0 * 0.75f
                val rim = max(rimU, rimL) * 0.8f
                or = m * (br * 0.6f + hr * 0.4f) + (vein * m + rim) * lr + eye * 1.1f
                og = m * (bg * 0.6f + hg * 0.4f) + (vein * m + rim) * lg + eye * 1.1f
                ob = m * (bb * 0.6f + hb * 0.4f) + (vein * m + rim) * lb + eye * 1.1f
            }
        }
    }
}
