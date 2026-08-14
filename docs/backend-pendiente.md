# Lo que la app espera del backend y todavía hay que confirmar

La app ya está llamando a todo esto. Aquí está lo que **no pude verificar** contra el
servidor y lo que **falta construir**.

> **Por qué no lo verifiqué.** La red desde la que trabajo tiene un FortiGate con
> inspección TLS que bloquea `api.undiamas.site` entero, categoría «Newly Registered
> Domain». Todas las peticiones —incluida `/healthz`— devuelven la página de bloqueo del
> cortafuegos con un certificado emitido por Fortinet, no por Let's Encrypt. Es exactamente
> el caso que advierte tu documento de despliegue. Comprueba tú desde datos móviles o por
> SSH.

---

## 1. Recuperación de contraseña — nombres de campo por confirmar

Esto es lo único donde **adiviné el contrato**. Tu documento menciona las rutas y que
responden 503 sin SMTP, pero no las formas del cuerpo. Lo que la app manda hoy:

### `POST /v1/auth/password/forgot`

```json
{ "email": "alguien@ejemplo.com" }
```

Espera `204`. Debe responder **igual exista o no la cuenta** — contestar distinto convierte
esta ruta en una forma de averiguar quién tiene cuenta en una app de adicciones.

### `POST /v1/auth/password/reset`

```json
{ "email": "alguien@ejemplo.com", "code": "123456", "password": "la-nueva" }
```

Espera `204`. La app inicia sesión sola justo después, así que no hace falta que devuelva
tokens.

**Si tus campos se llaman distinto** (`codigo` en vez de `code`, `nuevaPassword` en vez de
`password`…), cámbialos en un solo sitio: `ForgotPasswordRequest` y `ResetPasswordRequest`
en [`ApiDtos.kt`](../shared/src/commonMain/kotlin/com/eter/undiamas/core/data/api/ApiDtos.kt).

**503 ya está contemplado.** Si el servidor no tiene SMTP, la app no deja a nadie esperando
un correo que no llega: dice que la recuperación automática no está disponible y que
escriban. Puedes desplegar sin SMTP sin romper nada.

---

## 2. `DELETE /v1/users/me` — sigue faltando

Es el hueco más serio y ya venía de antes. Sin esta ruta:

- El «borrado de emergencia» de la app borra el diario y lo local, pero **los check-ins y el
  historial de recaídas se quedan en el servidor**. La app lo dice claro, pero es una
  promesa a medias.
- **Google Play lo exige.** Una app que recoge datos de cuenta necesita borrado de cuenta
  desde dentro de la app *y* desde una URL pública.

Cuando la montes, tiene que arrastrar también `community_stories`, `community_useful`,
`community_reports` y `community_blocks` de ese usuario.

---

## 3. Las dos páginas del dominio raíz

La app ya enlaza a estas dos desde Configuración → «Tus datos»:

| URL | Para qué |
|---|---|
| `https://undiamas.site/privacidad` | Política de privacidad. Play la pide accesible **sin instalar la app**. |
| `https://undiamas.site/borrar-cuenta` | Instrucciones de borrado de cuenta, también públicas. |

**Ninguna de las dos funciona hoy.** Tu propio documento lo detecta: el `CNAME` de `www`
apunta a `undiamas.site`, que no tiene registro `A`. Hace falta un `A` en el dominio raíz
apuntando a `153.75.249.146` y servir esas dos páginas con Caddy.

Si prefieres otras rutas, están en `PaginasLegales` en
[`LinkOpener.kt`](../shared/src/commonMain/kotlin/com/eter/undiamas/core/presentation/LinkOpener.kt).

Para la política de privacidad, algo que ahora es **más fuerte** de lo que era: al quitarse
el asistente de la app, **ya no sale nada hacia Gemini**. Antes había que explicar la
distinción entre el diario (que se queda) y el chat (que viajaba); ahora la frase es la
simple, que además es la que tranquiliza: nada de lo que la persona escribe se manda a
servicios de terceros. Está redactada tal cual en Configuración.

---

## 4. Lo que la app ya tolera sin que hagas nada

No hace falta que despliegues esto para que la app funcione:

| Falta en el servidor | Qué hace la app |
|---|---|
| `GEMINI_API_KEY` vacía → `/v1/ai/*` da 404 | **Indiferente: la app ya no tiene asistente.** Puedes dejarla sin configurar y ahorrarte la facturación de Gemini. |
| SMTP sin configurar → 503 | Explica que la recuperación automática no existe todavía. |
| Comunidad sin construir | Muro vacío con su mensaje. No hay datos de ejemplo. |
| Servidor entero caído | Abre con lo guardado en SQLite y encola lo que se escriba. |

---

## 5. Preguntas nuevas del alta y dónde acaba cada respuesta

El cuestionario pasó de 6 a 10 pasos. Las cinco primeras son obligatorias; las demás se
pueden saltar con «Prefiero contestar esto después».

| Pregunta | Dónde acaba |
|---|---|
| Qué estás dejando | Solo local — el servidor no guarda el tipo de adicción |
| Nombre | `PATCH /v1/users/me` → `displayName` |
| Días sobrio / récord | `/v1/sobriety` |
| Gasto diario | `/v1/sobriety` |
| **Por qué lo haces** (nueva) | `PATCH /v1/users/me` → **`porQuePersonal`** |
| **Meta de ahorro** (nueva) | Solo local |
| **Detonantes habituales** (nueva) | Solo local |
| Contacto de confianza **+ rol** (rol nuevo) | `/v1/contacts` → `contactosEmergencia` |
| **Hora del recordatorio** (nueva) | `/v1/reminders` |

El permiso de notificaciones del sistema se pide **al terminar el cuestionario**, no
escondido en Configuración: los avisos son la mitad del acompañamiento, y una app que nunca
los pide solo sirve los días en que la persona se acuerda de abrirla — que son justo los
días en los que menos falta hace.

**`porQuePersonal` llevaba tiempo viajando vacío.** El campo existía en el modelo y se
enviaba en cada guardado de perfil, pero nada lo preguntaba nunca. Ahora se pregunta al
crear la cuenta y es lo que aparece en las notificaciones de los días difíciles.

---

## 6. Direcciones

La app tiene **una sola** dirección compilada dentro, sin forma de cambiarla desde dentro:

```
https://api.undiamas.site
```

Está en `ApiConfig` en
[`ApiConfig.kt`](../shared/src/commonMain/kotlin/com/eter/undiamas/core/data/api/ApiConfig.kt).
Se quitó el selector de servidor de Configuración a propósito: dejar que alguien reapunte la
app a otro servidor es dejar que le roben la sesión y el historial.

Como efecto secundario, **la build de debug también apunta a producción**. Si quieres volver
a probar contra tu máquina (`10.0.2.2:8080` en el emulador), hay que reintroducir el campo
por `buildType`; dímelo y lo pongo solo para debug.

Para probar sin servidor está la beta local, que no habla con nada:

```
./gradlew :androidApp:assembleBeta
```
