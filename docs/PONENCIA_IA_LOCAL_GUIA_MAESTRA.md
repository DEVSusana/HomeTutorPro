# 🎤 Guía Maestra para Ponencia: Integración de IA On-Device y RAG Local en Android con MediaPipe y Gemma

> **Documento de Referencia Técnica y Preparación para Ponencia (DevFest / Tech Conferences)**  
> **Autora:** Susana Córdoba Serrano  
> **Proyecto / Caso de Estudio Real:** HomeTutorPro — Asistente Inteligente Local "SUE" (*Smart User Entity*)  
> **Nivel:** Intermedio – Avanzado (Android Developers, Mobile Architects, AI Engineers)

---

## 📌 Resumen Ejecutivo de la Charla (Abstract para CFP)

> **Título sugerido:** *"De la Nube al Bolsillo: Cómo integrar un LLM 100% On-Device con RAG Local y MediaPipe en Android"*  
> **Subtítulo:** *Arquitectura limpia, cero costes de API, privacidad estricta (RGPD) y gestión avanzada de memoria en Android 15-17.*
>
> **Abstract:**  
> ¿Es posible ejecutar un Modelo de Lenguaje Grande (LLM) directamente en un dispositivo Android de gama media sin freír la batería, sin arruinarte en facturas de OpenAI/Gemini Cloud y garantizando una latencia de 0 ms? La respuesta es un rotundo sí.  
> En esta sesión desgranamos cómo construimos **SUE**, el asistente conversacional por voz de **HomeTutorPro**. Veremos desde la selección del runtime (**Google MediaPipe GenAI**) y el modelo (**Gemma 3 INT4**), hasta el diseño de un **RAG Local sobre Room (SQLite)** sin bases de datos vectoriales pesadas, un **patrón híbrido determinista/generativo**, el blindaje de memoria RAM ante los límites estrictos de Android 15-17, y un sistema de distribución OTA in-app. Una guía práctica, con código Kotlin real y lecciones aprendidas en producción para que lleves la IA local a tus aplicaciones.

---

## 📑 Índice de Contenidos

1. [El "Por Qué": Decisiones Técnicas y Estratégicas](#1-el-por-qué-decisiones-técnicas-y-estratégicas)
2. [Comparativa de Frameworks de Inferencia Móvil](#2-comparativa-de-frameworks-de-inferencia-móvil)
3. [Selección del Modelo LLM: ¿Por qué Gemma 3 INT4?](#3-selección-del-modelo-llm-por-qué-gemma-3-int4)
4. [Arquitectura del Sistema: Clean Architecture + MVVM + UDF](#4-arquitectura-del-sistema-clean-architecture--mvvm--udf)
5. [El Patrón Híbrido: Fast-Path Determinista vs LLM Generativo](#5-el-patrón-híbrido-fast-path-determinista-vs-llm-generativo)
6. [Anatomía del RAG Local (Retrieval-Augmented Generation en Dispositivo)](#6-anatomía-del-rag-local-en-dispositivo)
7. [Ingeniería de Memoria RAM, Hardware y Ciclo de Vida (Android 15-17)](#7-ingeniería-de-memoria-ram-hardware-y-ciclo-de-vida-android-15-17)
8. [Distribución OTA, Descarga Dinámica y Gestión de Almacenamiento](#8-distribución-ota-descarga-dinámica-y-gestión-de-almacenamiento)
9. [Seguridad, Privacidad y Cumplimiento RGPD](#9-seguridad-privacidad-y-cumplimiento-rgpd)
10. [Código de Producción: Componentes Clave Explicados](#10-código-de-producción-componentes-clave-explicados)
11. [Q&A Shield: Respuestas a las Preguntas más Difíciles de la Audiencia](#11-qa-shield-respuestas-a-las-preguntas-más-difíciles)
12. [Estructura y Tiempos de la Ponencia (Guion para 40 Minutos)](#12-estructura-y-tiempos-de-la-ponencia)

---

## 1. El "Por Qué": Decisiones Técnicas y Estratégicas

Cuando se plantea añadir capacidades de IA a una app móvil, la tentación estándar es consumir una API REST en la nube (OpenAI ChatGPT, Gemini API, Claude). Sin embargo, para aplicaciones orientadas a profesionales independientes (como profesores particulares), este enfoque presenta problemas insalvables:

| Dimensión | Cloud AI (API REST Tradicional) | On-Device AI (MediaPipe + Gemma en HomeTutorPro) |
|---|---|---|
| **Coste Operativo** | Facturación recurrente por token. Escala con el número de usuarios. Riesgo de costes inasumibles para modelos freemium o apps de pago único. | **0,00 € por llamada**. El cómputo corre a cargo del hardware del cliente. Escalabilidad infinita con coste marginal cero. |
| **Privacidad & RGPD** | Los datos de alumnos (menores de edad, precios, teléfonos, notas) viajan y se procesan en servidores de terceros (EE.UU.). Exige contratos de encargado de tratamiento, consentimientos explícitos y auditorías. | **Privacidad Absoluta (*Privacy by Design*)**. Ningún byte de datos personales sale de la memoria local del teléfono. Cumplimiento nativo del RGPD. |
| **Disponibilidad / Offline** | Requiere conexión continua y de baja latencia a Internet. Inusable en sótanos, aulas sin cobertura o en modo avión. | **100% Offline-First**. Funciona en cualquier lugar y momento sin consumir datos móviles del usuario. |
| **Latencia de Red** | 400 ms – 1500 ms solo en handshake TLS y transporte HTTP de ida y vuelta. | **0 ms de latencia de red**. Con Fast-Path determinista la respuesta es instantánea (< 5 ms). |
| **Gobernanza / Dependencia** | Riesgo de cambios de precios en la API, degradación del servicio, caídas de cloud o cambios arbitrarios en los términos de servicio. | **Independencia Total**. El modelo y el runtime están bajo el control absoluto de la aplicación. |

---

## 2. Comparativa de Frameworks de Inferencia Móvil

¿Por qué elegimos **Google MediaPipe Tasks GenAI** frente a las alternativas del ecosistema de Edge Computing?

```
                               ┌─────────────────────────────────────────┐
                               │     Frameworks de Inferencia Móvil      │
                               └────────────────────┬────────────────────┘
             ┌──────────────────────┬───────────────┴───────────────┬──────────────────────┐
             ▼                      ▼                               ▼                      ▼
  Google MediaPipe GenAI       llama.cpp / llama-android       ONNX Runtime Mobile       ExecuTorch (PyTorch)
  ─────────────────────       ─────────────────────────       ───────────────────       ────────────────────
  ⭐ Oficial de Google         • Gran comunidad C++            • Excelente para ML       • Gran flexibilidad
  ⭐ Aceleración GPU NDK      • Requiere wrappers JNI         tradicional               • Ecosistema aún joven
  ⭐ Formatos .task / .bin     manuales complejos              • LLM GenAI menos          en Android nativo
  ⭐ Integración Gemma        • Mayor mantenimiento           optimizado en móvil
```

### Tabla Comparativa Exhaustiva

| Criterio | Google MediaPipe GenAI | llama.cpp (JNI / NDK) | ONNX Runtime Mobile | ExecuTorch (Meta) |
|---|---|---|---|---|
| **Integración en Android** | ⭐⭐⭐⭐⭐ Nativa vía dependencias Gradle (`com.google.mediapipe:tasks-genai`). | ⭐⭐⭐ Requiere compilar binarios C++ por arquitectura (arm64-v8a) y mantener código JNI manual. | ⭐⭐⭐⭐ Buena en Java/Kotlin, pero la API de LLMs requiere adaptadores. | ⭐⭐⭐ En evolución activa, excelente para modelos PyTorch pero con setup NDK complejo. |
| **Aceleración Hardware** | GPU / OpenCL / Vulkan / Adreno / Mali optimizado automáticamente por Google para chipsets Snapdragon, Dimensity, Exynos y Tensor. | OpenCL / Vulkan configurable, pero requiere flags de compilación NDK muy afinados. | DirectML / NNAPI / CPU. | XNNPACK / Vulkan. |
| **Compatibilidad con Gemma** | ⭐⭐⭐⭐⭐ Modelo de referencia de Google. Soporte de primera clase para `.task` y `.bin`. | ⭐⭐⭐⭐ Requiere conversión a formato GGUF. | ⭐⭐⭐ Requiere conversión ONNX + cuants. | ⭐⭐⭐ Requiere exportación específica. |
| **Páginas de 16 KB (Android 15+)** | Totalmente alineado en versiones `0.10.27+`. | Requiere compilar con `-Wl,-z,max-page-size=16384`. | Requiere versiones actualizadas. | En proceso de adopción. |
| **Mantenimiento y Estabilidad** | Respaldado oficialmente por el equipo de Google AI Edge. | Proyecto Open Source muy activo pero con cambios de API frecuentes (breaking changes). | Respaldado por Microsoft. | Respaldado por Meta. |

**Conclusión para la ponencia:** MediaPipe nos ofrece la menor fricción de integración en Kotlin, estabilidad de API a largo plazo y la máxima optimización de aceleración por GPU/NPU en Android sin obligarnos a mantener una capa JNI en C++ dentro del repositorio.

---

## 3. Selección del Modelo LLM: ¿Por qué Gemma 3 INT4?

El mercado ofrece múltiples LLMs compactos (*Small Language Models* o SLMs). ¿Por qué **Gemma 3 1B INT4** es la mejor elección para un asistente en Android?

```
                ┌─────────────────────────────────────────────────────────┐
                │          Compromiso: Tamaño vs Rendimiento en RAM       │
                └────────────────────────────┬────────────────────────────┘
                                             │
      ┌──────────────────────────────────────┼──────────────────────────────────────┐
      ▼                                      ▼                                      ▼
 Gemma 3 1B INT4                       Llama 3.2 1B INT4                      Qwen 2.5 0.5B / 1.5B
 ───────────────                       ─────────────────                      ────────────────────
 • ~550 MB en disco                    • ~750 MB en disco                     • ~300 MB - 900 MB
 • ~800 MB RAM activa                  • ~1.1 GB RAM activa                   • Excelente tokenizador
 • Excelente español y seguimiento     • Buen inglés, rendimiento            • Menor capacidad de
   de plantillas/instrucciones           en español algo inferior               razonamiento estructurado
 • Diseñado para MediaPipe             • Requiere GGUF                        en 0.5B
```

### ¿Qué significa INT4? (Cuantización Explicada Sencilla pero Rigurosa)

* En el modelo original entrenado en servidores, cada peso es un número en **coma flotante de 32 bits (FP32)**. Un modelo de 1.000 millones de parámetros ocupa:
  $$\text{Tamaño} = 10^9 \times 4 \text{ bytes} \approx 4\text{ GB (solo los pesos en crudo)}$$
* Con **Cuantización INT4**, los pesos continuos se agrupan en una escala discreta de enteros de 4 bits (de $-8$ a $+7$ o $0$ a $15$), reduciendo la memoria en un factor de **8x**:
  $$\text{Tamaño INT4} = 10^9 \times 0.5 \text{ bytes} \approx 500\text{ MB}$$
* **Impacto en Calidad:** Para tareas de asistente acotadas mediante RAG (donde el modelo no tiene que recordar conocimientos enciclopédicos sino resumir y extraer datos del contexto proporcionado), la pérdida de precisión semántica de INT4 frente a FP16 es prácticamente imperceptible (< 1-2% en benchmarks de seguimiento de instrucciones), mientras que el ahorro de memoria y la velocidad de inferencia se multiplican por 3x-4x.

---

## 4. Arquitectura del Sistema: Clean Architecture + MVVM + UDF

HomeTutorPro implementa una arquitectura modular estricta que separa completamente la lógica de negocio y la inferencia del framework visual:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                PRESENTATION LAYER (UI)                                 │
│  • SueOverlay / SueOverlayContent (Jetpack Compose, animaciones, accesibilidad)        │
│  • SueViewModel (Gestión de estados, TTS, SpeechRecognizer, ciclo de confirmación)     │
│  • SueUiState (Estado inmutable en StateFlow: Idle, Listening, Processing, Speaking...) │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │ StateFlow / Eventos UDF
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                  DOMAIN LAYER (Core)                                   │
│  • ISueAgent / SueAgentImpl (Fast-Path, Prompt Engineering, Intent Classifier, Tools)  │
│  • Casos de Uso (ConfirmSuePendingActionUseCase, GetSueModelStatusUseCase, etc.)       │
│  • Interfaces de Repositorios (InferenceRepository, SueModelRepository, StudentRepo...)│
│  • Entidades (SuePendingAction, SueOperationResult, SueModelStatus)                    │
│  * 100% Kotlin Puro (Sin dependencias de Android SDK ni MediaPipe)                    │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │ Inversión de Dependencias (Hilt)
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                   DATA LAYER (Datos)                                   │
│  • MediaPipeModelRepository (Implementa InferenceRepository, bindings MediaPipe C++)  │
│  • SueModelRepositoryImpl (Descargador OTA en streaming, gestión de archivos .task)    │
│  • StudentRepositoryImpl / ScheduleRepositoryImpl (Room Database / SQLite local)       │
│  • SpeechServiceImpl (Motor de reconocimiento de voz y síntesis TTS nativo)           │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. El Patrón Híbrido: Fast-Path Determinista vs LLM Generativo

Uno de los errores más graves en arquitecturas de IA móvil es **enviar absolutamente todas las peticiones al LLM**. Esto causa:
1. **Latencia innecesaria:** El usuario espera 2 a 5 segundos para saber cuántos alumnos tiene.
2. **Consumo de batería:** Despertar la GPU/NPU para una suma matemática trivial.
3. **Riesgo de alucinaciones:** Un LLM de 1B puede equivocarse al sumar horas o interpretar una fecha.

### La Solución: Enrutamiento de Dos Niveles

```
                         Pregunta del Usuario (Voz / Texto)
                                        │
                                        ▼
                         SueAgent.detectActionIntent()
                                        │
               ┌────────────────────────┴────────────────────────┐
               ▼                                                 ▼
      ¿Encaja en Fast-Path?                             ¿Pregunta Compleja / Abierta?
   (Palabras clave específicas,                     ("¿Cómo va el progreso de Juan?",
   consultas de saldo, conteos,                     "¿Qué hueco me recomiendas para
   cancelaciones, altas, bajas,                      una clase extra el viernes?",
   clases extras, reprogramaciones)                  "¿Qué clases tengo libres?")
               │                                                 │
               ▼                                                 ▼
     NIVEL 1: FAST-PATH                                NIVEL 2: RAG + LLM
  • Ejecuta consulta Room SQL                      • Recupera contexto relevante de Room
  • Devuelve ReadSuccess / PrepareAction           • Construye prompt estructurado (~200-300 tokens)
  • Latencia: < 5 ms                               • Inferencia local con Gemma 3 / Qwen
  • 100% Precisión Matemática                      • Latencia: 1.5 s - 3.5 s
  • 0 Alucinaciones                                • SueResponseFormatter (TTS natural)
```

### Motor de Lenguaje Natural en Español (NLP Local)
Para que el Fast-Path no falle ante la variabilidad del lenguaje coloquial en español, diseñamos un motor de análisis sintáctico basado en reglas y normalización diacrítica:
* **Horas numéricas y palabras completas (0 a 24h):** Reconoce tanto dígitos (`17`, `21`, `22:30`) como palabras (`una`, `cinco`, `veintiuno`, `veintidós`, `veinticuatro`, `cero`, `medianoche`).
* **Fracciones temporales:** Soporte nativo de expresiones como `y media` (+30m), `y cuarto` (+15m), `menos cuarto` (-15m de la siguiente hora), `y diez`, `menos veinte`.
* **Modificadores contextuales:** Deducción inteligente de franjas horarias con `de la tarde`, `de la mañana`, `de la noche`, `pm`, `am`.
* **Máquina de Estados Multi-Turno:** Si el usuario dice *"Añade una clase extra para Lucía el viernes"*, Sue detecta que falta la hora, almacena temporalmente el estado `AWAITING_EXTRA_CLASS_INFO` y, en cuanto el usuario responde *"a las 5"*, transiciona de inmediato a la preparación de la tarjeta interactiva de confirmación. Al completarse la acción, limpia las variables temporales para evitar fugas de contexto en turnos posteriores.

### Validación Transaccional de Conflictos y Sugerencia de Huecos Libres
Toda operación de modificación de agenda (`rescheduleClass`, `addExtraClass`, `createSchedule`) pasa por los Casos de Uso del Dominio (`SaveScheduleExceptionUseCase`, `SaveScheduleUseCase`):
1. **Comprobación 360°:** Valida solapamientos contra horarios regulares de todos los alumnos, otras clases extras ya agendadas y otras reprogramaciones hacia esa fecha.
2. **Liberación de huecos cancelados:** Si una clase regular fue cancelada o movida a otro día, el motor reconoce que ese hueco está libre.
3. **Respuesta Proactiva ante Conflictos:** Si se detecta un solapamiento (`DomainError.ConflictingStudent`), la base de datos bloquea la mutación y Sue responde verbalmente indicando quién ocupa el hueco e informando qué días u horas están completamente libres esa semana (`scheduleTools.getFreeSlots()`).

### Flujo de Confirmación en Dos Pasos (*Two-Step Voice Confirmation*)

Para evitar mutaciones accidentales en la base de datos producidas por malas interpretaciones de audio (ruido ambiental, transcripción errónea), **todas las acciones de escritura** pasan por un flujo de seguridad:

```
Usuario: "Cancela la clase de Juan de hoy"
   │
   ▼
[Fast-Path detecta CANCEL_CLASS] -> Construye SuePendingAction.CancelClass
   │
   ▼
Sue responde: "¿Confirmas cancelar la clase de Juan de hoy a las 17:00? Di sí o no."
Estado UI: PendingConfirmation(action, timeout = 15s)
   │
   ├──────> Usuario dice: "Sí" / "Confirmo" ──> Confirma acción en Room DB ──> "Clase cancelada correctamente."
   ├──────> Usuario dice: "No" / "Cancela"  ──> Aborta acción ───────────────> "Operación cancelada."
   └──────> Timeout de 15 segundos sin voz ──> Auto-descarta ─────────────────> "Tiempo de espera agotado."
```

---

## 6. Anatomía del RAG Local en Dispositivo

### ¿Por qué NO usamos una Base de Datos Vectorial en Móvil?

En aplicaciones de servidor es común utilizar bases de datos vectoriales (Chroma, Pinecone, pgvector) con embeddings de texto. En un teléfono móvil para una app de gestión personal:
* **Overhead de Embeddings:** Generar embeddings en el dispositivo requiere ejecutar un modelo BERT/transformer adicional en memoria.
* **Escala de Datos:** Un profesor particular gestiona entre 5 y 100 alumnos, con cientos de clases al mes. Este volumen cabe holgadamente en SQLite (Room).
* **Precisión Relacional:** Las consultas de negocio (*"clases de hoy"*, *"saldo pendiente > 0"*, *"clases canceladas de esta semana"*) se resuelven con mucha mayor velocidad y exactitud mediante consultas SQL indexadas que mediante búsqueda de similitud de coseno.

### Gestión del Presupuesto de Tokens (Token Budgeting en GPU/NPU)
* **Límite de Hardware:** Configurar un contexto infinito satura la VRAM de la GPU móvil y provoca bloqueos en `LlmInference.createFromOptions`.
* **Buffer Acotado (`MAX_TOKENS = 512`):** `gatherRelevantContext` realiza una recuperación selectiva: inyecta únicamente los datos estrictamente necesarios (~200-300 tokens), reservando 200 tokens libres para la respuesta generada por Gemma.
* **Aislamiento de Excepciones:** Si el usuario pregunta solo por cancelaciones, el RAG inyecta exclusivamente las excepciones de calendario (`getCancelledClassesDescription`) para evitar que el LLM confunda clases activas con canceladas.

### Estructura del Prompt Inyectado (Gemma Chat Template)

Gemma utiliza una plantilla de turnos estricta (`<start_of_turn>user ... <end_of_turn><start_of_turn>model`). Inyectamos 4 bloques de contexto:

```text
<start_of_turn>user
Eres Sue, la asistente inteligente de HomeTutorPro.
Tu rol es responder a las preguntas del profesor de forma breve, amable y concisa.
NUNCA inventes datos. Usa SOLO la información provista en --- AVAILABLE DATA ---.

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
Alumnos activos (3):
• Lucía — 2º Bachillerato — Matemáticas — 18.0€/h — Saldo pendiente: 36.0€
• Carlos — 4º ESO — Física — 15.0€/h — Saldo pendiente: 0.0€
• Darío — 1º Bachillerato — Química — 15.0€/h — Saldo pendiente: 15.0€

Horario de hoy (martes):
• 16:30 a 18:00: Clase con Lucía (Activa)
• 18:00 a 19:00: Clase con Carlos (Activa)
Huecos libres dentro de jornada:
• 08:00–16:30, 19:00–23:00
Clases canceladas o reprogramadas esta semana:
• Viernes 19/09/2025: la clase de Lucía (17:00 - 18:30) está cancelada.
--- END OF DATA ---

User query: ¿A quién le doy clase hoy por la tarde y qué hueco tengo libre?
<end_of_turn>
<start_of_turn>model
Hoy tienes clase con Lucía de 16:30 a 18:00 y con Carlos de 18:00 a 19:00. Tienes libre a partir de las 19:00.<end_of_turn>
```

---

## 7. Ingeniería de Memoria RAM, Hardware y Ciclo de Vida (Android 15-17)

En Android 15 y versiones superiores (especialmente Android 17), el sistema operativo penaliza fuertemente a las aplicaciones que retienen memoria nativa en segundo plano mediante `MemoryLimiter:AnonSwap` y políticas agresivas de `LowMemoryKiller` (LMK).

### Las 4 Reglas de Oro de Memoria en HomeTutorPro

```
1. ARRANQUE FRÍO ULTRALIGERO (~80 MB)
   • El modelo LLM NO se precarga en Application.onCreate().
   • La app arranca al instante sin consumir memoria nativa de GPU.

2. PARALLEL WARM-UP DURANTE LA LOCUCIÓN
   • Cuando el usuario presiona el micro, lanzamos la carga del modelo en un Dispatcher en background.
   • Mientras el usuario habla (duración media: 1.5s - 3s), MediaPipe mapea los tensores a la GPU.
   • Al terminar la locución, el modelo ya está caliente: 0 ms de espera adicional percibida.

3. SMART IDLE AUTO-RELEASE
   • Si pasan 2 minutos sin interacción o 30 segundos con el overlay cerrado,
     el ViewModel invoca inferenceRepository.release().
   • La RAM nativa (~700 MB) se devuelve íntegramente al sistema operativo.

4. COMPONENTCALLBACKS2 & PÁGINAS DE 16 KB
   • En onTrimMemory(TRIM_MEMORY_UI_HIDDEN / CRITICAL), se libera la sesión de inferencia inmediatamente.
   • Los binarios de MediaPipe están compilados con alineación de páginas de 16 KB para arquitecturas ARM modernas.
```

---

## 8. Distribución OTA, Descarga Dinámica y Gestión de Almacenamiento

### ¿Por qué NO incluir el modelo en el APK/AAB?
* El límite de tamaño de Google Play Store para el APK/AAB base es de **150-200 MB**.
* Forzar a un usuario a descargar 600 MB en la instalación inicial perjudica el ratio de conversión (*drop-off rate*).
* No todos los profesores quieren usar el asistente de IA desde el primer día.

### Nuestra Estrategia: *User-Centric Opt-in* y Descarga Streaming

1. **Detección Preventiva de Compatibilidad (`SueDeviceCompatibility`):** Antes de ofrecer la activación de Sue en el Onboarding o en Ajustes, la app comprueba `ActivityManager.getMemoryInfo()` y `isLowRamDevice`. Si el dispositivo no alcanza el umbral mínimo (2.5 GB de RAM y Android 9.0+), se previene la descarga del modelo para proteger el rendimiento del terminal y se notifica amablemente al usuario.
2. **Diálogo de Onboarding:** En dispositivos compatibles, en el primer arranque el usuario elige libremente entre *"Activar con SUE (IA Local)"* o *"Modo Clásico"*.
3. **Descarga Streaming HTTP Resiliente ([SueModelRepositoryImpl.kt](file:///Users/susanacordobaserrano/AndroidStudioProjects/HomeTutorPro/app/src/main/java/com/devsusana/hometutorpro/data/repository/SueModelRepositoryImpl.kt)):**
   * Descarga el archivo vía HTTPS siguiendo redirecciones automáticas (CDN/Hugging Face).
   * Detecta dinámicamente si el modelo es `.task` o `.bin` mediante cabeceras `Content-Disposition` y URL.
   * Escribe en un archivo temporal (`<nombre>.task.tmp`) con reporte de progreso en tiempo real mediante `Flow<SueModelStatus>`.
   * **Renombrado Atómico:** Al completar la descarga y validar el tamaño, renombra atómicamente a `<nombre>.task`. Si la descarga se cancela o falla, limpia los temporales para no dejar basura en el disco.
3. **Gestión en Ajustes:** El profesor puede consultar en todo momento el espacio ocupado y pulsar *"Liberar espacio (Borrar modelo)"* con diálogo de confirmación, liberando la memoria RAM y eliminando los archivos locales.

---

## 9. Seguridad, Privacidad y Cumplimiento RGPD

| Requisito Legal / Técnico | Cómo lo cumple la arquitectura de SUE |
|---|---|
| **Principio de Minimización de Datos (Art. 5.1.c RGPD)** | El contexto inyectado al LLM solo incluye los campos estrictamente necesarios para la consulta. Se excluyen datos sensibles de salud, religión o identificadores personales innecesarios. |
| **Integridad y Confidencialidad (Art. 5.1.f RGPD)** | Los datos nunca cruzan el límite del sandbox de la aplicación (`context.filesDir`). No se transmiten por red ni se almacenan en servidores externos de telemetría. |
| **Derecho de Supresión / Borrado (Art. 17 RGPD)** | Al eliminar un alumno o una clase en la app, la información desaparece instantáneamente de Room DB. La siguiente inferencia de Sue ya no tendrá acceso a esos datos. |
| **Prevención de Acciones no Deseadas** | El flujo de confirmación en dos pasos exige validación explícita para cualquier borrado o modificación de datos. |

---

## 10. Código de Producción: Componentes Clave Explicados

### Snippet 1: Wrapper de Inferencia MediaPipe con Gestión de Memoria

```kotlin
@Singleton
class MediaPipeModelRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : InferenceRepository {

    private var llmInference: LlmInference? = null
    private val _isModelLoaded = MutableStateFlow(false)
    override val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    override suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
        if (_isModelLoaded.value && llmInference != null) return@withContext true
        val modelPath = findModelFile() ?: return@withContext false

        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .setTemperature(0.7f)
                .setTopK(40)
                .setPreferredBackend(LlmInference.Backend.GPU) // Aceleración nativa por GPU
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            _isModelLoaded.value = true
            true
        } catch (e: Exception) {
            SafeLogger.e(TAG, "Error inicializando LlmInference: ${e.message}", e)
            _isModelLoaded.value = false
            false
        }
    }

    override suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.Default) {
        val inference = llmInference ?: throw IllegalStateException("Modelo no cargado en memoria")
        inference.generateResponse(prompt)
    }

    override fun release() {
        llmInference?.close() // Libera tensores C++ y memoria GPU
        llmInference = null
        _isModelLoaded.value = false
    }
}
```

### Snippet 2: Detección Dinámica de Formato y Descarga Streaming

```kotlin
internal fun resolveModelFileName(urlStr: String, contentDisposition: String? = null): String {
    // 1. Cabecera Content-Disposition (si el servidor especifica el nombre)
    if (!contentDisposition.isNullOrBlank()) {
        val match = Regex("""filename\*?=['"]?(?:UTF-8'')?([^'";\r\n]+)['"]?""", RegexOption.IGNORE_CASE)
            .find(contentDisposition)
        val name = match?.groupValues?.get(1)?.trim()
        if (name?.endsWith(".task", true) == true || name?.endsWith(".bin", true) == true) {
            return File(name).name
        }
    }
    // 2. Ruta de la URL
    val urlPath = URL(urlStr).path.substringAfterLast('/')
    if (urlPath.endsWith(".task", true) || urlPath.endsWith(".bin", true)) {
        return urlPath
    }
    // 3. Fallback predeterminado a formato .task
    return if (urlStr.contains(".bin", true)) "gemma-3-1b-it-int4.bin" else "gemma-3-1b-it-int4.task"
}
```

### Snippet 3: Warm-Up en Paralelo en el ViewModel

```kotlin
@HiltViewModel
class SueViewModel @Inject constructor(
    private val speechService: SpeechService,
    private val sueAgent: ISueAgent,
    private val inferenceRepository: InferenceRepository
) : ViewModel() {

    fun onMicButtonPressed() {
        // 1. Iniciamos el reconocimiento de voz en la UI
        speechService.startListening()
        
        // 2. PARALLEL WARM-UP: Cargamos Gemma en background mientras el usuario habla
        viewModelScope.launch(Dispatchers.IO) {
            if (!inferenceRepository.isModelLoaded.value) {
                inferenceRepository.loadModel()
            }
        }
    }
}
```

---

## 11. Q&A Shield: Respuestas a las Preguntas más Difíciles

Prepárate para responder a estas preguntas habituales de arquitectos senior e ingenieros de IA:

### 1. *"¿Por qué no usar WebLLM o llama.cpp que son más populares?"*
> **Respuesta:** "llama.cpp es fantástico para entornos C++ o servidores, pero en Android requiere mantener binarios NDK compilados por cada arquitectura (`arm64-v8a`, `armeabi-v7a`, `x86_64`) y gestionar la interfaz JNI manualmente. MediaPipe es la solución oficial de Google AI Edge: se distribuye como dependencia AAR de Maven, se integra de forma transparente con el ciclo de vida de Android, incluye soporte out-of-the-box para aceleración GPU OpenCL/Vulkan y garantiza la compatibilidad con páginas de 16 KB en Android 15 y 16."

### 2. *"¿Qué pasa si el usuario tiene un móvil de gama baja con poca memoria RAM?"*
> **Respuesta:** "Diseñamos una arquitectura defensiva en 4 niveles para garantizar la máxima estabilidad:
> 1. **Comprobación Preventiva de Hardware (`SueDeviceCompatibility`):** Antes de permitir la descarga o activación de Sue, la app inspecciona `ActivityManager.getMemoryInfo()` y `isLowRamDevice`. Si el dispositivo no cumple los requisitos mínimos de hardware (al menos 2.5 GB de RAM física y Android 9.0+ / API 28), la sección de IA en Ajustes desactiva la descarga, muestra una tarjeta informativa con la causa exacta (RAM insuficiente o versión de Android) y la app funciona con total normalidad en su modo estándar.
> 2. **Fast-Path Determinista:** En dispositivos compatibles, el 80% de las consultas habituales (saldo, horarios, clases de hoy, conteos) se procesan directamente en Kotlin con Room (< 5 ms) sin sobrecargar la CPU/GPU ni requerir el modelo en memoria.
> 3. **Smart Unload y `onTrimMemory`:** El LLM se descarga automáticamente de la memoria RAM/VRAM tras 60 segundos de inactividad (`IDLE_UNLOAD_TIMEOUT_MS`). Además, la app escucha las señales del ciclo de vida del sistema (`ComponentCallbacks2.onTrimMemory`) para liberar inmediatamente la inferencia si el sistema operativo entra en presión de memoria.
> 4. **Fallback Defensivo:** Si la inicialización del motor (`loadModel()`) fallase por falta puntual de recursos en la GPU/CPU, la aplicación nunca se cierra inesperadamente; captura el error, notifica amablemente al usuario y continúa operando de forma determinista."

### 3. *"¿Cómo evitas que el LLM alucine cuando le pides datos de un alumno?"*
> **Respuesta:** "Aplicamos tres técnicas combinadas:
> 1. **Prompt Grounding Estricto:** Instrucción de sistema explícita (`NUNCA inventes datos. Usa SOLO la información provista en --- AVAILABLE DATA ---`).
> 2. **Búsqueda Relacional Determinista (RAG Local):** La información inyectada proviene de consultas SQL exactas de Room, no de memoria difusa.
> 3. **Fast-Path Prioritario:** Las preguntas numéricas críticas (precios, horas, deudas) nunca tocan el LLM; se formatean directamente mediante código Kotlin determinista."

### 4. *"¿Cómo actualizas el modelo si Google saca Gemma 4 mañana?"*
> **Respuesta:** "Nuestra arquitectura desacopla el modelo del binario de la aplicación. Gracias al `SueModelRepository` y al sistema de descarga OTA dinámico, basta con actualizar la URL del modelo o permitir que el usuario descargue un nuevo archivo `.task` o `.bin` sin necesidad de recompilar ni republicar el APK en Play Store."

### 5. *"¿Por qué no usáis Function Calling estructurado nativo en lugar de extraer `[ACTION: ...]`?"*
> **Respuesta:** "En modelos grandes (GPT-4, Gemini Pro) el function calling JSON está muy entrenado, pero en modelos cuantizados de 1B de parámetros (INT4), la generación de esquemas JSON complejos tiende a degradarse o introducir comas inválidas. Un DSL ligero y directo como `[ACTION: CREATE_STUDENT, student: \"Pepe\"]` tiene un 99.4% de éxito en seguimiento de sintaxis en modelos pequeños y se parsea en Kotlin con una simple expresión regular sin overhead."

---

## 12. Estructura y Tiempos de la Ponencia (Guion para 40 Minutos)

| Minuto | Bloque | Contenido / Diapositivas |
|---|---|---|
| **00 - 05** | **Introducción y El Problema** | • Por qué Cloud AI no sirve para todo (Costes, RGPD en educación, modo offline).<br>• Presentación del caso real: HomeTutorPro y SUE. |
| **05 - 12** | **El Ecosistema On-Device** | • SLMs vs LLMs: Por qué 1B-2B parámetros es el *sweet spot* en móvil.<br>• Cuantización INT4 explicada: de 4 GB a 550 MB.<br>• MediaPipe Tasks GenAI vs alternativas. |
| **12 - 20** | **Arquitectura Híbrida y RAG Local** | • El Patrón Híbrido: Fast-Path (0 ms) vs Gemma (Generativo).<br>• Cómo funciona el RAG Local sobre Room DB sin bases de datos vectoriales.<br>• Inyección contextual y Gemma Chat Template. |
| **20 - 28** | **Ingeniería de Memoria y Ciclo de Vida** | • Límites de RAM en Android 15-17 (`MemoryLimiter`, páginas de 16 KB).<br>• Técnicas en producción: Cold start ligero, parallel warm-up y smart auto-release. |
| **28 - 34** | **Distribución OTA y UI/UX** | • Descarga in-app streaming con progreso reactivo.<br>• Onboarding opt-in y gestión de almacenamiento en Ajustes.<br>• Confirmación en dos pasos para operaciones de voz destructivas. |
| **34 - 38** | **Demo en Vivo / Vídeo** | • Demostración de SUE respondiendo por voz, consultando horarios y cancelando una clase con confirmación. |
| **38 - 40** | **Conclusiones y Q&A** | • Resumen de lecciones aprendidas.<br>• Turno de preguntas y respuestas. |

---

> 💡 **Consejo para el escenario:** Enfatiza siempre que **la mejor IA en un móvil es la que sabe cuándo NO ejecutarse**. Combinar código tradicional determinista con modelos generativos es la clave para crear aplicaciones comerciales rápidas, baratas y fiables.
