# 🎤 De la Nube al Bolsillo: Arquitectura de IA 100% On-Device en Android
**Autora:** Susana Córdoba Serrano | Senior Android Engineer & Tech Lead  
**Enfoque:** Arquitectura de Software, Edge Computing, SLMs Locales y Rendimiento  
**Estilo Visual Recomendado:** Minimalista claro (Fondo blanco/crema suave, tipografía sans-serif limpia, azul técnico, verde Android, gris pizarra)

---

# 1. Portada
### De la Nube al Bolsillo: Arquitectura de IA 100% On-Device en Android
* **Subtítulo:** Patrón Híbrido, RAG Local sobre Room y Gestión de Memoria en Android 15-17
* **Ponente:** Susana Córdoba Serrano
* **Rol:** Senior Android Engineer & Tech Lead
* *Clean Architecture • MediaPipe GenAI • Gemma 3 INT4 • SQLite FTS5 • Privacidad Local*

> **Notas de Oradora (Minuto 00:00 - 01:30):**  
> "¡Hola a todos! Durante los últimos dos años la industria nos ha transmitido que integrar Inteligencia Artificial en una aplicación móvil significa necesariamente abrir una conexión HTTP y llamar a una API en la nube como OpenAI o Gemini. Hoy venimos a desmontar ese mito desde la perspectiva de la arquitectura de software Android. Vamos a analizar cómo desplegar modelos de lenguaje pequeños (SLMs) corriendo de forma 100% local en el silicio del dispositivo, con latencias inferiores a 5 milisegundos, coste cero de servidor y garantía absoluta de privacidad."

---

# 2. El Paradigma Cloud Actual y sus Límites
### La trampa de las APIs REST en entornos móviles de producción

* 💸 **Coste Marginal Insostenible:** La facturación por token escala directamente con los usuarios activos, arruinando modelos freemium o de compra única.
* ⚖️ **Fricción Regulatoria y RGPD:** Transferir datos personales del usuario a centros de datos externos exige auditorías, consentimientos complejos y riesgos de fuga.
* 📵 **Dependencia Crítica de Conectividad:** Pérdida total de funcionalidad en zonas rurales, sótanos, transporte subterráneo o modo avión.
* ⏱️ **Latencia de Red:** 800 ms a 2.500 ms de ida y vuelta antes de que el primer token comience a generarse.

> **Notas de Oradora (Minuto 01:30 - 03:00):**  
> "La arquitectura cliente-servidor tradicional para IA tiene fugas graves en producción móvil. Cuando escalamos usuarios, el coste de API explota. Pero más importante aún: como ingenieros móviles, no podemos asumir conectividad ubicua ni podemos ignorar que los datos más sensibles del usuario están viviendo en el dispositivo. Llevar el cómputo al extremo (Edge Computing) es una necesidad arquitectónica."

---

# 3. La Alternativa: Small Language Models (SLMs)
### La revolución del silicio móvil y la inferencia local

* 🧠 **Evolución de Parámetros:** De los gigantes de 70B en la nube a modelos compactos y ultra-optimizados de 1B a 2B parámetros (ej. Gemma 3 1B/2B, Phi-3 Mini).
* ⚙️ **Compresión e Inferencia:** Cuantización INT4 y FP16 que reduce modelos gigamétricos a archivos de ~500 MB - 1.2 GB.
* ⚡ **Aceleración Hardware Nativa:** Inferencia en NPU (Neural Processing Unit) y GPU mediante aceleradores Vulkan y OpenCL.
* 🛡️ **Zero-Cloud Footprint:** Cero llamadas de red, cero tokens facturados, cero telemetría externa.

> **Notas de Oradora (Minuto 03:00 - 04:30):**  
> "Hoy un modelo de 1B parámetros cuantizado en 4 bits tiene una capacidad de razonamiento e instrucción comparable a lo que hace tres años requería un clúster de GPUs. Las NPUs y GPUs de los dispositivos móviles modernos son capaces de ejecutar estos modelos a más de 15 tokens por segundo sin consumir la batería en diez minutos."

---

# 4. El Reto Móvil: Los 4 Muros del Hardware
### Los límites que no existen en el backend pero gobiernan Android

* 🔋 **Thermal Throttling:** Si exprimimos la GPU al 100% de forma sostenida, el kernel reduce la frecuencia del procesador por seguridad térmica.
* 🧠 **RAM Budget Estricto:** Un proceso en Android no puede reclamar 3 GB de RAM impunemente sin despertar al Low Memory Killer (LMK).
* 📦 **Tamaño de Descarga:** La barrera psicológica de los usuarios en Google Play. Descargas masivas causan abandono inmediato.
* 📱 **Fragmentación de Silicio:** Snapdragons con Hexagon NPU, MediaTek APU, Tensor TPU, Exynos... la solución debe ser universal.

> **Notas de Oradora (Minuto 04:30 - 06:00):**  
> "En backend, si falta memoria añadimos un nodo en AWS. En Android, el sistema operativo es implacable: si superas el heap o retienes memoria en segundo plano, el Low Memory Killer liquida tu proceso. Además, la disipación térmica es pasiva. Cualquier arquitectura de IA local debe ser respetuosa y defensiva con el hardware."

---

# 5. La Pila Tecnológica en Android
### Comparativa de motores: MediaPipe GenAI vs llama.cpp / MLC-LLM

| Criterio | MediaPipe GenAI (Google) | llama.cpp / NDK Puro | MLC-LLM |
| :--- | :--- | :--- | :--- |
| **Integración Android** | Oficial de Google (AAR / Maven) | C++ / NDK JNI manual | Compilador Apache TVM |
| **Aceleración** | GPU (Vulkan/OpenCL) + CPU XNNPACK | CPU optimizada + Vulkan | WebGPU / Vulkan |
| **Modelos Soportados** | Gemma 3, Gemma 2, Phi, Falcon | GGUF universal | Empaquetado TVM propio |
| **Mantenimiento** | API de alto nivel en Kotlin | Código nativo C++ a bajo nivel | Complejidad de toolchain |

* **Elección de Ingeniería:** MediaPipe GenAI SDK por su compatibilidad nativa con el ciclo de vida de Android y estabilidad con Gemma 3.

> **Notas de Oradora (Minuto 06:00 - 08:00):**  
> "Evaluamos diferentes opciones para el runtime. Aunque llama.cpp es el rey de la flexibilidad en C++, MediaPipe GenAI proporciona una abstracción oficial de Google sobre Vulkan, empaquetada como dependencia estándar de Gradle, lo que reduce la fricción en pipelines de CI/CD y simplifica drásticamente el binding con Kotlin Coroutines."

---

# 6. Clean Architecture y Desacoplamiento de la IA
### El modelo como un DataSource intercambiable en la Capa de Datos

* 🧱 **Capa de Dominio (`domain`):**
  * Define la interfaz `LocalAiEngine` y contratos de casos de uso (`ExecuteVoiceCommandUseCase`).
  * Pure Kotlin: **Cero imports de Android SDK, MediaPipe o librerías de inferencia**.
* 💾 **Capa de Datos (`data`):**
  * `MediaPipeModelRepositoryImpl`: Implementa la inferencia, gestiona la sesión de MediaPipe y carga el archivo `.bin` en memoria.
  * El motor de IA se trata exactamente igual que un `RemoteDataSource` o un `LocalRoomDataSource`.
* 🖥️ **Capa de Presentación (`presentation`):**
  * ViewModels reciben flujos reactivos (`Flow<AiResponseState>`) mediante StateFlow y Unidirectional Data Flow (UDF).

> **Notas de Oradora (Minuto 08:00 - 10:00):**  
> "La regla de oro de la Clean Architecture: la UI y el Dominio nunca deben saber si la respuesta la generó Gemma 3, una expresión regular o un mock de testing. El motor de inferencia es un detalle de implementación confinado en la capa de datos. Si mañana Google lanza un SDK superior o cambiamos a ONNX Runtime, no tocamos una sola línea de lógica de negocio ni de Jetpack Compose."

---

# 7. El Patrón Híbrido: Fast-Path vs LLM
### No uses un cañón para matar moscas: La decisión arquitectónica crítica

* 🏎️ **Fast-Path Determinista (< 5 ms):**
  * Búsquedas exactas, mutaciones CRUD, filtros por estado, fechas directas.
  * Resuelto mediante expresiones regulares compiladas, clasificadores de patrones y consultas SQL indexadas en Room.
  * **Consumo de recursos:** 0% GPU, < 1 MB RAM, respuesta instantánea.
* 🐢 **Slow-Path Generativo (LLM Inferencia ~800 - 1.500 ms):**
  * Reservado **exclusivamente** para: desambiguación semántica, resúmenes abiertos, extracción de entidades complejas o intenciones ambiguas.
* 📊 **Resultado en Producción:** **El 80% del tráfico es absorbido por el Fast-Path**. El LLM solo se despierta en el 20% de los casos.

> **Notas de Oradora (Minuto 10:00 - 12:00):**  
> "El mayor error al implementar IA es pasar todas las frases del usuario por el LLM. Si el usuario dice 'cancela la cita de mañana a las 5', invocar un modelo de lenguaje de 1B para parsear esa orden es un desperdicio de batería y milisegundos. Nuestro pipeline evalúa primero un Fast-Path determinista. Si los slots se resuelven con 100% de certeza matemática, ejecutamos directamente en base de datos. El LLM solo actúa cuando hay ambigüedad lingüística."

---

# 8. Fast-Path en Profundidad: Slot Filling y Matchers Fonéticos
### Procesamiento de Lenguaje Natural determinista y tolerante a fallos en móvil

* 🧩 **Slot Filling Tipado:**
  * Extracción de parámetros tipados: fechas, horas, importes económicos, nombres propios y direcciones.
* 🇪🇸 **Resiliencia Diacrítica y Fonética Española:**
  * Normalización NFD para eliminar tildes y diéresis sin perder significado.
  * `SpanishPhoneticMatcher`: Algoritmo fonético adaptado a las particularidades del español (seseo, yeísmo, dígrafos 'ch', 'll', 'rr').
* 🔍 **Distancia de Levenshtein Acotada:**
  * Tolerancia a errores ortográficos de Whisper o teclado (`distancia <= 2`) sin degradar el rendimiento del hilo principal.

> **Notas de Oradora (Minuto 12:00 - 14:00):**  
> "El reconocimiento por voz en español tiene un reto enorme: tildes, seseo, confusiones entre 'b' y 'v', o variaciones fonéticas regionales. Diseñamos un componente ligero de normalización fonética y cálculo de Levenshtein en memoria. Esto permite reconocer entidades en menos de 2 milisegundos incluso si el reconocedor de voz transcribió una palabra con una errata fonética."

---

# 9. El Motor de Inferencia: MediaPipe GenAI & Gemma 3
### Pipeline técnico de ejecución on-device

* 📦 **Formato del Modelo:** Gemma 3 INT4 empaquetado en binario `.task`.
* 🧵 **Gestión de Hilos y Corrutinas:**
  * Carga asíncrona fuera del hilo principal usando `Dispatchers.Default`.
  * Conversión de la API de callbacks de MediaPipe a un flujo reactivo `Flow<String>` mediante `callbackFlow`.
* ⚡ **Streaming en Tiempo Real:**
  * Emisión token a token hacia la interfaz de usuario para reducir el Time-To-First-Token (TTFT) percibido a menos de 200 ms.
* 🛡️ **Aceleración Seleccionada:** Backend Vulkan para GPU móvil con fallback automático a CPU multihilo con XNNPACK si el hardware no soporta los shaders necesarios.

> **Notas de Oradora (Minuto 14:00 - 16:00):**  
> "MediaPipe GenAI opera internamente en C++, comunicándose con la GPU mediante Vulkan. En Kotlin, envolvemos esta ejecución nativa en un `callbackFlow`. De esta forma, el ViewModel consume los tokens de forma reactiva a medida que la GPU los produce, proyectando el texto en Compose en tiempo real mientras el usuario observa la animación fluida."

---

# 10. RAG Local: La Falacia de las Bases de Datos Vectoriales en Móvil
### Por qué importar FAISS, ChromaDB o Milvus en Android suele ser un antipatrón

* ❌ **El Enfoque Ingenuo de Backend:**
  * Generar embeddings para cada entidad local y montar una base de datos vectorial en el dispositivo.
* ⚠️ **Los Problemas Reales en Android:**
  * **Consumo de RAM disparatado:** Cargar modelos de embedding dedicados (ej. `all-MiniLM-L6-v2`) compite en RAM con el LLM principal.
  * **Almacenamiento duplicado:** Índices HNSW o IVFPQ duplican el tamaño de la base de datos local.
  * **Complejidad innecesaria:** Para el 99% de las apps con miles de registros locales, los vectores no aportan valor frente a un buen motor relacional.
* 💡 **La Solución Pragmática:** RAG Estructurado sobre SQLite y Room.

> **Notas de Oradora (Minuto 16:00 - 18:00):**  
> "En la literatura de backend, todo RAG empieza por 'instala una base de datos vectorial'. En móvil, intentar correr un modelo de embeddings en memoria junto a un LLM es una receta para el suicidio por falta de RAM. Las aplicaciones móviles ya tienen el motor de recuperación más optimizado de la historia del software: SQLite y Room. Veamos cómo hacer RAG de alta fidelidad con cero overhead vectorial."

---

# 11. RAG sobre SQLite y Room: Indexación y Búsqueda Determinista
### Recuperación de Contexto en Tiempo Constante

* 📑 **SQLite FTS5 (Full-Text Search):**
  * Tablas virtuales FTS5 integradas en Room para búsqueda léxica y de prefijos ultra-rápida (< 1 ms).
* 🧭 **Pre-filtrado Estructurado:**
  * Inyección de metadatos relacionales: estados, ventanas temporales relativas (*"últimos 7 días"*, *"próximo mes"*), y claves foráneas.
* 🎯 **Inyección de Contexto Mínimo Necesario:**
  * No inyectamos tablas enteras en el prompt: serializamos solo las filas relevantes en formato estructurado (YAML o JSON compacto).
  * Reducción drástica del tamaño de la ventana de contexto del LLM (menos de 512 tokens).

> **Notas de Oradora (Minuto 18:00 - 20:00):**  
> "En lugar de lanzar búsquedas difusas en un espacio vectorial multidimensional, usamos queries SQL indexadas y tablas FTS5. Si el usuario pregunta por notas o datos históricos, Room extrae exactamente los 3 registros relevantes en 1 milisegundo. Construimos un bloque de contexto compacto y se lo inyectamos al LLM. El resultado: precisión matemática del 100% sin alucinaciones relacionales."

---

# 12. Arquitectura de Prompts y Context Ingestion
### Maximizar la precisión de modelos de 1B parámetros con ventanas reducidas

* 🎯 **System Prompt Quirúrgico:**
  * Definición estricta del rol, directrices de concisión y prohibición explícita de inventar hechos ajenos al contexto inyectado.
* 🧱 **Delimitadores Estructurados:**
  * Separación clara entre instrucciones de sistema, datos de contexto inyectados y mensaje del usuario utilizando etiquetas semánticas (`<context>`, `<instruction>`, `<query>`).
* 📋 **Extracción Estructurada con Esquema JSON:**
  * Forzado del modelo a responder en formato JSON o claves fijas cuando se requiere una acción programática en la aplicación.

> **Notas de Oradora (Minuto 20:00 - 22:00):**  
> "Un modelo de 1B no es un GPT-4o con 128k tokens de contexto. Si le das un prompt de tres páginas con instrucciones complejas, el modelo se desorienta y alucina. El diseño de prompts para SLMs en el dispositivo debe ser minimalista, modular y fuertemente tipado. Menos es más: inyecta exactamente lo que el modelo necesita para razonar la respuesta actual."

---

# 13. Seguridad y Guardrails en el Dispositivo
### Prevención de Inyecciones de Prompt y Protección de Datos Locales

* 🛡️ **Sanitización de Entradas de Usuario:**
  * Filtrado de delimitadores del sistema antes de la concatenación en el prompt final.
* 🔒 **Validación Post-Inferencia:**
  * Verificación determinista de esquemas JSON antes de ejecutar cualquier mutación en Room.
* 🛑 **Confirmación Humana Obligatoria para Acciones Críticas:**
  * Las operaciones destructivas (eliminar registros, modificar datos críticos, cancelaciones) nunca se ejecutan en silencio: disparan un estado de confirmación visual o por voz en la UI.

> **Notas de Oradora (Minuto 22:00 - 24:00):**  
> "El hecho de que la IA se ejecute localmente no la exime de riesgos de inyección de prompt. Si un texto almacenado contiene instrucciones maliciosas, el modelo podría interpretar esa entrada como un comando. Implementamos sanitización de límites y una regla inquebrantable de arquitectura: ninguna acción con efectos secundarios en base de datos se ejecuta sin validación determinista y confirmación explícita."

---

# 14. UI Privacy Boundary en Widgets y Glance
### Seguridad y Privacidad en Superficies Expuestas del Sistema Operativo

* 📱 **El Reto de los Widgets en Android:**
  * Los widgets (Jetpack Glance) se renderizan en el proceso del Launcher del sistema operativo mediante IPC (`RemoteViews`).
* 🚪 **El Caso de Datos Sensibles (Códigos de Acceso, Notas Privadas):**
  * Información confidencial que el usuario almacena localmente nunca debe exponerse en un widget accesible desde la pantalla de bloqueo o miradas ajenas.
* ✂️ **Extractor de Privacidad (`DoorCodeExtractor`):**
  * Sanitización automática de campos de texto antes de transferirlos a la capa de presentación de Glance.
  * Separación estricta entre la UI privada de la app y la UI pública de los widgets del sistema.

> **Notas de Oradora (Minuto 24:00 - 25:30):**  
> "La privacidad en Android va más allá de no enviar datos a la nube. También abarca el entorno físico y las superficies del sistema operativo. Si la IA ayuda al usuario a recordar datos confidenciales como códigos de portero o notas personales, nuestro widget de Glance debe aplicar un Privacy Boundary estricto para evitar que esos datos aparezcan en la pantalla de inicio o de bloqueo sin autenticación."

---

# 15. Ingeniería de Memoria en Android 15-17
### Coexistencia pacífica con el Low Memory Killer y Páginas de 16 KB

* 📏 **Alineación de Páginas a 16 KB (Obligatorio en Android 15+):**
  * Todos los binarios nativos C++ y bibliotecas dinámicas compartidas (`.so`) deben estar compilados con alineación ELF a 16 KB para soportar los nuevos kernels de Android.
* 🧠 **RAM Budgeting:**
  * El modelo cuantizado INT4 ocupa ~550 MB en memoria virtual mapeada (`mmap`).
  * Gestión cuidadosa del Heap Java/Kotlin vs Memoria Nativa en C++/Vulkan.
* 📉 **Respuesta a `onTrimMemory`:**
  * Escucha activa de callbacks del sistema (`TRIM_MEMORY_RUNNING_CRITICAL`, `TRIM_MEMORY_UI_HIDDEN`).

> **Notas de Oradora (Minuto 25:30 - 27:30):**  
> "Android 15 introduce un cambio sísmico: la transición obligatoria a páginas de memoria de 16 KB en lugar de los tradicionales 4 KB. Cualquier librería C++ compilada sin este flag crasheará inmediatamente en dispositivos modernos. Además, el consumo de memoria de un SLM vive principalmente en la memoria nativa de la GPU; si no gestionamos el ciclo de vida, el sistema matará la app en cuanto el usuario abra la cámara."

---

# 16. Ciclo de Vida Defensivo: Evitando el OOM
### Carga bajo demanda y auto-descarga tras inactividad

* ⏳ **Carga Lazy On-Demand:**
  * El motor de IA no se inicializa en el arranque de la aplicación (`Application.onCreate`). Se inicializa únicamente cuando el usuario accede a la funcionalidad conversacional.
* ⏲️ **Auto-Release con Temporizador de Inactividad:**
  * Un job de corrutina monitorea la interacción: si transcurren 60 segundos sin consultas, el modelo se libera de la GPU/RAM.
* 🔄 **Reconstrucción Transparente:**
  * Si el usuario vuelve a interactuar, el modelo se re-mapea en memoria en segundo plano mediante un estado de carga no bloqueante.
* 📊 **Impacto:** La aplicación pasa de ocupar ~650 MB de RAM activa a tan solo **~80 MB en reposo**.

> **Notas de Oradora (Minuto 27:30 - 29:30):**  
> "Nadie quiere una app que consuma 600 megabytes de RAM mientras está en segundo plano. Diseñamos un gestor de ciclo de vida con política de descarga automática por inactividad. Si el usuario deja de hablar con el asistente, a los 60 segundos invocamos el cierre de sesión de MediaPipe. La memoria se libera instantáneamente y el dispositivo respira."

---

# 17. UX y Percepción de Rendimiento en Compose
### Cómo hacer que una inferencia local se sienta instantánea

* 🌊 **Token Streaming Fluido en Compose:**
  * Renderizado progresivo de texto con animaciones de desvanecimiento suave a medida que llegan los fragmentos del `Flow`.
* 🪄 **Optimistic UI y Feedback Háptico:**
  * Respuesta háptica inmediata al iniciar la captura de voz o el procesamiento de la orden.
* ⏳ **Estados de Carga Informativos (Shimmers):**
  * En consultas complejas del LLM, la UI informa explícitamente del estado (*"Consultando base de datos local..."*, *"Sintetizando respuesta..."*).
* 🗣️ **Síntesis de Voz (TTS) Sincronizada:**
  * Inicio de reproducción de audio TTS en cuanto se recibe la primera frase completa, sin esperar a que el modelo termine de generar el párrafo completo.

> **Notas de Oradora (Minuto 29:30 - 31:30):**  
> "En inteligencia artificial, la percepción de velocidad es tan importante como los milisegundos reales de cómputo. Al transmitir los tokens en streaming y comenzar a sintetizar la voz en cuanto se completa la primera frase sintáctica, el usuario percibe una respuesta prácticamente instantánea, aunque el modelo siga generando los últimos detalles en segundo plano."

---

# 18. Estrategia de Testing para IA Local
### Cómo garantizar determinismo y no romper regresiones en CI/CD

* 🧪 **Unit Tests Puros en JVM (Dominio y Casos de Uso):**
  * Mocking exhaustivo de `LocalAiEngine` para validar la lógica de negocio sin depender de hardware ni GPUs en CI/CD.
* 🎯 **Tests de Regresión de Prompts (`PromptEvaluationTest`):**
  * Suite automatizada de JUnit que evalúa 13 categorías críticas: desambiguación multi-turno, resiliencia diacrítica, consultas compuestas y cancelaciones.
* 📊 **Evaluación de Deriva de Respuestas:**
  * Verificación de que los cambios en los prompts o en las versiones del modelo siguen respetando los esquemas JSON esperados.

> **Notas de Oradora (Minuto 31:30 - 33:00):**  
> "Uno de los mayores miedos al meter IA en producción es la pérdida de control y la falta de determinismo en los tests. Nuestra suite en JUnit ejecuta baterías completas de evaluación en segundos sobre la máquina de desarrollo o CI, validando que cualquier ajuste en los prompts o en el Fast-Path no rompe intenciones previamente funcionales."

---

# 19. Caso de Estudio Empírico: SUE en Producción
### Validación real de la arquitectura en un asistente conversacional Android

* 📱 **El Escenario:** Asistente por voz inteligente integrado en una aplicación Android de gestión profesional en movilidad.
* ✈️ **La Prueba de Fuego: Modo Avión:**
  * Demostración empírica con conectividad completamente desactivada.
  * Consultas de agenda en < 5 ms mediante Fast-Path.
  * Desambiguación semántica y consultas complejas resueltas por Gemma 3 en local.
  * Mutaciones atómicas en Room Database con confirmación por voz.
* 🏆 **Conclusión:** La arquitectura es viable, robusta y está validada en condiciones reales de uso.

> **Notas de Oradora (Minuto 33:00 - 34:30):**  
> "Todo lo que os he explicado hoy no es una prueba de concepto teórica en un laboratorio. Está corriendo en producción dentro de nuestra aplicación HomeTutorPro a través del asistente SUE. En las pruebas de campo, activamos el modo avión delante de los usuarios: el asistente consulta, agenda y razona sin un solo byte de conexión a internet. La experiencia para el usuario es mágica."

---

# 20. Métricas Tangibles en Producción
### Resultados cuantitativos de la arquitectura On-Device

* 💰 **0,00 €** en Costes de Infraestructura Cloud e inferencia recurrente.
* ⚡ **< 5 ms** de Latencia media en el 80% de las consultas habituales (Fast-Path).
* 📦 **~550 MB** de Huella total en almacenamiento local (Gemma 3 INT4).
* 🔒 **100% Privacidad RGPD:** Cero bytes de datos personales abandonan el dispositivo del usuario.
* 🔋 **~80 MB** de Consumo de RAM en reposo gracias al gestor de ciclo de vida defensivo.

> **Notas de Oradora (Minuto 34:30 - 36:00):**  
> "Estas cifras resumen el impacto de un diseño arquitectónico riguroso. Cero euros en servidores, cinco milisegundos de respuesta para la gran mayoría de interacciones, respeto absoluto a la batería y la RAM del usuario, y la tranquilidad legal total de que los datos personales nunca viajan por internet."

---

# 21. Conclusiones y Principios de Arquitectura
### Las 3 leyes fundamentales de la IA en dispositivos móviles

1. ⚖️ **El Patrón Híbrido es Innegociable:** Usa código determinista (Room, regex, algoritmos fonéticos) para lo estructurado; reserva el LLM exclusivamente para el lenguaje natural.
2. 🗄️ **SQLite es tu Mejor Motor RAG en Móvil:** No compliques tu stack con bases de datos vectoriales pesadas cuando SQLite FTS5 resuelve el problema en 1 ms con cero consumo extra de RAM.
3. 🛡️ **Respeta Siempre el Hardware del Dispositivo:** Implementa alineación a 16 KB, gestiona `onTrimMemory`, descarga modelos inactivos y mantén una arquitectura Clean que aísle la IA en la capa de datos.

> **Notas de Oradora (Minuto 36:00 - 37:30):**  
> "Si tuviera que resumir esta charla en una sola lección, sería esta: la buena ingeniería de software no consiste en seguir modas y conectar APIs a lo loco, sino en elegir la herramienta adecuada para cada problema respetando los recursos de la plataforma. La IA on-device ya no es el futuro: es el presente de Android."

---

# 22. Cierre y Preguntas
### ¡Muchas gracias!

* 👩‍💻 **Susana Córdoba Serrano**
* Senior Android Engineer & Tech Lead
* 🌐 **GitHub:** `github.com/DEVSusana`
* 💼 **LinkedIn:** `linkedin.com/in/susanacordobaserrano/`
* 📧 **Email:** `susanacordobaserrano@gmail.com`
* ❓ **Turno de Preguntas y Respuestas (Q&A)**

> **Notas de Oradora (Minuto 37:30 - 40:00):**  
> "Muchas gracias por vuestro tiempo y vuestra atención. Tenéis el código, la arquitectura y los enlaces en mi GitHub y LinkedIn. Ahora abrimos el turno de preguntas: estaré encantada de responder cualquier duda sobre MediaPipe, Room, Gemma 3 o gestión de memoria en Android. ¡Gracias!"
