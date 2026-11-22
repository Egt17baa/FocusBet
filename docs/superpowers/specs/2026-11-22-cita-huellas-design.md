# Cita Huellas — diseño

Fecha: 2026-11-22

## Para qué y por qué

App Android de uso personal para pedir cita de **toma de huellas en Madrid** en la web oficial de cita previa de extranjería, para familiares y amigos. Hoy hay que teclear lo mismo en cada intento y vigilar la web a mano. La app rellena los pasos repetitivos, reintenta con un ritmo humano y avisa cuando aparece una cita; la persona usuaria decide y confirma.

## Objetivos

- Recorrer automáticamente los pasos fijos del trámite con los datos de un perfil guardado.
- Reintentar cuando no hay citas: por defecto cada **60 s**, máximo **10 intentos** (ajustable; intervalo mínimo 30 s).
- Avisar con **notificación con sonido y vibración** cuando haya cita, y quedarse quieta en esa pantalla.
- Guardar perfiles de personas **cifrados y solo en el móvil**.
- Código SMS: autorrellenarlo si llega al propio móvil (con permiso por mensaje), o escribirlo a mano si llega al móvil del familiar.

## No objetivos

- Otras provincias u otros trámites (se puede ampliar más adelante).
- Servidor, cuentas, sincronización o copia en la nube.
- Publicar en Google Play.

## Anti-objetivos (fracaso aunque "funcione")

- Resolver o saltarse captchas, falsificar huella de navegador, simular movimientos de ratón o cualquier técnica para ocultar la automatización.
- Reintentar sin límite o más rápido de 30 s.
- Confirmar una cita sin que la persona usuaria pulse confirmar.
- Que los datos personales salgan del móvil (más allá de enviarlos a la propia web oficial).

## Restricciones

- La web solo responde desde redes españolas/domésticas; no se puede probar desde el servidor de desarrollo. Las pruebas reales se hacen en el móvil.
- La web puede cambiar: buscar elementos por id conocido y, si falla, por texto visible.
- Android corta apps en segundo plano: la búsqueda funciona con la app **en primer plano y pantalla encendida**.
- Instalación por APK directo (origen desconocido), sin Play Store.

## Enfoques considerados

1. **App nativa Kotlin + WebView con JavaScript inyectado (elegido).** Permite rellenar y pulsar, pantalla encendida, notificaciones, lectura de SMS con consentimiento y almacenamiento cifrado.
2. Chrome / Custom Tabs: no permite inyectar ni leer la página. Descartado.
3. Marcador o PWA: sin SMS, sin avisos fiables, sin cifrado del sistema. Descartado.
4. Flutter / React Native: sin ventaja para una única app Android. Descartado.

## Recorrido de la web (decidido con la persona usuaria)

Base: `https://icp.administracionelectronica.gob.es/icpplustiem/`

| Paso | Pantalla | Acción de la app | Ids conocidos (fallback por texto) |
|---|---|---|---|
| 1 | Inicio | Provincia **Madrid** → Aceptar | select provincia, `btnAceptar` |
| 2 | Oficina y trámite | Oficina "Cualquier oficina"; trámites de extranjería sin tocar; en Policía elegir la opción que contiene **"TOMA DE HUELLAS"** → Aceptar | `sede`, `tramiteGrupo[1]` (valor 4010 orientativo), `btnAceptar` |
| 3 | Instrucciones | **Presentación sin Cl@ve** | `btnEntrar` |
| 4 | Datos | Tipo NIE, NIE, nombre y apellidos, país → Aceptar | `rdbTipoDocNie`, `txtIdCitado`, `txtDesCitado`, `txtPaisNac`, `btnEnviar` |
| 5 | Solicitar | **Solicitar cita** | `btnEnviar` |
| 5a | Sin citas | Texto "no hay citas" → esperar intervalo y volver al paso 1 | — |
| 5b | Con citas | **Parar, notificar**, dejar a la persona usuaria elegir oficina | `idSede`, `btnSiguiente` |
| 6 | Contacto | Rellenar teléfono y correo (del perfil o propios); la persona usuaria pulsa Siguiente | `txtTelefonoCitado`, `emailUNO`, `emailDOS` |
| 7 | Hueco + SMS | Persona usuaria elige hueco; código SMS autorrellenado o manual; **confirma la persona usuaria** | `rdbCita`, `txtCodigoVerificacion`, `btnConfirmar` |

Ids sacados del proyecto público cita-bot; solo como referencia de nombres de campos.

Paradas obligatorias: cualquier captcha visible, pantalla desconocida, error, página en blanco o mensaje de exceso de peticiones → **parar la búsqueda y avisar**, sin reintentar.

Ritmo: pausa de 2–4 s entre pantallas (lo que tardaría una persona), y el intervalo configurado entre intentos.

## Pantallas de la app

- **Personas**: lista de perfiles; añadir/editar/borrar. Campos: nombre y apellidos, NIE, país de nacionalidad, teléfono, correo, nota libre.
- **Buscar cita**: elegir persona y teléfono de contacto ("su número" / "mi número"), pulsar Buscar. Muestra la web, el estado ("Intento 3/10 — siguiente en 42 s"), y botón Cancelar. Botón "Rellenar esta pantalla" para uso manual.
- **Ajustes**: intervalo (30–300 s, defecto 60), intentos máximos (1–20, defecto 10), mi teléfono y mi correo.

## Aspecto visual (pedido por la persona usuaria)

Tema **muy amigable con efecto cristal/desenfocado** (glassmorphism):

- Fondo con degradado suave y manchas de color pastel (lavanda, menta, melocotón) que se ven desenfocadas detrás del contenido.
- Tarjetas, barra superior y barra inferior de "cristal": blanco translúcido con desenfoque de lo que hay detrás, borde fino claro y esquinas muy redondeadas (24 dp).
- Tipografía redondeada y grande, iconos simples, botones tipo píldora; estados con color (buscando = azul suave, cita encontrada = verde, parada = ámbar).
- Modo oscuro: mismas formas, cristal ahumado sobre fondo azul noche.
- Desenfoque real en Android 12+; en versiones anteriores, el mismo cristal translúcido sin desenfoque.
- La web oficial se muestra dentro de una tarjeta de cristal grande, con el estado del intento flotando encima.

## Decisiones técnicas (decisión de Claude salvo lo indicado)

- Kotlin, Jetpack Compose (Material 3 personalizado; desenfoque de fondo con la librería Haze) para las pantallas propias, `android.webkit.WebView` para la web.
- Motor de pasos: detectar la pantalla actual con JS (ids/textos presentes) y ejecutar la acción correspondiente; lógica de detección en Kotlin pura para poder probarla con tests unitarios.
- Perfiles: JSON cifrado con AES-GCM y clave en Android Keystore.
- SMS: API SMS User Consent de Google Play services (sin permiso READ_SMS).
- Notificación con canal de alta prioridad; `FLAG_KEEP_SCREEN_ON` mientras busca.
- minSdk 26, targetSdk 35. APK release firmado con una clave propia generada para el proyecto (no en el repositorio).
- Código en `cita-huellas/` dentro del repositorio FocusBet; APK entregado como archivo descargable.

## Para quien lo construya

- Lista de países: la app ofrece un buscador sobre las opciones reales del desplegable de la web; en el perfil se guarda el texto tal cual aparezca.
- Textos exactos de "no hay citas" y de error: usar coincidencias parciales en mayúsculas ("NO HAY CITAS", "EN ESTE MOMENTO NO HAY") y tratar todo lo desconocido como parada.
