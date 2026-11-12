package com.nioro.spectrumcolor

/**
 * Logros: metas con recompensa en monedas. Cada uno mira un contador (`stat`) y se gana al llegar a `goal`.
 * Sin Android: GameView calcula los contadores y entrega las monedas.
 */
object Logros {
    class Def(val name: String, val desc: String, val stat: Int, val goal: Int, val reward: Int)

    // contadores
    const val S_LEVELS = 0      // niveles superados
    const val S_STARS = 1       // estrellas totales
    const val S_PERFECT = 2     // niveles con 3 estrellas
    const val S_WORLDS = 3      // mundos completos del modo normal
    const val S_DEATHS = 4      // veces que se perdió
    const val S_ENDLESS = 5     // récord del modo infinito
    const val S_RUNS = 6        // partidas del modo infinito
    const val S_KILLS = 7       // enemigos derrotados en la Aventura
    const val S_BOSSES = 8      // jefes derrotados
    const val S_SAVED = 9       // mundos salvados en la Aventura
    const val S_COINS = 10      // monedas ganadas en total
    const val S_BUYS = 11       // compras en la Tienda
    const val S_SPHERES = 12    // esferas de la Tienda
    const val S_MYTHIC = 13     // esferas míticas
    const val S_TRAILS = 14     // estelas especiales
    const val S_STREAK = 15     // mejor racha del regalo diario
    const val S_ALL = 16        // los demás logros

    val list = arrayOf(
        Def("Primeros pasos", "Supera tu primer nivel", S_LEVELS, 1, 20),
        Def("Buen comienzo", "Supera 10 niveles", S_LEVELS, 10, 40),
        Def("Imparable", "Supera 50 niveles", S_LEVELS, 50, 150),
        Def("Maestro del color", "Supera los 120 niveles", S_LEVELS, 120, 600),
        Def("Brillante", "Consigue 30 estrellas", S_STARS, 30, 40),
        Def("Lluvia de estrellas", "Consigue 150 estrellas", S_STARS, 150, 150),
        Def("Constelación", "Consigue las 360 estrellas", S_STARS, 360, 800),
        Def("Sin un rasguño", "Supera un nivel sin perder", S_PERFECT, 1, 30),
        Def("Perfeccionista", "Consigue 3 estrellas en 25 niveles", S_PERFECT, 25, 200),
        Def("Mundo completo", "Completa un mundo", S_WORLDS, 1, 60),
        Def("Trotamundos", "Completa 6 mundos", S_WORLDS, 6, 300),
        Def("Nunca te rindas", "Pierde 100 veces y sigue jugando", S_DEATHS, 100, 60),
        Def("Infinito I", "Llega a 10 puntos en el modo infinito", S_ENDLESS, 10, 20),
        Def("Infinito II", "Llega a 25 puntos en el modo infinito", S_ENDLESS, 25, 50),
        Def("Infinito III", "Llega a 50 puntos en el modo infinito", S_ENDLESS, 50, 120),
        Def("Infinito IV", "Llega a 100 puntos en el modo infinito", S_ENDLESS, 100, 300),
        Def("Infinito V", "Llega a 200 puntos en el modo infinito", S_ENDLESS, 200, 800),
        Def("Maratonista", "Juega 50 partidas del modo infinito", S_RUNS, 50, 120),
        Def("Primer golpe", "Derrota a tu primer enemigo", S_KILLS, 1, 20),
        Def("Cazador", "Derrota a 100 enemigos", S_KILLS, 100, 120),
        Def("Exterminador", "Derrota a 1000 enemigos", S_KILLS, 1000, 500),
        Def("Caída del jefe", "Derrota a un jefe", S_BOSSES, 1, 80),
        Def("Matajefes", "Derrota a 6 jefes", S_BOSSES, 6, 300),
        Def("Salvador", "Salva un mundo de la Aventura", S_SAVED, 1, 100),
        Def("Héroe de los colores", "Salva los 12 mundos de la Aventura", S_SAVED, 12, 1000),
        Def("Alcancía", "Gana 500 monedas", S_COINS, 500, 50),
        Def("Tesoro", "Gana 5000 monedas", S_COINS, 5000, 400),
        Def("Primera compra", "Compra algo en la Tienda", S_BUYS, 1, 50),
        Def("Coleccionista", "Ten 10 esferas de la Tienda", S_SPHERES, 10, 400),
        Def("Mítico", "Consigue una esfera mítica", S_MYTHIC, 1, 600),
        Def("Con estilo", "Compra una estela especial", S_TRAILS, 1, 80),
        Def("Fiel", "Recoge el regalo diario 7 días seguidos", S_STREAK, 7, 250),
        Def("Leyenda de Color Flow", "Consigue todos los demás logros", S_ALL, 32, 2500)
    )
    val COUNT get() = list.size
}
