# CLAUDE.md — App de Prevención de Recaídas (KMP + Firebase)

Guía de trabajo por fases para Claude Code, siguiendo el mismo flujo del diagrama de referencia (Setup → Backend/Lógica con TDD → Plataforma Firebase → Entrega), adaptado al stack real de este proyecto: **Kotlin Multiplatform, Jetpack Compose Multiplatform, Clean Architecture + MVVM, y Firebase como único backend** (sin servidor propio).

Cada fase termina en un **checkpoint**: resumen del plan/resultado y espera de aprobación (OK) antes de avanzar a la siguiente.

---

## Fase 1 · Setup

**01 — Ambiente y assets**
- JDK 17+, Android Studio (Koala o superior) con soporte KMP, Kotlin Multiplatform plugin, Xcode (si se compila destino iOS), Node + firebase-tools, Firebase CLI.
- Descargar/organizar assets de diseño (íconos, ilustraciones del semáforo de riesgo, etc.) en `./assets/`.

**02 — CLAUDE.md + estructura**
- Crear estructura de módulos por feature: `inicio/ sobriedad/ checkin/ ia/ diario/ estadisticas/ calculadora/ emergencia/ perfil/ configuracion/`, más `core/` (Clean Architecture: domain/data/presentation compartidos) y `shared/` para código común KMP.
- `.gitignore` para Android/KMP (build/, .gradle/, local.properties, google-services.json si aplica), `.env.example` con claves de configuración (Firebase project id, proveedor de IA), `git init`.

**03 — Contratos + fixtures**
- Definir contratos (data classes / interfaces `@Serializable` con kotlinx.serialization) para: perfil de usuario, evento de recaída, entrada de check-in, mensaje de IA, resultado del semáforo de riesgo (verde/amarillo/rojo).
- Mocks del proveedor de IA (Firebase AI Logic o proveedor configurable) para poder escribir tests antes de integrar el servicio real.
- Tests en ROJO (esperado): casos base de cada módulo, aún sin implementación.

**Checkpoint:** Plan y estructura de módulos resumidos → espera OK antes de codificar.

---

## Fase 2 · Lógica de negocio con TDD (Rojo → Verde)

Esta app **no tiene servidor propio** — la "lógica de backend" vive en Cloud Functions y en los repositorios/casos de uso de la app. El ciclo TDD aplica igual: rojo → verde → refactor, por módulo.

**04 — TDD · Casos de uso core**
- Contador de sobriedad: cálculo de días/horas activos, detección de recaída (racha vuelve a cero, se conserva el récord histórico), mensajes motivacionales antes de superar el récord.
- Calculadora de ahorro: ahorro diario/semanal/mensual/total según gasto previo declarado, con equivalencias de compras.
- Cobertura ≥ 90% en la capa de dominio (domain/usecases), sin dependencias de Firebase en estos tests (mockear repos).

**05 — TDD · Check-in inteligente y semáforo de riesgo**
- Lógica del cuestionario adaptativo (las preguntas cambian según estado del usuario / detonantes previos).
- Reglas del semáforo: verde (refuerzo positivo), amarillo (investigación de detonantes + recomendaciones), rojo (protocolo de emergencia).
- IA conversacional mockeada (Firebase AI Logic simulado) con outputs estructurados (tono según nivel de riesgo, análisis de historial/emociones/hábitos).

**06 — TDD · Cloud Functions (orquestación de reglas de negocio)**
- Funciones que disparan protocolo de emergencia en rojo (notificación push vía FCM, sugerencia de contacto de confianza).
- Reglas de agregación de estadísticas (histórico de check-ins, tendencias de riesgo).

**Salida transversal:** log estructurado de interacciones IA (`log_ia.jsonl` o colección Firestore equivalente) con trazabilidad completa de check-ins → nivel de riesgo → respuesta de la IA, para depuración y para las estadísticas del usuario.

**Checkpoint:** Tests verdes + commit por fase (Conventional Commits).

---

## Fase 3 · Plataforma Firebase (sin servidor propio)

**07 — Auth + Firestore + Storage**
- Firebase Authentication: Google y anónimo.
- Modelado de colecciones Firestore por módulo (usuarios, check-ins, eventos de recaída, entradas de diario, mensajes de IA).
- Firebase Storage para adjuntos del diario si aplica.

**08 — Firestore rules**
- Aislamiento estricto por UID en todas las colecciones: `request.auth.uid == resource.data.uid` (o equivalente al crear/leer/actualizar).
- Reglas específicas para datos sensibles (historial de recaídas, conversaciones con la IA): nunca legibles/escribibles entre usuarios distintos.

**09 — Firebase Cloud Messaging + Cloud Functions desplegadas**
- Notificaciones del protocolo de emergencia (rojo) y recordatorios de check-in.
- Cloud Functions con validación de entrada y manejo de errores antes de desplegar.

**Demo local funcional:** app corriendo contra emuladores Firebase (Auth/Firestore/Functions) con al menos 2 sesiones (Google + anónima) aisladas correctamente por UID.

---

## Fase 4 · Entrega

**10 — E2E contra flujos reales**
- Pruebas de extremo a extremo de los flujos críticos: onboarding → check-in → semáforo rojo → protocolo de emergencia; recaída → reinicio de racha con récord conservado; cálculo de ahorro con distintos escenarios de gasto.
- Validar coincidencia con casos de referencia/oráculo (≥ 4/5 casos esperados, igual que el criterio del diagrama).

**11 — Deploy + README + demo**
- `firebase deploy` (Firestore rules, Functions, Hosting si hay landing/web companion).
- README con: instrucciones de build (Android/iOS/Desktop si aplica), variables de entorno, arquitectura (Clean Architecture + MVVM + patrones usados: State para el semáforo, Strategy para el cuestionario adaptativo, Factory/Facade para el proveedor de IA, Observer para notificaciones/estado reactivo con Flow).
- Commit final (Conventional Commits).

**Entrega:** repo + `log_ia` + build/APK o link de distribución + README.

---

## Opcional · Skill de diseño para companion web (Hallmark)

Si el proyecto incluye un landing/companion web (por ejemplo, algo servido desde Firebase Hosting), se puede instalar la skill de terceros **Hallmark** (Together AI / Nutlope) para evitar que ese frontend se vea genérico ("AI slop"): fuerza variedad estructural, temas, tokens de diseño consistentes y elimina métricas/testimonios inventados.

- No aplica al código de la app móvil (Kotlin Multiplatform/Compose) — Hallmark está pensado para UI web (HTML/Tailwind/React).
- Instalación: `npx -y skills add nutlope/hallmark --skill hallmark --agent claude-code`.
- **Es código de terceros**: revisarlo antes de darle permisos de escritura en el repo, y no otorgarle acceso para refactorizar lógica de negocio — su alcance debe limitarse a la capa de presentación del landing/web companion.

---

## Recordatorios para Claude Code en este proyecto

- **Sensibilidad del dominio:** esta app trata con usuarios en recuperación de adicciones. El código debe reflejar buen trato: mensajes motivacionales sin culpa, manejo cuidadoso de datos de recaídas (privados, aislados por UID, nunca expuestos en logs compartidos), y el protocolo de emergencia (rojo) debe ser confiable y probado a fondo — es la parte más crítica del sistema.
- **No hay backend propio:** toda "API" es Firebase (Firestore, Cloud Functions, Auth, Storage, FCM). No proponer un servidor adicional salvo que se pida explícitamente.
- **Patrones de diseño:** usar los patrones recomendados donde encajen naturalmente (State para semáforo de riesgo, Strategy para cuestionario adaptativo, Factory/Facade para el proveedor de IA configurable, Observer con Flow para estado reactivo) — no forzarlos donde no aporten.
- **TDD real:** escribir el test en rojo antes que la implementación en cada módulo de la Fase 2, especialmente en contador de sobriedad, semáforo de riesgo y protocolo de emergencia.
- **Checkpoints:** al cerrar cada fase, resumir qué se hizo y esperar aprobación antes de continuar, igual que en el diagrama de referencia.
