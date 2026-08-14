# Comunidad — lo que hay que construir en el backend

La app ya está lista y llamando a estas rutas. Aquí está todo lo que necesita del servidor:
tablas, endpoints, validaciones y reglas.

> **La regla que manda sobre todas.** El servidor **nunca** devuelve el `user_id` del autor
> de una historia. Solo el alias. Aquí la gente cuenta que está en recuperación de una
> adicción: poder cruzar un alias con una cuenta es lo peor que puede filtrar esta app.

---

## 1. Tablas

### `community_profiles`

Un registro por usuario que participa. Se crea la primera vez que pide `GET /v1/community/me`.

| Columna | Tipo | Notas |
|---|---|---|
| `user_id` | uuid PK FK→users | |
| `alias` | text UNIQUE | 2–24 caracteres. Único **case-insensitive** |
| `stories_count` | int NOT NULL DEFAULT 0 | Historias publicadas y no retiradas |
| `banned_until` | timestamptz NULL | Sanción por moderación; mientras esté vigente no puede publicar |
| `created_at` | timestamptz NOT NULL | |
| `updated_at` | timestamptz NOT NULL | |

### `community_stories`

| Columna | Tipo | Notas |
|---|---|---|
| `id` | uuid PK | |
| `user_id` | uuid FK→users NOT NULL | **Nunca sale en una respuesta** |
| `alias_snapshot` | text NOT NULL | Copia del alias al publicar. Si luego lo cambia, la historia conserva el de entonces |
| `streak_days` | bigint NOT NULL DEFAULT 0 | Días al publicar. **0 si `compartirRacha` era false** |
| `objetivo` | text NULL | `ALCOHOL`, `NICOTINA`, `OPIOIDES`, `ESTIMULANTES`, `CANNABIS`, `JUEGO`, `PANTALLAS`, `COMPRAS`, `AUTOLESIONES`, `ANSIEDAD`, `OTRA` |
| `titulo` | text NOT NULL | ≤ 80 |
| `cuerpo` | text NOT NULL | 120–4000 |
| `estado` | text NOT NULL DEFAULT `'PUBLICADA'` | `PUBLICADA` \| `EN_REVISION` \| `RETIRADA` |
| `useful_count` | int NOT NULL DEFAULT 0 | Desnormalizado; se ordena por él |
| `reports_count` | int NOT NULL DEFAULT 0 | |
| `created_at` | timestamptz NOT NULL | |
| `updated_at` | timestamptz NOT NULL | |

### `community_useful`

Quién marcó qué. La PK compuesta impide contar dos veces a la misma persona.

| Columna | Tipo |
|---|---|
| `story_id` | uuid FK→community_stories |
| `user_id` | uuid FK→users |
| `created_at` | timestamptz |

PK: `(story_id, user_id)`

### `community_reports`

| Columna | Tipo | Notas |
|---|---|---|
| `id` | uuid PK | |
| `story_id` | uuid FK NOT NULL | |
| `reporter_id` | uuid FK NOT NULL | |
| `motivo` | text NOT NULL | `APOLOGIA` \| `DATOS_PERSONALES` \| `ODIO` \| `SPAM` \| `OTRO` |
| `detalle` | text DEFAULT `''` | ≤ 500 |
| `resuelto` | bool NOT NULL DEFAULT false | |
| `created_at` | timestamptz NOT NULL | |

UNIQUE `(story_id, reporter_id)` — una persona reporta una historia una vez.

### `community_blocks`

| Columna | Tipo |
|---|---|
| `blocker_id` | uuid FK→users |
| `blocked_id` | uuid FK→users |
| `created_at` | timestamptz |

PK: `(blocker_id, blocked_id)`

### Índices

```sql
CREATE INDEX idx_stories_racha    ON community_stories (streak_days DESC, created_at DESC)
  WHERE estado = 'PUBLICADA';
CREATE INDEX idx_stories_reciente ON community_stories (created_at DESC)
  WHERE estado = 'PUBLICADA';
CREATE INDEX idx_stories_utiles   ON community_stories (useful_count DESC, created_at DESC)
  WHERE estado = 'PUBLICADA';
CREATE INDEX idx_stories_autor    ON community_stories (user_id, created_at DESC);
CREATE UNIQUE INDEX idx_alias_unico ON community_profiles (lower(alias));
```

---

## 2. Endpoints

Todos con `Authorization: Bearer`. Mismo formato de error que el resto: `{"error":"…","message":"…"}`.

### `GET /v1/community/me`

Crea el perfil si no existe.

```json
{
  "alias": "",
  "diasDeRacha": 0,
  "puedePublicar": false,
  "historiasPublicadas": 0,
  "diasParaPublicar": 0
}
```

`puedePublicar` = `diasDeRacha >= 30` **y** `alias != ''` **y** `banned_until` no vigente.
`diasDeRacha` sale de `sobriety_trackers.rachaSegundos / 86400` — **el servidor lo calcula, no lo manda la app**.

### `PUT /v1/community/me`

```json
{ "alias": "Ana R." }
```

→ mismo objeto que `GET`. Errores: `409 alias-taken`, `400 invalid-argument` si no cumple 2–24
caracteres o si contiene algo que parezca un correo, teléfono o URL.

### `GET /v1/community/stories`

Query: `sort` (`racha` por defecto \| `reciente` \| `utiles`), `limit` (1–50, defecto 20), `cursor`.

```json
{
  "items": [{
    "id": "…", "alias": "Ana R.", "diasDeRacha": 214,
    "objetivo": "ALCOHOL", "titulo": "…", "cuerpo": "…",
    "createdAt": "2026-08-07T12:00:00-06:00",
    "estado": "PUBLICADA", "utiles": 12,
    "marcada": false, "esMia": false
  }],
  "siguienteCursor": "…"
}
```

**Cursor, no número de página.** El muro ordena por racha y esta crece cada día: con `offset`,
las historias se desplazan entre peticiones y salen repetidas al avanzar. Codifica el par
`(campo_de_orden, id)` de la última fila, por ejemplo en base64.

Filtra siempre: `estado = 'PUBLICADA'` y `user_id NOT IN (bloqueados por quien pide)`.
`marcada` y `esMia` se calculan **para quien pide**.

### `POST /v1/community/stories`

```json
{ "titulo": "…", "cuerpo": "…", "objetivo": "ALCOHOL", "compartirRacha": true }
```

→ `201` con el `HistoriaDto`. Validar en el servidor (la app ya valida, pero no te fíes):

- `puedePublicar` — si no, `403 forbidden`
- `titulo` 1–80, `cuerpo` 120–4000 — si no, `400 invalid-argument`
- `objetivo` en la lista o `null`
- **Límite de frecuencia**: máximo 3 historias por usuario y día → `429 rate-limited`

`streak_days` lo pone el servidor desde el tracker, o `0` si `compartirRacha` es false.
`alias_snapshot` se copia del perfil.

### `DELETE /v1/community/stories/{id}`

Solo el autor. `204`. Si no es suya → **`404`, no `403`**: un 403 confirmaría que esa historia
existe. Decrementa `stories_count`.

### `PUT /v1/community/stories/{id}/useful`

```json
{ "util": true }
```

→ el `HistoriaDto` actualizado. Idempotente: marcar dos veces deja un solo registro.
No se puede marcar la propia → `400`.

### `POST /v1/community/stories/{id}/reports`

```json
{ "motivo": "APOLOGIA", "detalle": "" }
```

→ `204`. Incrementa `reports_count`. **Con 3 reportes distintos, pasa automáticamente a
`EN_REVISION`** y deja de salir en los listados hasta que alguien la mire.

### `POST /v1/community/stories/{id}/block-author`

→ `204`. Inserta en `community_blocks`. Bloquear al autor de la propia historia → `400`.

---

## 3. Reglas que no son obvias

**El alias se congela en la historia.** Por eso existe `alias_snapshot`. Si alguien cambia su
alias, sus historias viejas siguen con el de entonces: alguien podría reconocerse en una
historia y querer desligarse cambiando el alias, y eso solo funciona hacia adelante.

**`streak_days` no se recalcula nunca.** Es la racha *en el momento de publicar*. Si esa
persona recae, su historia sigue valiendo — de hecho contar 214 días y haber recaído después
es exactamente la clase de experiencia que sirve leer.

**Borrar la cuenta borra las historias.** Cuando montes `DELETE /v1/users/me` (que todavía
falta, y es lo que impide que el borrado de emergencia de la app funcione de verdad), tiene
que arrastrar `community_stories`, `community_useful`, `community_reports` y
`community_blocks` de ese usuario.

**404 en vez de 403 para lo ajeno**, igual que en el resto de tu API.

---

## 4. Moderación — no es opcional

Google Play rechaza apps con contenido de usuarios sin: forma de **reportar**, forma de
**bloquear**, y **revisión**. Las dos primeras ya están en la app. Del lado del servidor hace
falta al menos:

- Ver las historias `EN_REVISION` y las reportadas
- Pasarlas a `PUBLICADA` o `RETIRADA`
- `banned_until` para quien reincide

Un endpoint con rol `admin` basta para empezar.

**Un filtro que vale la pena** aunque sea simple: rechazar en `POST` un cuerpo que contenga
precios, gramajes, nombres de sustancias con dosis o sitios donde comprar. Alguien que entra
al muro está en un momento vulnerable, y una historia con "yo lo conseguía en X" no es una
experiencia: es un plan de consumo.

---

## 5. Cómo probarlo

La app está apuntando ya a estas rutas. Cuando las tengas, con esto ves si el contrato cuadra:

```fish
T=$(curl -s -X POST https://api.undiamas.site/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"…","password":"…"}' | python3 -c "import json,sys;print(json.load(sys.stdin)['accessToken'])")

curl -s -H "Authorization: Bearer $T" https://api.undiamas.site/v1/community/me
curl -s -H "Authorization: Bearer $T" "https://api.undiamas.site/v1/community/stories?sort=racha&limit=5"
```

La app no trae datos de ejemplo: hasta que existan estas rutas, el muro sale vacío con su
mensaje de "todavía no hay historias", y publicar da error de red. Es lo que pediste.
