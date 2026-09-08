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
La implementación envuelve `SpeechRecognizer` y `TextToSpeech` de Android en flujos de Kotlin (`StateFlow` y `SharedFlow`).

*Ejemplo (fragmento):*
```kotlin
// SpeechServiceImpl.kt lanza el reconocimiento de forma asíncrona
val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
}
speechRecognizer?.startListening(intent)
```

### Paso 2: Sistema Determinista (RAG con Base de Datos)

**Por qué:** Un LLM que se ejecuta en un teléfono móvil tiene un límite estricto de contexto (memoria RAM). Si le mandamos toda la base de datos, colapsará (Out of Memory). Por eso se usa un sistema *Retrieval-Augmented Generation (RAG)* "determinista": primero se usa lógica (expresiones regulares o keywords) para detectar de qué está hablando el usuario (ej. si menciona "finanzas", "saldo", o el nombre "María"). Solo entonces se consulta a la base de datos **Room** y se extrae esa información específica.

**Dónde mirar:**
- `domain/usecases/implementations/SueAgentImpl.kt` (Core del Agente)
- `domain/usecases/implementations/StudentTools.kt` y `ScheduleTools.kt` (Herramientas SQL)

**Detalles técnicos:**
En `SueAgentImpl`, el método `gatherRelevantContext` intercepta la consulta. Fíjate en cómo busca *keywords*:

*Ejemplo (fragmento):*
```kotlin
// SueAgentImpl.kt
private fun containsBalanceKeywords(query: String) =
    listOf("saldo", "balance", "deuda", "dinero", "cobrar", "pagar").any { it in query }

// Si detecta la keyword, usa las "Tools" para hacer la query en Room
if (containsBalanceKeywords(lowerQuery)) {
    // Inyecta el resultado formateado al prompt
    appendLine(formatResult(studentTools.getStudentsWithBalance()))
}
```

### Paso 3: Construcción del Prompt (Prompt Engineering)

**Por qué:** El LLM (Gemma) es un modelo generalista. Para que se comporte como la asistente "Sue" y no alucine datos, se le pasa un "System Prompt" estricto. Además, se le ordena que cuando deba hacer una acción (ej. cancelar una clase) devuelva un formato estandarizado que nuestra app pueda interpretar.

**Dónde mirar:**
- `SueAgentImpl.kt` (función `buildSystemPrompt` y `buildPromptWithContext`)

*Ejemplo (fragmento):*
```kotlin
// Se obliga al LLM a escupir una acción parametrizada
"""
INSTRUCCIÓN MUY IMPORTANTE (EXTRACCIÓN DE INTENCIONES):
Si la frase del usuario requiere ejecutar una acción en la app (crear, modificar o borrar clases, estudiantes o pagos), debes devolver UNA ÚNICA LÍNEA con el siguiente formato exacto:
[ACTION: TIPO_DE_ACCION, parametro1: valor, parametro2: valor]

Ejemplo:
Usuario: "Cancela la clase de María del viernes."
Tú: [ACTION: CANCEL_CLASS, student: "María", day: "viernes"]
"""
```

### Paso 4: Inferencia Local con Gemma 3 y MediaPipe

**Por qué:** Para ejecutar el modelo en el propio teléfono (On-Device), Google ofrece **MediaPipe LLM Inference**. Es altamente eficiente para correr modelos como Gemma 3. No usamos frameworks externos ("Koog" o APIs en la nube) por temas de privacidad de los alumnos de HomeTutorPro. El modelo se carga en memoria solo cuando es necesario.

**Dónde mirar:**
- `data/repository/MediaPipeModelRepository.kt`

**Detalles técnicos:**
Esta clase carga un archivo de modelo (`.bin` o `.task`) almacenado de forma segura en el almacenamiento interno de la App (`context.filesDir`).

*Ejemplo (fragmento):*
```kotlin
// MediaPipeModelRepository.kt
val options = LlmInference.LlmInferenceOptions.builder()
    .setModelPath(modelPath) // ej: /data/user/0/.../files/sue_model/gemma-3-2b-cpu.bin
    .setMaxTokens(2048)
    .build()

llmInference = LlmInference.createFromOptions(context, options)
```
*Decisión importante:* Presta atención a `ComponentCallbacks2` en esa clase. Si el sistema Android avisa de poca memoria (`onLowMemory`), la instancia de MediaPipe se destruye automáticamente para evitar que Android cierre toda la app.

### Paso 5: Orquestación (ViewModel y Acciones Pendientes)

**Por qué:** El LLM puede decidir borrar una clase, pero por seguridad, no lo hace automáticamente. Retorna la intención, el `SueAgentImpl` la parsea a un objeto sellado (`SuePendingAction`), y el `SueViewModel` expone esto a la UI para pedir confirmación al usuario antes de modificar Room.

**Dónde mirar:**
- `presentation/sue/SueViewModel.kt`

**Detalles técnicos:**
En `SueViewModel`, el flujo conecta el `SpeechService` con el Agente (`SueAgentImpl`), y luego evalúa la respuesta:
1. Si el LLM devuelve un texto normal, invoca el TTS (`speechService.speak(it)`).
2. Si el LLM devuelve un `[ACTION: ...]`, el ViewModel muta su estado a "A la espera de confirmación".

## Resumen: Cómo replicar en otro proyecto

Si quieres llevar esto a otra App:
1. Añade la librería de MediaPipe en tu `build.gradle` (`com.google.mediapipe:tasks-genai`).
2. Copia y adapta el **Paso 1** (`SpeechServiceImpl.kt`) si necesitas la interfaz por voz.
3. Copia el **Paso 4** (`MediaPipeModelRepository.kt`) para poder cargar el modelo LLM.
4. **Lo más importante:** Adapta el **Paso 2 y 3** (`SueAgentImpl.kt`). Tu aplicación tendrá otra base de datos y otras entidades. Tendrás que escribir tus propios detectores de intenciones (*keywords*) y herramientas que busquen en tu BBDD, e inyectar ese contexto en tu propio System Prompt.

¡Y eso es todo! Has construido un asistente privado y 100% offline.
