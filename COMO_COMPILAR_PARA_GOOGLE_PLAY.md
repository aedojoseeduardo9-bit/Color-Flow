# Cómo sacar una actualización para Google Play sin GitHub

Necesitas: un computador con **Android Studio** (gratis: developer.android.com/studio), esta carpeta del juego,
tu llave de subida **`upload.jks`** y su contraseña (archivo `LLAVE_PLAY_PRIVADA.txt`).

## La primera vez
1. Instala Android Studio (versión 2025 o más nueva) y ábrelo.
2. **File → Open** → elige la carpeta `ColorFlow` (la de este zip, descomprimida).
3. Espera a que termine la sincronización ("Gradle sync", abajo). La primera vez descarga cosas y tarda varios minutos.
   Si te pregunta por un SDK que falta (Android 16 / API 36), acepta instalarlo.

## Cada actualización
1. Menú **Build → Generate Signed App Bundle or APK…** → elige **Android App Bundle** → Next.
2. **Key store path**: elige tu `upload.jks`. **Key store password**: la de `LLAVE_PLAY_PRIVADA.txt`.
   **Key alias**: `upload`. **Key password**: la misma contraseña. Next.
3. Elige la variante **`playRelease`** (¡no `normal` ni `dev`!) → **Create**.
4. Al terminar, Android Studio muestra "locate": el archivo está en `app/play/release/app-play-release.aab`.
5. En Play Console: **Prueba cerrada → Crear versión nueva → Subir** ese `.aab` → notas de la versión → Guardar → Enviar a revisión.

El número de versión sube solo (se calcula con la fecha y hora), así que Play siempre lo acepta como más nuevo.
El nombre de paquete de esta variante ya es `nioro.color.flow`, el que Play exige.
