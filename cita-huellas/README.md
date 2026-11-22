# Cita Huellas

App Android personal para pedir cita de **toma de huellas (expedición de tarjeta) en Madrid** en la web oficial de cita previa, para familiares y amigos.

La app rellena y pulsa los pasos repetitivos, reintenta cuando no hay citas y te avisa cuando aparece una. **Tú eliges oficina y hueco, pones el código SMS y confirmas.** No resuelve captchas ni intenta ocultar que hay automatización.

## Instalar

1. Descarga `CitaHuellas-1.0.apk` en el móvil.
2. Ábrelo. Android pedirá permitir "instalar apps de origen desconocido" para el navegador o la app de archivos: acéptalo solo para esta instalación.
3. Al abrir la app, permite las notificaciones (sirven para avisarte cuando haya cita).

## Usar

1. **Personas** → *Añadir persona*: nombre y apellidos, NIE, país (como aparece en la web, p. ej. `PERU`), y si quieres su teléfono y correo.
2. **Ajustes**: tu teléfono y correo; intervalo (por defecto 60 s) e intentos (por defecto 10).
3. **Buscar**: elige persona y a qué teléfono llega el SMS, y pulsa *Buscar cita*. Deja la app abierta y el móvil cargando: la pantalla se queda encendida mientras busca.
4. Cuando haya cita suena una alarma. Elige oficina, revisa teléfono y correo, elige hueco, pon el código SMS y **pulsa Confirmar tú**.

La búsqueda se para sola y te avisa si sale un captcha, un error, una página que no reconoce o un aviso de demasiadas peticiones. En ese caso sigue a mano o espera un rato.

**Hacer este paso** rellena la pantalla en la que estés (útil si te has quedado a medias).

## Privacidad

Los datos se guardan cifrados (AES-GCM con clave del Android Keystore) solo en el móvil. No hay servidor. La app solo navega por `administracionelectronica.gob.es`. No pide permiso para leer tus SMS: Android te pregunta por cada mensaje.

## Desarrollo

```bash
scripts/setup-sdk.sh                       # Android SDK en ~/android-sdk
gradle :app:testDebugUnitTest              # tests (lógica, cifrado, puente JS)
node jstest/bridge.test.mjs                # bridge.js en Chromium con pantallas de prueba
gradle :app:testDebugUnitTest -Proborazzi.test.record=true   # capturas en app/build/outputs/roborazzi
gradle :app:assembleRelease                # requiere ~/.citahuellas/keystore.properties
```

Si la web cambia y la app se para con "Pantalla no reconocida" o "No encuentro …", hay que ajustar `engine/PageClassifier.kt` y `web/JsBridge.kt`.
