# 🧠 Guía Maestra: Inteligencia Artificial On-Device, RAG Local y Arquitectura en HomeTutorPro
> **Documento de estudio y referencia para ponencia técnica**  
> *Autora:* Susana Córdoba Serrano  
> *Proyecto:* HomeTutorPro — Asistente Inteligente Local "SUE"

---

## 📑 Tabla de Contenidos
1. [Visión General: ¿Por qué IA On-Device y RAG Local?](#1-visión-general)
2. [Stack Tecnológico: ¿Qué librerías usamos y por qué?](#2-stack-tecnológico-librerías-y-justificación)
3. [Arquitectura de Software: Clean Architecture + MVVM + UDF](#3-arquitectura-de-software)
4. [¿Qué es y cómo funciona el RAG Local en HomeTutorPro?](#4-anatomía-del-rag-local-en-sue)
5. [El Modelo LLM: Gemma y MediaPipe GenAI](#5-el-modelo-llm-gemma-y-mediapipe)
6. [El Patrón Híbrido: Determinismo vs Generación](#6-el-patrón-híbrido-determinismo-vs-generación)
7. [Ingeniería de Memoria RAM y Ciclo de Vida (Android 17)](#7-ingeniería-de-memoria-ram-y-ciclo-de-vida-android-17)
8. [Seguridad, Privacidad y Cumplimiento RGPD](#8-seguridad-privacidad-y-rgpd)
9. [Estructura Sugerida para tu Charla](#9-guion-y-diapositivas)

---

## 1. Visión General
HomeTutorPro es una aplicación Android diseñada para profesores particulares. Su núcleo diferencial es **SUE**, una asistente conversacional por voz que opera **100% en el dispositivo móvil (Edge Computing)**.

### El Reto
Integrar un Modelo de Lenguaje Grande (LLM) en un teléfono móvil presenta 3 desafíos críticos:
1. **Alucinaciones:** Los modelos pequeños (~1B - 2B de parámetros) tienden a equivocarse en cálculos matemáticos o inventar datos si se les pregunta directamente.
2. **Consumo de Hardware:** Un LLM exige mucha memoria RAM y GPU.
3. **Privacidad de Datos:** Un profesor gestiona información de menores de edad (nombres, teléfonos, precios, direcciones). Enviar estos datos a APIs cloud (OpenAI, Gemini Cloud) expone al desarrollador a severas sanciones del RGPD europeo.

### La Solución
Una **Arquitectura Híbrida On-Device** que combina:
* Reconocimiento y síntesis de voz locales (SpeechRecognizer & TTS).
* Base de datos local transaccional (Room/SQLite).
* **RAG Local (Retrieval-Augmented Generation)**: Inyección dinámica en el prompt de la información del profesor en tiempo de ejecución.
* **Gemma 3 (1B/2B INT4)** ejecutado en local mediante **Google MediaPipe GenAI**.
* **Capa determinista en Kotlin** para operaciones críticas (0 ms de latencia y 100% de precisión).

---

## 2. Stack Tecnológico: Librerías y Justificación

| Tecnología / Librería | ¿Para qué se usa en HomeTutorPro? | ¿Por qué esta y no otra alternativa? |
|---|---|---|
| **Kotlin + Coroutines & Flow** | Lenguaje base y gestión de asincronía reactiva. | Tipado estricto, seguridad frente a nulos (*null-safety*), y concurrencia estructurada sin bloquear el hilo principal de la UI. |
| **Jetpack Compose + Material 3** | Creación de la interfaz de usuario moderna y overlays. | Declarativo, reduce el código boilerplate en un 60% frente a XML, y se integra de forma natural con flujos unidireccionales de datos (UDF). |
| **Google MediaPipe Tasks GenAI** | Runtime de inferencia on-device para Gemma. | **¿Por qué no llama.cpp o ONNX?** MediaPipe es el runtime oficial de Google optimizado con aceleración de hardware nativa (GPU/NPU) para Android, con empaquetado directo y APIs limpias en Kotlin. |
| **Gemma 3 1B / Gemma 2B (INT4)** | Modelo fundacional de IA en el dispositivo. | Modelos abiertos de Google entrenados con la misma tecnología que Gemini. Cuantizados a 4 bits (INT4) ocupan solo ~500-800 MB, haciéndolos viables en terminales móviles. |
| **Room Database (SQLite)** | Persistencia local y base de conocimiento. | Transaccional, robusto, verificación en tiempo de compilación y soporte nativo para migraciones seguras y flujos reactivos. |
| **Dagger Hilt** | Inyección de dependencias. | Estándar oficial de Android. Facilita el desacoplamiento, la modularidad y el testeo unitario mediante inyección por constructor. |
| **Android SpeechRecognizer & TTS** | Entrada de audio a texto y lectura en voz alta. | Utiliza el motor nativo del dispositivo sin consumir servicios de pago en la nube ni requerir conexión a internet. |

---

## 3. Arquitectura de Software
Seguimos rigurosamente **Clean Architecture** estructurada en 3 capas independientes:

```
                      ┌────────────────────────────────────────┐
                      │        PRESENTATION LAYER (UI)         │
                      │  Jetpack Compose • ViewModels • TTS    │
                      └──────────────────┬─────────────────────┘
                                         │ Observa StateFlow / Emite Eventos
                                         ▼
                      ┌────────────────────────────────────────┐
                      │          DOMAIN LAYER (Core)           │
                      │   Entities • UseCases • SueAgent       │
                      │   (Pura lógica Kotlin, sin Android SDK)│
                      └──────────────────┬─────────────────────┘
                                         │ Implementa contratos (Interfaces)
                                         ▼
                      ┌────────────────────────────────────────┐
                      │           DATA LAYER (Datos)           │
                      │ Room DB • MediaPipe GenAI • Repositories│
                      └────────────────────────────────────────┘
```

* **Flujo Unidireccional de Datos (UDF):** La interfaz (`SueOverlayContent`) solo recibe un estado inmutable (`SueUiState`) y emite eventos al `SueViewModel`.
* **Desacoplamiento Total:** `SueAgent` (en Dominio) define qué contexto necesita; `MediaPipeModelRepository` (en Datos) se encarga de hablar con los binarios de C++ de MediaPipe.

---

## 4. Anatomía del RAG Local en SUE

### ¿Qué es RAG (Retrieval-Augmented Generation)?
Un LLM por sí solo solo sabe lo que aprendió durante su entrenamiento (no sabe quién es tu alumno "Darío", ni qué clases tienes hoy). 
**RAG** es la técnica de:
1. **Recuperar (Retrieve):** Buscar en la base de datos local la información exacta relevante para la consulta del usuario.
2. **Aumentar (Augment):** Inyectar esos datos dentro del prompt del sistema bajo un bloque delimitado (`--- AVAILABLE DATA ---`).
3. **Generar (Generate):** Pedirle al LLM que responda utilizando **exclusivamente** los datos proporcionados, eliminando el riesgo de alucinación factual.

### Flujo Paso a Paso de una Consulta en SUE:

```text
Profesor habla -> SpeechRecognizer -> Transcripción de texto
       │
       ▼
SueViewModel -> sueAgent.detectActionIntent(query)
       ├── [CASO A: Capa 1 - Fast-Path / SQL Directo en Room]
       │     ├── Acciones transaccionales: Crear alumno, reprogramar, cancelar, registrar pago, clase extra.
       │     │     └── Validación estricta de colisiones en DB -> SuePendingAction (Tarjeta interactiva con confirmación por voz)
       │     └── Consultas estructuradas: Saldo de alumno, conteo, agenda directa, clases canceladas.
       │           └── Consulta SQL directa -> ReadSuccess en 0 ms -> TTS inmediato (0 alucinaciones)
       │
       └── [CASO B: Capa 2 - RAG On-Device + LLM Generativo]
             ├── sueAgent.gatherRelevantContext(query) (Recuperación selectiva en Room DB)
             │     ├── Consultas Compuestas: Agenda del día/semana y huecos libres simultáneos (getFreeSlots)
             │     ├── Excepciones activas unificadas: canceladas, reprogramadas y extras (getCancelledClassesDescription)
             │     ├── RAG de Recursos y Materiales compartidos (Agrupamiento inteligente relacional)
             │     ├── Finanzas globales, facturación y saldos pendientes (getStudentsWithBalance, transacciones)
             │     ├── Historial global y asistencias de clases impartidas (getAllClassLogs)
             │     └── Ficha detallada de alumno: cursos, tarifas, saldos, notas pedagógicas y materiales (take 20)
             ├── Construcción de Prompt estructurado (Gemma Chat Template con ventana ampliada a 2048 tokens)
             ├── Inferencia local con MediaPipe Tasks GenAI (Gemma 3 INT4, maxTokens = 2048, temp = 0.3)
             └── SueResponseFormatter -> TTS (Audio saneado con pausas y puntuación natural)
```

### Gestión del Presupuesto de Tokens (Token Budgeting en GPU/NPU)
Los dispositivos móviles tienen límites estrictos de memoria de vídeo (VRAM). Enviar volcados masivos de base de datos provocaría bloqueos del subsistema gráfico y Out-Of-Memory (OOM). Para conciliar respuestas ricas y estabilidad absoluta aplicamos:
- **Inyección Contextual Selectiva y Compresión Previa en Kotlin:** `gatherRelevantContext` no delega la síntesis bruta al LLM. Filtra y resume los datos mediante consultas SQL y estructuras relacionales antes de generar el prompt.
- **RAG Inteligente con Agrupamiento de Recursos Compartidos:**
  - Cuando el profesor pregunta por materiales enviados a nivel global (*"¿Qué recursos he compartido?"*, *"¿Qué material le he enviado a cada alumno?"*):
    - **Si total $\le 15$ archivos:** Inyecta el desglose completo (Alumno, Archivo, Tipo, Fecha `dd/MM/yyyy`, Vía de envío como WhatsApp o Email).
    - **Si total $> 15$ archivos:** Aplica compresión semántica agrupando por alumno (`groupBy { it.studentId }`), inyectando el número total de archivos y el recurso más reciente de los 20 alumnos más activos, instruyendo a Sue a ofrecer un resumen conciso y sugerir consultar el detalle de un alumno en particular.
  - Para consultas sobre un alumno específico, se ordenan cronológicamente y se acotan a los 20 recursos más recientes (`take(20)`).
- **Ventana de Contexto Ampliada (`MAX_TOKENS = 2048`):** 
  Configurada en `MediaPipeModelRepository` (`MAX_TOKENS = 2048`, `TEMPERATURE = 0.3f`, `TOP_K = 20`). Se amplió desde 512 tokens para permitir consultas compuestas complejas (horario del día + huecos libres + materiales compartidos + deudas) y asegurar respuestas explicativas completas sin truncamiento de frases, manteniendo el consumo de VRAM estrictamente acotado gracias a las reglas de agrupamiento.

### Estructura del Prompt Inyectado (Gemma Chat Template)
```text
<start_of_turn>user
Eres Sue, la asistente inteligente de HomeTutorPro.
Tu rol es responder a las preguntas del profesor de forma breve, amable y concisa.
NUNCA inventes datos. Usa SOLO la información provista en --- AVAILABLE DATA ---.
Si el usuario pregunta por clases de hoy, canceladas, alumnos, finanzas o recursos y materiales compartidos, lee la sección correspondiente en --- AVAILABLE DATA --- y responde con claridad y precisión.
Si no hay datos en esa sección o la sección indica que no hay registros, dile al profesor con amabilidad que no tienes información registrada sobre eso.
NUNCA llames "Sue" al profesor (tú eres Sue; él es el usuario o profesor).

--- TEMPORAL CONTEXT ---
Current date/time: martes, 16 septiembre 2025, 09:20
--- END OF TEMPORAL CONTEXT ---

--- PROFESSOR WORKING HOURS (AGENDA BOUNDARIES) ---
Working hours limit: from 08:00 to 23:00
--- END OF WORKING HOURS ---

--- RECENT CONVERSATION HISTORY ---
User: Hola Sue
Sue: ¡Hola! ¿En qué puedo ayudarte hoy?
--- END OF HISTORY ---

--- AVAILABLE DATA ---
Horario del martes:
  16:30–18:00: Lucía Moreno García
  18:00–19:00: Carlos
Huecos libres dentro de jornada:
  08:00–16:30, 19:00–23:00
Clases canceladas o reprogramadas esta semana:
• Viernes 19/09/2025: la clase de Lucía (17:00 - 18:30) está cancelada.

--- RECURSOS / MATERIALES COMPARTIDOS CON ALUMNOS ---
Total de recursos compartidos: 3 entre 2 alumnos.
- Alumno: Lucía Moreno García | Archivo: examen_algebra.pdf (pdf) compartido el 15/09/2025 vía WHATSAPP
- Alumno: Carlos | Archivo: problemas_fisica.pdf (pdf) compartido el 14/09/2025 vía EMAIL
- Alumno: Carlos | Archivo: formulario_dinamica.pdf (pdf) compartido el 12/09/2025 vía WHATSAPP
--- FIN RECURSOS / MATERIALES COMPARTIDOS ---
--- END OF DATA ---

User query: ¿Qué clases tengo hoy por la tarde y qué material le he enviado a Carlos?
<end_of_turn>
<start_of_turn>model
```

---

## 5. El Modelo LLM: Gemma y MediaPipe

* **Familia Gemma:** Modelos ligeros de última generación creados por Google DeepMind a partir de la investigación de Gemini.
* **Cuantización INT4:** Los pesos originales en coma flotante de 32 bits (FP32) se reducen matemáticamente a enteros de 4 bits. Esto reduce el peso del modelo de 8 GB a ~600 MB sin perder coherencia semántica en tareas conversacionales.
* **Alineación NDK a 16 KB:** Esencial desde Android 15/17. `MediaPipe Tasks GenAI 0.10.27+` asegura que el mapeo de memoria (`mmap`) del archivo `.bin` o `.task` se divida en bloques de 16 KB en la memoria física del procesador.
* **Protección ante Descargas Incompletas:** Validación de tamaño mínimo (`MIN_MODEL_SIZE_BYTES = 50 MB`) y verificación de cabeceras HTTP antes de permitir la inicialización nativa en C++.

---

## 6. El Patrón Híbrido: Determinismo vs Generación

Un fallo común al crear apps con IA es **enviar absolutamente todo al LLM**. En un dispositivo móvil esto genera latencia innecesaria (8-12s) y riesgo de alucinaciones.

En HomeTutorPro implementamos una **Capa Híbrida de Dos Niveles**:

### 1. Nivel 1: Capa Determinista (Fast-Path en Kotlin / SQL Room)
- **Casos de uso:** Consultas de saldo pendiente, listado de clases canceladas, conteo de alumnos, horas libres, altas de estudiantes, pagos y reprogramaciones.
- **Latencia:** **< 5 ms** (inmediata).
- **Consumo:** 0% de GPU/NPU, 0% de impacto en batería.
- **Motor de Lenguaje Natural en Español:**
  - Reconocimiento de horas numéricas (`17:00`, `21`, `22:30`) y en palabras de `cero` a `veinticuatro` y `medianoche`.
  - Soporte de fracciones temporales (`y media`, `y cuarto`, `menos cuarto`, `y veinte`).
  - Modificadores contextuales (`de la tarde`, `de la mañana`, `de la noche`, `pm`, `am`).
  - **Inferencia Inteligente de Franja Horaria:** Si el usuario no especifica la franja (*"pon una clase a las 5"* o *"a las 6"*), el motor infiere automáticamente la tarde (`17:00` o `18:00`), adaptándose a la realidad habitual de las clases particulares extraescolares.
  - Máquina de estados conversacional multi-turno (ej. pedir hora tras solicitar clase extra) con reinicio automático de variables temporales al confirmar la acción.
- **Prevención Estricta de Colisiones y Reconciliación de Excepciones:**
  - Al reprogramar o añadir clases extras, `SaveScheduleExceptionUseCase` valida solapamientos contra horarios regulares, clases extras y reprogramaciones existentes.
  - Reconcilia en tiempo real las cancelaciones, de modo que un hueco cancelado queda inmediatamente disponible para nuevas clases.
  - Si hay colisión, bloquea la mutación y SUE informa proactivamente del alumno en conflicto y los días/huecos libres disponibles.

### 2. Nivel 2: Capa Generativa (RAG + Inferencia Local con Gemma)
- **Casos de uso:** Consultas complejas, abiertas, pedagógicas o conversacionales (*"¿Cómo debería organizar el repaso de matemáticas con Carlos?"*, *"¿Qué material le he enviado a cada alumno?"*, *"¿Qué clases tengo hoy y qué tengo libre por la tarde?"*).
- **Funcionamiento:** Se alimenta del contexto estructurado extraído en el paso RAG (con compresión relacional previa en Kotlin y ventana ampliada a 2048 tokens) para que el modelo redacte una respuesta natural, veraz y personalizada.

---

## 7. Ingeniería de Memoria RAM, Hardware y Ciclo de Vida (Android 15-17)

En Android 15 y versiones superiores (especialmente Android 17), Google introduce **límites estrictos de memoria por aplicación (*Per-App Memory Limits*)** y mata procesos que excedan su cuota (`MemoryLimiter:AnonSwap`).

### Nuestras 5 Estrategias de Optimización y Defensa:

1. **Detección Preventiva de Compatibilidad de Hardware (`SueDeviceCompatibility`):**
   - Antes de permitir la descarga o activación de SUE (tanto en Onboarding como en Ajustes), la app inspecciona `ActivityManager.getMemoryInfo()` y `isLowRamDevice`.
   - **Requisitos Mínimos:** Al menos **2.5 GB de RAM física** y **Android 9.0+ (API 28+)**.
   - Si el dispositivo no alcanza estos requisitos, se previene la descarga para evitar degradar el terminal, mostrándose una tarjeta informativa en Ajustes y funcionando con normalidad en modo clásico.
2. **Arranque Frío Ultraligero:** La app inicia consumiendo únicamente **~80 MB** de RAM (no se precarga el modelo al arrancar).
3. **Carga en Paralelo durante la Locución (*Parallel Warm-Up*):** Al pulsar el botón de voz, Gemma se carga en un hilo secundario en paralelo mientras el usuario habla. Cuando el usuario se calla, el modelo ya está listo (**0 ms de espera adicional percibida**).
4. **Smart Idle Auto-Release (Descarga Automática de RAM):** 
   - Tras **2 minutos (120 s)** de inactividad (`IDLE_RELEASE_TIMEOUT_MS = 120_000L`) o **30 segundos** tras cerrar el asistente (`DISMISS_RELEASE_TIMEOUT_MS = 30_000L`), el ViewModel descarga el modelo de la RAM mediante `inferenceRepository.release()`.
   - Incluye un temporizador de seguridad de escucha de 15 segundos (`LISTENING_TIMEOUT_MS = 15_000L`) para reanudar el estado si se produce una interrupción del micrófono.
5. **Protección en Segundo Plano (`onTrimMemory`):** Al minimizar la app (`TRIM_MEMORY_UI_HIDDEN`) o ante presión de memoria (`TRIM_MEMORY_RUNNING_CRITICAL`), se libera inmediatamente la memoria nativa de GPU/RAM.
6. **UX y Distribución OTA:** Al completarse la descarga del modelo en segundo plano, la notificación del sistema incluye navegación directa con auto-scroll a la sección de SUE en la pantalla de Ajustes.

---

## 8. Seguridad, Privacidad y RGPD

1. **Privacidad por Diseño (*Privacy by Design*):** Ningún dato de los alumnos sale del teléfono. No hay servidores intermedios, ni logs en la nube, ni venta de telemetría.
2. **Redacción de PII en Logs (`SafeLogger`):** Todas las trazas de depuración de SUE, autenticación y base de datos utilizan `SafeLogger`, que redacta automáticamente datos sensibles (PII, nombres, UIDs, identificadores) antes de emitirlos en logcat o enviarlos a Crashlytics.
3. **Minimización de Datos:** En el MVP, el modelo de datos de `Student` excluye campos sensibles (como diagnósticos médicos o problemas de aprendizaje) para cumplir con el principio de minimización de datos del RGPD.
4. **Confirmación en Dos Pasos para Escrituras:** Toda modificación destructiva en la base de datos (cancelar clase, borrar alumno, registrar pago) pasa por un estado `Prepare` donde Sue pide confirmación verbal explícita (*"¿Confirmas cancelar la clase de Pepe? Di sí o no"*), evitando acciones accidentales por malas interpretaciones de audio.
5. **Crashlytics Seguro:** Habilitado de forma controlada en `AppInitializerImpl` protegiendo los datos confidenciales locales del profesor.

---

## 9. Guion y Diapositivas

Si presentas esta arquitectura, esta es una estructura ganadora de 30-40 minutos:

| # | Título de la Diapositiva | Puntos Clave a Explicar |
|---|---|---|
| 1 | **La Era de la IA en el Dispositivo (Edge AI)** | Por qué no todo debe estar en la nube (Privacidad, Costes de Servidor = 0€, Modo Offline). |
| 2 | **El Problema: LLMs en Móvil y el RGPD** | Retos de memoria, latencia y gestión de datos de menores en educación. |
| 3 | **Arquitectura de HomeTutorPro** | Clean Architecture + MVVM + Jetpack Compose. Separación de capas. |
| 4 | **¿Qué es RAG Local y Agrupamiento Inteligente?** | Diferencia entre Fine-Tuning y RAG. Inyección de Room DB en Gemma con ventana de 2048 tokens y compresión relacional previa (agrupación de recursos si > 15). |
| 5 | **El Patrón Híbrido: Cuando NO usar el LLM** | Mostrar cómo resolver cancelaciones, huecos y saldos en 0 ms con Kotlin y dejar a Gemma solo para lenguaje natural y consultas compuestas. |
| 6 | **Ingeniería de Memoria en Android 15-17** | Retos de RAM, zRAM, páginas de 16 KB, validación de hardware (2.5 GB RAM) y solución de *Smart Auto-Release*. |
| 7 | **Demo en Vivo / Video de SUE** | Mostrar la app respondiendo a consultas por voz (agenda, materiales compartidos) y confirmando acciones en tiempo real. |
| 8 | **Conclusiones y Q&A** | La IA en el cliente ya es viable en Android hoy sin arruinarse en costes de API. |
