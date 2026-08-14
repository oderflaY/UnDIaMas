# Un Día Más — cómo está hecho y por qué

Una app de acompañamiento en recuperación de adicciones. Este documento explica qué hace,
cómo está construida, con qué se conecta y —sobre todo— **por qué cada decisión es la que
es**. Las decisiones técnicas de esta app casi nunca son neutrales: hay una persona en un
mal momento al otro lado, y eso cambia cuál es la respuesta correcta.

---

## 1. Qué es

Alguien que está dejando algo —alcohol, nicotina, juego, pantallas— abre esta app y ve
cuánto lleva. Cada día registra cómo va en un **check-in**, y de ahí sale un **semáforo**:
verde, amarillo o rojo. El semáforo decide el tono de la app y cuántas veces le escribe.

Alrededor de eso hay nueve herramientas más: diario, cálculo de lo ahorrado, anclas —los
motivos por los que lo hace—, cápsulas de tiempo, hábitos, estadísticas, un protocolo de
emergencia, un ejercicio para sostener un impulso, y un muro de comunidad.

Y un botón que importa tanto como el resto: **registrar una recaída**.

### El principio que ordena todo lo demás

> Caer es parte del proceso. La app nunca castiga, nunca culpa y nunca esconde la recaída.

De ahí salen decisiones concretas que aparecerán a lo largo del documento: el botón de
recaída está en la pantalla principal y no enterrado; el récord se conserva al recaer; el
botón de confirmar es ámbar y no rojo; el diálogo dice «esto no borra tu esfuerzo».

---

## 2. La estructura

Dos módulos de Gradle:

```
:shared      Casi toda la app. Código común Kotlin Multiplatform.
:androidApp  La cáscara de Android: una Activity, notificaciones, permisos, icono.
```

`:shared` está organizado por **funcionalidad**, no por capa técnica:

```
core/
  domain/      modelos y puertos (interfaces). No sabe que existen ni la red ni SQLite.
  data/
    api/       cliente HTTP, DTOs, repositorios
    local/     SQLite, cola de envío, conectividad
  presentation/  AppState, navegación, tema, componentes compartidos
features/
  anclas/ auth/ avisos/ calculadora/ capsulas/ checkin/ comunidad/
  configuracion/ diario/ emergencia/ estadisticas/ habitos/ inicio/
  onboarding/ perfil/ sobriedad/ splash/
```

**Por qué por funcionalidad.** Organizar por capas (`todas las pantallas/`, `todos los
modelos/`) hace que un cambio pequeño toque cuatro carpetas lejanas. Aquí, cambiar el
semáforo se hace casi entero dentro de `features/checkin/`. Cada carpeta se puede leer, y
borrar, sin desenredarla del resto — que es exactamente lo que pasó cuando se quitaron la
biometría y el asistente: dos carpetas fuera y poco más.

**La regla de dependencia**: `domain` no importa nada de `data` ni de `presentation`. Las
reglas de negocio —contar la racha, decidir el semáforo, calcular el ahorro— no saben si
hay servidor. Por eso se pueden probar sin levantar nada.

---

## 3. Cómo funciona por dentro

### Offline-first: el orden importa más que las piezas

El patrón, que se repite en todos los repositorios:

```
escribir → guardar en SQLite (marcado como pendiente) → encolar → intentar enviar
leer     → SQLite, siempre; la red solo actualiza esa copia
```

Lo decisivo es que **se guarda antes de intentar la red**. La alternativa habitual
—intentar enviar y guardar en local solo si falla— pierde datos justo cuando peor sienta
perderlos: si la app se cierra a mitad del envío, ese check-in no existió nunca.

De ahí sale una consecuencia que se nota al usar la app: **escribir nunca falla por falta
de red**. Guardar un check-in en el metro y guardarlo con wifi son la misma acción, con la
misma respuesta inmediata.

**La cola de envío** (`Outbox`) es una tabla de SQLite con tres reglas:

1. **Orden estricto.** Se envía por número de secuencia, que es el orden en que pasaron las
   cosas. Una recaída y un check-in en desorden le cuentan al servidor una historia que no
   ocurrió.
2. **Se borra solo cuando el servidor confirma.** Si algo falla, la fila se queda. Mandar
   algo dos veces es mejor que perderlo.
3. **Nada caduca solo.** Un registro de hace tres días sigue siendo el día de esa persona.

Las nueve tablas locales: `check_ins`, `diario`, `animos`, `recaidas`, `alertas`,
`semaforo`, `perfil`, `outbox` y `meta`.

### Abrir la app sin conexión

Este fue un fallo real y merece explicarse. Al arrancar, la app pide la sesión. Hay tres
caminos y **la diferencia entre ellos importa mucho**:

| Situación | Qué hace |
|---|---|
| No hay tokens guardados | Pantalla de entrada. No hay sesión. |
| El servidor responde 401 | La sesión murió de verdad. Se borra todo y a la pantalla de entrada. |
| **No se pudo preguntar** (sin red, servidor caído) | **Se entra con la copia local.** |

Confundir los dos últimos significa echar a alguien de su propia app porque el wifi no va.
En una app de recuperación eso no es un fallo menor: es cruel.

### El estado en pantalla

`AppState` es el punto único donde vive el estado. Las pantallas leen de ahí y nunca hablan
con la red. Cada repositorio publica un flujo; `AppState` los refleja en listas de Compose,
y Compose repinta solo lo que cambió.

Cuando el semáforo cambia, se rehace el plan de avisos del día. Cuando llega la racha del
servidor, si supera al récord guardado, el récord se actualiza — eso sobrevive a la próxima
recaída, porque es lo único que le queda a alguien de un tramo que costó meses.

### Las notificaciones

Son **locales**: las programa el propio teléfono, así que llegan con la app cerrada y sin
conexión. Cuántas llegan lo decide el semáforo:

| Semáforo | Avisos al día | Franja horaria |
|---|---|---|
| Verde | 2 | 09:00 – 21:00 |
| Amarillo | 5 | 08:00 – 22:00 |
| Rojo | 11 | 07:00 – 23:00 |

La franja también se abre con el riesgo, y no por simetría: las horas malas son de
madrugada y a última hora, justo las que en verde no hace falta cubrir.

Hay 24 plantillas repartidas en cinco categorías, y el reparto dice a qué le da peso la
app: 10 de ayuda, 5 de racha, 4 de anclas, 3 de ahorro y 2 de check-in. Se rotan para que
no se repita el mismo mensaje.

Las plantillas se escribieron con dos reglas fijas: **ninguna culpa** —ni siquiera en
rojo, porque un aviso que hace sentir mal a alguien que está a punto de consumir empuja en
la dirección contraria— y **todas ofrecen algo que hacer**: «respira conmigo», «llama a
Luis», «abre tus anclas». Un ánimo sin salida concreta no sirve de nada en rojo.

Los avisos mencionan **el «por qué» que la persona escribió** y **cuánto lleva ahorrado**.
Un recordatorio genérico se ignora a la tercera vez; el motivo propio de alguien, escrito
con sus palabras, no.

Y una plantilla se descarta sola si no se puede redactar con los datos que hay: una de
ahorro sin gasto declarado saldría como «Has ahorrado 0», que es peor que no mandar nada.

El permiso del sistema se pide **al terminar el cuestionario inicial**, no escondido en
Configuración y no nada más abrir. Después de haber explicado para qué son y de que la
persona haya elegido su hora — así el cuadro del sistema aparece cuando ya se sabe qué se
está aceptando.

---

## 4. Con qué se conecta

```
App (Android / iOS)
   │  HTTPS
   ▼
https://api.undiamas.site      ← Caddy (TLS) → API en Go → PostgreSQL 17
```

Una sola dirección, compilada dentro de la app. Los 26 endpoints cubren sesión, perfil,
tracker de sobriedad, check-ins, semáforo, diario, ánimos, recaídas, alertas,
recordatorios, estadísticas y comunidad. Hay además un canal abierto (`/v1/events`) por el
que el servidor empuja alertas mientras la app está abierta.

**No se puede cambiar la dirección desde la app, a propósito.** Hubo un selector de servidor
para pruebas y se quitó: dejar que alguien reapunte la app a otro servidor es dejar que le
roben la sesión y el historial de recaídas. Basta con convencer a una persona de escribir
una dirección.

La app tolera un servidor incompleto sin romperse: sin SMTP la recuperación de contraseña lo
dice claro en vez de dejar esperando un correo que no llega; sin comunidad, el muro sale
vacío con su mensaje; y con el servidor entero caído, la app abre igual con lo que hay en
SQLite. Los detalles, en [`backend-pendiente.md`](backend-pendiente.md).

### Autenticación

Tokens JWT: uno de acceso corto y otro de refresco largo. Cuando el corto caduca, el cliente
HTTP renueva y repite la petición original sin que la pantalla se entere. Si el refresco
también falla, se borran los tokens y se manda a la pantalla de entrada.

Al cerrar sesión, **los tokens locales se borran pase lo que pase**, aunque el servidor no
conteste. Si no, «cerrar sesión» no habría cerrado nada en ese teléfono — que es justo lo
contrario de lo que alguien espera al pulsarlo por privacidad.

---

## 5. Cómo se usa

### Compilar

```bash
./gradlew :androidApp:assembleDebug     # APK de desarrollo
./gradlew :androidApp:assembleBeta      # beta sin backend, todo local (~10 MB)
./gradlew :androidApp:bundleRelease     # AAB firmado para Play
./gradlew :shared:testAndroidHostTest   # los tests
```

La firma sale de `keystore.properties`, que **no está en el repositorio**: una clave de
firma filtrada permite publicar actualizaciones falsas de la app en nombre de quien la
publicó. Hay un `keystore.properties.example` con la forma.

### La beta local

`assembleBeta` produce una app que **no habla con ningún servidor**: sin sesión que pedir,
sin cola de envío, con el muro de comunidad explicando que llega más adelante. Todo se
guarda en SQLite. Sirve para probar la app entera con el backend apagado, y se instala junto
a la de verdad porque lleva otro identificador.

Que lo decida la compilación y no un ajuste dentro de la app es deliberado: una beta que se
pudiera reconectar a media sesión acabaría con los datos de alguien partidos entre dos
sitios, sin forma limpia de juntarlos.

### El primer arranque

Splash → diapositivas de bienvenida → crear cuenta → cuestionario de 10 pasos → inicio.

Del cuestionario, **las cinco primeras preguntas son obligatorias** (qué estás dejando,
nombre, días que llevas, récord anterior, gasto diario) y el resto se saltan con «Prefiero
contestar esto después».

La división no es arbitraria. Quien crea la cuenta suele estar en un mal momento, y un
cuestionario de diez pantallas obligatorias es una forma eficaz de que cierre la app antes
de llegar al final. Las cinco primeras son las que el contador y el ahorro necesitan para
existir; las otras cinco afinan el acompañamiento y se pueden llenar después desde Perfil.

---

## 6. Por qué estas herramientas

### Kotlin Multiplatform + Compose Multiplatform

**Qué resuelve:** una sola base de código para Android e iOS, incluidas las pantallas.

**Por qué esto y no otra cosa.** Frente a **nativo por separado**, escribir dos veces la
lógica del semáforo y del contador de racha significa dos sitios donde puede aparecer un
error en la parte más delicada de la app. Frente a **Flutter o React Native**, KMP no
sustituye la plataforma: el código específico —notificaciones, Health Connect en su
momento, marcar un teléfono— se escribe en Kotlin o Swift nativo mediante `expect`/`actual`,
sin puentes ni plugins de terceros. Y frente a esos dos, la lógica compilada es Kotlin, con
la misma biblioteca estándar y las mismas corrutinas en ambas plataformas.

**El coste**, que conviene decir: el ecosistema es más pequeño y algunas versiones son
`alpha`/`beta`. Se asume porque la alternativa era duplicar la lógica más crítica.

### Ktor Client

**Qué resuelve:** las peticiones HTTP.

**Por qué esto y no Retrofit/OkHttp.** Retrofit es la opción por defecto en Android, pero
**solo existe en JVM** — en iOS no hay nada que hacer. Ktor es multiplataforma y usa el
motor nativo de cada sistema por debajo (OkHttp en Android, Darwin en iOS), así que no
renuncia a nada. Su plugin de autenticación resuelve el refresco de token de forma
transparente, que es exactamente el problema que había que resolver.

### kotlinx.serialization

**Qué resuelve:** convertir JSON en objetos.

**Por qué esto y no Gson/Moshi.** Ambos dependen de reflexión en tiempo de ejecución, que
no existe en el Kotlin nativo de iOS. `kotlinx.serialization` genera el código al compilar:
funciona en las dos plataformas y, además, **un campo mal escrito es un error de
compilación**, no una sorpresa en el teléfono de alguien.

### androidx.sqlite con driver empaquetado

**Qué resuelve:** la base de datos local.

**Por qué esto y no Room o SQLDelight.** Los dos son mejores herramientas en abstracto —
generan código y verifican las consultas. Pero los dos añaden un plugin de compilación, y
este proyecto va sobre versiones muy recientes de Gradle, AGP y Kotlin, donde los plugins de
terceros son la primera pieza que se rompe. `androidx.sqlite` es la capa de más abajo: SQL a
mano, sin generación de código y sin plugin. **Menos comodidad a cambio de que el proyecto
compile.** Es una decisión de riesgo, no de gusto.

El driver va *empaquetado* (`sqlite-bundled`) en vez de usar el SQLite del sistema: así la
misma versión corre en todos los teléfonos y en los tests, sin diferencias de comportamiento
entre un Android viejo y uno nuevo.

### DataStore

**Qué resuelve:** los tokens de sesión y las cuatro preferencias que hacen falta antes de
abrir la base de datos.

Es lo que sustituye a `SharedPreferences`, y es asíncrono, así que no bloquea el arranque.
No guarda datos de la persona: eso vive en SQLite.

### Notificaciones locales en vez de push

**Por qué no Firebase Cloud Messaging.** Las notificaciones de esta app son previsibles: se
sabe a qué hora y cuántas según el semáforo. Programarlas en el teléfono significa que
**funcionan sin conexión y sin depender de los servicios de Google** — y que el contenido,
que menciona la racha y los motivos personales de alguien, no pasa por un servidor de
terceros. El servidor solo empuja las alertas en vivo, y solo con la app abierta.

### Lo que se quitó, y por qué

- **Firebase** (era el backend original). Se sustituyó por el servidor propio en Go y
  PostgreSQL: control sobre dónde viven los datos, y sin atarse a un proveedor.
- **Health Connect / lectura de pulsera.** Detectaba picos de pulso como posible episodio de
  abstinencia. Se retiró junto con sus permisos de salud, que son de los más sensibles que
  puede pedir una app y añadían fricción en la revisión de Play.
- **Asistente conversacional.** Se retiró entero, incluida la capa de datos. Efecto
  colateral que juega a favor: **ya no sale nada de la app hacia servicios de IA de
  terceros**, y la política de privacidad puede decir la frase simple.

---

## 7. Cómo se prueba

**199 pruebas automáticas**, todas sobre la capa de dominio y de datos: contador de racha,
semáforo, calculadora de ahorro, planificador de avisos, cola de envío, base local, cliente
HTTP con motor falso, y validación de historias de comunidad.

Hay además una batería de integración contra un backend real, que **se salta sola** si no
hay servidor: que Postgres no esté levantado no es un error del código de la app. Su
dirección sale de una variable de entorno, no de una constante, para que nadie las lance sin
querer contra producción — crean cuentas de verdad.

Las pruebas se escribieron primero en las tres piezas donde un error hace daño de verdad:
**el contador de sobriedad, el semáforo y el protocolo de emergencia**.

Un ejemplo de por qué existen. Al crear una cuenta, el cuestionario se cerraba solo a media
pregunta: la app deducía «ya contestó» de que la persona tuviera nombre, y el formulario de
registro ya lo pide. Quien pasaba por ahí entraba sin fecha de inicio ni gasto diario, es
decir, **sin contador de racha y sin cálculo de ahorro** — las dos cosas por las que existe
la app. Ahora hay tres pruebas que fijan qué cuenta como haber contestado.

---

## 8. Seguridad y privacidad

| Medida | Por qué |
|---|---|
| Solo HTTPS, tráfico sin cifrar bloqueado | El historial de recaídas de alguien no puede viajar en claro por una wifi pública. |
| Sin copia de seguridad automática | Aquí hay tokens de sesión y diario. No puede acabar en el Drive de nadie ni restaurarse en otro teléfono sin volver a entrar. |
| Dirección del servidor no editable | Evita que se pueda redirigir la app a un servidor ajeno. |
| Beta no depurable | Con `debuggable`, cualquiera con el teléfono en la mano saca el diario por USB sin desbloquear nada. |
| Claves de firma fuera del repositorio | Una clave filtrada permite publicar actualizaciones falsas en nombre de quien publicó. |
| Modo camuflaje | Oculta los textos sensibles cuando alguien más puede ver la pantalla. |
| Borrado de emergencia | Borra el diario y lo local, y dice con claridad qué queda en el servidor. |

**Lo que la app dice de sí misma es literal.** El borrado de emergencia no promete borrar lo
que no puede borrar: mientras el servidor no tenga la ruta para eliminar la cuenta entera,
la app avisa de que los check-ins y el historial siguen ahí. Prometer un borrado que no
ocurre es peor que no ofrecerlo.

---

## 9. Detalles de diseño que no son estéticos

**Iconos, nunca emojis.** Un emoji se dibuja distinto en cada teléfono y en cada versión del
sistema; un icono vectorial se ve igual en todas partes y se adapta al tema claro y oscuro.

**Tema claro y oscuro siguiendo al teléfono.** Quien abre esta app de madrugada —que es
cuando más se abre— no debería recibir un pantallazo blanco.

**El botón de recaída: visible pero imposible de pulsar sin querer.** Está en la pantalla
principal, justo debajo de «hoy completé mi día», porque esconderlo diría que recaer es algo
que se oculta. Pero es un enlace discreto y no un botón, y siempre pasa por confirmación,
porque reinicia el contador y perder una racha de meses por un roce sería cruel.

**Ámbar, no rojo.** El rojo de esta app es el del semáforo y el del botón de emergencia.
Registrar una recaída no es una emergencia ni un error del sistema; pintarlo de rojo lo
convertiría en una alarma y en un reproche.

**El aviso de sincronización afirma primero lo que sí pasó.** Dice «guardado en tu teléfono»
y solo después explica lo que falta. Y no aparece cuando no hay nada pendiente: un aviso
permanente de «sin conexión» no ayuda a nadie y hace la app más ansiosa de usar.

**Los detonantes propios salen primero.** Los que la persona marcó al crear su cuenta
aparecen arriba en el check-in y al registrar una recaída. Buscar el tuyo entre seis
opciones, en el orden en que las escribió otra persona, justo cuando peor estás, es fricción
en el peor momento posible.

---

## 10. Lo que falta

- **Borrado de cuenta en el servidor.** Es el hueco más serio, y Google Play lo exige.
- **Las páginas públicas de privacidad y borrado**, que también exige Play. El dominio raíz
  todavía no resuelve.
- **Confirmar el contrato de recuperación de contraseña**: los nombres de los campos que
  manda la app están asumidos, no verificados.
- **Reponer las alarmas tras reiniciar el teléfono.** Hoy se reponen al abrir la app.
- **El historial del muro de comunidad**, que espera a que existan sus rutas en el servidor.

Todo con detalle en [`backend-pendiente.md`](backend-pendiente.md) y
[`backend-comunidad.md`](backend-comunidad.md).

---

## Resumen de versiones

| | |
|---|---|
| Kotlin | 2.4.10 |
| Compose Multiplatform | 1.11.1 |
| Gradle / AGP | 9.4.1 / 9.2.1 |
| Ktor | 3.5.2 |
| androidx.sqlite | 2.7.0 |
| Android | mínimo 29, objetivo 36 |
