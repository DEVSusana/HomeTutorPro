# Documentación Técnica del Sistema de Agentes de Desarrollo (`agents_dev`)

Este documento sirve como manual de referencia para entender la arquitectura actual de los agentes de desarrollo en **HomeTutorPro** y los pasos técnicos para su evolución hacia un sistema agéntico autónomo.

---

## 1. Mapeo Conceptual del Proyecto Android

Para que un agente (o un desarrollador) trabaje de manera segura en **HomeTutorPro**, debe conocer los pilares del proyecto:

### A. Capas de la Clean Architecture (Capa de Presentación MVVM)
El código se organiza bajo los principios de separación estricta definidos en `AGENTS.md`:

*   **`domain`**: Contiene la lógica de negocio pura. Entidades (`entities`), interfaces de Casos de Uso (`usecases`) e interfaces de Repositorios. Es 100% independiente del SDK de Android.
*   **`data`**: Implementa las interfaces de repositorios, bases de datos (Room en modo local prioritario debido a costes de infraestructura en la nube) y mapeadores.
*   **`presentation` / `ui`**: Implementa Jetpack Compose con Material 3. La regla de oro en esta capa es la separación de componentes:
    *   **`Screen` Composable**: Instancia el ViewModel, recolecta estados en forma de `StateFlow` y gestiona efectos globales (ej. Scaffold).
    *   **`Content` Composable**: Es un componente puro y sin estado (`stateless`). Recibe todos los datos e inputs mediante parámetros (State Hoisting), lo que facilita su testabilidad.

### B. Reglas de Dominio Inflexibles (de BUSINESS_RULES.md)
*   **Saldo Negativo de Alumnos:** El saldo de un estudiante (`pendingBalance`) puede ser **negativo** (saldo a favor). Las validaciones del dominio no deben prohibir valores negativos.
*   **Manejo de Excepciones del Calendario:** Diferencia la plantilla recurrente (`Schedule`) del alumno, de las excepciones temporales del calendario (`ScheduleException` de tipo `CANCELLED`, `RESCHEDULED`, o `EXTRA`).

---

## 2. Inventario y Funcionamiento de los Agentes Actuales

Actualmente, las herramientas de asistencia al desarrollo viven en `agents_dev/skills/`. A continuación, se detalla la responsabilidad de cada archivo y cómo interactúan:

### A. Los Módulos Especialistas (`agents_dev/skills/`)
Todos los agentes consumen el cliente centralizado en `config.py` y utilizan el modelo `gemini-2.0-flash-lite`:

1.  **`config.py`:**
    *   *Propósito:* Configura el SDK de Google GenAI mediante `genai.Client`.
    *   *Lógica:* Implementa un bucle de reintento con retardo exponencial ante errores `429 (Resource Exhausted)` para mitigar los límites de uso de la API gratuita.
2.  **`architecture_auditor.py`:**
    *   *Propósito:* Evalúa si el código en un archivo Kotlin cumple con las reglas arquitectónicas y las directrices de negocio.
    *   *Lógica:* Toma como contexto el contenido de `AGENTS.md` y `BUSINESS_RULES.md`, lo adjunta al prompt junto al código a analizar y le pide a Gemini que señale las desviaciones.
3.  **`refactor_assistant.py`:**
    *   *Propósito:* Busca malas prácticas y oportunidades de refactorización según los patrones limpios del proyecto.
    *   *Lógica:* Envía el código a Gemini solicitando una evaluación detallada de calidad de código y sugerencias concretas de optimización.
4.  **`security_scanner.py`:**
    *   *Propósito:* Analiza el código Kotlin buscando claves cableadas (hardcoded), usos inseguros de Coroutines y que los nombres de las variables/clases sigan el estándar en inglés.
5.  **`test_generator.py`:**
    *   *Propósito:* Diseña los planes de pruebas y define qué escenarios unitarios o de integración deben probarse según los requisitos de `AGENTS.md`.
6.  **`kdoc_specialist.py`:**
    *   *Propósito:* Revisa que las clases y funciones públicas cuenten con la documentación KDoc requerida.

### B. El Orquestador Agéntico Autónomo (`agent_orchestrator.py`)
El orquestador une a todos los especialistas bajo un **bucle cerrado (Closed-Loop)** enfocado en la fiabilidad del código:
1.  **Detección:** Si se ejecuta sin parámetros, detecta automáticamente los archivos Kotlin (`.kt`) modificados en el espacio de trabajo local consultando a Git (`git status --porcelain` o el último commit). Si se le pasa una ruta de archivo como argumento, procesa exclusivamente ese archivo.
2.  **Refactorización Quirúrgica (Search & Replace):** Invoca a `refactor_assistant.py` para obtener propuestas de refactorización en un formato estructurado JSON. Python aplica de forma local y quirúrgica los parches de código sobre el archivo destino, en lugar de reescribirlo completo, para proteger el código preexistente y ahorrar cuota de tokens.
3.  **Bucle de Autocorrección (Self-Healing Loop):** 
    *   Ejecuta `./gradlew compileDebugKotlin` usando subprocesos en segundo plano para verificar que el código refactorizado compila sin problemas.
    *   Si la compilación falla, captura la salida de error de consola de Gradle (`stderr`), se la envía a Gemini junto al archivo y solicita parches específicos de reparación del error, iterando hasta 3 veces de forma automática.
4.  **Verificación de Tests:** Si la compilación es exitosa, ejecuta `./gradlew test`. Si las pruebas unitarias fallan, solicita una corrección de lógica a Gemini con base en el reporte de error del test de JUnit.
5.  **Aprobación Humana (Human-in-the-Loop):** Al finalizar con éxito la verificación, el orquestador presenta un `git diff` de los cambios en pantalla y pregunta: `🤔 ¿Desea APROBAR y CONSERVAR estos cambios en el archivo? (y/n):`.
    *   Si respondes **`y` (yes)**, los cambios se consolidan físicamente en el disco.
    *   Si respondes **`n` (no)**, el script descarta los cambios de inmediato ejecutando `git checkout -- <file_path>` y dejando tu archivo original intacto.

---

## 3. Instrucciones de Uso y Configuración

Para que el sistema de agentes funcione correctamente en tu máquina, sigue estos pasos:

### Paso 1: Configurar las Variables de Entorno
El orquestador requiere conectarse a la API de Gemini. Asegúrate de exportar tu clave de API en tu terminal antes de correr los agentes:
```bash
export GEMINI_API_KEY="tu_clave_de_api_aquí"
```

*Nota: El script en `config.py` implementa un Mock Fallback inteligente de simulación para desarrollo local u offline. Si tu clave de API gratuita llega a agotar su cuota (Resource Exhausted 429), el script activará de forma automática el simulador local al detectar el archivo de prueba de desarrollo, permitiéndote probar la lógica sin interrupción.*

### Paso 2: Ejecutar el Orquestador
Sitúate en el directorio raíz del proyecto y ejecuta el comando de Python:

*   **Para procesar todos tus archivos Kotlin modificados (Detectados por Git):**
    ```bash
    python agents_dev/skills/agent_orchestrator.py
    ```
*   **Para procesar un archivo específico del proyecto:**
    ```bash
    python agents_dev/skills/agent_orchestrator.py app/src/main/java/com/devsusana/hometutorpro/data/repository/StudentRepositoryImpl.kt
    ```

---

## 4. Bucle del Pre-Commit Hook (Disparo Automático en Git)

Para que no tengas que recordar ejecutar el comando manualmente antes de consolidar tu trabajo, puedes configurar un trigger automático de Git.

Crea un archivo llamado `pre-commit` dentro de la carpeta `.git/hooks/` de tu proyecto con el siguiente contenido:

```bash
#!/bin/bash
echo "🔍 Ejecutando auditoría y refactorización agéntica autónoma..."
python agents_dev/skills/agent_orchestrator.py
```
Asegúrate de darle permisos de ejecución:
```bash
chmod +x .git/hooks/pre-commit
```
Ahora, cada vez que hagas `git commit`, el agente auditará, refactorizará y corregirá automáticamente tus archivos, y te solicitará aprobación interactiva antes de permitir guardar el commit.

