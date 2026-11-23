# Manual Completo de Ingeniería Agéntica y Arneses

> Documento de estudio y referencia técnica aplicado a **HomeTutorPro** (`agents_dev/`) y **devsusana-portfolio** (`agents_web/`).  
> Aquí está explicado **cómo se hacen las cosas bien, por qué se hacen así y qué errores evitar**, antes de tocar una sola línea de código.

---

## Índice

1. [Qué es un "Arnés" (Harness) y por qué importa más que el modelo](#1-qué-es-un-arnés-harness-y-por-qué-importa-más-que-el-modelo)
2. [Bloque 1: Investigación, Discovery y Fundamentos de LLMs](#2-bloque-1-investigación-discovery-y-fundamentos-de-llms)
3. [Bloque 2: Contexto, Contratos y Especificaciones (Specs)](#3-bloque-2-contexto-contratos-y-especificaciones-specs)
4. [Bloque 3: Roles Separados, Control de Alcance y Hooks](#4-bloque-3-roles-separados-control-de-alcance-y-hooks)
5. [Bloque 4: Paralelismo (Worktrees), Loop Engineering, MCPs y Evals](#5-bloque-4-paralelismo-worktrees-loop-engineering-mcps-y-evals)
6. [Bloque 5: IA dentro del Producto (AI Product)](#6-bloque-5-ia-dentro-del-producto-ai-product)
7. [Bloque 6: Orquestación Completa y Portabilidad](#7-bloque-6-orquestación-completa-y-portabilidad)

---

## 1. Qué es un "Arnés" (Harness) y por qué importa más que el modelo

Cuando trabajas con IA para desarrollar software, el error más común es pensar que todo depende de "qué modelo uses" o de "escribir un buen prompt". En ingeniería seria, el modelo es solo el **motor**; lo que determina si el resultado es fiable en producción es el **arnés (*harness*)** que rodea al motor.

El arnés es el conjunto de piezas deterministas y estructuradas que guían, limitan y verifican al modelo:

```text
[ Idea / Problema ]
       │
       ▼
1. Discovery (acotar problema y no-objetivos)
       │
       ▼
2. Spec (contrato: qué tocar, qué no tocar, cómo verificar)
       │
       ▼
3. Agente Constructor (lee contexto mínimo + spec -> implementa + tests)
       │
       ▼
4. Barreras Deterministas (Git hooks: ktlint/detekt + métricas + tests)
       │
       ▼
5. Agente Validador Ciego (sesión nueva: spec + git diff + logs -> PASS/FAIL)
       │
       ▼
6. PR en GitHub con evidencias -> Revisión humana y Merge
```

**Principio fundamental:**  
Todo lo que pueda comprobarse de forma **determinista** (compilación, linter, reglas de arquitectura por AST/regex, tests, límites de archivos) **no se le pide al LLM que "lo prometa"**: se impone con herramientas y hooks. Al LLM se le deja lo que requiere criterio semántico.

---

## 2. Bloque 1: Investigación, *Discovery* y Fundamentos de LLMs

### 2.1. Cómo funciona la ventana de contexto en un bucle agéntico
En un bucle ReAct como el de [`agents_dev/core/engine.py`](../agents_dev/core/engine.py), cada iteración envía al modelo:
1. `system_prompt` (instrucciones + `AGENTS.md` + `BUSINESS_RULES.md` + `spec`).
2. Todo el historial de mensajes previos (petición del usuario, respuestas anteriores del modelo, llamadas a herramientas y **salidas completas de esas herramientas**).

**Tres problemas clásicos que debes conocer:**
* **Efecto bola de nieve en costes y latencia:** Si tu bucle hace 15 iteraciones y en cada una inyectas `AGENTS.md` + `BUSINESS_RULES.md` + logs largos de `./gradlew`, estás pagando y procesando decenas de miles de tokens repetidos en cada paso.
* ***Lost in the middle* (pérdida en el centro):** Los modelos prestan mucha más atención al principio del prompt (inicio del `system_prompt`) y al final (último mensaje/error), y tienden a ignorar reglas enterradas en medio de un contexto enorme.
* ***Context rot* (degradación del contexto):** Cuantos más errores fallidos, parches descartados y logs antiguos se acumulan en un mismo chat, peor razona el modelo. **Regla de oro:** cuando cambies de tarea o de fase (ej. de construir a validar), **abre una sesión limpia**.

### 2.2. Qué es una *Skill de Discovery* y cómo se hace bien
El error típico es pedirle al agente directamente: *"Añade un badge de pagos pendientes a los alumnos"*. Sin *discovery*, el agente asume:
* Qué significa "pago pendiente" (`> 0` vs umbral, clases sin pagar vs deuda).
* En qué pantalla va (`StudentList`, `StudentDetail`, o ambas).
* Cómo se ve y si es clickable.

**Cómo funciona una buena skill de Discovery:**
1. **No escribe código ni propone implementación técnica todavía.**
2. **Lee el estado actual relevante** (entidades de dominio o pantallas existentes) para no hacer preguntas obvias que ya están en el código.
3. **Hace tandas cortas de preguntas (3–5 máximo)** enfocadas únicamente en decisiones de producto y casos límite (*edge cases*).
4. **Produce un artefacto cerrado** (no guarda la conversación de preguntas, como ocurrió en [`specs/a_adir_un_badge_de_aviso_en_lo.md`](../specs/a_adir_un_badge_de_aviso_en_lo.md), sino el documento final aprobado).

**Las 5 preguntas que todo Discovery debe responder antes de pasar a Spec:**
1. **Problema y objetivo:** ¿Qué problema real del usuario resuelve?
2. **Fuera de alcance (*Non-goals*):** ¿Qué NO vamos a hacer ahora aunque sea tentador?
3. **Reglas de negocio y *edge cases*:** ¿Qué pasa si el valor es 0, negativo, nulo, offline o sin permisos?
4. **Criterio de éxito:** ¿Cómo sabremos sin lugar a dudas que funciona?
5. **Impacto en datos/migraciones:** ¿Cambia el esquema de Room o Firestore?

### 2.3. *Research* previo antes de especificar
Cuando una tarea involucra una librería nueva (ej. MediaPipe, Room migrations, Kobweb markdown) o una parte desconocida del repo:
* Separa la fase de **investigación** de la fase de **implementación**.
* Usa fuentes fiables (documentación oficial, código actual del proyecto, o herramientas de síntesis como NotebookLM para cruzar PDFs/docs largos) para generar un **resumen técnico breve** con enlaces/rutas reales, y descarta el ruido antes de empezar a programar.

---

## 3. Bloque 2: Contexto, Contratos y Especificaciones (*Specs*)

### 3.1. Qué debe ir en `AGENTS.md` y qué NO
Un buen `AGENTS.md` actúa como la constitución estable del proyecto, no como un diario de todo lo que existe.

* **SÍ debe estar en `AGENTS.md`:**
  * Stack obligatorio y versiones/herramientas clave.
  * Arquitectura y fronteras entre capas (ej. `domain/` sin imports de Android).
  * Comandos exactos de verificación (`./gradlew compileDebugKotlin`, `./gradlew testDebugUnitTest`).
  * Convenciones no negociables (idioma, naming, DI, navegación type-safe).
* **NO debe estar en `AGENTS.md`:**
  * Reglas de una pantalla concreta o detalles de una sola feature.
  * Explicaciones largas de por qué existe una librería.
  * Información duplicada que ya está en otro doc que también cargas entero.

**Mejora clave (*Just-in-Time Context*):**  
En lugar de cargar siempre toda la guía de presentación cuando el agente solo va a tocar `domain/`, se pueden tener reglas modulares por capa (ej. un `AGENTS.md` raíz corto + `domain/AGENTS.md`, `data/AGENTS.md`, `presentation/AGENTS.md`) que el agente lee solo cuando entra en esa carpeta.

### 3.2. Anatomía de una *Spec* como contrato técnico
Una *spec* profesional no es una charla; es un documento estructurado que sirve tanto para que el **Constructor** ejecute sin ambigüedad como para que el **Validador Ciego** audite sin piedad.

**Plantilla estándar recomendada para `specs/<slug>.md`:**

```markdown
# SPEC: [Título corto de la funcionalidad]
Estado: APPROVED | DRAFT
Rama / Worktree sugerido: feat/<slug>

## 1. Objetivo
Descripción en 2-3 líneas del comportamiento esperado para el usuario.

## 2. Alcance (Scope) — Archivos permitidos
- Modificar:
  - `app/src/main/java/com/devsusana/hometutorpro/presentation/ui/screens/...`
- Crear:
  - `app/src/test/java/com/devsusana/hometutorpro/...`
- PROHIBIDO modificar:
  - `data/local/entities/*` (sin cambios de esquema)
  - `gradle/*`, `AGENTS.md`

## 3. Reglas de negocio aplicables
- Cumplir `BUSINESS_RULES.md` sección X.
- [Regla específica 1: ej. el badge solo se muestra si `pendingBalance > 0.0`].

## 4. Criterios de aceptación (Verificables)
- [ ] **AC1 (Dado/Cuando/Entonces):** Dado un alumno con `pendingBalance > 0.0`, cuando se renderiza la tarjeta en `StudentListContent`, entonces se muestra el badge de aviso con `contentDescription` accesible.
- [ ] **AC2:** Dado un alumno con `pendingBalance == 0.0`, entonces el badge NO está en el árbol de composición.
- [ ] **AC3:** Existen tests unitarios / de UI Compose cubriendo AC1 y AC2.

## 5. Comandos de verificación obligatorios
- `./gradlew compileDebugKotlin`
- `./gradlew testDebugUnitTest --tests "*StudentList*"`
```

Fíjate en la diferencia: con este formato, si un agente toca `build.gradle.kts` o un archivo de `data/` sin permiso, **falla automáticamente la verificación de alcance**, aunque compile.

---

## 4. Bloque 3: Roles Separados, Control de Alcance y Hooks

### 4.1. Por qué el Constructor y el Validador NO pueden ser el mismo agente
En [`agents_dev/core/engine.py`](../agents_dev/core/engine.py), `AgenticEngine` hace todo en un solo hilo: lee la spec, escribe código, ejecuta `./gradlew` y `audit_*`, y al final dice *"todo verificado"*.

**Por qué esto falla en tareas medianas o complejas:**
1. **Sesgo de confirmación:** El modelo ya "cree" que su solución es correcta porque él mismo la razonó hace 4 mensajes.
2. **Atajos silenciosos:** Si un test falla o un requisito de la spec es incómodo, un mismo agente a veces suaviza el test o ignora un criterio de aceptación para poder terminar el bucle.
3. **Contaminación de contexto:** Para cuando termina de implementar, la ventana de contexto está llena de intentos fallidos y diffs parciales.

### 4.2. El patrón **Constructor + Validador Ciego (*Blind Reviewer*)**
Así se separan correctamente ambos roles:

| Aspecto | Agente Constructor (*Builder*) | Agente Validador Ciego (*Reviewer*) |
| :--- | :--- | :--- |
| **Objetivo** | Implementar la `spec` y hacer pasar los tests | Demostrar si la implementación cumple o incumple la `spec` |
| **Sesión / Contexto** | Nueva sesión con la `spec` + código relevante | **Sesión completamente nueva** (cero historial del Constructor) |
| **Entradas** | `spec` + codebase + `AGENTS.md` | `spec` + `git diff` + salida real de comandos/tests |
| **Permisos** | Leer, editar archivos dentro del `Scope`, ejecutar Gradle | **Solo lectura** (o sin permiso de editar código de producción) |
| **Salida esperada** | Código + tests + resumen de comandos ejecutados | Veredicto estricto: `PASS` o `FAIL` con lista exacta de incumplimientos |

Si el Validador devuelve `FAIL`, se lanza una nueva iteración del Constructor pasándole **solo** la lista de fallos del Validador (y después vuelve a revisar el Validador en limpio).

### 4.3. Control de Alcance (*Scope Enforcement*)
El control de alcance se aplica en dos capas:
1. **Preventiva (en herramientas):** Si la `spec` declara los archivos permitidos, la herramienta de escritura/parcheo rechaza cualquier ruta fuera de esa lista (salvo confirmación humana explícita).
2. **Detectiva (con Git):** Tras terminar el Constructor, se ejecuta `git diff --name-only` y se compara con el `Scope` de la `spec`. Cualquier archivo extra dispara un fallo automático antes incluso de llamar al Validador.

### 4.4. Estructura estándar `.agents/`
Para no depender de un único script Python ni de un único IDE, la industria está convergiendo en una carpeta `.agents/` en la raíz del proyecto:
```text
.agents/
├── roles/
│   ├── builder.md          # Instrucciones del Agente Constructor
│   └── reviewer.md         # Instrucciones del Agente Validador Ciego
├── skills/
│   ├── discovery/SKILL.md  # Cómo entrevistar antes de especificar
│   └── spec-writer/SKILL.md
└── workflows/
    └── feature-loop.md     # Secuencia spec -> build -> hooks -> review -> PR
```
De este modo, tanto tu CLI `agents_dev`, como Antigravity, Claude Code o cualquier otra herramienta leen exactamente las mismas definiciones.

### 4.5. Git Hooks reales (`pre-commit` y `pre-push`)
Hoy en `.git/hooks/` solo tienes archivos `.sample` (inactivos). Un hook real convierte tus reglas en **barreras físicas** que ni tú ni ningún agente podéis saltaros por descuido:

* **`pre-commit` (debe ser rápido, < 10-20 segundos):**
  * Formateo / linter (`ktlintCheck` o `detekt` sobre archivos modificados).
  * Tus chequeos estáticos rápidos (`check_code_metrics.py`, `hilt_checker.py`, búsqueda de secretos hardcodeados).
  * Bloqueo si hay archivos prohibidos en staging (ej. `local.properties`, claves `.json` de Firebase).
* **`pre-push` (puede tardar algo más):**
  * `./gradlew testDebugUnitTest`
  * Verificación de compilación limpia.

> **Nota importante:** Los archivos dentro de `.git/hooks/` no se versionan en Git por defecto. Por eso en equipos se guardan en una carpeta versionada (por ejemplo `.githooks/` o usando una herramienta como `lefthook`) y se enlazan con `git config core.hooksPath .githooks`.

---

## 5. Bloque 4: Paralelismo (*Worktrees*), *Loop Engineering*, MCPs y *Evals*

### 5.1. Git Worktrees en la práctica (más allá de la teoría)
Ya sabes qué es un `git worktree` (tener varias ramas abiertas simultáneamente en carpetas distintas compartiendo el mismo `.git`). **Cómo se usa bien con agentes:**

1. **Regla de oro antes de paralelizar:** Dos worktrees en paralelo **nunca deben tener specs que toquen los mismos archivos** (si ambos modifican `NavRoutes.kt` o `StudentListScreen.kt`, tendrás conflictos al hacer merge). El diseño de *specs* con *scopes* disjuntos es lo que hace viable el paralelismo.
2. **Estructura de carpetas recomendada:**
   ```text
   ~/AndroidStudioProjects/
   ├── HomeTutorPro/              # main (limpio, tu repo principal)
   ├── HomeTutorPro-wt-badge/     # worktree para feat/payment-badge
   └── HomeTutorPro-wt-export/    # worktree para feat/csv-export
   ```
   *(Crear los worktrees como carpetas hermanas fuera de `HomeTutorPro/` evita que Android Studio o Gradle indexen un proyecto dentro de otro).*
3. **Particularidades de Android/Gradle en worktrees:**
   * `local.properties` está ignorado por Git (`.gitignore`), así que al crear un worktree nuevo puede que falte `sdk.dir`. Conviene tener un pequeño script de setup del worktree que copie `local.properties` desde el repo principal.
   * La caché global de Gradle (`~/.gradle/caches`) se comparte, pero cada worktree tendrá su propia carpeta `build/` (más espacio en disco, cero colisiones de compilación).
4. **Limpieza:** Cuando la PR se mergea en GitHub, se borra el worktree (`git worktree remove ../HomeTutorPro-wt-badge`) y la rama.

### 5.2. *Loop Engineering* y Pull Requests automatizadas
*Loop Engineering* es diseñar el bucle completo para que un agente trabaje de forma autónoma pero acotada:
1. Lee una `spec` aprobada.
2. Crea su `worktree` y rama.
3. Itera: `editar -> compilar -> pasar tests -> corregir` (con un máximo de reintentos para no entrar en bucles infinitos).
4. Pasa el `pre-commit` y al **Validador Ciego**.
5. Si todo es `PASS`, hace push y ejecuta `gh pr create` generando una Pull Request en GitHub con:
   * Enlace a la `spec`.
   * Resumen de cambios.
   * Evidencia pegada de los comandos ejecutados (`testDebugUnitTest`: X passed, 0 failed).
6. **Tú revisas la PR en GitHub** (o en tu IDE) y decides el `merge`.

### 5.3. Model Context Protocol (MCP): cuándo y por qué usarlo
Hoy tus herramientas en `agents_dev/core/skill_tools.py` son funciones Python acopladas a tu propio motor `AgenticEngine`. Si mañana usas Claude Code, Antigravity o Cursor, esas herramientas no existen ahí.

**MCP** es un estándar abierto (cliente-servidor) para exponer capacidades a cualquier cliente de IA:
* **Resources:** Datos de solo lectura que el cliente puede adjuntar (ej. el esquema actual de Room, `BUSINESS_RULES.md`, lista de specs abiertas).
* **Tools:** Acciones ejecutables con esquema JSON tipado (ej. `audit_architecture(file_path)`, `run_scoped_tests(spec_id)`).
* **Prompts:** Plantillas reutilizables (ej. `review-against-spec`).

**Cuándo merece la pena crear un MCP:** Cuando tienes lógica específica de tu proyecto o infraestructura (ej. consultar tu esquema de Firestore, validar reglas de HomeTutorPro, o consultar métricas de tu portfolio) y quieres usarla desde **múltiples clientes** sin duplicar código.

### 5.4. *Evals* (Evaluaciones reproducibles del arnés)
Un **unit test** comprueba que `CalculatePendingBalanceUseCase` suma bien dos números.  
Una **eval** comprueba si **tu arnés** (prompt + `AGENTS.md` + herramientas + modelo) es capaz de resolver tareas de ingeniería de forma fiable.

**Cómo construir un mini-suite de Evals en 4 pasos:**
1. **Elige 5 tareas reales pasadas** (de bugs o features pequeñas que ya resolviste en HomeTutorPro) y guarda su `commit_base` + su `spec`.
2. **Define aserciones automáticas por tarea:**
   * ¿Compila (`./gradlew compileDebugKotlin`)?
   * ¿Pasan los tests nuevos y los de regresión?
   * ¿Respetó el `Scope` de la `spec` (0 archivos prohibidos tocados)?
   * ¿Pasó `audit_architecture` y `audit_security`?
3. **Ejecuta las 5 tareas en worktrees temporales** cuando hagas un cambio importante en `AGENTS.md`, en el prompt del Constructor, o cuando pruebes un modelo nuevo.
4. **Mide 3 métricas:**
   * **Pass@1 / Tasa de éxito:** cuántas de las 5 pasaron a la primera sin intervención humana.
   * **Iteraciones medias:** cuántas vueltas necesitó el bucle.
   * **Coste / Tokens totales:** cuánto costó resolverlas.

Sin *evals*, cambiar prompts es ir a ciegas ("me da la sensación de que funciona mejor"). Con *evals*, tienes datos objetivos.

---

## 6. Bloque 5: IA dentro del Producto (*AI Product*)

Aquí el foco cambia por completo: **la IA ya no es tu asistente de programación, sino una funcionalidad para el usuario final** de HomeTutorPro o de una web.

### 6.1. RAG (*Retrieval-Augmented Generation*) bien diseñado
Un RAG no es "pegar un texto en el prompt". En producción tiene dos fases claras:

1. **Pipeline de Ingesta (Offline / Background):**
   * **Extracción y limpieza** del texto (notas de clases, resúmenes de alumnos, PDFs).
   * ***Chunking* inteligente:** dividir en fragmentos coherentes (ej. 300–500 tokens con un 10-15% de solapamiento para no cortar frases/ideas a la mitad) conservando **metadatos** (`studentId`, `date`, `subject`).
   * **Embeddings:** convertir cada fragmento en un vector numérico y guardarlo en una base vectorial (o SQLite con extensión vectorial en local).
2. **Pipeline de Consulta (Runtime):**
   * **Pre-filtrado por metadatos:** si preguntas sobre la alumna Ana, filtras primero por `studentId == "ana"` antes de buscar por similitud vectorial (evita mezclar datos de otros alumnos — crítico en privacidad).
   * **Búsqueda híbrida:** combinar similitud semántica (vectores) + coincidencia exacta de palabras clave (BM25 / FTS), porque los nombres propios o códigos se pierden solo con vectores.
   * **Generación con abstención:** instruir al modelo para que responda **solo** con los fragmentos recuperados y diga *"No tengo información suficiente en tus notas"* si no aparece, citando de qué fecha/nota sacó el dato.

### 6.2. *Function Calling* / *Tool Use* orientado al usuario
Cuando permites que un usuario diga *"Apunta que hoy di 1 hora de Matemáticas con Luis y me pagó 20€"*:
1. El modelo NO escribe en Room ni en Firestore directamente.
2. El modelo devuelve una llamada estructurada a una herramienta tipada (ej. `register_class_session(studentName="Luis", subject="Matemáticas", durationMinutes=60, paidAmount=20.0)`).
3. Tu capa de aplicación resuelve `studentName -> studentId` (manejando ambigüedades si hay dos "Luis"), valida contra `BUSINESS_RULES.md` y **muestra una tarjeta de confirmación al usuario** (*human-in-the-loop*) antes de ejecutar el `UseCase` que persiste en base de datos.

### 6.3. Modelos locales (*Open-Weights*) vs. Cloud
* **Cloud (Gemini, Claude, etc.):** Máxima calidad de razonamiento y ventanas grandes, pero requiere conexión, tiene coste por token y envía datos fuera del dispositivo.
* **Local / On-Device (Ollama en desktop, o MediaPipe / llama.cpp / Gemini Nano en Android):**
  * **Ventajas:** Cero coste por token, privacidad total (datos sensibles de alumnos nunca salen del teléfono), funciona offline.
  * **Limitaciones reales que debes dominar:**
    * **Cuantización (Q4_K_M, Q8_0, etc.):** reducir precisión de pesos para que quepa en RAM/VRAM sacrificando algo de capacidad de razonamiento.
    * **Ventana de contexto más pequeña** y menor fiabilidad en *function calling* complejo con modelos pequeños (1B–7B parámetros).
    * **Regla de arquitectura:** Define una interfaz en `domain/` (ej. `AiAssistantRepository` o `TextGenerator`), exactamente igual que haces con cualquier fuente de datos, para poder alternar entre motor local y motor cloud sin tocar la UI.

---

## 7. Bloque 6: Orquestación Completa y Portabilidad

El objetivo final de dominar estos 6 bloques es que **tú diriges el sistema desde el diseño y la revisión**, no picando manualmente cada boilerplate ni peleándote con prompts improvisados:

1. **Tú aportas el criterio técnico y de producto:** haces *discovery*, apruebas la `spec` (definiendo alcance y criterios de aceptación) y revisas la PR final.
2. **El arnés hace el trabajo pesado de forma fiable:**
   * Aísla cada tarea en su `git worktree`.
   * Ejecuta al Constructor con el contexto justo.
   * Impone barreras deterministas (`pre-commit`, linter, métricas, Gradle tests).
   * Pasa la auditoría del **Validador Ciego** contra la `spec`.
   * Entrega una Pull Request lista para revisar con evidencias reales.
3. **El conocimiento vive en el repositorio, no en un chat:** Si mañana cambias de modelo o de IDE, tu carpeta `.agents/`, tus `specs/`, tus `hooks`, tus `MCPs` y tus `evals` siguen funcionando exactamente igual.
