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

> **Visual:** *Caso práctico: HomeTutorPro es el Entorno de Validación para Arquitectura Offline-First. 100% Offline-First, Latencia Instantánea, Dispositivo Target Android 14+, RAM < 2.5 GB.*  
> **Tiempo estimado:** 03:00 – 04:30 (1.5 min)

### Discurso:

«Para ilustrar estos patrones con código y métricas reales de producción, utilizaremos el caso de estudio de **HomeTutorPro**.

HomeTutorPro es una herramienta orientada a la gestión de docentes particulares en movilidad. La aplicación integra **SUE** (*Smart User Entity*), que es el motor de asistencia conversacional por voz ejecutado íntegramente en local.

Este entorno nos sirve como banco de pruebas representativo por dos motivos principales:
* En primer lugar, los datos gestionados corresponden con frecuencia a información de alumnos menores de edad (domicilios, teléfonos, honorarios, notas), donde la contención de datos en local es una exigencia directa de diseño y cumplimiento normativo.
* En segundo lugar, el perfil de uso requiere respuestas inmediatas mientras el docente se desplaza, garantizando un funcionamiento estable en dispositivos de gama media con un techo de consumo de memoria acotado a terminales desde 2,5 GB de RAM.

Veamos a continuación los límites de hardware que debemos gestionar al ejecutar estos modelos en local.»

---

## Slide 4: Límites Físicos del Dispositivo

> **Visual:** *Tres desafíos críticos: Asfixia de RAM y VRAM, Drenaje térmico y de batería, Alucinaciones en estructurado.*  
> **Tiempo estimado:** 04:30 – 06:00 (1.5 min)

### Discurso:

«La ejecución de modelos generativos en cliente impone restricciones físicas directas:

* **Gestión de memoria unificada (RAM/VRAM):** En Android no disponemos de memoria de vídeo aislada; la GPU y la CPU comparten el mismo pool físico. La asignación descontrolada de tensores y pesos provoca que el kernel dispare eventos del `LowMemoryKiller`, provocando la finalización forzada del proceso.
* **Presupuesto térmico y consumo energético:** Mantener la GPU o la NPU activas de forma continuada genera disipación térmica. El sistema operativo aplica *thermal throttling* para reducir las frecuencias de reloj, penalizando los tiempos de inferencia posteriores y degradando la autonomía de la batería.
* **Precisión en tareas deterministas:** Los modelos de lenguaje pequeños (1B a 2B parámetros) presentan limitaciones conocidas al realizar cómputo aritmético, interpretar rangos de fechas absolutas o emitir estructuras JSON complejas de forma consistente. Delegar la persistencia de datos a la salida directa de un SLM puede provocar inconsistencias en la base de datos.

Con estas limitaciones claras, el primer paso es seleccionar el framework de inferencia óptimo.»

---

## Slide 5: Benchmark de Frameworks Móviles

> **Visual:** *MediaPipe GenAI (Recomendado) vs llama.cpp vs ONNX Runtime.*  
> **Tiempo estimado:** 06:00 – 08:30 (2.5 min)

### Discurso:

«Al comparar las opciones técnicas disponibles para inferencia local en Android, evaluamos tres alternativas principales:

1. **llama.cpp / llama-android:** Es una solución muy sólida en entornos de escritorio y servidores C++, pero en un pipeline nativo de Android exige compilar y mantener toolchains de NDK específicos (`arm64-v8a`), enlazar wrappers manuales mediante JNI y gestionar la compatibilidad binaria entre versiones del NDK.
2. **ONNX Runtime Mobile:** Funciona bien para modelos tradicionales de machine learning, pero en tareas de LLM generativo requiere adaptadores específicos y pipelines de exportación que incrementan la complejidad del CI/CD.
3. **Nuestra elección: Google MediaPipe Tasks GenAI:**
   * Es la solución oficial de Google para Android, disponible directamente a través de dependencias AAR en Gradle (`com.google.mediapipe:tasks-genai`).
   * Gestiona la aceleración por hardware directamente contra la GPU mediante Vulkan y OpenCL sin necesidad de mantener capas C++ intermedias en el proyecto.
   * **Compatibilidad con páginas de 16 KB:** A partir de la versión `0.10.27`, MediaPipe soporta nativamente la alineación de páginas de 16 KB, un requisito técnico que Android 15 introduce de forma progresiva en arquitecturas ARM modernas y que será el estándar en versiones posteriores.

Habiendo seleccionado el runtime, el siguiente reto es la optimización de los pesos y su estrategia de entrega.»

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
* **Domain Layer (Kotlin Puro):** No contiene dependencias del framework de Android ni del SDK de MediaPipe. Descompone la lógica del asistente en componentes con responsabilidades específicas: `RuleIntentDetector` clasifica la intención, `ContextBuilder` prepara la información contextual, el extractor de entidades parsea parámetros y `ConversationContext` retiene el estado conversacional.
* **Data Layer:** Implementa los contratos de repositorio definidos en el dominio. Aquí conviven Room Database como fuente de verdad estructurada y `MediaPipeModelRepository` como adaptador del motor de inferencia nativo.

Esta estructura modular es la que permite implementar de forma limpia el patrón de inferencia híbrido.»

---

## Slide 8: Estrategia de Inferencia Híbrida (Fast-Path vs LLM)

> **Visual:** *Diagrama del Router: RuleIntentDetector divide entre Nivel 1 Fast-Path (< 5 ms, 80% consultas) y Nivel 2 RAG Local (1.5s - 3.0s, 20% consultas).*  
> **Tiempo estimado:** 13:00 – 15:30 (2.5 min)

### Discurso:

«Una de las conclusiones de diseño más relevantes en Edge AI es que **la inferencia generativa debe reservarse para tareas que realmente la requieran**.

Si un usuario solicita información determinista —como su próxima clase agendada o el saldo pendiente de un estudiante—, dirigir esa petición a un LLM introduce una latencia innecesaria de 2 segundos, consume ciclos de GPU y añade riesgo de alucinación en datos numéricos.

Por ello, implementamos un enrutamiento en dos niveles:

* **Nivel 1: Fast-Path Determinista (Kotlin + Room):** Resuelve en torno al **80%** de las interacciones habituales. Las operaciones de consulta directa, altas, cancelaciones o modificaciones de datos se procesan en **menos de 5 milisegundos**, con 0% de uso de GPU y sin impacto apreciable en la batería.
* **Nivel 2: RAG Local (Gemma 3 INT4):** El **20%** restante —consultas abiertas, análisis pedagógicos o preguntas compuestas donde confluyen agenda y síntesis conversacional— se delega al modelo con una ventana acotada a 2048 tokens y una latencia de entre 1,5 y 3 segundos.

Veamos cómo hacemos que este Fast-Path maneje las variaciones de entrada del usuario.»

---

## Slide 9: Rendimiento y Eficiencia del Fast-Path (NLP Local)

> **Visual:** *Fast-Path Metric: < 5 ms. SpanishPhoneticMatcher (precisión > 98%), TemporalNormalizer ("después de comer" -> 15:00).*  
> **Tiempo estimado:** 15:30 – 18:00 (2.5 min)

### Discurso:

«Para que una capa determinista basada en reglas sea viable en entornos reales de voz, debe tolerar imprecisiones acústicas y variaciones dialectales sin requerir servicios en la nube:

1. **Normalización fonética con `SpanishPhoneticMatcher`:** El motor *Speech-to-Text* nativo suele intercambiar grafías homófonas en español (*b/v*, *c/z/s*, *y/i*, *k/c*) y omite diacríticos frecuentemente (por ejemplo, *'lucia'* en lugar de *'Lucía'*). Este componente extrae el esqueleto consonántico canónico del término antes de consultar SQLite, logrando una tasa de acierto superior al **98%** en la resolución de entidades de alumnos.
2. **Normalización de expresiones idiomáticas temporales:** Mapeamos modismos del lenguaje cotidiano a marcas de tiempo estructuradas de 24 horas:
   * *«después de comer»* se normaliza directamente a **15:00**.
   * *«a media tarde»* se resuelve como **17:30**.
   * *«a primera hora de la mañana»* se mapea a **09:00**.
3. **Inferencia contextual de franja horaria:** Ante entradas ambiguas como *«pon clase a las 5»*, el resolvedor temporal prioriza por contexto pedagógico la franja de tarde (**17:00**), alineándose con el patrón habitual de clases particulares extraescolares.

Estas transformaciones se ejecutan en microsegundos dentro de la capa de dominio antes de interrogar a la base de datos.»

---

## Slide 10: Control de Diálogo Determinista (Slot-Filling)

> **Visual:** *Máquina de estados inmutable data class IncompleteIntent(intentName, missingSlots). Paso 1: Detección -> Paso 2: Fusión (0 ms).*  
> **Tiempo estimado:** 18:00 – 20:00 (2.0 min)

### Discurso:

«Cuando el usuario emite un comando al que le falta un parámetro obligatorio —por ejemplo, *«Inicia una clase de 45 minutos»* sin especificar el estudiante—, no es necesario mantener un historial conversacional complejo en un LLM.

Utilizamos una **máquina de estados finitos tipada** (`IncompleteIntent`):

1. **Detección de slots faltantes:** El analizador identifica que el slot `STUDENT_NAME` no ha sido provisto. El sistema formula una pregunta directa y acotada: *«¿Para qué estudiante deseas agendar la sesión?»*.
2. **Fusión de parámetros:** El estado temporal queda suspendido en `ConversationContext`. Cuando el usuario responde con el dato aislado (*«Con Lucía»*), la máquina de estados completa la acción pendiente sin reevaluar todo el contexto.

La resolución se ejecuta en **0 ms** de inferencia, manteniendo un control predecible sobre el flujo de estados de la aplicación.»

---

## Slide 11: Integridad de Acciones (Confirmación en Dos Pasos y DSL)

> **Visual:** *Cero escrituras accidentales: Comando de Voz -> PendingConfirmation (Timeout 15s) -> Mutación en DB. DSL plano [ACTION: UPDATE_PRICE, value: 18] vs Schema JSON (99.4% fiabilidad).*  
> **Tiempo estimado:** 20:00 – 22:30 (2.5 min)

### Discurso:

«Para evitar mutaciones no deseadas en la base de datos originadas por interpretaciones acústicas erróneas, aplicamos dos mecanismos de control:

* **Flujo de confirmación en dos pasos:** Toda acción con efectos persistentes (cancelar una cita, registrar un cobro o modificar una tarifa) se suspende en un estado transitorio `PendingConfirmation`. La interfaz presenta una tarjeta interactiva con un timeout de 15 segundos y solicita confirmación explícita por voz (*«¿Confirmas cancelar la clase de Lucía a las 17:00? Di sí o no»*). Si no hay confirmación afirmativa, la mutación se cancela automáticamente.
* **DSL plano frente a JSON estructurado:** Cuando el LLM generativo interviene y necesita emitir una acción, evitamos requerir un esquema JSON completo. En modelos cuantizados de 1B de parámetros, la generación de JSON es propensa a fallos de sintaxis en delimitadores y corchetes. Empleamos un DSL plano delimitado como el que veis en la slide:  
  `[ACTION: UPDATE_PRICE, value: 18]`  
  Este formato alcanza un **99,4% de consistencia sintáctica** en pruebas automatizadas, reduce el consumo de tokens de contexto en torno a un 35% y se parsea en Kotlin mediante expresiones regulares sencillas.»

---

## Slide 12: Motor RAG Local: SQLite vs Bases Vectoriales

> **Visual:** *SQLite Indexado Supera a las Bases Vectoriales en RAG Local. Sobrecoste vectorial vs SQLite exacto (0 ms, compresión relacional).*  
> **Tiempo estimado:** 22:30 – 25:00 (2.5 min)

### Discurso:

«En arquitecturas RAG tradicionales suele asumirse el uso de bases de datos vectoriales y embeddings densos. En un entorno móvil con recursos compartidos, este esquema introduce sobrecostes relevantes:
* Requiere cargar un modelo adicional de embeddings en memoria (~250 MB de RAM).
* La indexación periódica incrementa el consumo de CPU y batería.
* La búsqueda aproximada por similitud de coseno puede omitir registros exactos en filtros deterministas.

Para nuestro dominio, **Room (SQLite) actúa como motor de recuperación exacto**:
* Las necesidades de recuperación (*clases del día*, *alumnos con balance pendiente*, *historial reciente*) se resuelven mediante consultas SQL indexadas en **< 1 ms**.
* **Compresión relacional previa:** Antes de construir el prompt, agregamos y estructuramos la información en Kotlin. Si el usuario cuenta con decenas de registros compartidos, aplicamos un agrupamiento relacional (`groupBy { it.studentId }`) inyectando recuentos y los metadatos del recurso más reciente de los alumnos más activos.
* Este filtrado previo nos permite mantener la ventana de contexto de Gemma 3 en **2048 tokens**, preservando margen suficiente para respuestas elaboradas sin riesgo de saturar la memoria gráfica.»

---

## Slide 13: Blindaje de Prompts y Mitigación de Alucinaciones

> **Visual:** *Template Gemma 3 con etiquetas <start_of_turn>, Directivas, Contexto Temporal, Historial y Datos de Room ([DATOS_ROOM: {alumno: "Carlos G.", hora: "17:00", materia: "Matemáticas"}]).*  
> **Tiempo estimado:** 25:00 – 27:30 (2.5 min)

### Discurso:

«La estructura del prompt inyectado a Gemma 3 está diseñada para limitar la generación estrictamente a la información contextual provista:

1. **Delimitadores de turno estandarizados:** Empleamos la plantilla oficial de Gemma con `<start_of_turn>user` y `<end_of_turn>`, garantizando el aislamiento frente a posibles inyecciones en las consultas.
2. **Directivas de sistema estrictas:** Instrucción explícita de responder exclusivamente con la información provista bajo el bloque de datos y reportar ausencia de información si el dato no figura en el contexto.
3. **Inyección temporal determinista:** Al carecer el modelo de noción temporal, inyectamos la marca de tiempo del sistema (`Current date/time`) en la cabecera.
4. **Contexto estructurado delimitado:** Los registros recuperados desde Room se introducen bajo el bloque `[DATOS_ROOM: ...]` con los campos estructurados esenciales (`alumno`, `hora`, `materia`), tal y como se refleja en la plantilla.

Al actuar sobre datos ya filtrados y relacionalmente consolidados, el modelo se limita a sintetizar la respuesta en lenguaje natural sin requerir extrapolaciones ni inventar horarios.»

---

## Slide 14: Ciclo de Vida de Memoria (Android 15-17)

> **Visual:** *Estrategias de Memoria: 1. Gatekeeper (≥ 2.5 GB), 2. Parallel Warm-Up (0 ms percibidos), 3. Smart Auto-Release (Descarga ~800 MB tras 120s).*  
> **Tiempo estimado:** 27:30 – 30:30 (3.0 min)

### Discurso:

«En las versiones recientes de Android, la gestión de memoria nativa y el control de procesos en segundo plano se han endurecido notablemente a través de políticas agresivas de `LowMemoryKiller` y mecanismos como `MemoryLimiter:AnonSwap`.

Para operar de forma estable en producción, articulamos una estrategia de ciclo de vida en tres puntos:

1. **Hardware Gatekeeper:** Durante el arranque o la inicialización del módulo, la compatibilidad del dispositivo evalúa `ActivityManager.getMemoryInfo()` y descarta terminales con el flag `isLowRamDevice` activo o con menos de 2,5 GB de RAM física. Si el terminal no cumple el umbral, se inhabilita la carga del modelo generativo y la aplicación opera exclusivamente en modo determinista.
2. **Parallel Warm-Up:** El modelo no se precarga en `Application.onCreate()`, manteniendo el consumo base de la app en torno a **80 MB**. La inicialización de tensores en GPU se dispara en un hilo de fondo en el instante en que el usuario pulsa el botón del micrófono; durante los 2 a 3 segundos habituales de locución, el runtime completa el mapeo, logrando que el modelo esté disponible al finalizar la entrada de audio.
3. **Smart Auto-Release y gestión de `onTrimMemory`:** Tras 120 segundos continuados de inactividad, o 30 segundos tras cerrar el asistente, el ViewModel invoca la liberación del repositorio de inferencia, cerrando la instancia nativa y liberando los ~800 MB de RAM. Asimismo, ante avisos del sistema como `TRIM_MEMORY_UI_HIDDEN`, los recursos nativos se liberan de forma inmediata.»

---

## Slide 15: Calidad y Protección de Datos (RGPD & Testing)

> **Visual:** *Frontera de Privacidad (DoorCodeExtractor en Widgets) y Golden Prompts Benchmark (140 tests en CI/CD con 13 categorías sintácticas).*  
> **Tiempo estimado:** 30:30 – 32:30 (2.0 min)

### Discurso:

«El diseño del sistema incorpora salvaguardas tanto en el tratamiento de datos sensibles como en la prevención de regresiones lógicas:

* **Frontera de privacidad en widgets:** En componentes visuales como el widget de pantalla de inicio (`ClassTimerWidget`), se aplica una política de minimización estricta. Para evitar la exposición de direcciones postales completas de menores en entornos públicos, el componente `DoorCodeExtractor` aísla únicamente metadatos no sensibles de acceso, como el código del portero (`🔔 Portero: 1402`), manteniendo la utilidad práctica sin exponer datos de localización.
* **Anonimización en logging (`SafeLogger`):** Todos los eventos de diagnóstico y excepciones enviadas a crash reporting pasan por un pipeline que redacta identificadores personales y nombres antes de escribir en disco o red.
* **Golden Prompts Benchmark:** Para mitigar el riesgo de regresiones silenciosas al actualizar heurísticas o modificar plantillas de prompt, mantenemos una suite automatizada en JUnit (`SuePromptEvaluationTest`) con más de 140 casos estructurados en 13 categorías representativas, ejecutada en cada ciclo de integración continua.»

---

## Slide 16: Resumen de Arquitectura y Métricas

> **Visual:** *Métricas: 0,00 € Coste API, < 5 ms Latencia Fast-Path, 550 MB Disco, ~80 MB RAM reposo. Tres Principios de Diseño.*  
> **Tiempo estimado:** 32:30 – 34:30 (2.0 min)

### Discurso:

«Para sintetizar el enfoque técnico presentado, los tres principios fundamentales de diseño son:

1. **Principio 1: Enrutamiento selectivo.** La arquitectura más eficiente en cliente es aquella que discrimina entre operaciones transaccionales y tareas generativas, resolviendo las primeras mediante lógica determinista de baja latencia.
2. **Principio 2: Eficiencia relacional en local.** Para volúmenes de datos personales y estructurados en cliente, los motores relacionales indexados como SQLite ofrecen mejor rendimiento, consistencia y consumo de memoria que las arquitecturas vectoriales.
3. **Principio 3: Gestión activa del ciclo de vida.** La memoria nativa debe considerarse un recurso de asignación temporal: cargas solapadas con la interacción del usuario y liberación proactiva ante inactividad o eventos de presión del sistema.

Con este esquema se consolida una solución con **0 € en consumo de APIs externas, latencias por debajo de 5 ms en el 80% de las consultas y contención estricta de datos en el dispositivo**.»

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
> **Respuesta:** «En modelos de gran escala como GPT-4 o Gemini 1.5 Pro, el seguimiento de esquemas JSON estructurados es muy consistente. Sin embargo, en modelos cuantizados a 4 bits de 1B a 2B parámetros, forzar salidas JSON complejas eleva la tasa de error por caracteres de escape o delimitadores incompletos. Un DSL plano del tipo `[ACTION: UPDATE_PRICE, value: 18]` alcanza un 99,4% de éxito sintáctico en nuestras pruebas, reduce los tokens de contexto en torno a un 35% y se parsea en Kotlin mediante expresiones regulares de bajo coste.»

### P2: «¿Cómo se comporta el sistema en terminales que no alcanzan los requisitos de hardware?»
> **Respuesta:** «La capa de compatibilidad evalúa `ActivityManager.getMemoryInfo()` y descarta terminales con flag `isLowRamDevice` o con menos de 2,5 GB de RAM física. En esos casos, la app bloquea preventivamente la descarga del modelo generativo, explica la limitación de hardware en Ajustes y opera con normalidad en modo clásico determinista, garantizando que el terminal no sufra degradación de rendimiento ni cierres forzosos.»

### P3: «¿Cómo se gestiona la actualización del modelo si se publica una versión más reciente?»
> **Respuesta:** «Gracias a la separación en la capa de datos, el modelo está completamente desacoplado del binario del aplicativo. Una actualización de la URL en la configuración remota permite que el repositorio descargue el nuevo archivo `.task` o `.bin` mediante el pipeline OTA existente sin necesidad de emitir una nueva versión de la app en Google Play Store.»

### P4: «¿Por qué no emplear directamente Gemini Nano a través de Android AICore?»
> **Respuesta:** «Gemini Nano vía AICore es una tecnología prometedora de Google, pero actualmente su disponibilidad está acotada a una lista reducida de terminales flagship (como Pixel 8/9 o Galaxy S24) y depende de la versión y soporte del fabricante en el sistema operativo. Con **Google MediaPipe Tasks GenAI y Gemma 3 INT4** logramos independencia total de marca: garantizamos aceleración nativa por GPU (Vulkan/OpenCL) en cualquier terminal Android 14+ con chipsets Qualcomm, MediaTek o Tensor, asegurando un comportamiento homogéneo para todos los usuarios.»
