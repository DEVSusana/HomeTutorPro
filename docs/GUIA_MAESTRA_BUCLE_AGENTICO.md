# 📘 Guía Maestra del Bucle Agéntico: De Novato a Experto

**Guía completa de estudio, arquitectura y referencia para entender, instalar, ejecutar y explicar al 100% el motor de desarrollo autónomo de HomeTutorPro (`agents_dev/`).**

---

## 🧭 Índice de Contenidos
1. [¿Qué es este Bucle Agéntico y por qué existe? (La Gran Idea)](#1-qué-es-este-bucle-agéntico-y-por-qué-existe)
2. [Anatomía de la Carpeta `agents_dev/`: Qué hace cada archivo](#2-anatomía-de-la-carpeta-agents_dev)
3. [La Caja de Herramientas: `system_tools` vs `skill_tools` vs `interactive`](#3-la-caja-de-herramientas-manos-cerebro-y-control)
4. [Instalación y Configuración Paso a Paso (Desde Cero)](#4-instalación-y-configuración-paso-a-paso)
5. [Aclaración sobre Modelos y Claves de API](#5-aclaración-sobre-modelos-y-claves-de-api)
6. [La Cadena de Arranque Interna de Python](#6-la-cadena-de-arranque-interna-de-python)
7. [Los 2 Modos de Uso: En GitHub (CI) y en Local (Terminal)](#7-los-2-modos-de-uso)
8. [Referencia Completa de Comandos (Cheat Sheet)](#8-referencia-completa-de-comandos-cheat-sheet)
9. [Cómo Funciona por Dentro: Diagrama de Secuencia y Ciclo ReAct](#9-cómo-funciona-por-dentro-el-ciclo-react)
10. [Guion de Estudio: Cómo Explicarlo en una Entrevista o a tu Equipo](#10-guion-de-estudio-cómo-explicarlo-con-seguridad)
11. [Anexo Técnico: Estrategia Shift-Left con Git Hooks (pre-push) y Gestión de Claves](#11-anexo-técnico-estrategia-shift-left-con-git-hooks-pre-push-y-gestión-de-claves)

---

## 1. ¿Qué es este Bucle Agéntico y por qué existe?

### La Diferencia entre un Chatbot Normal y un Agente Autónomo
* **Un Chatbot normal (ChatGPT en web, Copilot básico):** Solo puede devolverte texto en una ventana de chat. Tú tienes que copiar el código, pegarlo en Android Studio, compilar a mano, ver el error de compilación, volver a copiar el error, pegárselo al chat y repetir el proceso diez veces.
* **Un Agente Autónomo en Bucle (Nuestro `agents_dev`):** Es un programa en Python que conecta un Modelo de Lenguaje (Google Gemini o Anthropic Claude) directamente con tu proyecto. El agente tiene **ojos** (puede leer tus archivos), **manos** (puede modificar líneas de código con precisión quirúrgica) y **oídos técnicos** (puede ejecutar `./gradlew compileDebugKotlin` y leer los errores del compilador para corregirse a sí mismo).

### ¿Por qué está en una carpeta propia (`agents_dev/`)?
Porque **no ensucia tu app Android**. Tu aplicación (`app/`) está escrita en Kotlin con Jetpack Compose y Gradle. El agente es tu "asistente de taller" escrito en Python que vive fuera de la app, no añade peso al APK final, no interfiere con las dependencias de Android y puede replicarse en cualquier otro proyecto de software (iOS, Backend, Web).

---

## 2. Anatomía de la Carpeta `agents_dev/`

Imagina `agents_dev/` como un equipo de trabajo donde cada módulo tiene una responsabilidad única:

```text
agents_dev/
│
├── .venv_agente/               # 📦 LA BURBUJA: Entorno virtual aislado de Python 3.11
├── requirements.txt            # 📜 LA LISTA DE LIBRERÍAS: google-genai, anthropic, PyGithub...
├── cli.py                      # 🎛️ EL PARSER DE COMANDOS: Procesa flags (--provider, --model, --spec)
├── __main__.py                 # 🚀 PUNTO DE ENTRADA: Permite ejecutar `python -m agents_dev`
│
├── core/                       # 🦾 EL MOTOR FÍSICO (Manos y ojos del agente)
│   ├── engine.py               # El director de orquesta que hace girar el bucle ReAct
│   ├── system_tools.py         # Herramientas del sistema: read_file, patch_file, run_command, list_files
│   └── interactive.py          # Comunicación con el usuario: ask_human y confirmación de git diff (y/n)
│
├── skills/                     # 🧠 EL CEREBRO CONSULTOR (Estándares de calidad)
│   ├── PROJECT_STANDARDS.md    # La constitución: matriz de capas, OWASP Mobile y reglas de HomeTutorPro
│   ├── context_engine.py       # El clasificador: identifica si un archivo es DAO, Caso de Uso o ViewModel
│   ├── unified_reviewer.py     # El auditor consolidado: analiza Arquitectura, Seguridad y KDoc en 1 llamada
│   ├── architecture_auditor.py # Auditoría especializada de Clean Architecture
│   ├── security_scanner.py     # Escáner de seguridad OWASP Mobile (criptografía, Keystore, datos sensibles)
│   ├── kdoc_specialist.py      # Verificador de documentación técnica (@param, @return)
│   ├── refactor_assistant.py   # Asistente de limpieza de código (SOLID, DRY)
│   ├── test_generator.py       # Diseñador de pruebas unitarias
│   ├── test_scaffolder.py      # Generador físico de archivos *Test.kt en disco
│   ├── hilt_checker.py         # Verificador instantáneo de inyección de dependencias Hilt
│   └── config.py               # Conexión con Gemini y manejo de reintentos exponenciales ante cuotas
│
├── ci/
│   └── pr_review.py            # 🛡️ EL GUARDIÁN DE GITHUB: Ejecuta la revisión automática en cada PR
│
├── planner/
│   └── spec_interviewer.py     # 🎙️ EL ENTREVISTADOR: Te entrevista para pulir requisitos antes de codificar
│
├── providers/                  # 🔌 LOS ENCHUFES DE IA: GeminiProvider y AnthropicProvider
├── specs/                      # 📑 LAS ESPECIFICACIONES: Documentos .md de requisitos aprobados
└── tests/                      # 🧪 LOS TESTS DEL MOTOR: Verifican que todas las herramientas funcionen (100% OK)
```

---

## 3. La Caja de Herramientas: Manos, Cerebro y Control

El agente no tiene ojos físicos para mirar tu pantalla ni manos para teclear en Android Studio. Interactúa con el proyecto a través de **herramientas (*tools*)**, divididas estrictamente en tres categorías:

```mermaid
flowchart TD
    User(["Tú en la Terminal<br><code>python -m agents_dev run ...</code>"]) --> Engine["Motor Agéntico (engine.py)"]

    subgraph CajaHerramientas ["Caja de Herramientas del Agente"]
        direction TB

        subgraph SystemTools ["SYSTEM_TOOLS (Manos y Ojos)"]
            T_list["list_files<br><i>Busca y localiza archivos en el proyecto</i>"]
            T_read["read_file<br><i>Abre y lee el código fuente</i>"]
            T_patch["patch_file<br><i>Cirugía de precisión: cambia solo líneas necesarias</i>"]
            T_write["write_new_file<br><i>Crea archivos nuevos sin sobreescribir existentes</i>"]
            T_cmd["run_command<br><i>Ejecuta Gradle, compilación y tests</i>"]
        end

        subgraph SkillTools ["SKILL_TOOLS (Cerebro Experto Android)"]
            S_arch["audit_architecture<br><i>Audita Clean Architecture y Business Rules</i>"]
            S_sec["audit_security<br><i>Escaneo OWASP Mobile, criptografía y secretos</i>"]
            S_ref["audit_refactoring<br><i>Detecta code smells y complejidad ciclomática</i>"]
            S_test["design_tests / scaffold_test<br><i>Diseña y genera esqueletos de tests unitarios</i>"]
            S_kdoc["audit_kdoc<br><i>Audita completitud y estilo de documentación KDoc</i>"]
            S_hilt["check_hilt_bindings<br><i>Verifica inyección de dependencias en módulos Hilt</i>"]
        end

        subgraph Interactive ["INTERACTIVE_TOOLS (Voz y Control)"]
            I_human["ask_human<br><i>Pausa el bucle y te pregunta ante dudas de negocio</i>"]
        end
    end

    Engine --> CajaHerramientas
    CajaHerramientas --> Project[("Tu Proyecto Android<br>HomeTutorPro")]
```

### Tabla Comparativa de Responsabilidades

| Categoría | Módulo | Función Principal | ¿Qué pasaría si no existiera? |
| :--- | :--- | :--- | :--- |
| **`SYSTEM_TOOLS`** | [`system_tools.py`](file:///Users/susanacordobaserrano/AndroidStudioProjects/HomeTutorPro/agents_dev/core/system_tools.py) | **Manos y Ojos:** Interactúa físicamente con el disco duro y el compilador (leer, parchear, compilar). | El agente sabría qué corregir pero no podría abrir ni tocar ningún archivo. |
| **`SKILL_TOOLS`** | [`skill_tools.py`](file:///Users/susanacordobaserrano/AndroidStudioProjects/HomeTutorPro/agents_dev/core/skill_tools.py) | **Cerebro Consultor:** Evalúa Clean Architecture, OWASP Mobile, documentación KDoc y contratos Hilt. | El agente sabría editar archivos, pero no tendría tus estándares y generaría código frágil o inseguro. |
| **`INTERACTIVE_TOOLS`** | [`interactive.py`](file:///Users/susanacordobaserrano/AndroidStudioProjects/HomeTutorPro/agents_dev/core/interactive.py) | **Voz y Control:** Pregunta al desarrollador y muestra el `git diff` final para aprobación (`y/n`). | El agente trabajaría a ciegas y aplicaría cambios sin tu supervisión directa. |

---

## 4. Instalación y Configuración Paso a Paso

Para configurar el entorno en un equipo nuevo o tras clonar el repositorio, sigue estos **4 pasos**:

### Paso 1: Activar el Entorno Virtual de Python
El proyecto incluye un entorno virtual llamado `.venv_agente` (Python 3.11):
```bash
source agents_dev/.venv_agente/bin/activate
```
*(Aparecerá `(.venv_agente)` al inicio de la línea de tu terminal, indicando que estás dentro del entorno).*

### Paso 2: Instalar Dependencias
```bash
pip install -r agents_dev/requirements.txt
```
Librerías principales que instala:
* `google-genai`: SDK oficial para conectar con modelos Google Gemini.
* `anthropic`: SDK oficial para conectar con modelos Anthropic Claude.
* `PyGithub`: Para interactuar con la API de GitHub en las Pull Requests.

### Paso 3: Configurar tus Claves de API (Variables de Entorno)
En tu terminal o en tu archivo de configuración de shell (`~/.zshrc`):
```bash
# Para Google Gemini (Recomendado y gratuito en AI Studio):
export GEMINI_API_KEY="AIzaSyTuClaveDeGoogleStudio..."

# Si deseas usar Anthropic Claude:
export ANTHROPIC_API_KEY="sk-ant-api03-tuClaveDeAnthropic..."

# Opcional: Configurar modelo por defecto
export GEMINI_MODEL="gemini-3.1-flash-lite"
```

### Paso 4: Comprobar la Suite de Pruebas del Motor
Verifica que todos los componentes y herramientas están listos ejecutando:
```bash
python3 -m unittest discover agents_dev/tests
```
**Resultado esperado:**
```text
Ran 15 tests in 0.065s
OK
```

---

## 5. Aclaración sobre Modelos y Claves de API

### Tu Clave de Gemini es Universal
La API Key de Google AI Studio (`AIzaSy...`) configurada en tu entorno **es válida para todos los modelos de Gemini**. No necesitas generar claves distintas para cambiar de versión:
* **`gemini-3.1-flash-lite` (Por defecto):** El modelo oficial actual recomendado por Google para migración, optimizado para alta eficiencia, baja latencia y function calling en bucles de desarrollo.
* **`gemini-2.5-flash`:** Modelo rápido para tareas estándar y CI.
* **`gemini-2.5-pro`:** Recomendado cuando se requiere razonamiento profundo para diseñar arquitecturas complejas o resolver dependencias enrevesadas.
* **`claude-3-5-sonnet-20241022` / `claude-3-7-sonnet`:** El referente de Anthropic para programación agéntica.

---

## 6. La Cadena de Arranque Interna de Python

¿Por qué al escribir `python -m agents_dev run ...` el sistema sabe qué archivo debe arrancar?

```mermaid
flowchart LR
    Cmd["1. Terminal:<br><code>python -m agents_dev run ...</code>"] 
    --> Main["2. agents_dev/__main__.py<br><i>(Punto de entrada estándar)</i>"]
    --> CLI["3. agents_dev/cli.py<br><i>(Parser de argumentos)</i>"]
    --> Decision{"¿Qué subcomando?"}
    Decision -- plan --> Interviewer["planner/spec_interviewer.py<br><i>Entrevista interactiva de requisitos</i>"]
    Decision -- run --> Engine["core/engine.py<br><code>AgenticEngine.run()</code>"]
```

1. **`python -m agents_dev`:** El flag `-m` de Python busca el archivo [`__main__.py`](file:///Users/susanacordobaserrano/AndroidStudioProjects/HomeTutorPro/agents_dev/__main__.py) dentro del paquete `agents_dev/`.
2. **`__main__.py`:** Invoca inmediatamente la función `main()` de [`cli.py`](file:///Users/susanacordobaserrano/AndroidStudioProjects/HomeTutorPro/agents_dev/cli.py).
3. **`cli.py`:** Utiliza `argparse` para leer la palabra clave (`plan` o `run`) y las banderas opcionales (`--provider`, `--model`, `--spec`, `--iterations`).
4. **Despacho:** Si es `plan`, arranca `spec_interviewer.py`. Si es `run`, instancia `AgenticEngine` y comienza el bucle ReAct.

---

## 7. Los 2 Modos de Uso

El motor agéntico tiene dos modos de operación complementarios: **en GitHub (CI)** y **en local (terminal)**.

### Modo 1: Automático en GitHub (CI en cada Pull Request)
* **¿Cuándo se ejecuta?** Cada vez que haces `git push` a una rama de Pull Request.
* **¿Cómo funciona?**
  1. GitHub Actions lanza el workflow `agent-review.yml`.
  2. Ejecuta `agents_dev/ci/pr_review.py`.
  3. Detecta qué archivos Kotlin han sido modificados o agregados.
  4. Llama a `unified_reviewer.py`, que clasifica cada archivo según su capa con `context_engine.py` y ejecuta **1 sola llamada consolidada por archivo** (reduciendo un 85% las llamadas a la API y evitando errores 429 de cuota).
  5. Elimina comentarios antiguos del bot en la PR para no acumular ruido.
  6. Publica un informe claro:
     * Si todo está correcto: publica `Todo en Orden ✅`.
     * Si hay alertas reales: muestra únicamente filas críticas (`🔴`) o advertencias (`🟡`). **Las informativas verdes (`🟢 info`) se filtran automáticamente** para que el informe sea 100% accionable.
  7. Si existen hallazgos críticos bloqueantes, el check de CI falla (Guardrails).

---

### Modo 2: Local en tu Terminal de Android Studio (Pedirle tareas)

Se realiza en un flujo profesional de **2 fases**:

#### Fase A: Planificar mediante Entrevista Interactiva (`plan`)
Antes de escribir código, el agente te entrevista para clarificar requisitos:
```bash
python3 -m agents_dev plan "Quiero añadir un filtro de estudiantes por curso escolar"
```
1. El agente te formula preguntas en la terminal sobre casos límite, transaccionalidad y contratos de repositorios.
2. Le respondes directamente en la terminal.
3. **Cerrojo estricto de confirmación:** La entrevista **NO termina** hasta que tú escribas expresamente `OK` o `APROBADO`.
4. Al confirmar, guarda la especificación validada en `specs/<nombre_feature>.md`.

#### Fase B: Ejecutar la tarea de forma autónoma (`run`)
Con la especificación aprobada, el agente construye la solución:
```bash
python3 -m agents_dev run --spec specs/filtro_estudiantes.md
```
O bien para una refactorización directa:
```bash
python3 -m agents_dev run "Corrige la inyección de dependencias en StudentRepositoryImpl y compila"
```

---

## 8. Referencia Completa de Comandos (Cheat Sheet)

### Activar el Entorno (Obligatorio siempre antes de ejecutar):
```bash
source agents_dev/.venv_agente/bin/activate
```

---

### Subcomando `plan` (Entrevista de Requisitos):
```bash
# 1. Entrevista estándar (con Gemini 3.1 Flash-Lite por defecto):
python3 -m agents_dev plan "Añadir un sistema de caché offline para los estudiantes en Room"

# 2. Entrevista con Gemini 2.5 Pro (recomendado para arquitectura compleja o rediseños):
python3 -m agents_dev --provider=gemini --model=gemini-2.5-pro plan "Refactorizar sincronización Firestore para soportar conflictos"

# 3. Entrevista con Anthropic Claude:
python3 -m agents_dev --provider=claude plan "Diseñar el flujo de suscripciones in-app con Google Play Billing"
```
> **Nota:** La spec solo se guarda en disco al escribir `OK` o `APROBADO`.

---

### Subcomando `run` (Ejecución Autónoma del Bucle):

#### A. Ejecución directa por objetivo (ideal para refactorizaciones y revisiones rápidas):
```bash
# 1. Auditoría completa de seguridad y arquitectura de una clase:
python3 -m agents_dev run "Revisa StudentRepositoryImpl.kt, ejecuta audit_security y audit_architecture y si detectas algo corrígelo con patch_file"

# 2. Creación asistida de pruebas unitarias:
python3 -m agents_dev run "Diseña con design_tests y genera con scaffold_test las pruebas para AuthRepositoryImpl.kt"

# 3. Verificación de inyección de dependencias Hilt:
python3 -m agents_dev run "Ejecuta check_hilt_bindings para todas las interfaces de domain/repository/ y reporta si falta algún binding"

# 4. Auditoría de documentación KDoc:
python3 -m agents_dev run "Audita KDoc en ScheduleDao.kt y añade las etiquetas @param y @return faltantes"
```

#### B. Ejecución desde una Especificación Aprobada (`--spec`):
```bash
# 1. Construir a partir de una spec:
python3 -m agents_dev run --spec specs/mi_feature_aprobada.md

# 2. Ejecutar con Gemini 2.5 Pro para máxima capacidad de razonamiento:
python3 -m agents_dev --provider=gemini --model=gemini-2.5-pro run --spec specs/mi_feature_aprobada.md

# 3. Ejecutar con Claude 3.7 Sonnet:
python3 -m agents_dev --provider=claude --model=claude-3-7-sonnet run --spec specs/mi_feature_aprobada.md
```

#### C. Control de Iteraciones y Parámetros Avanzados:
```bash
# Limitar a 10 iteraciones para tareas cortas:
python3 -m agents_dev run "Audita KDoc en SueAgentImpl.kt" --iterations 10

# Ampliar a 40 iteraciones para refactorizaciones grandes:
python3 -m agents_dev run --spec specs/refactor_sync.md --iterations 40
```

#### D. Prórroga Interactiva de Turnos (+10 Pasos):
Si una tarea compleja alcanza el límite configurado (25 pasos por defecto), **el agente no se detiene abruptamente ni descarta el trabajo**. Te pregunta interactivamente en la terminal:
```text
⚠️ Se ha alcanzado el límite de 25 iteraciones, pero el agente sigue trabajando activamente.
👉 ¿Deseas concederle 10 pasos adicionales para que termine la tarea? (y/n) [y]:
```
* **Pulsar Enter o `y`**: Concede una prórroga de +10 pasos. El agente continúa trabajando sin perder contexto.
* **Escribir `n`**: Finaliza el bucle y te muestra el `git diff` acumulado para decidir si conservar o descartar.

---

### Comandos de Verificación Rápida:
```bash
# Ejecutar la suite completa de tests del motor agéntico (15 tests en verde):
python3 -m unittest discover agents_dev/tests

# Ejecutar el escáner de seguridad directamente en un archivo específico desde Python:
python3 -c "import sys; sys.path.insert(0, 'agents_dev/skills'); import security_scanner; print(security_scanner.run_security_scan('app/src/main/java/com/devsusana/hometutorpro/data/repository/StudentRepositoryImpl.kt'))"
```

---

## 9. Cómo Funciona por Dentro: El Ciclo ReAct

El agente no es una caja negra; opera bajo el patrón formal **ReAct (Reasoning + Acting)**:

```mermaid
flowchart TD
    Start(["Inicio: Tarea o Spec"]) --> Think["1. Pensamiento (El LLM analiza el estado actual)"]
    Think --> Action["2. Acción (Elige una herramienta: leer, parchear, compilar)"]
    Action --> Observe["3. Observación (Recibe el resultado del sistema o Gradle)"]
    Observe --> Check{"¿Compila y pasan los tests?"}
    Check -- "Error de compilación" --> Fix["Auto-corrección: Analiza el error de Gradle y aplica nuevo parche"]
    Fix --> Action
    Check -- "Éxito (Verde)" --> Diff["4. Git Diff: Muestra en consola los cambios exactos"]
    Diff --> Approval{"¿Apruebas los cambios? (y/n)"}
    Approval -- "y" --> Done(["Cambios conservados con éxito"])
    Approval -- "n" --> Revert(["Deshace los cambios con git checkout"])
```

### Diagrama de Secuencia de un Caso Real

Cuando ejecutas un comando de refactorización como:
> `python -m agents_dev run "Revisa StudentRepositoryImpl.kt, ejecuta audit_security y audit_architecture y si detectas algo corrígelo con patch_file"`

El flujo exacto que ocurre paso a paso es:

```mermaid
sequenceDiagram
    autonumber
    actor Dev as Susana (Tú)
    participant Agent as Agente Autónomo (LLM)
    participant Sys as System Tools
    participant Skills as Skill Tools
    participant Gradle as Gradle / Compilador
    participant Disk as Archivos Kotlin

    Dev->>Agent: python -m agents_dev run "Revisa StudentRepositoryImpl.kt..."
    Agent->>Sys: list_files / read_file
    Sys->>Disk: Abre StudentRepositoryImpl.kt
    Disk-->>Agent: Devuelve el código Kotlin
    Agent->>Skills: audit_architecture + audit_security
    Skills-->>Agent: Diagnóstico detallado (OWASP, Clean Architecture)

    opt Si se detectan mejoras necesarias
        Agent->>Sys: patch_file (search, replace)
        Sys->>Disk: Aplica la cirugía exacta (Minimal Blast Radius)
        Agent->>Sys: run_command("./gradlew compileDebugKotlin")
        Sys->>Gradle: Compila el proyecto
        Gradle-->>Agent: BUILD SUCCESSFUL (o error para auto-reparación)
    end

    Agent-->>Dev: Muestra git diff interactivo y solicita confirmación (y/n)
```

### Las 3 Garantías Innegociables del Motor:
1. **Minimal Blast Radius (`patch_file`):** El agente tiene prohibido borrar un archivo y reescribirlo por completo. Utiliza búsqueda y reemplazo (*search & replace*), cambiando únicamente las líneas necesarias.
2. **Auto-sanación con Gradle:** Tras cada cambio, ejecuta `./gradlew compileDebugKotlin`. Si comete un error de sintaxis o tipos, el compilador le avisa, el agente lee el error de Gradle y se auto-corrige solo.
3. **El Botón Rojo es Tuyo (`y/n`):** Al finalizar, te muestra el `git diff` coloreado y solicita tu aprobación interactiva. Si escribes `n`, el código vuelve exactamente a su estado inicial.

---

## 10. Guion de Estudio: Cómo Explicarlo con Seguridad

Si estás en una entrevista de trabajo técnica, reunión de arquitectura o explicando tu proyecto, utiliza esta narrativa:

> *"En HomeTutorPro no utilizo la IA como un simple autocompletador de código, sino como un **Bucle Agéntico Universal (Universal Agentic Loop)** integrado directamente en el ciclo de vida del desarrollo.*
>
> *Construí una carpeta modular `agents_dev/` desacoplada del código Android. Este sistema opera en dos niveles:*
>
> 1. * **Nivel Local (Human-in-the-loop):** *A través de un CLI en terminal (`plan` y `run`), el agente me realiza una entrevista técnica previa, genera una especificación formal y luego implementa la tarea mediante parches quirúrgicos (`patch_file`). Se valida a sí mismo ejecutando Gradle en un bucle cerrado de auto-reparación, y al final siempre me pide confirmación humana interactiva mediante `git diff` antes de conservar nada.*
>
> 2. * **Nivel CI/CD (GitHub Actions Guardrails):** *En cada Pull Request, un script unificado (`pr_review.py`) clasifica los archivos modificados según su capa mediante un motor de contexto (`context_engine`) y realiza una auditoría integral (Arquitectura Clean, Seguridad OWASP Mobile y documentación KDoc) en una sola llamada optimizada. Esto reduce las llamadas a la API en un 85%, evita el agotamiento de cuotas y bloquea falsos positivos porque el agente sabe qué responsabilidades corresponden a cada clase.*
>
> *El resultado es que el código se mantiene impecable, con 100% de cumplimiento de Clean Architecture y seguridad garantizada, sin gastar tiempo en revisiones manuales repetitivas."*

---

### Resumen de los 5 Comandos Esenciales:
```bash
# 1. Activar el entorno
source agents_dev/.venv_agente/bin/activate

# 2. Comprobar que las herramientas están al 100%
python3 -m unittest discover agents_dev/tests

# 3. Planificar una funcionalidad con entrevista interactiva
python3 -m agents_dev plan "Título de tu idea"

# 4. Construir la solución con la especificación aprobada
python3 -m agents_dev run --spec specs/tu_especificacion.md

# 5. Tarea o refactorización directa
python3 -m agents_dev run "Descripción de tu tarea directa"
```

---

## 11. Anexo Técnico: Estrategia Shift-Left con Git Hooks (pre-push) y Gestión de Claves

> 💡 **Nota de contexto:** Este anexo describe una estrategia avanzada de control de calidad para **entornos corporativos o de trabajo** donde el desarrollador no tiene permisos para modificar la infraestructura de CI/CD (los archivos `.github/workflows/*.yml` en el servidor) o donde se desea asegurar que ningún compañero pueda subir código defectuoso al repositorio remoto.

### 11.1. ¿Qué es «Shift-Left» y por qué usar un Hook?
En ingeniería de software, **Shift-Left** significa mover la validación de calidad lo más a la izquierda posible en la línea temporal:
* **Tradicional (tarde):** El desarrollador sube el código -> la CI en GitHub tarda 15 minutos -> falla por un fallo de seguridad o arquitectura -> el desarrollador tiene que cambiar de contexto y volver a corregirlo.
* **Shift-Left (temprano):** El motor valida los estándares en el ordenador del desarrollador **antes** de que el código salga de su máquina. Si hay un problema crítico, la subida se bloquea al instante.

---

### 11.2. ¿Por qué `pre-push` y no `pre-commit`?

Git ofrece varios tipos de "hooks" (disparadores automáticos locales):

| Disparador | Momento de Ejecución | Comportamiento con IA | Veredicto |
| :--- | :--- | :--- | :---: |
| **`pre-commit`** | En cada `git commit` local. | Se ejecuta en cada micro-commit («WIP», «Fix typo»). Hace llamadas constantes a la API, ralentiza el flujo de trabajo diario y consume cuotas innecesariamente. | ⚠️ No recomendado para auditorías de IA |
| **`pre-push`** | Solo al hacer `git push`. | Se ejecuta **únicamente cuando el desarrollador decide compartir su trabajo con el equipo**. Revisa todos los archivos modificados en la rama y, si hay un fallo crítico (`🔴`), aborta el envío (`exit 1`). | **✅ Recomendado: 100% eficiente y no frena el trabajo local** |

---

### 11.3. Implementación del Hook en `.git/hooks/pre-push`

Los hooks de Git se ubican en la carpeta `.git/hooks/` de cada repositorio. Para activar la auditoría agéntica previa al push, se crea un archivo ejecutable `.git/hooks/pre-push`:

```bash
#!/usr/bin/env bash
# .git/hooks/pre-push: Auditoría automática del bucle agéntico antes de subir cambios

echo "🔍 [Bucle Agéntico] Verificando estándares de calidad antes del push..."

# 1. Comprobar que existe al menos un proveedor de IA configurado
if [ -z "$GEMINI_API_KEY" ] && [ -z "$ANTHROPIC_API_KEY" ] && [ -z "$OPENAI_API_KEY" ]; then
    echo "⚠️ AVISO: No se detectó ninguna API Key (GEMINI_API_KEY / ANTHROPIC_API_KEY). Saltando auditoría de IA."
    exit 0 # Cambiar a 'exit 1' si en tu equipo es obligatorio bloquear el push sin clave
fi

# 2. Activar el entorno virtual del agente si existe
if [ -f "agents_dev/.venv_agente/bin/activate" ]; then
    source agents_dev/.venv_agente/bin/activate
fi

# 3. Analizar los archivos Kotlin modificados entre la rama actual y origin/main
python3 -c "
import sys
import subprocess

try:
    from agents_dev.skills.unified_reviewer import run_unified_review
except ImportError:
    print('⚠️ Módulo unified_reviewer no encontrado en agents_dev. Continuando push.')
    sys.exit(0)

# Obtener archivos Kotlin modificados respecto al remoto
try:
    output = subprocess.check_output(['git', 'diff', '--name-only', 'origin/main...HEAD']).decode()
    files = [line.strip() for line in output.splitlines() if line.endswith('.kt') and not line.endswith('Test.kt')]
except Exception:
    files = []

if not files:
    print('✅ No hay archivos Kotlin modificados que auditar.')
    sys.exit(0)

print(f'Auditan do {len(files)} archivo(s) Kotlin con Unified Reviewer...')
has_critical = False

for file_path in files:
    result = run_unified_review(file_path)
    critical_issues = result.get('critical_issues', [])
    if critical_issues:
        print(f'\n🔴 [BLOQUEO] Fallos críticos detectados en {file_path}:')
        for issue in critical_issues:
            print(f'   - {issue}')
        has_critical = True

if has_critical:
    print('\n❌ PUSH CANCELADO: Corrige los fallos críticos de arquitectura o seguridad antes de subir.')
    sys.exit(1)
else:
    print('✅ Auditoría superada: Código limpio y listo para subir.')
    sys.exit(0)
"

# Si el script devolvió código de error, cancelamos el git push
if [ $? -ne 0 ]; then
    exit 1
fi

exit 0
```

> **Para darle permisos de ejecución en Linux/Mac:**
> ```bash
> chmod +x .git/hooks/pre-push
> ```

---

### 11.4. Cómo Configurar las API Keys de los Diferentes Proveedores

El motor agéntico está desacoplado del proveedor de IA. Puedes alimentarlo con cualquiera de los tres ecosistemas principales mediante variables de entorno del sistema:

#### 1. Google Gemini (`GEMINI_API_KEY`)
* **Cómo obtenerla:** Entra en [Google AI Studio](https://aistudio.google.com/), inicia sesión con una cuenta de Google y pulsa en **"Get API key"** -> **"Create API key"** (es gratuita para desarrollo).
* **Configuración en terminal (`~/.zshrc` o `~/.bashrc`):**
  ```bash
  export GEMINI_API_KEY="AIzaSyTuClaveDeGoogleAqui..."
  export GEMINI_MODEL="gemini-3.1-flash-lite" # Opcional (por defecto: 3.1-flash-lite)
  ```

#### 2. Anthropic Claude (`ANTHROPIC_API_KEY`)
* **Cómo obtenerla:** Accede a la consola de [Anthropic Console](https://console.anthropic.com/), dirígete a **API Keys** y genera una nueva clave.
* **Configuración en terminal:**
  ```bash
  export ANTHROPIC_API_KEY="sk-ant-api03-tuClaveDeAnthropic..."
  export ANTHROPIC_MODEL="claude-3-5-sonnet-20241022" # o claude-3-7-sonnet
  ```

#### 3. OpenAI / GitHub Copilot / Proxies Corporativos (`OPENAI_API_KEY`)
* **Entornos con OpenAI:**
  Si en el entorno de trabajo se utiliza OpenAI, se exporta:
  ```bash
  export OPENAI_API_KEY="sk-proj-tuClaveOpenAIAqui..."
  export OPENAI_BASE_URL="https://api.openai.com/v1" # O la URL del proxy corporativo interno
  ```
* **GitHub Copilot CLI / Tokens Personales:**
  GitHub Copilot utiliza la autenticación de la CLI de GitHub (`gh auth login`) o un token de acceso personal (PAT) con permisos de Copilot (`COPILOT_API_TOKEN`). Las herramientas corporativas pueden dirigir las llamadas al endpoint interno de Copilot enrutando a través de un proxy compatible con la especificación OpenAI (`http://localhost:port/v1`).

---

### 11.5. Cómo Compartir el Hook con el Equipo sin Tocar la CI

Por seguridad de Git, el directorio `.git/hooks/` **no se sincroniza en el repositorio remoto**. Si deseas que todos los miembros del equipo utilicen el mismo hook sin tener permisos en la CI, existen dos soluciones estándar:

#### Opción A: Configurar una carpeta compartida de hooks en Git (Recomendada)
1. Creas una carpeta versionada en el proyecto: `scripts/hooks/pre-push`.
2. Cada desarrollador (o mediante un script de bienvenida) ejecuta una sola vez:
   ```bash
   git config core.hooksPath scripts/hooks
   ```
   A partir de ese comando, Git leerá los hooks directamente desde `scripts/hooks/`, que sí está versionado en el repositorio.

#### Opción B: Tarea Gradle automática en Android (`installGitHooks`)
Se añade una tarea sencilla en el archivo `build.gradle.kts` raíz o del módulo:
```kotlin
tasks.register("installGitHooks") {
    group = "verification"
    description = "Copia los hooks de calidad del bucle agéntico a .git/hooks/"
    doLast {
        val hookSource = file("scripts/hooks/pre-push")
        val hookDest = file(".git/hooks/pre-push")
        if (hookSource.exists()) {
            hookSource.copyTo(hookDest, overwrite = true)
            hookDest.setExecutable(true)
            println("✅ Hook pre-push agéntico instalado con éxito en .git/hooks/")
        }
    }
}
```
Con esto, cualquier desarrollador del equipo solo tiene que ejecutar `./gradlew installGitHooks` una vez, y su máquina quedará protegida contra subidas con fallos de arquitectura o seguridad.
