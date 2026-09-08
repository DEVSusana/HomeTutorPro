# Guía de Implementación del Asistente IA Offline (Sue)

Esta guía explica paso a paso cómo está construido el asistente inteligente "Sue" en HomeTutorPro. Su propósito es servir de referencia para un desarrollador Junior (o cualquier persona con conocimientos en Android, Kotlin, Corrutinas y Hilt) que desee replicar este sistema —o construir uno similar— en otra aplicación, entendiendo **el porqué** de las decisiones arquitectónicas tomadas.

## 1. Arquitectura General y Decisiones de Diseño

El sistema de Sue funciona **100% offline (on-device)**. No utiliza APIs en la nube (como OpenAI o Google Cloud) para procesar el lenguaje, y todo se ejecuta de manera determinista combinando varias tecnologías. Las principales razones son la **privacidad de datos** (no se envían nombres ni finanzas de los estudiantes a la nube), el **ahorro de costes**, y asegurar que el profesor puede usarla sin conexión.

El flujo de trabajo es el siguiente:
1. **Interfaz de Voz (STT):** El `SpeechRecognizer` nativo de Android captura la voz del usuario y la convierte a texto.
2. **Reconocimiento de Intenciones (Determinista):** Se analiza el texto usando lógica estructurada para encontrar acciones ("cancela la clase", "añade saldo") antes de llamar al modelo generativo.
3. **Retrieval-Augmented Generation (RAG):** Se extrae de la base de datos (SQLite/Room cifrado con SQLCipher) únicamente la información relevante para la intención del usuario.
4. **Construcción del Prompt (Prompt Engineering):** Se construye un texto que combina el contexto, las reglas de la IA, y los datos inyectados de SQLite.
5. **Inferencia Local (LLM):** **MediaPipe LLM Inference** carga un modelo **Gemma 3** (en formato `.bin` o `.task`) para generar una respuesta o estructurar una acción (ej. devolver un JSON-like string `[ACTION: CANCEL_CLASS...]`).
6. **Respuesta por Voz (TTS):** El modelo devuelve un texto, que luego se lee usando el `TextToSpeech` nativo de Android.

---

### Diagrama de Flujo (ASCII)

```text
 ┌─────────────────┐      1. STT      ┌──────────────────────────┐
 │ Usuario (Voz)   ├─────────────────►│ SpeechService (Android)  │
 └─────────────────┘                  └────────────┬─────────────┘
                                                   │ 2. Texto
                                                   ▼
 ┌─────────────────┐      3. RAG      ┌──────────────────────────┐
 │ Room + SQLCipher│◄─────────────────┤ SueAgentImpl (Agente)    │
 │ (BBDD Local)    ├─────────────────►│ (Intenciones + Contexto) │
 └─────────────────┘   4. Datos       └────────────┬─────────────┘
                                                   │ 5. Prompt Enriquecido
                                                   ▼
 ┌─────────────────┐      6. Generar  ┌──────────────────────────┐
 │ Gemma 3 (.bin)  │◄─────────────────┤ MediaPipeModelRepository │
 └─────────────────┘      (MediaPipe) └────────────┬─────────────┘
                                                   │ 7. Respuesta / Acción
                                                   ▼
 ┌─────────────────┐     8. Hablar    ┌──────────────────────────┐
 │ Altavoz / UI    │◄─────────────────┤ SueViewModel (UI State)  │
 └─────────────────┘                  └──────────────────────────┘
```

---

## 2. Paso a Paso y Explicación de Clases Clave

A continuación, detallamos cada bloque del sistema y qué clases debes consultar.

### Paso 1: Interfaz de Voz (STT y TTS)

**Por qué:** Para interactuar como un verdadero "asistente", la app necesita escuchar (Speech-to-Text) y hablar (Text-to-Speech) sin depender de librerías externas de pago. Se usan las herramientas nativas del SDK de Android.

**Dónde mirar:**
- `domain/repository/SpeechService.kt` (Interfaz)
- `data/repository/SpeechServiceImpl.kt` (Implementación)

**Detalles técnicos:**
La implementación envuelve `SpeechRecognizer` y `TextToSpeech` de Android en flujos de Kotlin (`StateFlow` y `SharedFlow`). El ViewModel arranca el micro, captura texto en tiempo real (`partialTranscriptions`) y final (`transcriptions`), y luego reproduce la respuesta vía `speak(text)`.

### Paso 2: Sistema Determinista (RAG con Base de Datos) - Profundidad

**¿Qué es RAG?** RAG significa *Retrieval-Augmented Generation* (Generación Aumentada por Recuperación). Como nuestro modelo LLM local (Gemma 3) no conoce la base de datos (no sabe quiénes son tus alumnos), la técnica RAG consiste en **recuperar** los datos relevantes de tu base de datos y pasárselos como contexto al LLM.

**¿Por qué Determinista?** Los sistemas RAG tradicionales usan búsquedas vectoriales y embeddings pesados. Como estamos en un entorno móvil offline con poca memoria, optamos por un RAG **determinista o basado en reglas**. Analizamos la frase del usuario con expresiones regulares y palabras clave exactas para extraer parámetros e inyectar sólo los datos necesarios, evitando colapsar la memoria del modelo (Out of Memory).

**Dónde mirar:**
- `domain/usecases/implementations/SueAgentImpl.kt` (El cerebro de las reglas)
- `domain/usecases/implementations/StudentTools.kt` y `ScheduleTools.kt` (Conexiones a Room)

**Cómo funciona paso a paso (El corazón del RAG):**

1. **Extracción de Entidades:** El método `gatherRelevantContext(query)` recibe la frase del usuario y extrae días, nombres y tiempos.
   ```kotlin
   // Intenta encontrar el nombre de un alumno basándose en lo que hay en BBDD
   val studentName = extractStudentName(lowerQuery)
   // Extrae días de la semana (ej: de "mañana" deduce "Martes")
   val day = extractRelativeDayOfWeek(lowerQuery) ?: extractDayOfWeek(lowerQuery)
   ```

2. **Detección de Intenciones (Keywords):** Luego busca palabras clave en el texto.
   ```kotlin
   val isFinanceQuery = listOf("ganado", "ingresos", "facturado", "cobrado").any { it in lowerQuery }
   ```

3. **Recuperación en BBDD (Retrieval):** Si detecta que preguntas por finanzas y detectó un nombre, consulta a SQL/Room mediante las Tools, **limitando el contexto**.
   ```kotlin
   // Si el nombre es "Pepe" y pregunta por clases completadas
   val logs = studentTools.getClassLogs("Pepe")

   if (logs.isNotEmpty()) {
       appendLine("--- CLASES COMPLETADAS PARA Pepe ---")
       logs.forEach { l ->
           appendLine("- Clase del ${l.date} a las ${l.startTime} a ${l.endTime}")
       }
       appendLine("--- FIN CLASES COMPLETADAS ---")
   }
   ```
   **La Magia:** Si le preguntas "¿Cuánto me debe Pepe?", el código sólo extrae de SQLite el perfil de Pepe y su deuda. El modelo Gemma 3 **sólo lee esto** y no sabe nada de los demás 50 alumnos que tengas, por tanto la respuesta será rapidísima y consumirá muy poca memoria RAM.

### Paso 3: Construcción del Prompt (Prompt Engineering)

**Por qué:** El LLM necesita un marco estricto. Construimos una cadena de texto gigante (`buildPromptWithContext`) que se pasa al LLM. En ella juntamos el rol (System Prompt), la fecha actual, el contexto que nos dio el RAG y la pregunta del usuario.

**Dónde mirar:**
- `SueAgentImpl.kt` (función `buildSystemPrompt` y `buildPromptWithContext`)

**Así es cómo le llega al LLM (El Prompt Final):**
```text
<start_of_turn>user
Eres Sue, la asistente inteligente de HomeTutorPro.
Tu rol es ayudar a profesores particulares a gestionar su trabajo.

INSTRUCCIÓN MUY IMPORTANTE: Si es una acción, devuelve UNA LÍNEA EXACTA:
[ACTION: TIPO, param1: valor]

--- TEMPORAL CONTEXT ---
Current date/time: Martes, 15 Octubre 2026, 17:00
--- END OF TEMPORAL CONTEXT ---

--- AVAILABLE DATA --- (¡Esto lo inyectó el RAG en el Paso 2!)
--- SALDOS PENDIENTES ---
- Pepe: 30.00 euros pendientes
--- END OF DATA ---

User query: ¿Cuánto dinero me debe Pepe?
<end_of_turn>
<start_of_turn>model
```

### Paso 4: Inferencia Local con Gemma 3 y MediaPipe

**Por qué:** Google ofrece **MediaPipe LLM Inference** para ejecutar modelos en el móvil sin necesidad de internet (On-Device). Usamos un modelo Gemma 3 en formato `.bin`.

**Dónde mirar:**
- `data/repository/MediaPipeModelRepository.kt`

**Detalles técnicos:**
Esta clase carga el archivo `.bin` en memoria. Al inyectar el Prompt gigante del Paso 3 mediante `session.addQueryChunk(prompt)` y pedir la respuesta `session.generateResponse()`, el modelo procesa la información de forma nativa.

*Decisión de Memoria:* Implementamos `ComponentCallbacks2` en este repositorio. Si Android nos manda un aviso `onLowMemory` (porque el móvil se queda sin RAM), la instancia de MediaPipe se destruye automáticamente, evitando que Android mate toda la aplicación. El modelo se volverá a cargar sólo cuando el usuario pulse el micrófono.

### Paso 5: Orquestación (ViewModel y Acciones Pendientes)

**Por qué:** Es muy peligroso que una IA tenga permiso para escribir, borrar o alterar bases de datos por sí sola (alucinaciones). Por tanto, la respuesta que emite el modelo (ej. `[ACTION: DELETE_STUDENT, student: "Pepe"]`) no va directa a SQLite.

**Dónde mirar:**
- `presentation/sue/SueViewModel.kt`

**Detalles técnicos:**
En `SueViewModel`, el flujo conecta el `SpeechService` con el Agente (`SueAgentImpl`), y luego parsea la respuesta:
1. **Respuesta Lectura:** Si el LLM devuelve un texto normal (ej. *"Pepe te debe 30 euros"*), el ViewModel invoca el TTS y el altavoz suena (`speechService.speak(it)`).
2. **Respuesta Acción:** Si el LLM devuelve el patrón `[ACTION: ...]`, el método `parseLlmActionResponse` en `SueAgentImpl` genera un objeto sellado en Kotlin llamado `SuePendingAction.DeleteStudent`. El ViewModel intercepta esto, muta su estado a "A la espera de confirmación", mostrando en la interfaz un cuadro para que el humano pulse "Aceptar" o "Cancelar".

## Resumen: Cómo replicar en otro proyecto

Si quieres llevar esto a otra App:
1. Añade la librería de MediaPipe en tu `build.gradle` (`com.google.mediapipe:tasks-genai`).
2. Copia y adapta el **Paso 1** (`SpeechServiceImpl.kt`) para tener STT y TTS nativo.
3. Copia el **Paso 4** (`MediaPipeModelRepository.kt`) para poder cargar tu modelo `.bin`.
4. **Crea tu propio RAG Determinista:** Adapta `SueAgentImpl.kt`. Tu aplicación tendrá otra base de datos (ej. recetas de cocina en vez de horarios de profesores). Tendrás que escribir tus propios detectores (*keywords* como "receta", "pollo"), consultar tu BBDD usando Room, y volcar el texto como "Contexto" dentro de un String para armar el Prompt final.
5. Gestiona de forma segura los `[ACTION: ...]` pidiendo siempre confirmación antes de mutar tu BBDD.

¡Y eso es todo! Has construido un asistente inteligente privado, altamente seguro, y 100% offline.
