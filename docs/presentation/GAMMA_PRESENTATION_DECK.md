# 🎤 De la Nube al Bolsillo: IA 100% On-Device con RAG Local y MediaPipe en Android
**Autora:** Susana Córdoba Serrano | Senior Android Engineer & Tech Lead  
**Caso de Estudio:** HomeTutorPro — Asistente Inteligente Local "SUE"  
**Paleta Recomendada en Gamma:** *Clean Tech Light* (Fondo claro / Off-white, Azul tecnológico, Verde Android y Acentos violeta)

---

## 📌 Guía de Uso Rápido con Gamma (gamma.app)

1. Entra en [gamma.app](https://gamma.app) y pulsa en **"Crear nuevo con IA"** -> **"Pegar texto"** (o **"Importar archivo"**).
2. Selecciona formato **"Presentación" (16:9)** y elige aproximadamente **20-25 tarjetas**.
3. Pega el contenido estructurado a continuación (las tarjetas están separadas por líneas horizontales `---`).
4. Selecciona un **Tema Claro Moderno** (ej. *Oasis*, *Nimbus*, *Air*, *Prism* o *Minimal*).
5. Cuando Gamma genere las diapositivas:
   * Revisa que las cifras clave (`0,00 €`, `< 5 ms`, `550 MB`, `16 KB`) queden en tarjetas destacadas.
   * Haz clic arriba a la derecha en los **tres puntos (`...`)** -> **Exportar** -> **"Exportar a PowerPoint (.pptx)"** y también **"Exportar a PDF"**.
6. ¡Copia ambos archivos a tu pen drive y lista para el evento!

---

# 1. De la Nube al Bolsillo
### Cómo integrar un LLM 100% On-Device con RAG Local y MediaPipe en Android

* **Susana Córdoba Serrano**
* Senior Android Engineer & Tech Lead
* Caso Real de Producción: **HomeTutorPro (Asistente SUE)**
* *Arquitectura Limpia • Privacidad RGPD • Cero Costes de API • Android 15-17*

> **Notas de Oradora (Minuto 00:00 - 01:30):**  
> *"¡Hola a todos! Bienvenidos. Hoy os quiero hablar de una de las mayores tendencias en el desarrollo móvil: llevar los Modelos de Lenguaje Grande directamente a nuestros bolsillos. Durante los últimos dos años nos han vendido que para usar IA en una app hay que llamar a una API en la nube. Hoy vamos a desmontar ese mito y os enseñaré con código real cómo construimos SUE, un asistente 100% local, offline y con coste cero."*

---

# 2. La Tentación de la Nube (Y su Factura)
### Por qué las APIs REST tradicionales no sirven para todo

* 💸 **Facturación por Token:** Escala con el número de usuarios. Para una app freemium o de pago único, una factura recurrente de OpenAI o Gemini Cloud arruina el modelo de negocio.
* ⚖️ **Privacidad y RGPD en Educación:** Enviar nombres de alumnos, teléfonos, horarios o precios a servidores en EE.UU. exige contratos de tratamiento de datos y consentimientos explícitos.
* 📵 **El Mundo Real no tiene 5G Constante:** Un profesor particular en un sótano, en un aula rural o en el metro se queda sin asistente si no hay conexión continua.
* ⏱️ **Latencia Inaceptable:** 800 ms a 2 segundos solo en viaje de red HTTP antes de que el modelo empiece a responder.

> **Notas de Oradora (Minuto 01:30 - 03:00):**  
> *"La primera reacción al añadir IA a un proyecto es: 'meto Retrofit y llamo a OpenAI'. Pero cuando tus usuarios son profesores particulares que gestionan menores de edad y necesitan inmediatez, la nube se convierte en tu mayor problema: facturas impredecibles, sanciones de privacidad y cuelgues si no hay cobertura."*

---

# 3. El Caso de Estudio: HomeTutorPro
### Un asistente conversacional por voz para profesores particulares

* 👩‍🏫 **El Problema del Usuario:** Profesores particulares autónomos que dan clase a 15-30 alumnos a domicilio y necesitan gestionar agenda, pagos y materiales sin perder tiempo en menús complejos.
* 🎙️ **La Solución: SUE (Smart User Entity):** Asistente por voz integrado en Android que atiende peticiones como:
  * *«¿Qué clases tengo hoy por la tarde?»*
  * *«Pepe se ha mudado a Calle Mayor 12»*
  * *«Cancela la clase de Juan y dime qué huecos libres tengo el viernes»*
* 🔒 **Requisito Innegociable:** **Edge Computing 100% On-Device**. Nada sale del teléfono.

> **Notas de Oradora (Minuto 03:00 - 05:00):**  
> *"Para validar esto creamos HomeTutorPro. Su asistente SUE no es un chatbot de juguete: gestiona cobros, cancelaciones, huecos libres y perfiles completos. Y la premisa fue tajante: 'Si desconectas el móvil de internet, SUE debe seguir funcionando al 100%'."*

---

# 4. Los Tres Grandes Desafíos de la IA en Móvil
### Lo que nadie te cuenta antes de meter un LLM en un APK

1. 🧠 **Memoria RAM y VRAM Asfixiadas:**  
   Un smartphone medio tiene entre 4 y 8 GB de RAM compartida entre sistema operativo, apps y GPU. Un modelo mal gestionado provoca cierres forzados por el temido *Out-Of-Memory (OOM)*.
2. 🔋 **Consumo de Batería y Calor:**  
   Despertar la GPU/NPU a máxima frecuencia drena la batería y calienta el terminal en cuestión de minutos.
3. 🌀 **Alucinaciones Factuales:**  
   Los modelos compactos (~1B - 2B de parámetros) tienden a inventar cifras o equivocarse en sumas y fechas si se les pregunta directamente.

> **Notas de Oradora (Minuto 05:00 - 07:30):**  
> *"Llevar un LLM a un móvil no es gratis. Tienes tres enemigos mortales: los límites estrictos de RAM en Android, el consumo de batería y la tendencia de los modelos pequeños a alucinar. La clave de esta charla es cómo superamos estos tres obstáculos mediante arquitectura software."*

---

# 5. Selección del Runtime: ¿Por qué Google MediaPipe?
### Comparativa de frameworks de inferencia en Android

| Criterio | Google MediaPipe GenAI | llama.cpp (JNI/C++) | ONNX Runtime Mobile |
|:---|:---:|:---:|:---:|
| **Integración Android** | ⭐⭐⭐⭐⭐ Nativa Gradle (`tasks-genai`) | ⭐⭐⭐ Wrappers JNI C++ manuales | ⭐⭐⭐⭐ Adaptadores adicionales |
| **Aceleración Hardware** | GPU / OpenCL / Vulkan automática | Requiere flags NDK muy afinados | CPU / NNAPI |
| **Soporte Gemma** | ⭐⭐⭐⭐⭐ Oficial de Google (.task / .bin) | Conversión a GGUF obligada | Conversión a ONNX compleja |
| **Páginas de 16 KB** | ✅ Totalmente alineado (Android 15+) | Requiere recompilar binarios | En transición |
| **Mantenimiento** | Cero código C++ en tu repositorio | Alto mantenimiento de librerías .so | Complejidad de operadores |

> **Notas de Oradora (Minuto 07:30 - 10:00):**  
> *"Evaluamos varias opciones. llama.cpp es genial pero te ata a compilar binarios NDK para cada arquitectura de procesador y mantener JNI en C++. MediaPipe Tasks GenAI nos dio soporte oficial de Google, aceleración por GPU transparente y cero dolores de cabeza con páginas de 16 KB."*

---

# 6. El Modelo Fundacional: Gemma 3 1B INT4
### De 4 GB a 550 MB sin perder fluidez semántica

* 📉 **Cuantización a 4 Bits (INT4):**  
  Los pesos matemáticos originales de 32 bits en coma flotante se comprimen a enteros de 4 bits. El peso del modelo baja de ~4 GB a **solo ~550 MB en almacenamiento**.
* ⚡ **Consumo en RAM Activa:**  
  Solo ocupa **~800 MB de RAM** mientras está en inferencia activa.
* 🇪🇸 **Dominio del Español:**  
  A diferencia de otros modelos pequeños en inglés, Gemma 3 cuenta con un tokenizador multilingüe excelente que comprende modismos y conjugaciones verbales en español.
* 📦 **Alineación a 16 KB:**  
  Compatible con el nuevo estándar de paginación de memoria obligatorio desde Android 15.

> **Notas de Oradora (Minuto 10:00 - 12:30):**  
> *"Usamos Gemma 3 de 1 billón de parámetros cuantizado a 4 bits. Con solo 550 MB, cabe perfectamente en el almacenamiento y entiende el español coloquial de maravilla. Pero la pregunta del millón es: ¿cómo le enseñamos los datos del usuario?"*

---

# 7. Arquitectura Limpia: Donde la IA no Manda
### Clean Architecture estricta con desacoplamiento absoluto

* 🎨 **Presentation Layer (UI):**  
  * Jetpack Compose con Material 3 + Overlays flotantes.
  * Flujo Unidireccional de Datos (UDF): `SueUiState` inmutable emitido por `SueViewModel`.
* 🏛️ **Domain Layer (Core Kotlin Puro):**  
  * Cero dependencias de Android SDK ni de MediaPipe.
  * Contratos: `ISueAgent`, Casos de Uso, `SuePendingAction`, `SlotType`.
* 💾 **Data Layer (Infraestructura):**  
  * Room Database (SQLite como fuente de verdad única).
  * `MediaPipeModelRepositoryImpl` (gestión de tensores y backend GPU).
  * Descargador dinámico OTA en streaming con verificación atómica.

> **Notas de Oradora (Minuto 12:30 - 15:00):**  
> *"La regla de oro de la arquitectura: el LLM es un detalle de implementación de la capa de datos. Tu capa de dominio no sabe si hay un LLM, una regla regex o una base de datos. Si mañana Google saca Gemma 4 o cambiamos el runtime, el 90% de nuestra aplicación no cambia ni una sola línea."*

---

# 8. El Patrón Híbrido: La Mejor IA Sabe Cuándo NO Ejecutarse
### El enrutador inteligente de dos niveles

```
                 Petición de Voz del Usuario
                             │
                             ▼
              SueAgent.detectActionIntent()
               (SueRuleIntentDetector + NLP)
                             │
         ┌───────────────────┴───────────────────┐
         ▼                                       ▼
¿Encaja en Fast-Path?                   ¿Consulta Compleja / Abierta?
• Saldo de alumnos                      • "¿Cómo va el progreso de Juan?"
• Ver clases de hoy                     • "Recomiéndame un hueco para clase"
• Cancelaciones y mudanzas              • "¿Qué material he compartido?"
• Tarifas, teléfonos y pagos            • Consultas compuestas con dudas
         │                                       │
         ▼                                       ▼
  NIVEL 1: FAST-PATH                     NIVEL 2: RAG + LLM
• Consulta SQL en Room                 • SueContextBuilder extrae contexto
• Latencia: < 5 ms                     • Inferencia local en GPU con Gemma 3
• 100% Precisión Matemática            • Ventana ampliada a 2048 tokens
• 0 Alucinaciones                      • Latencia: 1.5 s - 3 s
```

> **Notas de Oradora (Minuto 15:00 - 18:00):**  
> *"Este es el diagrama más importante de toda la charla. El mayor error al integrar IA es enviar absolutamente todo al LLM. Si el profesor dice '¿cuántos alumnos tengo?', llamar al modelo es una aberración: tardas 3 segundos, gastas batería y te arriesgas a que alucine. Con el Fast-Path lo resuelves en 5 milisegundos con Room."*

---

# 9. Capa 1: Fast-Path Determinista a < 5 ms
### Precisión matemática instantánea sin tocar la GPU

* ⚡ **Latencia Prácticamente Nula:** Menos de 5 ms en procesar y responder.
* 🔋 **Impacto Cero en Batería:** 0% de uso de GPU/NPU; no despierta los tensores del modelo.
* 🎯 **Cero Alucinaciones:** Las cuentas matemáticas (saldos, deudas, horas) las realiza SQLite, no un modelo probabilístico.
* 📋 **Operaciones Soportadas en Fast-Path:**
  * Consultas de horario: *«¿Qué clases tengo el viernes a las 5?»*, *«¿Cuál es mi próxima clase?»*.
  * Disponibilidad: *«¿Tengo huecos libres hoy?»*, *«Horas libres esta semana»*.
  * Finanzas: *«¿Quién me debe dinero?»*, *«Carlos me ha pagado 30€ en efectivo»*.
  * Mutaciones de agenda: Reprogramar, cancelar, añadir clases extra.

> **Notas de Oradora (Minuto 18:00 - 20:00):**  
> *"El Fast-Path asume el 80% de las operaciones diarias de un profesor. Consultar saldo, ver clases o añadir refuerzos se ejecuta sobre SQLite indexado. Precisión del 100% y respuesta por voz instantánea."*

---

# 10. NLP Local: El Oído de SUE en Español
### Resiliencia fonética ante imperfecciones del micrófono

* 👂 **El Reto Acústico:**  
  El dictado por voz de Google suele transcribir nombres propios intercambiando consonantes homófonas (*b/v, c/z/s, y/i*) o ignorando tildes (*"lucia"* en lugar de *"Lucía"*).
* 🛡️ **`SpanishPhoneticMatcher` (En Kotlin Puro):**  
  Extrae el esqueleto consonántico canónico del alumno. Si el micro transcribe *"bictor"*, SUE lo asocia con *"Víctor"* sin fallar.
* 📚 **Mapeo de Jerga Escolar Cotidiana:**  
  * *«mates»* $\rightarrow$ **Matemáticas** | *«cono»* $\rightarrow$ **Conocimiento del Medio**
  * *«lengua»* $\rightarrow$ **Lengua y Literatura** | *«quim»* $\rightarrow$ **Química**
* 🕒 **Modismos Temporales de España:**  
  * *«después de comer»* $\rightarrow$ **15:00** | *«a media tarde»* $\rightarrow$ **17:30**
  * *«a primera hora de la tarde»* $\rightarrow$ **16:00** | *«al mediodía»* $\rightarrow$ **12:00**
  * Inferencia de tarde: *«a las 5»* se interpreta automáticamente como **17:00**.

> **Notas de Oradora (Minuto 20:00 - 22:30):**  
> *"Para que la voz funcione en el mundo real, necesitas un procesador fonético. La gente no habla como robots: dicen 'a media tarde' o 'clase de mates'. Nuestro normalizador en Kotlin traduce esas expresiones a horas y asignaturas oficiales antes de tocar la base de datos."*

---

# 11. Diálogo Multi-Turno: Máquina de Estados (Slot Filling)
### Resolver órdenes incompletas de forma natural

* 🧩 **El Modelo Tipado `IncompleteIntent`:**
  ```kotlin
  data class IncompleteIntent(
      val type: IntentType,
      val filledSlots: Map<String, Any>,
      val missingSlot: SlotType 
      // STUDENT_NAME, DAY_OF_WEEK, TIME, DURATION, AMOUNT, NOTES, ADDRESS, PRICE, PHONE, COURSE
  )
  ```
* 🗣️ **Conversación Fluida en 2 Turnos:**
  * **Turno 1:** El profesor dice: *«Inicia una clase de 45 minutos»*.
  * **SUE detecta slot faltante (`STUDENT_NAME`):** *«¿Para qué alumno quieres iniciar la clase?»*.
  * **Turno 2:** El profesor responde escuetamente: *«Con Lucía»*.
  * **SUE completa la acción:** Prepara la tarjeta y confirma sin reiniciar el diálogo.

> **Notas de Oradora (Minuto 22:30 - 24:30):**  
> *"La gente casi nunca da una orden completa en una sola frase. En lugar de dar error o reiniciar la conversación, SUE recuerda la intención en una máquina de estados tipada, pregunta exactamente el dato que le falta y ejecuta la orden cuando el usuario le contesta con una sola palabra."*

---

# 12. SUE como CRM por Voz: Edición Integral de Alumnos
### Más allá de la agenda: gestión completa del perfil estudiantil

* 🚚 **Mudanzas y Direcciones:**  
  Reconoce frases naturales donde el sujeto va al principio:  
  *«Pepe se ha mudado a Calle Mayor 12»* | *«Carlos ahora vive en Avenida de la Paz 8»*.
* 🔔 **Telefonillo y Códigos de Acceso (`DoorCodeExtractor`):**  
  Aísla y añade códigos de portero al perfil sin sobreescribir la calle:  
  *«Añade a Hugo en su dirección el código de portero 387»* | *«El portero de Ana es el 14B»*.
* 💶 **Tarifas y Honorarios:**  
  *«Cambia el precio de Carlos a 18 la hora»* | *«Ponle la tarifa a Ana a 15 euros»*.
* 📱 **Teléfono y Curso Académico:**  
  *«El nuevo teléfono de Carlos es 612345678»* | *«Cambia el curso de Pepe a 2º de Bachillerato»*.

> **Notas de Oradora (Minuto 24:30 - 26:30):**  
> *"SUE no solo mueve clases: edita perfiles completos. Si un alumno se muda, te da su nuevo teléfono o le cambias la tarifa por hora, se lo dices por voz y SUE actualiza su ficha en Room DB con todas las validaciones de negocio."*

---

# 13. Seguridad Operativa: Confirmación en Dos Pasos
### Protección contra mutaciones accidentales por audio

```
        Profesor: "Cancela la clase de Juan de hoy"
                           │
                           ▼
               [Detecta: CANCEL_CLASS]
             Construye acción pendiente
                           │
                           ▼
          SUE: "¿Confirmas cancelar la clase de Juan 
              de hoy a las 17:00? Di sí o no."
                           │
      ┌────────────────────┼────────────────────┐
      ▼                    ▼                    ▼
Dice: "Sí"           Dice: "No"           Silencio (15s)
      │                    │                    │
      ▼                    ▼                    ▼
Muta Room DB        Aborta acción       Auto-descarta
"Clase cancelada"   "Cancelado"         "Tiempo agotado"
```

* 🛡️ **Zero Accidental Writes:** Ninguna acción destructiva (cancelar, borrar, pagar) se ejecuta directamente por error de audio.
* 🎙️ **Confirmación por Voz o Táctil:** El profesor puede decir *"Sí"* o pulsar el botón en la tarjeta de UI.

> **Notas de Oradora (Minuto 26:30 - 28:00):**  
> *"En asistentes de voz, el ruido ambiente o una mala transcripción pueden ser un desastre si borran datos. En HomeTutorPro toda acción de escritura pasa por un estado pendiente donde SUE pide confirmación explícita con timeout de seguridad de 15 segundos."*

---

# 14. Anatomía del RAG Local: SQLite Indexado vs Vector DB
### Por qué NO usamos bases de datos vectoriales en móvil

* ❌ **El Mito de las Vector DB en Móvil (Chroma, Pinecone):**  
  Generar embeddings en el teléfono exige un modelo transformer adicional en memoria, saturando la RAM y la batería.
* ✅ **La Realidad de una App de Gestión:**  
  Un profesor tiene entre 10 y 100 alumnos. Esos datos caben holgadamente en SQLite indexado.
* 🔍 **Precisión Relacional Insuperable:**  
  Las preguntas de negocio (*«alumnos con saldo > 0»*, *«clases de hoy»*) se responden con consultas SQL de 0 ms, no con similitud de coseno difusa.
* 💉 **Inyección RAG en Tiempo Real:**  
  `SueContextBuilder` busca en Room, pre-comprime los datos y los inyecta en el prompt bajo la etiqueta `--- AVAILABLE DATA ---`.

> **Notas de Oradora (Minuto 28:00 - 30:00):**  
> *"En los tutoriales de internet siempre te meten una base de datos vectorial para hacer RAG. En móvil eso es una locura innecesaria. SQLite (Room) con consultas SQL indexadas es miles de veces más rápido, no consume RAM extra y te da datos 100% exactos."*

---

# 15. Estructura del Prompt Grounding
### Plantilla estructurada para Gemma 3 (Gemma Chat Template)

```text
<start_of_turn>user
Eres Sue, la asistente inteligente de HomeTutorPro.
Usa SOLO la información provista en --- AVAILABLE DATA ---.
NUNCA inventes datos. Si no hay información, dilo amablemente.

--- TEMPORAL CONTEXT ---
Current date/time: martes, 16 septiembre 2025, 09:20
--- END OF TEMPORAL CONTEXT ---

--- AVAILABLE DATA ---
Horario del martes:
  16:30–18:00: Lucía Moreno García
Huecos libres dentro de jornada:
  08:00–16:30, 18:00–23:00
--- RECURSOS / MATERIALES COMPARTIDOS ---
Total: 2 archivos compartidos con Carlos (problemas_fisica.pdf).
--- END OF DATA ---

User query: ¿Qué clases tengo hoy por la tarde y qué material le he enviado a Carlos?
<end_of_turn>
<start_of_turn>model
```

> **Notas de Oradora (Minuto 30:00 - 32:00):**  
> *"Este es el prompt real que le enviamos a Gemma. Le delimitamos el contexto temporal, el horario del profesor y los datos extraídos de Room. Con la directiva de sistema 'Usa SOLO la información en AVAILABLE DATA', el modelo actúa como un sintetizador de lenguaje natural sin inventar nada."*

---

# 16. Token Budgeting y Compresión Relacional
### Cómo evitar desbordar la memoria VRAM de la GPU

* 🛑 **El Riesgo:** Inyectar un volcado masivo de años de clases saturaría los 2048 tokens y provocaría un crash de GPU.
* 🗜️ **Compresión Relacional Previa en Kotlin:**  
  * **Si total $\le 15$ recursos:** Inyecta desglose detallado (Alumno, Archivo, Fecha, Canal WhatsApp/Email).
  * **Si total $> 15$ recursos:** Aplica `groupBy { it.studentId }` e inyecta solo el número de archivos por alumno y el documento más reciente de los 20 más activos.
* 📐 **Ventana Optimizada a 2048 Tokens:**  
  Configurada en `MediaPipeModelRepository` (`MAX_TOKENS = 2048`, `TEMPERATURE = 0.3f`, `TOP_K = 20`).
* ⚡ **Resultado:** Respuestas ricas y fluidas con consumo predecible y cero cuelgues.

> **Notas de Oradora (Minuto 32:00 - 33:30):**  
> *"Si un profesor lleva 2 años usando la app y tiene 300 PDFs compartidos, no puedes meter 300 líneas en el prompt. En Kotlin agrupamos y resumimos relacionalmente antes de construir el texto. Así mantenemos la inferencia en 2 segundos constantes."*

---

# 17. Ingeniería de Memoria: Sobrevivir a Android 15-17
### Límites estrictos por aplicación (Per-App Memory Limits)

* ⚠️ **El Escenario Hostil:**  
  Android 15 y 17 incorporan `MemoryLimiter:AnonSwap` y matan cualquier proceso que supere su cuota de memoria anónima.
* 🛡️ **Blindaje en 4 Niveles:**
  1. **Validación Preventiva de Hardware (`SueDeviceCompatibility`):**  
     Exige mínimo **2.5 GB de RAM física** y **Android 9.0+**. Si el móvil no llega, bloquea la descarga para proteger el teléfono.
  2. **Páginas de Memoria de 16 KB:**  
     Binarios NDK de MediaPipe alineados a 16 KB obligatorios para nuevos procesadores ARM64.
  3. **Arranque en Frío Ultraligero:**  
     La app arranca consumiendo solo **~80 MB**. El modelo nunca se precarga al abrir la app.
  4. **Protección `onTrimMemory`:**  
     Al minimizar la app (`UI_HIDDEN`) o ante presión crítica del sistema, se libera inmediatamente la memoria nativa de GPU.

> **Notas de Oradora (Minuto 33:30 - 35:00):**  
> *"En Android 15 y 17 el sistema operativo no perdona: si tu app gasta más RAM de la cuenta, el watchdog te mata sin avisar. Con nuestra verificación preventiva y la alineación a 16 KB garantizamos que HomeTutorPro nunca desestabilice el teléfono del usuario."*

---

# 18. Ciclo de Vida Defensivo: Smart Auto-Release
### Gestión inteligente de RAM para una experiencia invisible

```
Usuario pulsa botón Micrófono
      │
      ├──────────────────────────────────────────┐
      ▼                                          ▼
Inicia SpeechRecognizer               PARALLEL WARM-UP (GPU)
(Usuario habla durante 3-4s)          Carga Gemma 3 en background
      │                                          │
      └──────────────────┬───────────────────────┘
                         ▼
        Usuario termina de hablar -> Modelo LISTO
             (0 ms de espera percibida)
                         │
                         ▼
               Genera respuesta por voz
                         │
                         ▼
        TEMPORIZADOR SMART AUTO-RELEASE
   • Si hay inactividad durante 120 segundos
   • O si el usuario cierra el asistente (30 s)
                         │
                         ▼
             inferenceRepository.release()
          (Libera 800 MB de RAM y GPU al SO)
```

> **Notas de Oradora (Minuto 35:00 - 36:30):**  
> *"La mejor optimización: el Parallel Warm-Up. Cuando el profesor pulsa el botón del micro, empezamos a cargar Gemma en background en la GPU. Para cuando el usuario termina de hablar, el modelo ya está caliente: la espera percibida es cero. Y tras 2 minutos sin uso, lo liberamos de la RAM automáticamente."*

---

# 19. Privacy by Design en UI: La Frontera del Widget
### Protección de datos sensibles en la pantalla de inicio

* 📱 **El Reto:** El widget de Android (`ClassTimerWidget`) reside en el lanzador del sistema, visible para cualquiera que mire la pantalla en el metro o una cafetería.
* 🚫 **El Riesgo RGPD:** Mostrar la dirección postal física completa expone el domicilio privado de menores de edad.
* 💡 **La Frontera de Privacidad (*Privacy Boundary*):**
  * La app **nunca** proyecta la calle ni el número en el widget de inicio.
  * Utiliza el extractor determinista `DoorCodeExtractor` para aislar exclusivamente el código del telefonillo o portero automático.
* 🔔 **Resultado en el Widget:**  
  *«Clase con Hugo Morales • 17:00 • 🔔 Portero: 1402»*  
  Máxima utilidad para el profesor, cero exposición de datos sensibles.

> **Notas de Oradora (Minuto 36:30 - 37:30):**  
> *"El RGPD no es solo cifrar bases de datos, es pensar en la UI. Si vas en el autobús y alguien mira tu móvil, no debe ver la dirección de un menor. En nuestro widget de inicio ocultamos la dirección física pero proyectamos solo el código del portero: privacidad por diseño en estado puro."*

---

# 20. Calidad y Testing: Golden Prompts Benchmark
### Blindaje contra regresiones en 13 categorías reales

* 🧪 **`SuePromptEvaluationTest` (Suite Automatizada JUnit):**
  * Ejecuta cientos de variaciones de frases en local antes de cada release.
  * Valida que ningún ajuste en expresiones regulares o en el prompt rompa intenciones previas.
* 📊 **Categorías Evaluadas:**
  1. Horarios relativos y absolutos (*«clases de mañana»*, *«el jueves»*).
  2. Próxima clase y huecos libres en jornada.
  3. Cancelaciones y reprogramaciones naturales.
  4. Registro de pagos, bizums y deudas.
  5. Desambiguación multi-turno de slots incompletos.
  6. Mudanzas, códigos de portero, teléfonos y tarifas.
  7. Resiliencia fonética y diacrítica (*«Lucia»* vs *«Lucía»*).

> **Notas de Oradora (Minuto 37:30 - 38:30):**  
> *"Cuando tocas un sistema con NLP y RAG, el mayor peligro son las regresiones silenciosas. Con nuestra suite de Golden Prompts en JUnit probamos 13 categorías críticas en cada compilación para garantizar que SUE nunca deje de entender lo que antes entendía."*

---

# 21. DEMO EN VIVO: SUE en Modo Avión ✈️
### Demostración real sin red ni servidores externos

* 📱 **Paso 1:** Activación visible del **Modo Avión** en el teléfono móvil.
* 🎙️ **Paso 2:** Consulta rápida por Fast-Path:  
  *«¿Qué clases tengo hoy por la tarde?»* $\rightarrow$ Respuesta instantánea (< 5 ms).
* 🚚 **Paso 3:** Edición de alumno:  
  *«Pepe se ha mudado a Calle Mayor 12 con portero 4B»* $\rightarrow$ Petición de confirmación en dos pasos.
* 🧠 **Paso 4:** Consulta RAG compleja:  
  *«¿Qué material le he enviado a Carlos y qué hueco me recomiendas el viernes?»* $\rightarrow$ Síntesis generativa con Gemma 3 en GPU local.

> **Notas de Oradora (Minuto 38:30 - 41:00):**  
> *"Y ahora, la prueba del algodón. Activo el Modo Avión en mi teléfono... y mirad cómo SUE me responde al segundo con la voz del sistema, sin un solo paquete saliendo a internet. La IA local ya está aquí."*

---

# 22. Métricas Reales en Producción
### El impacto tangible de la arquitectura On-Device

* 💰 **0,00 € Coste por Consulta:** Escalabilidad infinita con coste marginal cero para el desarrollador.
* ⚡ **< 5 ms Latencia Fast-Path:** El 80% de las consultas se resuelven de forma instantánea.
* 📦 **550 MB en Disco:** Huella mínima gracias a la cuantización INT4 de Gemma 3.
* 🔒 **100% Privacidad RGPD:** Cero bytes de datos personales abandonan el sandbox del móvil.
* 🔋 **~80 MB RAM en Reposo:** Smart auto-release que no drena batería ni sobrecalienta el procesador.

> **Notas de Oradora (Minuto 41:00 - 42:30):**  
> *"Estas son las cifras que justifican esta arquitectura: cero euros en servidores, cinco milisegundos de respuesta para la mayoría de tareas y privacidad absoluta para nuestros usuarios."*

---

# 23. Tres Lecciones para tu Próximo Proyecto
### Conclusiones clave para desarrolladores y arquitectos

1. 🚫 **La mejor IA sabe cuándo NO ejecutarse:**  
   Combina siempre una capa determinista en Kotlin/Room para lo transaccional y reserva el LLM para lenguaje natural y síntesis abierta.
2. 🗜️ **SQLite es tu mejor base RAG en móvil:**  
   Olvídate de bases de datos vectoriales pesadas en apps de gestión. Consultas SQL indexadas y compresión relacional previa son imbatibles.
3. 🛡️ **El hardware manda:**  
   Gestiona la memoria de forma defensiva: validación de 2.5 GB RAM, páginas de 16 KB, parallel warm-up y auto-release tras inactividad.

> **Notas de Oradora (Minuto 42:30 - 44:00):**  
> *"Si os quedáis con tres ideas de esta charla: primero, la mejor IA es la que no se ejecuta si un código determinista lo resuelve mejor; segundo, SQLite es vuestro mejor aliado para RAG local; y tercero, respetad la memoria del teléfono con ciclos de vida defensivos."*

---

# 24. ¡Muchas Gracias!
### Preguntas y Recursos del Proyecto

* 👩‍💻 **Susana Córdoba Serrano**
* Senior Android Engineer & Tech Lead
* 💼 **LinkedIn:** `linkedin.com/in/susanacordobaserrano/`
* 🐙 **GitHub:** `github.com/DEVSusana`
* 📧 **Email:** `susanacordobaserrano@gmail.com`
* 📱 **Proyecto:** HomeTutorPro — *Kotlin • Compose • MediaPipe • Gemma 3*

> **Notas de Oradora (Minuto 44:00 - 45:00):**  
> *"Muchísimas gracias a todos por venir y a la organización del evento. Tenéis el código y los detalles de arquitectura en mi GitHub. ¡Estaré encantada de responder cualquier duda que tengáis!"*
