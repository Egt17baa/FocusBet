# Cita Huellas Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** APK Android personal que recorre la web de cita previa (toma de huellas, Madrid) con datos de un perfil, reintenta con ritmo humano y avisa cuando hay cita.

**Architecture:** App Kotlin de una sola Activity con Compose. La web se muestra en un `WebView`; un JS inyectado devuelve una "foto" de la página (ids presentes + texto) y ejecuta acciones (rellenar, elegir opción por texto, pulsar). La decisión de qué hacer en cada pantalla vive en Kotlin puro (`PageClassifier` + `SearchEngine`) con tests JVM. Perfiles cifrados con AES-GCM y clave de Android Keystore.

**Tech Stack:** Kotlin 2.0, AGP 8.7, Gradle 8.14.3 (`/opt/gradle`), Compose BOM 2024.10, Material 3, Haze 1.0 (`dev.chrisbanes.haze`), kotlinx-serialization-json, play-services-auth-api-phone 18.1.0 (SMS User Consent), JUnit 4. Pruebas JS con Playwright de Node ya instalado.

**Spec:** `docs/superpowers/specs/2026-11-22-cita-huellas-design.md`

## Global Constraints

- Paquete `com.citahuellas`, nombre visible "Cita Huellas", carpeta `cita-huellas/`.
- minSdk 26, targetSdk 35, compileSdk 35.
- URL base `https://icp.administracionelectronica.gob.es/icpplustiem/`; entrada `.../icpplustiem/index.html`.
- Intervalo por defecto 60 s (rango 30–300); intentos por defecto 10 (rango 1–20).
- Pausa entre pantallas aleatoria 2–4 s.
- Nunca: resolver captchas, falsificar huella, pulsar `btnConfirmar`, pulsar `btnSiguiente` del paso de contacto, reintentar ante pantalla desconocida/error.
- Sin permisos `READ_SMS`/`RECEIVE_SMS`; sin `INTERNET` a hosts distintos de la web oficial (no hay backend).
- Textos de la UI en español.
- Keystore de firma fuera del repo (`~/.citahuellas/release.jks`); `.gitignore` excluye `*.jks`, `local.properties`, `build/`.

## Review Focus

1. Web cambia un id: la app debe parar con "Pantalla no reconocida" y no repetir en bucle → test en Task 3 (`unknownPageStops`).
2. Página de captcha visible (`reCAPTCHA`, `img.img-thumbnail`, campo `captcha`) → parar y avisar, no reintentar → Task 3 (`captchaStops`).
3. Mensaje de exceso de peticiones / página en blanco / error HTTP → parar sin reintento → Task 3 (`blankOrThrottleStops`).
4. NIE o nombre con comillas, tildes o `</script>` → el JS no se rompe ni ejecuta código → Task 4 (`fillEscapesHostileText`).
5. Opción de país o trámite no encontrada en el desplegable → parar con mensaje claro → Task 4 (`selectByTextMissingReturnsFalse`) y Task 3 (engine para con `Stop(reason)`).

---

### Task 1: Toolchain y esqueleto que compila

**Files:**
- Create: `cita-huellas/settings.gradle.kts`, `cita-huellas/build.gradle.kts`, `cita-huellas/gradle.properties`, `cita-huellas/app/build.gradle.kts`, `cita-huellas/app/src/main/AndroidManifest.xml`, `cita-huellas/app/src/main/java/com/citahuellas/MainActivity.kt`, `cita-huellas/.gitignore`, `cita-huellas/scripts/setup-sdk.sh`

**Interfaces:**
- Produces: `./gradlew`-less build via `gradle -p cita-huellas :app:assembleDebug`; `ANDROID_HOME=$HOME/android-sdk`.

- [ ] **Step 1:** `scripts/setup-sdk.sh` descarga commandline-tools a `$HOME/android-sdk`, acepta licencias e instala `platforms;android-35`, `build-tools;35.0.0`, `platform-tools`.
- [ ] **Step 2:** Ejecutarlo. Expected: `$HOME/android-sdk/platforms/android-35` existe.
- [ ] **Step 3:** Gradle con Compose, Haze, serialization, play-services-auth-api-phone; Activity que muestra "Cita Huellas".
- [ ] **Step 4:** Run `gradle -p cita-huellas :app:assembleDebug`. Expected: `BUILD SUCCESSFUL`, APK en `app/build/outputs/apk/debug/`.
- [ ] **Step 5:** Commit `feat(cita-huellas): esqueleto Android`.

### Task 2: Modelos y almacenamiento cifrado

**Files:**
- Create: `app/src/main/java/com/citahuellas/data/Models.kt`, `data/Crypto.kt`, `data/ProfileStore.kt`, `data/SettingsStore.kt`
- Test: `app/src/test/java/com/citahuellas/data/CryptoTest.kt`, `ProfileStoreTest.kt`

**Interfaces:**
- Produces:
  - `@Serializable data class Profile(val id: String, val fullName: String, val nie: String, val country: String, val phone: String, val email: String, val note: String = "")`
  - `@Serializable data class AppSettings(val intervalSec: Int = 60, val maxAttempts: Int = 10, val myPhone: String = "", val myEmail: String = "")` con `fun AppSettings.clamped(): AppSettings` (30–300, 1–20).
  - `class Crypto(keyProvider: () -> SecretKey)` con `encrypt(plain: ByteArray): ByteArray` (IV 12 B + ciphertext) y `decrypt(blob: ByteArray): ByteArray`; `fun keystoreKey(alias: String = "citahuellas"): SecretKey`.
  - `class ProfileStore(file: File, crypto: Crypto)` con `load(): List<Profile>`, `save(list: List<Profile>)`, `upsert(p: Profile)`, `delete(id: String)`.
  - `class SettingsStore(context: Context)` (SharedPreferences, no sensible salvo myPhone/myEmail → también vía Crypto) con `load(): AppSettings`, `save(s: AppSettings)`.

- [ ] **Step 1:** Tests: `roundTrip` (decrypt(encrypt(x)) == x), `ivIsRandom` (dos cifrados distintos), `tamperedFails` (byte cambiado → `AEADBadTagException`), `storeRoundTrip` (upsert+delete con clave AES en memoria y `TemporaryFolder`), `fileIsNotPlaintext` (el NIE no aparece en bytes del fichero), `missingFileIsEmpty`, `clampedLimits` (`AppSettings(5, 99).clamped() == AppSettings(30, 20)`).
- [ ] **Step 2:** Run `gradle -p cita-huellas :app:testDebugUnitTest`. Expected: FAIL (clases no existen).
- [ ] **Step 3:** Implementar. AES/GCM/NoPadding, tag 128.
- [ ] **Step 4:** Run tests. Expected: PASS.
- [ ] **Step 5:** Commit `feat(cita-huellas): perfiles cifrados`.

### Task 3: Clasificador de pantallas y motor de búsqueda

**Files:**
- Create: `app/src/main/java/com/citahuellas/engine/PageClassifier.kt`, `engine/SearchEngine.kt`
- Test: `app/src/test/java/com/citahuellas/engine/PageClassifierTest.kt`, `SearchEngineTest.kt`

**Interfaces:**
- Produces:
  - `data class PageSnapshot(val url: String, val ids: Set<String>, val text: String)` (text en mayúsculas).
  - `enum class PageKind { PROVINCE, OFFICE_PROCEDURE, INSTRUCTIONS, PERSONAL_DATA, REQUEST, NO_APPOINTMENTS, OFFICE_AVAILABLE, CONTACT, SLOT_OR_SMS, CAPTCHA, THROTTLED, ERROR, UNKNOWN }`
  - `object PageClassifier { fun classify(s: PageSnapshot): PageKind }` — orden: CAPTCHA (`reCAPTCHA_site_key`, `captcha`, `img-thumbnail` marcado como id sintético `__captchaimg`) → THROTTLED (texto vacío/<20 chars, "TOO MANY", "DEMASIADAS", "REQUEST REJECTED") → ERROR ("ERROR", "SESIÓN HA CADUCADO") → NO_APPOINTMENTS ("NO HAY CITAS", "EN ESTE MOMENTO NO HAY") → SLOT_OR_SMS (`txtCodigoVerificacion`, `rdbCita`, `CitaMAP_HORAS`, `btnConfirmar`) → CONTACT (`txtTelefonoCitado`) → OFFICE_AVAILABLE (`idSede`) → PERSONAL_DATA (`txtIdCitado`) → REQUEST (`btnEnviar` + "SOLICITAR CITA") → INSTRUCTIONS (`btnEntrar`) → OFFICE_PROCEDURE (`tramiteGrupo[1]` o `sede`) → PROVINCE (`form` + `btnAceptar`) → UNKNOWN.
  - `sealed interface Action { SelectProvince; SelectProcedure; EnterWithoutClave; FillPersonal(p: Profile); Request; WaitAndRestart(delayMs: Long, attempt: Int); FillContact(phone: String, email: String); Found(kind: PageKind); Stop(reason: String) }`
  - `class SearchEngine(profile: Profile, contactPhone: String, contactEmail: String, settings: AppSettings)` con `var attempt: Int` (empieza en 1), `fun next(kind: PageKind): Action`, `val finished: Boolean`. Reglas: NO_APPOINTMENTS → si `attempt < maxAttempts` `WaitAndRestart(intervalSec*1000, attempt+1)` e incrementa; si no `Stop("Sin citas tras N intentos")`. OFFICE_AVAILABLE → `Found` (para; no rellena). CONTACT → `FillContact` una sola vez, luego `Found`. SLOT_OR_SMS → `Found`. CAPTCHA → `Stop("Captcha: resuélvelo tú")`. THROTTLED/ERROR/UNKNOWN → `Stop(...)` con motivo. Misma PageKind 3 veces seguidas sin progreso → `Stop("La página no avanza")`.

- [ ] **Step 1:** Tests clasificador: un caso por PageKind con snapshots mínimos; `captchaWinsOverPersonalData`; `unknownPage`; `blankOrThrottleStops` (texto "" → THROTTLED).
- [ ] **Step 2:** Tests motor: `happyPathOrder` (PROVINCE→SelectProvince … REQUEST→Request); `retriesUntilMax` (10 NO_APPOINTMENTS con maxAttempts 10 → 9 WaitAndRestart con delay 60000 y attempt 2..10, luego Stop "Sin citas tras 10 intentos"); `officeAvailableIsFound`; `contactFilledOnceThenFound`; `captchaStops`; `unknownPageStops`; `stuckPageStops`; `neverConfirms` (ninguna Action pulsa confirmar — garantizado por el tipo).
- [ ] **Step 3:** Run `gradle -p cita-huellas :app:testDebugUnitTest`. Expected: FAIL.
- [ ] **Step 4:** Implementar.
- [ ] **Step 5:** Run tests. Expected: PASS.
- [ ] **Step 6:** Commit `feat(cita-huellas): motor de pasos`.

### Task 4: Puente JavaScript

**Files:**
- Create: `app/src/main/assets/bridge.js`, `app/src/main/java/com/citahuellas/web/JsBridge.kt`, `cita-huellas/jstest/fixtures/*.html`, `cita-huellas/jstest/bridge.test.mjs`
- Test: `app/src/test/java/com/citahuellas/web/JsBridgeTest.kt`

**Interfaces:**
- Consumes: `PageSnapshot`, `Action`, `Profile` (Task 2–3).
- Produces:
  - `bridge.js` define `window.CH = { snapshot(): string /*JSON {url,ids,text}*/, setValue(id, value): boolean, selectByText(id, needle): boolean /*coincidencia parcial sin tildes, mayúsculas*/, selectFirstNonEmpty(id): boolean, check(id): boolean, click(id): boolean }`. `click` usa `element.click()`; `setValue` dispara `input` y `change`. Ids con corchetes se buscan por `getElementById` o `[name="..."]`.
  - `object JsBridge { fun js(action: Action, myText: …): String? }` → genera la llamada JS para cada Action (null para Wait/Found/Stop). Argumentos siempre vía `Json.encodeToString(String)`.
  - Mapeo: SelectProvince → `selectByText("form","MADRID") && click("btnAceptar")`; SelectProcedure → `selectByText("tramiteGrupo[1]","TOMA DE HUELLAS") && click("btnAceptar")` (oficina queda "Cualquier oficina"); EnterWithoutClave → `click("btnEntrar")`; FillPersonal → `check("rdbTipoDocNie")`, `setValue("txtIdCitado",nie)`, `setValue("txtDesCitado",fullName)`, `selectByText("txtPaisNac",country)`, `click("btnEnviar")`; Request → `click("btnEnviar")`; FillContact → `setValue("txtTelefonoCitado")`, `setValue("emailUNO")`, `setValue("emailDOS")` sin pulsar.
  - Resultado de cada llamada: `"ok"` o `"missing:<id>"`; Kotlin convierte `missing` en `Stop("No encuentro <campo> en la web")`.

- [ ] **Step 1:** Tests Kotlin: `fillEscapesHostileText` (nombre `O'Brien "</script>` aparece JSON-escapado y no cierra la cadena), `waitAndStopProduceNoJs`, `fillContactNeverClicks` (no contiene `click(`).
- [ ] **Step 2:** Test JS con Playwright sobre fixtures HTML que imitan cada pantalla: `selectByText` ignora tildes (`"Perú"` encuentra `PERU`), `selectByTextMissingReturnsFalse`, `snapshot` lista ids incluidos `tramiteGrupo[1]`, `setValue` dispara `change`.
- [ ] **Step 3:** Run tests (gradle y `node jstest/bridge.test.mjs`). Expected: FAIL.
- [ ] **Step 4:** Implementar.
- [ ] **Step 5:** Run tests. Expected: PASS.
- [ ] **Step 6:** Commit `feat(cita-huellas): puente JS`.

### Task 5: Tema de cristal y pantallas

**Files:**
- Create: `ui/theme/Theme.kt`, `ui/theme/Glass.kt`, `ui/PeopleScreen.kt`, `ui/ProfileEditScreen.kt`, `ui/SettingsScreen.kt`, `ui/SearchScreen.kt`, `ui/App.kt`, `search/SearchController.kt`, `search/Notifier.kt`, `search/SmsCode.kt`; Modify: `MainActivity.kt`, `AndroidManifest.xml`

**Interfaces:**
- Consumes: todo lo anterior.
- Produces: app usable.
  - `Glass.kt`: `@Composable fun GlassBackground(content)` (degradado + 3 manchas pastel lavanda `#C9B8FF`, menta `#B5F0DC`, melocotón `#FFD3B5`; oscuro: fondo `#0E1630`), `Modifier.glass(hazeState)` (blanco 55 % / ahumado 35 %, borde 1 dp blanco 60 %, radio 24 dp, desenfoque Haze en API 31+).
  - `SearchController(webView, engine, notifier, onState: (SearchUiState) -> Unit)`: en `onPageFinished` espera 2–4 s aleatorios, inyecta `bridge.js`, pide `snapshot`, clasifica, ejecuta `JsBridge.js(action)`; `WaitAndRestart` → cuenta atrás visible y `loadUrl(entrada)`; `Found` → `Notifier.found()` y para; `Stop` → `Notifier.stopped(reason)`. `cancel()` detiene todo.
  - `SearchUiState(status: String, attempt: Int, max: Int, secondsLeft: Int?, color: StatusColor)`; `StatusColor { SEARCHING /*azul #7FA8FF*/, FOUND /*verde #5BD6A0*/, STOPPED /*ámbar #FFC56B*/ }`.
  - `Notifier`: canal `cita_encontrada` IMPORTANCE_HIGH con vibración y sonido; pide `POST_NOTIFICATIONS` en API 33+.
  - `SmsCode`: `SmsRetriever.getClient(ctx).startSmsUserConsent(null)` al llegar a SLOT_OR_SMS si el contacto es "mi número"; extrae el primer número de 4–8 dígitos y hace `setValue("txtCodigoVerificacion", code)` sin confirmar. Campo manual grande siempre visible en ese paso.
  - `FLAG_KEEP_SCREEN_ON` mientras `SearchController` está activo.
  - Navegación con barra inferior de cristal: Personas · Buscar · Ajustes. Botón "Rellenar esta pantalla" en Buscar.

- [ ] **Step 1:** Implementar tema y pantallas de Personas/Edición/Ajustes con validación: NIE `^[XYZ]\d{7}[A-Z]$` (aviso, no bloqueo), correo con `@`.
- [ ] **Step 2:** Implementar SearchScreen + SearchController + Notifier + SmsCode.
- [ ] **Step 3:** Run `gradle -p cita-huellas :app:testDebugUnitTest :app:assembleDebug`. Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 4:** Instalar en emulador no es posible aquí; renderizar capturas con Compose Preview no disponible sin dispositivo → comprobar con `aapt2 dump badging` que el manifiesto tiene paquete, permisos `INTERNET`, `POST_NOTIFICATIONS`, `WAKE_LOCK` ausente, sin `READ_SMS`.
- [ ] **Step 5:** Commit `feat(cita-huellas): interfaz de cristal y búsqueda`.

### Task 6: Firma y entrega del APK

**Files:**
- Modify: `app/build.gradle.kts` (signingConfig leyendo `~/.citahuellas/keystore.properties`), `cita-huellas/README.md` (instalación y uso en español)

- [ ] **Step 1:** `keytool -genkeypair` → `~/.citahuellas/release.jks` (RSA 4096, 30 años); propiedades fuera del repo.
- [ ] **Step 2:** Run `gradle -p cita-huellas :app:assembleRelease`. Expected: `app-release.apk`; `apksigner verify --print-certs` OK.
- [ ] **Step 3:** Copiar a `cita-huellas/dist/CitaHuellas-1.0.apk` (ignorado por git) y enviarlo al usuario con SendUserFile.
- [ ] **Step 4:** Commit README + config y push a `claude/agent-browser-setup-0lix72`.
