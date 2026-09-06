# Documentación técnica — Un Día Más

Referencia pieza por pieza. Para cada componente: **qué hace**, **por qué existe** y **por
qué está hecho así y no de otra forma**.

Para la visión general y la justificación de las herramientas elegidas, ver
[`el-proyecto.md`](el-proyecto.md). Este documento es el detalle.

### Índice

1. [Los dos módulos](#1-los-dos-módulos)
2. [`core/domain` — las reglas](#2-coredomain--las-reglas)
3. [`core/data/local` — el teléfono](#3-coredatalocal--el-teléfono)
4. [`core/data/api` — el servidor](#4-coredataapi--el-servidor)
5. [`core/presentation` — el estado y lo compartido](#5-corepresentation--el-estado-y-lo-compartido)
6. [Las funcionalidades, una por una](#6-las-funcionalidades-una-por-una)
7. [Código de plataforma](#7-código-de-plataforma)
8. [Compilación y variantes](#8-compilación-y-variantes)
9. [Las pruebas](#9-las-pruebas)

---

## 1. Los dos módulos

| Módulo | Qué contiene |
|---|---|
| `:shared` | Toda la lógica y todas las pantallas. Kotlin Multiplatform. |
| `:androidApp` | Una Activity, las notificaciones de Android, el icono, la firma. |

**Por qué solo dos.** Un módulo por funcionalidad daría compilación incremental más fina,
pero multiplica la configuración de Gradle por diecisiete y obliga a declarar dependencias
entre funcionalidades que hoy se resuelven con una importación. Con este tamaño de proyecto
el coste supera al beneficio.

**Por qué `:androidApp` es tan pequeño.** Todo lo que puede vivir en común, vive en común.
En `:androidApp` solo queda lo que no existe en iOS: `AlarmManager`, los canales de
notificación, el permiso de notificaciones y el icono adaptativo. Si mañana se compila para
iOS, no hay que reescribir ninguna pantalla.

---

## 2. `core/domain` — las reglas

Modelos y contratos. **No importa nada de `data` ni de `presentation`**: no sabe que existen
la red ni SQLite. Por eso todo lo de aquí se prueba sin levantar nada.

### `model/`

| Archivo | Qué define | Por qué así |
|---|---|---|
| `UserProfile.kt` | Perfil, contactos de confianza, meta de ahorro, detonantes habituales | Contactos con **rol** (padrino, terapeuta, familiar, amistad): en una llamada de emergencia importa a quién se está llamando. |
| `CheckInEntry.kt` | Check-in y el enum `Trigger` | Las respuestas son un `Map<String, String>` y no campos fijos, porque el cuestionario es adaptativo y sus preguntas cambian. |
| `RiskLevel.kt` | `VERDE`, `AMARILLO`, `ROJO` | Tres niveles, no cinco. Es lo que se puede distinguir de un vistazo y lo que cabe en una metáfora que todo el mundo ya entiende. |
| `RelapseEvent.kt` | Recaída, con detonantes y racha previa | Guarda **la racha que había antes**: sin ese dato, una recaída borra la información de cuánto se había aguantado. |
| `AddictionType.kt` | Once tipos | `substance` separa sustancias de conductas: en las primeras la abstinencia puede tener síntomas físicos, y eso cambia el texto de los avisos. |

**Por qué `UserProfile` guarda `habitualTriggers`.** Los detonantes que la persona reconoce
al crear su cuenta se ponen primero en el check-in y al registrar una recaída. Buscar el
tuyo entre seis opciones, en el orden en que las escribió otra persona, justo cuando peor
estás, es fricción en el peor momento posible.

### `repository/`

Los puertos: `AuthRepository`, `PerfilRepository`, `CheckInRepository`, `DiaryRepository`,
`MoodRepository`, `RelapseRepository`, `TrafficLightRepository`, `AlertRepository`,
`ReminderRepository`, y el genérico `RemoteList<T>`.

**Por qué interfaces y no clases directas.** Permite tres implementaciones del mismo
contrato sin que nada de arriba se entere: la que habla con el servidor, la de la beta local
y la falsa de los tests. Es lo que hace posible que `assembleBeta` produzca una app sin
backend cambiando una línea del grafo de dependencias.

**Un detalle que se ganó a base de un fallo:** `PerfilRepository` expone
`onboardingCompleto`. La app deducía «ya contestó el cuestionario» de que la persona tuviera
nombre — y el formulario de registro ya lo pide. Resultado: el cuestionario se cerraba solo
a media pregunta, y quien pasaba por ahí entraba **sin fecha de inicio ni gasto diario**, es
decir, sin contador de racha y sin cálculo de ahorro. La señal correcta es tener fecha de
inicio en el servidor, porque solo la escribe el propio cuestionario.

---

## 3. `core/data/local` — el teléfono

### `UndiamasDatabase.kt`

SQLite con el driver empaquetado. Nueve tablas: `check_ins`, `diario`, `animos`, `recaidas`,
`alertas`, `semaforo`, `perfil`, `outbox`, `meta`.

- **Conexión perezosa**: se abre en el primer uso y fuera del hilo principal. Abrirla en el
  arranque añadiría medio segundo de pantalla en blanco.
- **Protegida por un `Mutex`**: SQLite no admite escrituras concurrentes, y aquí escriben
  la interfaz y el sincronizador a la vez.
- **Si no se puede abrir el archivo, cae a una base en memoria.** La app funciona esa
  sesión aunque no persista, en vez de no arrancar. Un fallo de disco no puede dejar a
  alguien sin su protocolo de emergencia.

### `Outbox.kt`

La cola de lo que falta enviar. Tres reglas:

1. **Orden estricto por número de secuencia.** Una recaída y un check-in en desorden le
   cuentan al servidor una historia que no ocurrió.
2. **Se borra solo con confirmación del servidor.** Mandar algo dos veces es mejor que
   perderlo.
3. **Nada caduca solo.** Un registro de hace tres días sigue siendo el día de esa persona.

Tiene un interruptor `activa`. En la beta local está apagada: sin servidor, la cola solo
crecería para siempre con cambios que nadie va a recoger.

### `LocalStore.kt`

La caché de lectura. Publica un `StateFlow` por colección, así que las pantallas se
actualizan solas.

- `abrirSesion(userId)` **borra la caché si cambió la cuenta**. Sin esto, entrar con otra
  cuenta en el mismo teléfono mostraría el diario de la anterior. En esta app es lo peor que
  podría filtrar.
- El interruptor `marcarPendientes`, apagado en la beta local: sin servidor, marcar todo
  como pendiente dejaría a la app avisando para siempre de una espera que no existe.

### `SyncManager.kt`

Vacía la cola, **de una en una y en orden**.

- Un `Mutex` con `tryLock()` impide dos vaciados a la vez: al volver la red, el aviso de
  conectividad y el reintento periódico pueden dispararse juntos.
- Distingue fallos **definitivos** de **reintentables**: 400, 403 y 404 significan que ese
  envío nunca va a funcionar y se descarta; 401 y 429 son temporales y se reintentan. Sin
  esa distinción, una fila mala bloquea la cola entera para siempre.

### `Connectivity.kt`

`expect`/`actual`. Android usa `NetworkCallback`; iOS, `NWPathMonitor`.

**Por qué observar y no preguntar.** Preguntar «¿hay red?» antes de cada envío es una
carrera perdida: la respuesta caduca en el momento en que se recibe. Observando, la app
reacciona al volver la conexión en lugar de esperar a que alguien abra una pantalla.

### `UserPreferences.kt`

DataStore. Guarda los tokens, si el cuestionario se completó, el nombre y el tipo de
adicción.

**Por qué separado de SQLite.** Estos cuatro datos hacen falta *antes* de poder abrir la
base de datos, y son los que deciden qué pantalla se pinta primero.

---

## 4. `core/data/api` — el servidor

### `ApiConfig.kt`

Una constante: `https://api.undiamas.site`.

**Por qué una constante y no un ajuste.** Hubo un selector de servidor para pruebas y se
quitó. Dejar que alguien reapunte la app a otro servidor es dejar que le roben la sesión y
el historial de recaídas: basta con convencer a una persona de escribir una dirección.

### `ApiClient.kt`

El cliente HTTP, con tres piezas:

1. **Renovación de token transparente.** Si el de acceso caduca, se renueva y se repite la
   petición original. La pantalla no se entera.
2. **Traducción de errores.** Toda respuesta no exitosa se convierte en `ApiException` con
   un **código estable**, no con el texto del servidor — ese texto es para depurar. Un
   volcado técnico en mitad de una crisis es ruido en el peor momento posible.
3. **La dirección se resuelve en cada petición**, no al construir el cliente, para que los
   tests puedan apuntar a su propio servidor sin rehacer el grafo entero.

### `ApiError.kt`

Códigos estables (`invalid-argument`, `unauthenticated`, `email-taken`, `rate-limited`,
`unavailable`…) y su traducción a lenguaje humano.

`isServiceMissing` distingue «esta ruta no existe en este servidor» de «falló la llamada».
Es un caso real: la recuperación por correo solo se monta si hay SMTP configurado, y lo que
la app le dice a la persona es distinto en cada caso.

### `ApiAuthRepository.kt`

Sesión contra el servidor, con copia local. Tres caminos al recuperar la sesión:

| Situación | Qué hace |
|---|---|
| Sin tokens | No hay sesión. A la pantalla de entrada. |
| El servidor responde 401 | La sesión murió. Se borra todo. |
| **No se pudo preguntar** | **Se entra con la copia local.** |

Confundir los dos últimos —y así estaba— significa echar a alguien de su propia app porque
el wifi no va. Fue un fallo real.

### `LocalAuthRepository.kt`

La implementación de la beta local: crea una cuenta local en el primer arranque y **no pide
credenciales**. Pedir un correo y una contraseña que no se comprueban contra nada sería
teatro, y además invitaría a reutilizar una contraseña de verdad.

### `OfflineRepositories.kt`

Todos los repositorios de datos. El patrón, idéntico en cada uno:

```
escribir → guardar en SQLite (pendiente) → encolar → intentar enviar
leer     → SQLite, siempre; la red solo actualiza esa copia
```

**Se guarda antes de intentar la red.** La alternativa habitual —enviar y guardar en local
solo si falla— pierde datos justo cuando peor sienta perderlos. De ahí que escribir nunca
falle por falta de red: guardar un check-in en el metro y con wifi son la misma acción.

Un detalle del diario: al borrar una entrada que aún no había salido del teléfono, **se
quita también de la cola**. Mandar al servidor un «crea esto» seguido de un «bórralo» deja
rastro de algo que la persona decidió que no existiera.

### `EventStream.kt`

Canal abierto con el servidor (`/v1/events`) con reconexión y espera creciente.

**Por qué esto en vez de notificaciones push.** Solo funciona con la app abierta, y es
suficiente: lo que llega por aquí son alertas en vivo. Nada se pierde cuando la app está
cerrada, porque queda en `/v1/alerts`, que es lo que se vuelve a leer al abrir. A cambio, el
contenido no pasa por servidores de terceros.

### `ApiGraph.kt`

Arma la capa de datos entera y decide qué implementación entra según el modo.

**Por qué existe.** Para que `AppState` no tenga que conocer a Ktor, ni a SQLite, ni el
orden en que se construye cada pieza. Y para poder cambiar el motor HTTP o la base por unos
de prueba sin tocar nada más — que es exactamente lo que hacen los tests.

---

## 5. `core/presentation` — el estado y lo compartido

### `AppState.kt`

El punto único donde vive el estado. Las pantallas leen de aquí y **nunca hablan con la
red**.

**Por qué una sola clase y no un ViewModel por pantalla.** Casi todo aquí está conectado: un
check-in cambia el semáforo, el semáforo cambia el plan de avisos, la racha cambia el
récord. Repartir eso en quince ViewModels obligaría a un bus de eventos entre ellos, que es
la misma dependencia con más pasos.

Responsabilidades: sesión y arranque, espejo de los repositorios, escrituras, plan de
avisos, comunidad y recuperación de contraseña.

**Una regla que se ganó con un fallo:** `isOnboarded` solo puede encenderse por hechos
explícitos, y **nunca apagarse desde el espejo del perfil**. Si el servidor tarda mientras
alguien está a media pregunta, la respuesta que llega no puede cerrarle el cuestionario en
la cara.

### `Navigation.kt`

Una pila de pantallas con un `enum`, sin librería de navegación.

**Por qué a mano.** Dieciséis pantallas sin rutas profundas ni parámetros complejos. Una
librería de navegación aportaría URLs y grafos que aquí no hacen falta, a cambio de una
dependencia más y de otro sitio donde puede romperse la compilación multiplataforma.

### `theme/`

Paleta, degradados, tipografía, movimiento e iconos.

- **Claro y oscuro siguiendo al teléfono**, con opción de forzar. Quien abre esta app de
  madrugada —que es cuando más se abre— no debería recibir un pantallazo blanco.
- **`AppIcons` centraliza todos los iconos.** Nada de emojis: se dibujan distinto en cada
  teléfono y en cada versión del sistema; un icono vectorial se ve igual en todas partes y
  se adapta al tema.

### `components/`

| Componente | Para qué |
|---|---|
| `LogoUnDiaMas` | El logo dibujado en Compose. Está en código común porque un drawable de Android no existe en iOS, y tenerlo dos veces es tenerlo mal una de las dos. |
| `SyncBanner` | Afirma primero lo que **sí** pasó («guardado en tu teléfono») y solo después lo que falta. Y no aparece si no hay nada pendiente: un aviso permanente de «sin conexión» hace la app más ansiosa de usar. |
| `BreathingCircle`, `CountdownRing`, `BilateralFocus`, `BubblePopGame` | Ejercicios de regulación para el protocolo de emergencia: respiración guiada, temporizador, estimulación bilateral y una distracción manual. |
| `StreakRing`, `CleanDaysCalendar`, `TrafficLight`, `Confetti` | Visualizaciones de progreso. |
| `BackGuard` | Evita salir por accidente de pantallas donde perder el paso duele. |

**Por qué cuatro ejercicios distintos y no uno.** Lo que sirve para calmar a una persona no
sirve para otra, y en un impulso agudo no hay margen para probar. Ofrecer varias salidas es
más útil que ofrecer la mejor en promedio.

### `StartupDiagnosis.kt`

Traduce un fallo de arranque a una explicación con una acción concreta, en vez de un código
de error.

---

## 6. Las funcionalidades, una por una

### `sobriedad` — el contador

`SobrietyCounter`, `Milestones`, `SobrietyScreen`, `RegistrarRecaida`.

**Por qué es el corazón.** Es el dato por el que la mayoría abre la app.

- El récord se calcula así: **si la racha de ahora ya superó al récord guardado, el récord
  *es* la racha de ahora.** Enseñar «42 días · mejor racha 30» sería absurdo, y aquí además
  desalentador.
- Al recaer, **la racha vuelve a cero y el récord se conserva**. Es lo único que le queda a
  alguien de un tramo que costó meses.
- El acceso a registrar la recaída está en la pantalla principal, **visible pero imposible
  de pulsar sin querer**: enlace discreto en vez de botón, y siempre con confirmación.
  Esconderlo diría que recaer es algo que se oculta; ponerlo como botón grande haría que se
  perdiera una racha de meses por un roce.
- El botón de confirmar es **ámbar, no rojo**. El rojo de esta app es el del semáforo y el
  de emergencia. Registrar una recaída no es una emergencia ni un error del sistema.
- Confirmación y detonantes en **un solo diálogo**. Antes eran dos encadenados: encadenar
  pantallas a alguien que acaba de recaer se siente como un interrogatorio.

### `checkin` — el semáforo

`RiskAssessor`, `CheckInHistory`, `CheckInScreen`.

Reglas actuales, deliberadamente simples:

| Respuesta | Nivel |
|---|---|
| Hay impulso de consumo | **ROJO** |
| Hay un detonante presente | **AMARILLO** |
| Ninguna de las dos | **VERDE** |

**Por qué tan simples.** Una fórmula con pesos sería más difícil de explicar y de probar, y
no está claro que acertara más. Estas reglas se leen en voz alta y se entienden, y la
persona puede predecir qué va a salir — que es lo que hace que confíe en el resultado.

El semáforo no es solo un color: **decide el tono de la app y cuántos avisos llegan**.

### `avisos` — las notificaciones

`Aviso`, `PlantillasDeAviso`, `PlanificadorDeAvisos`, `Notificador`.

| Semáforo | Avisos/día | Franja |
|---|---|---|
| Verde | 2 | 09:00 – 21:00 |
| Amarillo | 5 | 08:00 – 22:00 |
| Rojo | 11 | 07:00 – 23:00 |

La franja también se abre con el riesgo, y no por simetría: las horas malas son de
madrugada y a última hora, justo las que en verde no hace falta cubrir.

24 plantillas en cinco categorías, y el reparto dice a qué le da peso la app: **10 de ayuda,
5 de racha, 4 de anclas, 3 de ahorro, 2 de check-in**. Se rotan para no repetir mensaje.

Dos reglas fijas al escribirlas:

1. **Ninguna culpa**, ni siquiera en rojo. Un aviso que hace sentir mal a alguien que está a
   punto de consumir empuja en la dirección contraria.
2. **Todas ofrecen algo que hacer**: «respira conmigo», «llama a Luis», «abre tus anclas».
   Un ánimo sin salida concreta no sirve de nada en rojo.

Los avisos mencionan **el «por qué» que la persona escribió** y **cuánto lleva ahorrado**.
Un recordatorio genérico se ignora a la tercera vez; el motivo propio de alguien, con sus
palabras, no. Y una plantilla se descarta sola si no se puede redactar con los datos que
hay: una de ahorro sin gasto declarado saldría como «Has ahorrado 0», peor que no mandar
nada.

**Por qué locales y no push.** Son previsibles: se sabe a qué hora y cuántas. Programarlas
en el teléfono significa que funcionan sin conexión y que su contenido —racha y motivos
personales— no pasa por un servidor de terceros.

### `emergencia` — el protocolo

`UrgeSurfingSession`, `EmergenciaScreen`, `HelplineButtons`, `UrgeSurfingScreen`.

**Es la parte más crítica de la app.** Se llega desde el semáforo en rojo o desde el banner
del inicio.

La sesión de *urge surfing* dura **15 minutos en tres etapas de cinco**, porque el pico de
un impulso agudo suele durar ese arco. La clase es **pura**: recibe los segundos
transcurridos y devuelve en qué punto va, sin reloj propio. Así se prueba entera sin esperar
quince minutos.

Los botones de línea de ayuda **abren el marcador con el número cargado, sin llamar solos**.
La persona confirma. Eso evita pedir el permiso de llamada y evita una llamada accidental.

### `diario`

`DiaryEntry`, `SentimentAnalyzer`, `DiarioScreen`.

El análisis de sentimiento es **local, por palabras clave**: sin red y sin enviar el diario
a ningún sitio. Y es deliberadamente conservador — ante señales mezcladas marca
`VULNERABLE`, porque aquí **subestimar un mal momento cuesta más que sobreestimarlo**.

El diario se puede bloquear desde Configuración.

### `calculadora` — el ahorro

`SavingsCalculator`, `CalculadoraScreen`.

Ahorro diario, semanal, mensual y total desde el gasto declarado, con equivalencias en
compras y una proyección de interés compuesto.

**Por qué equivalencias y no solo la cifra.** «Llevas 4 380 pesos» es abstracto; «llevas un
par de zapatos y medio viaje» se entiende. La proyección se presenta como **estimación
ilustrativa, no asesoría financiera** — el código lo dice explícito.

### `anclas`

`Anchor`, `AnclasScreen`.

Los motivos por los que la persona lo hace: personas, metas, recuerdos.

**Por qué es un muro visual y no una lista de texto.** En un impulso fuerte el cerebro
racional no lee bien. Estímulos cortos y muy visuales funcionan mejor que un párrafo.

### `capsulas`

`TimeCapsule`, `TimeCapsuleVault`, `CapsulasScreen`.

Un mensaje que la persona se escribe a sí misma **en un buen día** para leerlo más adelante,
cuando le cueste recordar por qué empezó. Nadie convence mejor a alguien que su propia voz
de hace tres meses.

Los días que faltan se calculan sobre el número de día absoluto, para no depender de meses
de distinto largo.

### `habitos`

`HabitTracker`, `HabitosScreen`.

«No romper la cadena». La cadena **solo cuenta hacia atrás desde hoy**: si ayer se rompió,
la racha es 0 aunque haya semanas perfectas más atrás. Es lo que hace que la cadena
signifique algo.

### `estadisticas`

`RiskInsights`, `RiskPatternDetector`, `EstadisticasScreen`.

Calendario de 28 días, distribución del semáforo, mapa de horas críticas.

El detector de patrones es **deliberadamente conservador**: exige un mínimo de repeticiones
antes de afirmar nada. Decirle a alguien «los viernes recaes» a partir de dos datos sueltos
sería inventar un patrón y sembrar una profecía que se cumple sola.

### `comunidad`

`Historia`, `ValidadorDeHistoria`, `ComunidadRepository`, dos pantallas.

Quien lleva más tiempo cuenta cómo lo hizo. Requisitos: **30 días mínimos** para publicar,
título ≤ 80 caracteres, cuerpo entre 120 y 4 000.

- **El mínimo de 30 días** evita que el muro se llene de mensajes del primer día, que no es
  lo que alguien viene a leer.
- **El cuerpo mínimo de 120** obliga a contar algo, no a soltar una frase.
- El validador **bloquea** los errores de formato, pero **solo avisa** si detecta correo,
  teléfono o URL: puede haber un motivo legítimo, y decidir por la persona sería
  paternalista. Se avisa y ella decide.
- **El servidor nunca devuelve el `user_id` del autor**, solo el alias. Poder cruzar un
  alias con una cuenta es lo peor que puede filtrar esta app.
- Hay **reportar y bloquear** en la app: Google Play rechaza contenido de usuarios sin
  ambas cosas y sin revisión.
- **Sin datos de ejemplo.** Hasta que existan las rutas, el muro sale vacío con su mensaje.

### `onboarding`

`OnboardingScreen`, `AddictionCarousel`, `IntroSlides`.

Diez pasos; **los cinco primeros obligatorios**, el resto salteables con «Prefiero contestar
esto después».

**Por qué la división.** Quien crea la cuenta suele estar en un mal momento, y diez
pantallas obligatorias son una forma eficaz de que cierre la app antes del final. Los cinco
primeros son los que el contador y el ahorro necesitan para existir; el resto afina el
acompañamiento y se puede llenar luego desde Perfil.

**El permiso de notificaciones se pide al terminar**, no nada más abrir. Después de haber
explicado para qué son y de que la persona haya elegido su hora, para que el cuadro del
sistema aparezca cuando ya se sabe qué se está aceptando.

### `auth`

`AuthScreen`, `RecuperarContrasena`.

Crear cuenta o entrar. **Aquí no se pregunta por la adicción ni por nada sensible**: eso
viene después, ya dentro de la app.

La recuperación de contraseña existe porque sin ella la única salida para quien la olvida es
crear otra cuenta — y otra cuenta significa **empezar la racha de cero**. Perder ocho meses
registrados por no acordarse de una contraseña sería la peor forma posible de fallarle a
alguien.

Si el servidor no tiene correo configurado, lo dice claro en vez de dejar esperando algo que
no va a llegar.

### `configuracion`

Notificaciones, apariencia, cuenta, privacidad del diario y **Zona de Seguridad**.

La Zona de Seguridad va **enmarcada y al final**, para que nada de ahí se toque por
accidente. Contiene el modo camuflaje —oculta los textos sensibles cuando alguien más puede
ver la pantalla— y el borrado de emergencia.

**El borrado de emergencia dice la verdad sobre lo que no puede borrar.** Mientras el
servidor no tenga la ruta para eliminar la cuenta entera, avisa de que los check-ins y el
historial de recaídas siguen ahí. Prometer un borrado que no ocurre es peor que no
ofrecerlo.

### `inicio`, `perfil`, `splash`

- **Inicio**: racha, semana, botón de completar el día, acceso a recaída, ánimo, frase,
  rejilla de siete herramientas y banner de emergencia. Se lee de arriba abajo **en orden de
  urgencia**.
- **Perfil**: identidad, «mi por qué», contactos, insignias.
- **Splash**: tapa el arranque —recuperar sesión y abrir la base local— para que nadie vea
  una pantalla a medio montar.

---

## 7. Código de plataforma

Seis pares `expect`/`actual`. La regla: **solo lo que de verdad no existe en común**.

| Contrato | Android | iOS |
|---|---|---|
| `databaseFilePath()` | `filesDir` | Documents |
| `preferencesFilePath()` | `filesDir` | Documents |
| `ConnectivityMonitor` | `NetworkCallback` | `NWPathMonitor` |
| `rememberPhoneDialer()` | Intent `ACTION_DIAL` | `tel:` |
| `rememberLinkOpener()` | Intent `ACTION_VIEW` | `UIApplication.openURL` |
| `closeApp()` | Cierra la Activity y la quita de recientes | `exitProcess(0)`, con reservas |

**`closeApp()` es el ejemplo de por qué `expect`/`actual` es mejor que un puente genérico.**
En Android es lo que espera cualquiera al pulsar «salir». En iOS técnicamente funciona, pero
las guías de revisión de Apple (2.5.x) prohíben terminar la app por código y es motivo
habitual de rechazo; además, para la persona y para los informes de fallos es
indistinguible de un cierre inesperado. Está implementado y **el propio archivo lo
documenta**, con la recomendación de ocultar el botón en iOS antes de publicar. La
diferencia queda escrita en el código en vez de descubrirse en una revisión rechazada.

En `:androidApp`:

- **`NotificadorAndroid`** — programa las alarmas y pide el permiso.
- **`AvisoReceiver`** — publica el aviso cuando salta la alarma. Usa **dos canales**:
  *acompañamiento* y *emergencia*. Separarlos permite silenciar los recordatorios diarios
  sin silenciar los avisos de riesgo, que es justo la distinción que alguien querría hacer.
  El urgente va con prioridad alta y categoría de alarma; el otro, como recordatorio normal.
  Al tocar la notificación se abre la app, no una pantalla suelta: el aviso invita a entrar,
  y desde dentro la persona decide qué necesita.
- **`MainActivity`** — la única Activity, porque toda la interfaz es Compose dentro de ella.

---

## 8. Compilación y variantes

```bash
./gradlew :androidApp:assembleDebug     # desarrollo
./gradlew :androidApp:assembleBeta      # sin backend, todo local, ~10 MB
./gradlew :androidApp:bundleRelease     # AAB firmado para Play
```

| Variante | Servidor | Ofuscada | Depurable |
|---|---|---|---|
| `debug` | producción | no | sí |
| `beta` | **ninguno** | sí | **no** |
| `release` | producción | sí | no |

**La beta no es depurable a propósito.** Aquí dentro hay diario e historial de recaídas: con
`debuggable`, cualquiera con el teléfono en la mano los saca por USB sin desbloquear nada.

**Que el modo local lo decida la compilación y no un ajuste** evita que una beta se
reconecte a media sesión y deje los datos de alguien partidos entre dos sitios, sin forma
limpia de juntarlos.

Medidas de seguridad de la build: solo HTTPS —el tráfico sin cifrar está bloqueado—, sin
copia de seguridad automática —aquí hay tokens y diario, y no puede acabar en el Drive de
nadie— y claves de firma fuera del repositorio, porque una clave filtrada permite publicar
actualizaciones falsas en nombre de quien publicó.

---

## 9. Las pruebas

**199 pruebas** sobre dominio y datos. Se escribieron primero en las tres piezas donde un
error hace daño de verdad: **contador de sobriedad, semáforo y protocolo de emergencia**.

| Área | Qué se prueba |
|---|---|
| Sobriedad | Racha, récord, hitos, formato de duración |
| Semáforo | Reglas de riesgo, historial, patrones |
| Ahorro | Cálculo y proyección |
| Avisos | Cadencia por nivel, rotación, disponibilidad |
| Datos locales | Base, cola de envío, sincronizador |
| Red | Cliente HTTP con motor falso, sesión, errores, mapeos |
| Comunidad | Validación de historias |
| Alta | Qué cuenta como cuestionario completado |

Hay además una batería de integración contra un backend real que **se salta sola** si no hay
servidor: que Postgres no esté levantado no es un error del código de la app. Su dirección
sale de una variable de entorno y no de una constante, para que nadie las lance sin querer
contra producción — crean cuentas de verdad.

**Por qué no hay pruebas de interfaz.** Son caras de mantener y frágiles ante cualquier
cambio de diseño. El esfuerzo está donde un fallo tiene consecuencias reales: si el contador
de racha se equivoca, alguien pierde la cuenta de su progreso; si un botón está tres píxeles
más abajo, no pasa nada.
