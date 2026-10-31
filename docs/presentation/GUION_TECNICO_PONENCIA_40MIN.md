# 🎤 Guion Técnico de Ponencia: De la Nube al Bolsillo (IA 100% On-Device en Android)

* **Ponente:** Susana Córdoba Serrano (Senior Android Engineer & Tech Lead)
* **Duración total:** 40 minutos (35 min exposición + 5 min Q&A)
* **Público:** Android Developers, Mobile Architects, AI Engineers
* **Enfoque:** Técnico, sobrio, analítico y centrado en decisiones de arquitectura y métricas de producción.
* **Documentos de referencia:** [`docs/PONENCIA_IA_LOCAL_GUIA_MAESTRA.md`](file:///Users/susanacordobaserrano/AndroidStudioProjects/HomeTutorPro/docs/PONENCIA_IA_LOCAL_GUIA_MAESTRA.md) y [`docs/ON_DEVICE_AI_AND_RAG_ARCHITECTURE.md`](file:///Users/susanacordobaserrano/AndroidStudioProjects/HomeTutorPro/docs/ON_DEVICE_AI_AND_RAG_ARCHITECTURE.md)

---

## ⏱️ Mapa Temporal de la Sesión

* **[00:00 - 04:00] Bloque 1:** Introducción, Trade-offs de Cloud AI y Caso de Estudio (Slides 1 a 3)
* **[04:00 - 11:00] Bloque 2:** Restricciones de Hardware, Ecosistema Móvil y Pesos/OTA (Slides 4 a 6)
* **[11:00 - 24:00] Bloque 3:** Clean Architecture, Fast-Path, NLP en Español y Diálogo (Slides 7 a 11)
* **[24:00 - 31:00] Bloque 4:** RAG Local con SQLite y Estructuración de Prompts (Slides 12 y 13)
* **[31:00 - 35:00] Bloque 5:** Gestión de Memoria en Android 15-17, RGPD y Conclusiones (Slides 14 a 17)
* **[35:00 - 40:00] Bloque 6:** Preguntas y Respuestas (Q&A)

---

## Slide 1: Portada

> **Visual:** *Título: "De la Nube al Bolsillo: IA 100% On-Device en Android" | Subtítulo: Patrones de arquitectura híbrida con RAG Local, SQLite y Google MediaPipe.*  
> **Badges:** Clean Architecture • RGPD Local • Cero Costes API • Android 15-17 Ready  
> **Tiempo estimado:** 00:00 – 01:30 (1.5 min)

### Discurso:

«Buenos días a todos. Hoy vamos a analizar la viabilidad técnica y los patrones de diseño necesarios para mover cargas de trabajo de IA generativa directamente al hardware del dispositivo en Android.

El enfoque tradicional basado en invocar APIs REST en la nube funciona para ciertos casos de uso, pero introduce compromisos significativos en términos de costes operativos recurrentes, latencia y cumplimiento de privacidad.

En esta sesión vamos a desgranar una arquitectura completa para ejecutar **Gemma 3 INT4** en local mediante **Google MediaPipe Tasks GenAI**. Veremos cómo diseñar un sistema **RAG local sobre Room y SQLite** sin necesidad de bases de datos vectoriales en memoria, cómo estructurar un enrutador híbrido determinista con una latencia inferior a **5 ms**, y qué mecanismos debemos implementar para adaptarnos a las restricciones de memoria de **Android 15 y versiones superiores**. 

Empecemos repasando los trade-offs que plantea la inferencia puramente en la nube.»

---

## Slide 2: Diagnóstico de Infraestructura

> **Visual:** *Las APIs Cloud en el Mundo Real Presentan Fricciones Críticas: Facturación inasumible, Fricción RGPD, Dependencia de cobertura, Latencia.*  
> **Tiempo estimado:** 01:30 – 03:00 (1.5 min)

### Discurso:

«Al evaluar una arquitectura dependiente de APIs de LLM en la nube para clientes móviles, nos encontramos con cuatro factores de diseño que condicionan su viabilidad:

1. **Estructura de costes y escala:** El modelo de facturación por token hace que el coste operativo crezca linealmente con el uso. En aplicaciones con modelos de monetización fija, freemium o compra única, la variabilidad de consumo de los usuarios dificulta proyectar la rentabilidad del backend.
2. **Requisitos de privacidad y RGPD:** Enviar datos personales a servidores externos para inferencia requiere formalizar contratos de encargado de tratamiento, evaluar transferencias internacionales y gestionar el consentimiento expreso del usuario, lo que añade complejidad operativa y legal.
3. **Disponibilidad offline:** Cualquier interrupción de conectividad —entornos rurales, transportes o zonas sin cobertura— invalida por completo las capacidades del asistente, impidiendo un diseño Offline-First.
4. **Presupuesto de latencia:** La suma de resolución DNS, handshake TLS y los tiempos de ida y vuelta de red (RTT) sitúa la latencia previa al primer token entre 500 ms y 1,5 segundos en conexiones móviles medias.

Mover la inferencia al dispositivo nos permite mitigar estos cuatro puntos de forma simultánea, trasladando el coste computacional al hardware del cliente.»

---

## Slide 3: Caso Práctico Real: HomeTutorPro como Sandbox

> **Visual:** *Caso práctico: HomeTutorPro: Sandbox de Validación Offline-First. Inferencia 100% Local, Respuesta Rápida, Dispositivo Target Android 14+, RAM < 2.5 GB.*  
> **Tiempo estimado:** 03:00 – 04:30 (1.5 min)

### Discurso:

«Para ilustrar estos patrones con código y decisiones reales de ingeniería, utilizaremos el caso de estudio de **HomeTutorPro**.

HomeTutorPro es una herramienta orientada a la gestión de docentes particulares en movilidad. La aplicación integra **SUE** (*Smart User Entity*), que es el motor de asistencia conversacional ejecutado íntegramente en local.

Este entorno nos sirve como banco de pruebas representativo por dos motivos principales:
* En primer lugar, los datos gestionados corresponden a información de alumnos menores de edad (domicilios, teléfonos, honorarios, notas), donde la contención de datos en local es una exigencia directa de diseño y cumplimiento normativo. Ni un solo dato de menores sale a un servidor central.
* En segundo lugar, el perfil de uso requiere inmediatez mientras el docente se desplaza entre clases, a menudo en transportes o sótanos sin cobertura constante.

*Un matiz técnico importante respecto a la conectividad y la voz:* La inferencia generativa, la base de datos Room y la lógica de negocio son **100% locales y operan sin conexión**. Ahora bien, para la entrada de voz dependemos del `SpeechRecognizer` nativo de Android. Para que la voz funcione plenamente desconectada, el dispositivo debe contar con el paquete de reconocimiento de voz sin conexión del sistema operativo; en caso contrario, la interacción se realiza mediante entrada de texto local.

Veamos a continuación los límites de hardware que debemos gestionar al ejecutar estos modelos en local.»

---

## Slide 4: Límites Físicos del Dispositivo

> **Visual:** *Tres desafíos críticos: Asfixia de RAM y VRAM (Lección Gemma 2B), Drenaje térmico y de batería, Inconsistencias en estructurado.*  
> **Tiempo estimado:** 04:30 – 06:00 (1.5 min)

### Discurso:

«La ejecución de modelos generativos en cliente impone restricciones físicas directas:

* **Gestión de memoria unificada (RAM/VRAM) y la lección de Gemma 2B:** En Android no disponemos de memoria de vídeo aislada; la GPU y la CPU comparten el mismo pool físico. En nuestras primeras pruebas evaluamos **Gemma 2B**. La asignación masiva de tensores en la GPU provocaba picos de memoria que disparaban el `LowMemoryKiller` y elevaban drásticamente el riesgo de ANR (*Application Not Responding*) en terminales de gama media con 4 GB de RAM. Ese choque con la realidad del hardware nos obligó a replantear el diseño: descender a **Gemma 3 1B cuantizado en INT4** y construir un enrutador determinista para reservar el modelo únicamente a lo imprescindible.
* **Presupuesto térmico y consumo energético:** Mantener la GPU o la NPU activas de forma continuada genera disipación térmica. El sistema operativo aplica *thermal throttling* para reducir las frecuencias de reloj, penalizando los tiempos de inferencia posteriores y degradando la autonomía de la batería.
* **Inconsistencias en tareas deterministas:** Los modelos de lenguaje pequeños (1B a 2B parámetros) presentan limitaciones conocidas al realizar cómputo aritmético, interpretar rangos de fechas absolutas o emitir estructuras JSON complejas de forma consistente. Delegar la persistencia de datos a la salida directa de un SLM puede provocar inconsistencias en la base de datos.

Con estas limitaciones claras, el primer paso es seleccionar el framework de inferencia óptimo.»

---

## Slide 5: Selección de Runtime: MediaPipe GenAI y Hoja de Ruta a LiteRT-LM

> **Visual:** *Runtime Móvil: De MediaPipe Tasks GenAI hacia Google LiteRT-LM. MediaPipe GenAI (Implementado v0.10.x) vs llama.cpp vs ONNX Runtime.*  
> **Tiempo estimado:** 06:00 – 08:30 (2.5 min)

### Discurso:

«Al comparar las opciones técnicas disponibles para inferencia local en Android, evaluamos tres alternativas principales:

1. **llama.cpp / llama-android:** Es una solución muy sólida en entornos de escritorio y servidores C++, pero en un pipeline nativo de Android exige compilar y mantener toolchains de NDK específicos (`arm64-v8a`), enlazar wrappers manuales mediante JNI y gestionar la compatibilidad binaria entre versiones del NDK.
2. **ONNX Runtime Mobile:** Funciona bien para modelos tradicionales de machine learning, pero en tareas de LLM generativo requiere adaptadores específicos y pipelines de exportación que incrementan la complejidad del CI/CD.
3. **Nuestra elección actual: Google MediaPipe Tasks GenAI v0.10.x:**
   * Es la solución empaquetada oficialmente por Google en Gradle (`com.google.mediapipe:tasks-genai`).
   * Gestiona la aceleración por hardware directamente contra la GPU mediante Vulkan y OpenCL sin necesidad de mantener capas C++ intermedias en el proyecto.
   * **Compatibilidad con páginas de 16 KB:** A partir de la versión `0.10.27`, MediaPipe soporta nativamente la alineación de páginas de 16 KB, un requisito técnico que Android 15 introduce de forma progresiva en arquitecturas ARM modernas.
   * **La realidad sobre su evolución: migración a LiteRT-LM:** Debemos ser muy claros y transparentes aquí: Google ha anunciado que la API LLM Inference de MediaPipe entra en modo de mantenimiento, consolidando toda su estrategia de inferencia Edge bajo la marca **LiteRT** (el sucesor de TensorFlow Lite), y específicamente **LiteRT-LM** como el runtime oficial de alto rendimiento para LLMs en Android.
     * **¿Por qué Google hace este movimiento con LiteRT-LM?** Porque MediaPipe era una capa de abstracción centrada en pipelines multimedia. LiteRT-LM, en cambio, está diseñado desde el metal para modelos de lenguaje: optimiza la aceleración directa en NPU (Qualcomm QNN, MediaTek Neuropilot, Tensor), soporta reutilización de prefijos de KV-cache y permite adaptaciones dinámicas con pesos LoRA sin recalentar el SoC.
     * **La lección de arquitectura (Clean Architecture al rescate):** Nuestra implementación actual corre con total estabilidad sobre MediaPipe v0.10, pero aplicamos Clean Architecture confinándolo estrictamente detrás de la interfaz `InferenceRepository` en la Capa de Datos. Esto significa que cuando LiteRT-LM alcance paridad completa y estabilidad en Maven central, la migración consistirá únicamente en crear un nuevo `LiteRtModelRepository` y cambiar una línea en el módulo de inyección de dependencias de Hilt. Ni un solo caso de uso, ni el enrutador de intenciones, ni la UI de Compose sufrirán la menor afectación.

Habiendo seleccionado el runtime y definido su hoja de ruta, el siguiente reto es la optimización de los pesos y su estrategia de entrega.»

---

## Slide 6: Gestión de Modelos, Cuantización y Distribución OTA

> **Visual:** *Gemma 3 1B INT4: 550 MB | Patrón de Descarga OTA: AAB (<200MB) -> Streaming HTTPS atómico con StateFlow -> Modelo listo.*  
> **Tiempo estimado:** 08:30 – 11:00 (2.5 min)

### Discurso:

«El modelo base elegido es **Gemma 3 1B**. En precisión FP32, un modelo de 1.000 millones de parámetros requeriría en torno a 4 GB de almacenamiento, lo cual es inviable en cliente.

Mediante cuantización **INT4-AWQ**, los pesos continuos se proyectan a representaciones discretas de enteros de 4 bits. Esto reduce la huella a **550 MB en disco** y aproximadamente **800 MB de RAM activa** durante la ejecución, reteniendo la capacidad lingüística requerida para tareas conversacionales.

Respecto a la distribución: Google Play Store establece un límite estricto de **200 MB** para el artefacto Android App Bundle (AAB). Por tanto, empaquetar los pesos dentro del bundle inicial no es una opción viable.

Implementamos un patrón de **distribución OTA (*Over-The-Air*) desacoplado**:
1. El AAB se distribuye con una huella base inferior a 25 MB.
2. Tras validar la compatibilidad de hardware del dispositivo, la capa de datos inicia una descarga streaming HTTPS controlada mediante corrutinas.
3. El repositorio escribe en un archivo temporal (`.tmp`), reporta el progreso en tiempo real mediante un `StateFlow<SueModelStatus>` y verifica el checksum SHA256 antes de ejecutar un renombrado atómico al archivo definitivo `.task`. Si la descarga se interrumpe, se limpia el temporal para evitar fragmentación de almacenamiento.

*Nota técnica sobre Google Play Console:* Aunque la inferencia ocurra íntegramente en local, es preceptivo completar el formulario de **Declaración de Contenido Generado por IA** en la consola de Google Play, documentando los mecanismos de moderación del sistema y los controles disponibles para el usuario.»

---

## Slide 7: Diseño de Capas (Clean Architecture)

> **Visual:** *Clean Architecture: Presentation Layer (Compose, UDF) -> Domain Layer (100% Pure Kotlin, 6 casos de uso) -> Data Layer (Room DB, MediaPipe).*  
> **Tiempo estimado:** 11:00 – 13:00 (2.0 min)

### Discurso:

«A nivel de diseño de software, la integración de un asistente conversacional puede derivar fácilmente en una *God Class* si no se desacoplan las responsabilidades adecuadamente.

Aplicamos **Clean Architecture modular** dividida en tres capas:

* **Presentation Layer:** Desarrollada con Jetpack Compose siguiendo un patrón unidireccional (UDF). La interfaz solo observa un estado inmutable (`UiState`) y remite eventos de usuario.
* **Domain Layer (Kotlin Puro):** No contiene dependencias del framework de Android ni del SDK de MediaPipe ni de ningún motor de IA. Define la abstracción `InferenceRepository` (con métodos reactivos como `loadModel()`, `generateResponse()` y `StateFlow<Boolean>` para el ciclo de vida) y descompone la lógica del asistente en componentes con responsabilidades específicas: `RuleIntentDetector` clasifica la intención, `ContextBuilder` prepara la información contextual, el extractor de entidades parsea parámetros y `ConversationContext` retiene el estado conversacional.
* **Data Layer:** Implementa los contratos de repositorio definidos en el dominio. Aquí conviven Room Database como fuente de verdad estructurada y `MediaPipeModelRepository` como adaptador del motor de inferencia nativo. Actúa como cortafuegos: si mañana cambiamos MediaPipe por LiteRT-LM o un proveedor cloud en caso de emergencia, el dominio y la presentación ni se enteran.

Esta estructura modular es la que permite implementar de forma limpia el patrón de inferencia híbrido.»

---

## Slide 8: Estrategia de Inferencia Híbrida (Fast-Path vs LLM)

> **Visual:** *Diagrama del Router: RuleIntentDetector divide entre Nivel 1 Fast-Path (< 5 ms lógico CPU, Operaciones Transaccionales) y Nivel 2 RAG Local (1.5s - 3.0s, Consultas Complejas).*  
> **Tiempo estimado:** 13:00 – 15:30 (2.5 min)

### Discurso:

«Una de las conclusiones de diseño más relevantes en Edge AI es que **la inferencia generativa debe reservarse para tareas que realmente la requieran**.

Si un usuario solicita información determinista —como su próxima clase agendada o el saldo pendiente de un estudiante—, dirigir esa petición a un LLM introduce una latencia innecesaria de 2 segundos, consume ciclos de GPU y añade riesgo de alucinación en datos numéricos.

Por ello, implementamos un enrutamiento en dos niveles por tipología de consulta:

* **Nivel 1: Fast-Path Determinista (Kotlin + Room):** Absorbe las operaciones transaccionales cotidianas (agenda, cobros, consultas directas y modificaciones). El procesamiento lógico y la consulta SQL se ejecutan en **menos de 5 milisegundos de tiempo de CPU**, con 0% de uso de GPU y sin despertar al modelo.
* **Nivel 2: RAG Local (Gemma 3 INT4):** Reservado para consultas abiertas, análisis pedagógicos o preguntas compuestas donde confluyen agenda y síntesis conversacional, delegándose al modelo con una ventana acotada a 2048 tokens y una latencia de entre 1,5 y 3 segundos en GPU.

Veamos cómo hacemos que este Fast-Path maneje las variaciones de entrada del usuario.»

---

## Slide 9: Rendimiento y Eficiencia del Fast-Path (NLP Local)

> **Visual:** *Fast-Path: Lógica Transaccional en < 5 ms de CPU. SpanishPhoneticMatcher (Validado en test suite JUnit), TemporalNormalizer ("después de comer" -> 15:00 CET).*  
> **Tiempo estimado:** 15:30 – 18:00 (2.5 min)

### Discurso:

«Para que una capa determinista basada en reglas sea viable en entornos reales, debe tolerar imprecisiones acústicas y variaciones dialectales sin requerir servicios en la nube:

1. **Normalización fonética con `SpanishPhoneticMatcher`:** El dictado por voz suele intercambiar grafías homófonas en español (*b/v*, *c/z/s*, *y/i*, *k/c*) y omite tildes con frecuencia (por ejemplo, *'lucia'* en lugar de *'Lucía'*). Este componente extrae el esqueleto consonántico canónico del término antes de consultar SQLite. Validamos este comportamiento contra una batería de 140 variaciones léxicas en nuestra suite JUnit (`SuePromptEvaluationTest`), garantizando que la resolución de nombres de alumnos funcione con solidez ante erratas fonéticas habituales.
2. **Normalización de expresiones idiomáticas temporales:** Mapeamos modismos del lenguaje cotidiano a marcas de tiempo estructuradas de 24 horas:
   * *«después de comer»* se normaliza directamente a **15:00**.
   * *«a media tarde»* se resuelve como **17:30**.
   * *«a primera hora de la mañana»* se mapea a **09:00**.
3. **Inferencia contextual de franja horaria:** Ante entradas ambiguas como *«pon clase a las 5»*, el resolvedor temporal prioriza por contexto pedagógico la franja de tarde (**17:00**), alineándose con el patrón habitual de clases particulares extraescolares.

Estas transformaciones se ejecutan en microsegundos dentro de la capa de dominio antes de interrogar a la base de datos, garantizando una respuesta lógica casi instantánea.»

---

## Slide 10: Control de Diálogo Determinista (Slot-Filling)

> **Visual:** *Máquina de estados inmutable data class IncompleteIntent(intentName, missingSlots). Paso 1: Detección -> Paso 2: Fusión (0 ms).*  
> **Tiempo estimado:** 18:00 – 20:00 (2.0 min)

### Discurso:

«Cuando el usuario emite un comando al que le falta un parámetro obligatorio —por ejemplo, *«Inicia una clase de 45 minutos»* sin especificar el estudiante—, no es necesario mantener un historial conversacional complejo en un LLM.

Utilizamos una **máquina de estados finitos tipada** (`IncompleteIntent`):

1. **Detección de slots faltantes:** El analizador identifica que el slot `STUDENT_NAME` no ha sido provisto. El sistema formula una pregunta directa y acotada: *«¿Para qué estudiante deseas agendar la sesión?»*.
2. **Fusión de parámetros:** El estado temporal queda suspendido en `ConversationContext`. Cuando el usuario responde con el dato aislado (*«Con Lucía»*), la máquina de estados completa la acción pendiente sin reevaluar todo el contexto.

La resolución se ejecuta en **0 ms** de cómputo de inferencia, manteniendo un control predecible sobre el flujo de estados de la aplicación.»

---

## Slide 11: Integridad de Acciones (Confirmación en Dos Pasos y DSL)

> **Visual:** *Cero escrituras accidentales: Comando de Voz -> PendingConfirmation (Timeout 15s) -> Mutación en DB. DSL Plano vs Schema JSON (Prevención de errores de parseo en 1B).*  
> **Tiempo estimado:** 20:00 – 22:30 (2.5 min)

### Discurso:

«Para evitar mutaciones no deseadas en la base de datos originadas por interpretaciones acústicas erróneas, aplicamos dos mecanismos de control:

* **Flujo de confirmación en dos pasos:** Toda acción con efectos persistentes (cancelar una cita, registrar un cobro o modificar una tarifa) se suspende en un estado transitorio `PendingConfirmation`. La interfaz presenta una tarjeta interactiva con un timeout de 15 segundos y solicita confirmación explícita por voz (*«¿Confirmas cancelar la clase de Lucía a las 17:00? Di sí o no»*). Si no hay confirmación afirmativa, la mutación se cancela automáticamente.
* **DSL plano frente a JSON estructurado:** Cuando el LLM generativo interviene y necesita emitir una acción, evitamos requerir un esquema JSON complejo. En modelos cuantizados de 1B de parámetros, la generación de JSON es propensa a fallos de sintaxis en delimitadores, comillas y corchetes no cerrados. Empleamos un DSL plano delimitado como el que veis en la slide:  
  `[ACTION: UPDATE_PRICE, value: 18]`  
  En nuestra batería de pruebas automatizadas, este formato eliminó prácticamente los fallos de parseo sintáctico que sufríamos con esquemas JSON anidados, reduce el consumo de tokens de contexto en torno a un 35% y se parsea en Kotlin mediante expresiones regulares de muy bajo coste computacional.»

---

## Slide 12: Motor RAG Local: SQLite vs Bases Vectoriales

> **Visual:** *SQLite Indexado Supera a las Bases Vectoriales en RAG Local. Sobrecoste vectorial vs SQLite exacto (0 ms, compresión relacional).*  
> **Tiempo estimado:** 22:30 – 25:00 (2.5 min)

### Discurso:

«En arquitecturas RAG tradicionales suele asumirse el uso de bases de datos vectoriales y embeddings densos. En un entorno móvil con recursos compartidos, este esquema introduce sobrecostes relevantes:
* Requiere cargar un modelo adicional de embeddings en memoria (~250 MB de RAM).
* La indexación periódica incrementa el consumo de CPU y batería.
* La búsqueda aproximada por similitud de coseno puede omitir registros exactos en filtros deterministas.

Para el dominio de una app de gestión (CRM docente con cientos o miles de registros personales), **Room (SQLite) actúa como motor de recuperación exacto**:
* Las necesidades de recuperación (*clases del día*, *alumnos con balance pendiente*, *historial reciente*) se resuelven mediante consultas SQL indexadas en **< 1 ms** de tiempo de base de datos.
* **Compresión relacional previa:** Antes de construir el prompt, agregamos y estructuramos la información en Kotlin. Si el usuario cuenta con decenas de registros compartidos, aplicamos un agrupamiento relacional (`groupBy { it.studentId }`) inyectando recuentos y los metadatos del recurso más reciente de los alumnos más activos.
* Este filtrado previo nos permite mantener la ventana de contexto de Gemma 3 en **2048 tokens**, preservando margen suficiente para respuestas elaboradas sin riesgo de saturar la memoria gráfica.»

---

## Slide 13: Delimitación de Prompts y Mitigación de Alucinaciones

> **Visual:** *Template Gemma 3 con etiquetas <start_of_turn>, Directivas, Contexto Temporal, Historial y Datos de Room. Mitigación y Acotación Contextual.*  
> **Tiempo estimado:** 25:00 – 27:30 (2.5 min)

### Discurso:

«La estructura del prompt inyectado a Gemma 3 está diseñada con el principio de *defensa en profundidad* para acotar la generación estrictamente a la información contextual provista:

1. **Delimitadores de turno estandarizados:** Empleamos la plantilla oficial de Gemma con `<start_of_turn>user` y `<end_of_turn>`, mitigando riesgos de desbordamiento de contexto o inyecciones accidentales procedentes de notas de usuario.
2. **Directivas de sistema estrictas:** Instrucción explícita de responder exclusivamente con la información provista bajo el bloque de datos y reportar amablemente la ausencia de datos si la información no figura en el contexto.
3. **Inyección temporal determinista:** Al carecer el modelo de noción temporal interna, inyectamos la marca de tiempo exacta del sistema (`Current date/time`) en la cabecera.
4. **Contexto estructurado delimitado:** Los registros recuperados desde Room se introducen bajo el bloque `[DATOS_ROOM: ...]` con los campos estructurados esenciales (`alumno`, `hora`, `materia`), tal y como se refleja en la plantilla.

En inteligencia artificial probabilística nunca se debe prometer 'cero alucinaciones', pero al operar sobre datos ya filtrados, estructurados y relacionalmente consolidados, reducimos drásticamente la superficie de error del modelo, acotando su función a la de un sintetizador de lenguaje natural.»

---

## Slide 14: Ciclo de Vida de Memoria (Android 15-17)

> **Visual:** *Estrategias de Memoria: 1. Gatekeeper (≥ 2.5 GB), 2. Parallel Warm-Up (0 ms percibidos), 3. Smart Auto-Release (Descarga ~800 MB tras 120s).*  
> **Tiempo estimado:** 27:30 – 30:30 (3.0 min)

### Discurso:

«En las versiones recientes de Android, la gestión de memoria nativa y el control de procesos en segundo plano se han endurecido notablemente a través de políticas agresivas de `LowMemoryKiller` y mecanismos como `MemoryLimiter:AnonSwap`.

Hablemos con realismo: **800 MB de RAM activa en inferencia para un modelo no es una huella ligera; es un consumo agresivo en cliente**. En un terminal con 4 GB de RAM física, donde el sistema operativo y los servicios en segundo plano consumen 2,5 GB, retener 800 MB te sitúa al borde del cierre forzoso.

Por ello, articulamos una estrategia de ciclo de vida defensiva en tres puntos:

1. **Hardware Gatekeeper:** Durante el arranque o la inicialización del módulo, evaluamos `ActivityManager.getMemoryInfo()` y descartamos terminales con el flag `isLowRamDevice` activo o con menos de 2,5 GB de RAM física. Si el terminal no cumple el umbral, se inhabilita la carga del modelo generativo y la aplicación opera exclusivamente en modo determinista.
2. **Parallel Warm-Up:** El modelo no se precarga en `Application.onCreate()`, manteniendo el consumo base de la app en torno a **80 MB**. La inicialización de tensores en GPU se dispara en un hilo de fondo en el instante en que el usuario pulsa el botón del micrófono; durante los 2 a 3 segundos habituales de locución, el runtime completa el mapeo, logrando que el modelo esté disponible al finalizar la entrada de audio.
3. **Smart Auto-Release y gestión de `onTrimMemory`:** Mantener el modelo en memoria tras responder sería negligente. Tras 120 segundos continuados de inactividad, o 30 segundos tras cerrar el asistente, invocamos `release()`, cerrando la instancia nativa y devolviendo los ~800 MB de RAM al sistema operativo. Asimismo, ante avisos del sistema como `TRIM_MEMORY_UI_HIDDEN`, los recursos nativos se liberan de forma inmediata.»

---

## Slide 15: Calidad y Protección de Datos (RGPD & Testing)

> **Visual:** *Frontera de Privacidad (DoorCodeExtractor en Widgets) y Golden Prompts Benchmark (140 tests en CI/CD con 13 categorías sintácticas).*  
> **Tiempo estimado:** 30:30 – 32:30 (2.0 min)

### Discurso:

«El diseño del sistema incorpora salvaguardas tanto en el tratamiento de datos sensibles como en la prevención de regresiones lógicas:

* **Frontera de privacidad en widgets:** En componentes visuales como el widget de pantalla de inicio (`ClassTimerWidget`), se aplica una política de minimización estricta. Para evitar la exposición de direcciones postales completas de menores en entornos públicos, el componente `DoorCodeExtractor` aísla únicamente metadatos no sensibles de acceso, como el código del portero (`🔔 Portero: 1402`), manteniendo la utilidad práctica sin exponer datos de localización.
* **Anonimización en logging (`SafeLogger`):** Todos los eventos de diagnóstico y excepciones enviadas a crash reporting pasan por un pipeline que redacta identificadores personales y nombres antes de escribir en disco o red.
* **Golden Prompts Benchmark:** Para mitigar el riesgo de regresiones silenciosas al actualizar heurísticas o modificar plantillas de prompt, mantenemos una suite automatizada en JUnit (`SuePromptEvaluationTest`) con más de 140 casos y aserciones estructurados en 13 categorías representativas (desambiguación, modismos temporales, horarios, pagos y cancelaciones). Esta batería se ejecuta en milisegundos en cada ciclo de integración continua sin requerir GPU ni conexión de red.»

---

## Slide 16: Resumen de Arquitectura y Métricas

> **Visual:** *Métricas: 0,00 € Coste API, < 5 ms Latencia Fast-Path, 550 MB Disco, ~80 MB RAM reposo. Tres Principios de Diseño.*  
> **Tiempo estimado:** 32:30 – 34:30 (2.0 min)

### Discurso:

«Para sintetizar el enfoque técnico presentado, los tres principios fundamentales de diseño son:

1. **Principio 1: Enrutamiento selectivo.** La arquitectura más eficiente en cliente es aquella que discrimina entre operaciones transaccionales y tareas generativas, resolviendo las primeras mediante lógica determinista de baja latencia en CPU.
2. **Principio 2: Eficiencia relacional en local.** Para volúmenes de datos personales y estructurados en cliente, los motores relacionales indexados como SQLite ofrecen mejor rendimiento, consistencia y consumo de memoria que las arquitecturas vectoriales.
3. **Principio 3: Gestión activa del ciclo de vida.** La memoria nativa debe considerarse un recurso de asignación temporal: cargas solapadas con la interacción del usuario y liberación proactiva ante inactividad o eventos de presión del sistema (`onTrimMemory`).

Con este esquema consolidamos **cero costes recurrentes por llamadas a APIs externas de inferencia, tiempos de resolución de milisegundos en la gran mayoría de interacciones habituales y la contención estricta de la información personal en el dispositivo**.»

---

## Slide 17: Cierre y Preguntas (Q&A)

> **Visual:** *¡Muchas Gracias! ¿Preguntas? Contacto: Susana Córdoba Serrano (LinkedIn, GitHub) | Stack Tecnológico Resumen.*  
> **Tiempo estimado:** 34:30 – 40:00 (5.5 min)

### Discurso:

«La madurez actual de los Small Language Models y de los runtimes optimizados para hardware móvil permite desplegar capacidades generativas locales viables, estables y seguras en Android sin depender necesariamente de infraestructuras en la nube.

Quedo a vuestra disposición para responder a las preguntas que queráis plantear. Muchas gracias.»

---

## 🛡️ Q&A Shield: Respuestas Técnicas a Preguntas Frecuentes

### P1: «¿Por qué no emplear Function Calling o Tool Calling nativo en formato JSON?»
> **Respuesta:** «En modelos de gran escala como GPT-4 o Gemini 1.5 Pro en cloud, el seguimiento de esquemas JSON estructurados es consistente. Sin embargo, en modelos cuantizados a 4 bits de 1B a 2B parámetros en cliente, forzar salidas JSON anidadas eleva notablemente la tasa de error por caracteres de escape o delimitadores incompletos. Un DSL plano del tipo `[ACTION: UPDATE_PRICE, value: 18]` eliminó prácticamente la totalidad de fallos de parseo sintáctico en nuestra suite de pruebas automatizadas, reduce los tokens de contexto en torno a un 35% y se parsea en Kotlin mediante expresiones regulares de muy bajo coste computacional.»

### P2: «¿Cómo se comporta el sistema en terminales que no alcanzan los requisitos de hardware?»
> **Respuesta:** «La capa de compatibilidad evalúa `ActivityManager.getMemoryInfo()` y descarta terminales con flag `isLowRamDevice` o con menos de 2,5 GB de RAM física. En esos casos, la app bloquea preventivamente la descarga del modelo generativo, explica la limitación de hardware en Ajustes y opera con normalidad en modo clásico determinista (Fast-Path), garantizando que el terminal no sufra degradación de rendimiento ni cierres forzosos.»

### P3: «¿Cómo se gestiona la actualización del modelo si se publica una versión más reciente?»
> **Respuesta:** «Gracias a la separación en la capa de datos (`SueModelRepository`), el modelo está completamente desacoplado del binario del aplicativo. Una actualización de la URL en la configuración remota permite que el repositorio descargue el nuevo archivo `.task` o `.bin` mediante el pipeline OTA existente sin necesidad de emitir una nueva versión de la app en Google Play Store.»

### P4: «Google está migrando MediaPipe GenAI hacia LiteRT-LM. ¿Cómo afecta esto a tu solución?»
> **Respuesta:** «Es la evolución natural y más esperada en el ecosistema Android. Para esta versión implementamos MediaPipe Tasks GenAI v0.10.x porque era la solución empaquetada oficialmente en Gradle con soporte probado para GPU (Vulkan/OpenCL) y páginas de 16 KB en Android 15. Sin embargo, MediaPipe era un framework pensado originalmente para visión computacional y grafos multimedia. Google ha consolidado su estrategia de Edge AI bajo la marca **LiteRT**, creando **LiteRT-LM** específicamente para inferencia de LLMs con acceso directo a NPUs (Qualcomm, MediaTek, Exynos y Tensor), optimización de KV-cache y adaptadores LoRA. 
> 
> En términos de arquitectura de software, **el impacto en nuestro código es exactamente cero en un 95% de la base de código**. Gracias a Clean Architecture, el motor vive confinado tras la interfaz `InferenceRepository` en la Capa de Datos. Migrar a LiteRT-LM se reduce a crear un `LiteRtModelRepository`, verificar la compatibilidad de los pesos del modelo y actualizar el enlace en Hilt (`@Binds`). El Dominio, los 6 casos de uso del asistente, la máquina de estados y la UI de Compose permanecen 100% inalterados.»

### P5: «¿De dónde salen las métricas de acierto fonético y la distribución de consultas?»
> **Respuesta:** «La precisión fonética proviene de nuestra suite automatizada en JUnit (`SuePromptEvaluationTest`), que evalúa 140 casos con variaciones acústicas y ortográficas reales en español (confusiones b/v, c/z/s, tildes omitidas por Speech-to-Text). La división entre Fast-Path y LLM no es una estadística de telemetría masiva, sino una distribución de diseño por tipología de tarea: las operaciones transaccionales frecuentes (agenda, cobros, alumnos) se resuelven de forma determinista en Room, y el LLM se reserva para consultas complejas o síntesis abierta.»

### P6: «Dices que es offline, pero ¿cómo funciona el reconocimiento de voz (STT) y la descarga inicial?»
> **Respuesta:** «Es un matiz técnico esencial: la descarga inicial del modelo (~550 MB) requiere conexión a internet puntual (OTA). Una vez descargado, la inferencia generativa, la base de datos Room y la lógica de negocio son 100% locales y offline. Para la voz, nos apoyamos en el `SpeechRecognizer` nativo de Android, que depende de que el usuario tenga descargado el paquete de voz offline del sistema operativo. Si no lo tiene o está en modo avión sin dicho paquete, la interacción se realiza mediante la interfaz de texto local.»
