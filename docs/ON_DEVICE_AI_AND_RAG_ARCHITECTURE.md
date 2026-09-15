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
             │     ├── Agenda del día/semana y huecos libres (getFreeSlots)
             │     ├── Excepciones activas: canceladas y reprogramadas (getCancelledClassesDescription)
             │     └── Ficha de alumnos: cursos, tarifas, saldos y notas pedagógicas
             ├── Construcción de Prompt estructurado (Gemma Chat Template, acotado a ~200-300 tokens)
             ├── Inferencia local con MediaPipe Tasks GenAI (Gemma 3 / Qwen)
             └── SueResponseFormatter -> TTS (Audio saneado con pausas y puntuación natural)
```

### Gestión del Presupuesto de Tokens (Token Budgeting en GPU/NPU)
Los dispositivos móviles tienen límites estrictos de memoria de vídeo (VRAM). Enviar el dump completo de la base de datos al LLM desbordaría la memoria y provocaría congelamientos en el subsistema gráfico.
- **Inyección Contextual Selectiva:** `gatherRelevantContext` solo extrae las tablas y registros vinculados a la intención detectada (evitando sobrecargar el prompt con alumnos no relacionados).
- **Buffer Acotado (`MAX_TOKENS = 512`):** El tamaño del contexto inyectado se mantiene en ~200-300 tokens, dejando un margen holgado de 200 tokens para la respuesta generada por el LLM.

### Estructura del Prompt Inyectado (Gemma Chat Template)
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
Horario del martes:
  16:30–18:00: Lucía Moreno García
  18:00–19:00: Carlos
Huecos libres dentro de jornada:
  08:00–16:30, 19:00–23:00
Clases canceladas o reprogramadas esta semana:
• Viernes 19/09/2025: la clase de Lucía (17:00 - 18:30) está cancelada.
--- END OF DATA ---

User query: ¿Qué clases tengo hoy y qué tengo libre por la tarde?
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
  - Máquina de estados conversacional multi-turno (ej. pedir hora tras solicitar clase extra) con reinicio automático de variables temporales al confirmar la acción.
- **Prevención Estricta de Colisiones Horarias:**
  - Al reprogramar o añadir clases extras, `SaveScheduleExceptionUseCase` valida solapamientos contra horarios regulares, clases extras y reprogramaciones existentes.
  - Si hay colisión, bloquea la mutación y SUE informa proactivamente del alumno en conflicto y los días/huecos libres disponibles.

### 2. Nivel 2: Capa Generativa (RAG + Inferencia Local con Gemma)
- **Casos de uso:** Consultas complejas, abiertas, pedagógicas o conversacionales (*"¿Cómo debería organizar el repaso de matemáticas con Carlos?"*, *"Explícame qué horario tengo más despejado esta semana para meter más alumnos"*).
- **Funcionamiento:** Se alimenta del contexto estructurado extraído en el paso RAG para que el modelo redacte una respuesta natural, veraz y personalizada.

---

## 7. Ingeniería de Memoria RAM y Ciclo de Vida (Android 17)

En Android 17, Google introduce **límites estrictos de memoria por aplicación (*Per-App Memory Limits*)** y mata procesos que excedan su cuota (`MemoryLimiter:AnonSwap`).

### Nuestras 4 Estrategias de Optimización:

1. **Arranque Frío Ultraligero:** La app inicia consumiendo únicamente **~80 MB** de RAM (no se precarga el modelo al arrancar).
2. **Carga en Paralelo durante la Locución:** Al pulsar el botón de voz, Gemma se carga en un hilo secundario en paralelo mientras el usuario habla. Cuando el usuario se calla, el modelo ya está listo (**0 ms de espera adicional percibida**).
3. **Smart Idle Auto-Release:** Tras 2 minutos de inactividad o 30 segundos tras cerrar el asistente, el ViewModel descarga el modelo de la RAM mediante `inferenceRepository.release()`.
4. **Protección en Segundo Plano (`onTrimMemory`):** Al minimizar la app (`TRIM_MEMORY_UI_HIDDEN`), se libera inmediatamente la memoria nativa de GPU/RAM.

---

## 8. Seguridad, Privacidad y RGPD

1. **Privacidad por Diseño (*Privacy by Design*):** Ningún dato de los alumnos sale del teléfono. No hay servidores intermedios, ni logs en la nube, ni venta de telemetría.
2. **Minimización de Datos:** En el MVP, el modelo de datos de `Student` excluye campos sensibles (como diagnósticos médicos o problemas de aprendizaje) para cumplir con el principio de minimización de datos del RGPD.
3. **Confirmación en Dos Pasos para Escrituras:** Toda modificación destructiva en la base de datos (cancelar clase, borrar alumno, registrar pago) pasa por un estado `Prepare` donde Sue pide confirmación verbal explícita (*"¿Confirmas cancelar la clase de Pepe? Di sí o no"*), evitando acciones accidentales por malas interpretaciones de audio.

---

## 9. Guion y Diapositivas

Si presentas esta arquitectura, esta es una estructura ganadora de 30-40 minutos:

| # | Título de la Diapositiva | Puntos Clave a Explicar |
|---|---|---|
| 1 | **La Era de la IA en el Dispositivo (Edge AI)** | Por qué no todo debe estar en la nube (Privacidad, Costes de Servidor = 0€, Modo Offline). |
| 2 | **El Problema: LLMs en Móvil y el RGPD** | Retos de memoria, latencia y gestión de datos de menores en educación. |
| 3 | **Arquitectura de HomeTutorPro** | Clean Architecture + MVVM + Jetpack Compose. Separación de capas. |
| 4 | **¿Qué es RAG Local?** | Diferencia entre Fine-Tuning y RAG. Cómo inyectar Room DB en el contexto de Gemma. |
| 5 | **El Patrón Híbrido: Cuando NO usar el LLM** | Mostrar cómo resolver cancelaciones y saldos en 0 ms con Kotlin y dejar a Gemma solo para lenguaje natural. |
| 6 | **Ingeniería de Memoria en Android 17** | Retos de RAM, zRAM, páginas de 16 KB y nuestra solución de *Smart Auto-Release*. |
| 7 | **Demo en Vivo / Video de SUE** | Mostrar la app respondiendo a consultas por voz y confirmando acciones en tiempo real. |
| 8 | **Conclusiones y Q&A** | La IA en el cliente ya es viable en Android hoy sin arruinarse en costes de API. |
