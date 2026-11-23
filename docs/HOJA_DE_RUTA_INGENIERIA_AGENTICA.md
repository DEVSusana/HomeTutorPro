# Hoja de Ruta: Ingeniería de Arneses y Sistemas Agénticos

> Guía práctica de referencia personal basada en el estado real de **HomeTutorPro** (`agents_dev/`) y **devsusana-portfolio** (`agents_web/`).  
> Objetivo: pasar de tener un bucle agéntico propio funcional a dominar un **arnés de ingeniería completo, paralelo, verificable y desacoplado del modelo**.

---

## Diagnóstico de partida: qué tienes hoy vs. qué te falta

Tu intuición al revisar [`agents_dev/core/engine.py`](../agents_dev/core/engine.py) es exacta: **hoy tienes un único agente (`AgenticEngine`) que planifica/construye y él mismo se autovalida al final** dentro del mismo historial de conversación.

Además, ya tienes piezas muy avanzadas que mucha gente tarda meses en montar:
* Reglas de arquitectura y negocio claras ([`AGENTS.md`](../AGENTS.md) y [`BUSINESS_RULES.md`](../BUSINESS_RULES.md)).
* Un motor agéntico multi-proveedor en Python (`agents_dev/providers/` para Gemini y Claude) con parches quirúrgicos (`patch_file`) y auto-reparación con Gradle.
* Documentación previa sobre IA local y RAG ([`ON_DEVICE_AI_AND_RAG_ARCHITECTURE.md`](./ON_DEVICE_AI_AND_RAG_ARCHITECTURE.md)).

Lo que sigue es el temario completo de lo que te queda por dominar y practicar **directamente sobre tu proyecto**.

---

## Bloque 1: Investigación, *Discovery* y Fundamentos de LLMs

### Qué es y por qué importa
Antes de escribir código o lanzar un agente, hace falta acotar el problema sin desperdiciar tokens ni arrastrar ambigüedades al código.

### Qué tienes hoy
* Tienes una fase `plan` en `agents_dev/planner/` que hace preguntas al usuario antes de ejecutar.
* Tienes `context_engine.py` y manejo de rate-limits/reintentos en tus proveedores.

### Qué te falta aprender y practicar
1. **Skill de *Discovery* portable (independiente de tu CLI):**
   * En lugar de que el entrevistador viva solo dentro de Python en `agents_dev/planner/`, aprende a definir una *skill* en Markdown (`.agents/skills/discovery/SKILL.md`) que cualquier agente (Antigravity, Claude Code, Codex, OpenCode) pueda leer para transformar una idea difusa en un MVP acotado.
   * La skill debe forzar siempre 4 salidas: **Objetivo**, **No-objetivos (qué queda fuera)**, **Casos límite** y **Criterios de aceptación medibles**.
2. **Ingeniería de contexto y costes con criterio técnico:**
   * Aprende a medir cuántos tokens consume tu `system_prompt` actual en `engine.py` al inyectar `AGENTS.md` + `BUSINESS_RULES.md` completos en cada iteración del bucle ReAct (`max_iterations = 30`).
   * **Práctica:** Implementar *context budgeting* (cargar solo las reglas relevantes de la capa que se va a tocar en lugar de volcar toda la documentación en cada turno) y distinguir entre tokens de entrada cacheados vs. no cacheados.
3. **Flujos de *Research* previo:**
   * Combinar herramientas de síntesis documental (como NotebookLM o búsquedas estructuradas) antes de redactar especificaciones técnicas.

---

## Bloque 2: Contexto, Contratos y Especificaciones (*Specs*)

### Qué es y por qué importa
Una *spec* no es un resumen de chat; es un **contrato técnico verificable** que impide que el agente divague, invente requisitos o modifique ficheros fuera de su responsabilidad.

### Qué tienes hoy
* La carpeta `specs/` existe, pero por ejemplo [`specs/a_adir_un_badge_de_aviso_en_lo.md`](../specs/a_adir_un_badge_de_aviso_en_lo.md) contiene texto conversacional de preguntas pendientes en lugar de un contrato cerrado.

### Qué te falta aprender y practicar
1. **Formato estándar de Spec como contrato:**
   Toda spec aprobada en `specs/` debería tener exactamente estas secciones:
   * **Contexto y objetivo** (1-3 líneas).
   * **Archivos permitidos (*Scope*)**: lista explícita de ficheros o paquetes que se pueden crear/editar.
   * **Requisitos funcionales y reglas de negocio**: enlazando a `BUSINESS_RULES.md`.
   * **Fuera de alcance (*Out of Scope*)**: lo que está prohibido tocar.
   * **Criterios de aceptación (Given / When / Then)** y **comandos de verificación** (`./gradlew test...`).
2. **Contexto mínimo suficiente (*Just-in-Time Context*):**
   * En lugar de un `AGENTS.md` gigante para todo, aprender a modularizar reglas por carpeta o capa (ej. reglas de `domain/` vs reglas de `presentation/`) para que el agente no pierda atención (*lost in the middle*).

---

## Bloque 3: Roles Separados, Control de Alcance y Hooks

### Qué es y por qué importa
**Este es el cambio mental más grande respecto a tu `engine.py` actual.**  
Cuando el mismo agente que escribe el código es el que lo revisa al final del mismo hilo, arrastra su propio sesgo de confirmación (*"yo ya creo que lo hice bien porque lo acabo de escribir"*).

### Qué tienes hoy
* En `agents_dev/core/engine.py`, `AgenticEngine.run()` ejecuta en el mismo bucle: lee spec -> edita código -> llama a `audit_security`, `audit_architecture`, `./gradlew compileDebugKotlin`, `./gradlew test` -> redacta su propio informe final.
* En `.git/hooks/` solo tienes los ficheros `.sample` por defecto de Git (no hay ningún hook activo).

### Qué te falta aprender y practicar
1. **Separación estricta Constructor vs. Validador Ciego:**
   * **Agente A (Constructor):** Recibe la `spec` y escribe el código + tests. No aprueba su propio trabajo.
   * **Agente B (Validador / Revisor):** Se lanza en una **sesión nueva y limpia** (sin el historial de razonamiento del Agente A). Solo recibe:
     1. La `spec` original.
     2. El `git diff` resultante.
     3. La salida real de compilación/tests.
   * Su único trabajo es buscar incumplimientos de la `spec`, regresiones o cambios fuera de alcance y devolver `PASS` o `FAIL` con lista de fallos.
2. **Restricción de *Scope* (límites duros):**
   * Bloquear por código (en `system_tools.py` o en el validador) que un agente pueda modificar archivos fuera de la lista declarada en la `spec` (por ejemplo, que una tarea de UI no toque `data/local/` ni `gradle/`).
3. **Estandarización en `.agents/`:**
   * Crear una carpeta `.agents/` en la raíz del proyecto con definiciones de roles, comandos y checklists portables entre herramientas.
4. **Git Hooks reales (`pre-commit` / `pre-push`):**
   * Configurar un `.git/hooks/pre-commit` real (o gestionado con `lefthook` / script en repo) que ejecute automáticamente antes de aceptar cualquier commit (humano o de un agente):
     * Linter / formato (`ktlint` o `detekt`).
     * Tus chequeos rápidos (`check_code_metrics.py`, `hilt_checker.py`).
     * Tests unitarios afectados.

---

## Bloque 4: Trabajo en Paralelo, *Loop Engineering*, MCPs y *Evals*

### Qué es y por qué importa
Escalar de "un agente ayudándote en una rama" a "varios flujos autónomos avanzando en paralelo con garantías matemáticas de calidad".

### Qué tienes hoy
* Acabas de estudiar la teoría de **git worktrees**.
* En `agents_dev/skills/` tienes herramientas en Python que solo entiende tu CLI.

### Qué te falta aprender y practicar
1. **Git Worktrees en paralelo con GitHub PRs:**
   * Crear 2 o 3 worktrees simultáneos (`git worktree add ../htp-feat-a -b feat/a`, `../htp-feat-b -b feat/b`) para tareas independientes definidas en `specs/`.
   * Ejecutar un agente por worktree sin que se pisen `build/` ni el índice de Git.
   * Automatizar con `gh` (GitHub CLI) la apertura de una **Pull Request** por worktree que incluya en la descripción el resumen de cambios y la evidencia de `./gradlew test` en verde.
2. **Model Context Protocol (MCP) propio:**
   * Aprender a envolver tus utilidades (por ejemplo, consulta de esquema Room, `audit_architecture`, o estado de `specs/`) en un **servidor MCP** estándar en lugar de tenerlas acopladas a `skill_tools.py`. Así cualquier cliente compatible (Antigravity, Claude Code, etc.) puede usarlas sin reescribir nada.
3. **Evals (Evaluaciones del arnés):**
   * No confundir *unit tests de Android* con *evals del agente*.
   * Una **eval** consiste en tener 5–10 tareas de referencia congeladas (con su estado inicial de repo y criterios automáticos de éxito) y ejecutar tu arnés contra ellas cada vez que cambias un prompt, una regla en `AGENTS.md` o de modelo (Gemini vs Claude), midiendo tasa de éxito, iteraciones usadas y coste.

---

## Bloque 5: IA dentro del Producto (*AI Product*)

### Qué es y por qué importa
Pasar de usar la IA **para programar** a construir **funcionalidades con IA para el usuario final** dentro de la app.

### Qué tienes hoy
* Tienes documentación de diseño en [`ON_DEVICE_AI_AND_RAG_ARCHITECTURE.md`](./ON_DEVICE_AI_AND_RAG_ARCHITECTURE.md) y [`GUIA_MAESTRA_IA_LOCAL_ON_DEVICE.md`](./GUIA_MAESTRA_IA_LOCAL_ON_DEVICE.md), pero tu implementación práctica de IA actual está centrada en `agents_dev/` (desarrollo).

### Qué te falta aprender y practicar
1. **RAG (*Retrieval-Augmented Generation*) end-to-end:**
   * *Chunking* de documentos/notas de alumnos o clases, generación de **embeddings**, almacenamiento vectorial (local o remoto) y recuperación híbrida con citación de fuentes.
2. **Function Calling / Tool Use en producto:**
   * Diseñar herramientas seguras para que el usuario de la app pueda hacer peticiones en lenguaje natural (ej. *"programa una clase con Ana el martes a las 17h"*) que el modelo traduzca a llamadas tipadas y validadas contra tus `UseCases` de `domain/`, con confirmación humana antes de persistir.
3. **Modelos locales / *Open-Weights* e inferencia propia:**
   * Probar modelos abiertos ejecutándose en local (por ejemplo con Ollama / llama.cpp / MediaPipe / Gemini Nano según el caso) y aprender a evaluar latencia, tamaño, cuantización y privacidad frente a APIs en la nube.

---

## Bloque 6: Orquestación Completa y Arnés Agnóstico

### Qué es y por qué importa
Cerrar el ciclo completo desde que nace una necesidad hasta que se integra en `main`, sin quedar atada a una sola herramienta o proveedor.

### Qué tienes hoy
* Un CLI funcional (`agents_dev/cli.py`) con comandos `plan` y `run`.

### Qué te falta aprender y practicar
1. **Orquestación de ciclo completo (`spec -> worktree -> build -> blind review -> PR -> merge`):**
   * Un orquestador ligero que lea una cola de specs aprobadas, cree el worktree, lance al Agente Constructor, ejecute los hooks/tests, lance al Agente Validador Ciego y, solo si pasa, abra la PR esperando tu revisión humana final (*human-in-the-loop*).
2. **Desacoplar el arnés del proveedor:**
   * Asegurar que toda la inteligencia del proyecto viva en el repositorio (`.agents/`, `AGENTS.md`, `specs/`, hooks, MCPs, evals) y no dentro de un prompt hardcodeado en Python, para poder cambiar de modelo o de IDE agéntico en minutos.

---

## Plan de acción sugerido (estado actual en HomeTutorPro)

1. [x] **Paso 1 — Barrera determinista `pre-commit` y plantilla de contrato:** [`.githooks/pre-commit`](../.githooks/pre-commit) activado con `core.hooksPath` + [`specs/_TEMPLATE.md`](../specs/_TEMPLATE.md).
2. [x] **Paso 2 — Entrevista conectada a contrato limpio:** [`spec_interviewer.py`](../agents_dev/planner/spec_interviewer.py) genera specs limpias siguiendo `_TEMPLATE.md`.
3. [x] **Paso 3 — Constructor + Validador Ciego + `.agents/`:** [`blind_reviewer.py`](../agents_dev/core/blind_reviewer.py), [`engine.py`](../agents_dev/core/engine.py) y [`.agents/`](../.agents/roles/reviewer.md) con verificación determinista de `Scope` y comandos.
4. [x] **Paso 4 — Paralelismo, Evals y MCP:** [`scripts/worktree.py`](../scripts/worktree.py), [`agents_dev/evals/eval_harness.py`](../agents_dev/evals/eval_harness.py) (5/5) y [`agents_dev/mcp_server.py`](../agents_dev/mcp_server.py).
5. [x] **Paso 5 — IA dentro del Producto (Bloque 5):** Tu diseño en [`ON_DEVICE_AI_AND_RAG_ARCHITECTURE.md`](./ON_DEVICE_AI_AND_RAG_ARCHITECTURE.md) + SUE intacto + laboratorio aislado de RAG y embeddings en [`agents_dev/rag_lab/rag_demo.py`](../agents_dev/rag_lab/rag_demo.py) y [`agents_dev/rag_lab/README.md`](../agents_dev/rag_lab/README.md).
6. [x] **Paso 6 — Orquestación completa (Bloque 6):** [`agents_dev/core/orchestrator.py`](../agents_dev/core/orchestrator.py) (`python3 -m agents_dev orchestrate --spec ...`) encadenando `spec -> worktree -> Builder + BlindReviewer -> PR opcional`.
