# Laboratorio de RAG Híbrido y Embeddings

> **Propósito:** Entender de forma práctica cómo funcionan los *embeddings*, el *chunking* y el RAG híbrido **sin tocar ni sobrecargar la app Android (SUE)**.

---

## 1. Por qué está fuera de la app Android

En **HomeTutorPro**, SUE ya resuelve muy bien las consultas estructuradas mediante reglas deterministas (`SueRuleIntentDetector`) + consultas SQL a Room. Añadir un modelo neuronal de *embeddings* dentro del APK exigiría descargar decenas de megabytes extra y consumir más RAM para un beneficio pequeño en ese caso concreto.

Aun así, **entender cómo funcionan los *embeddings* y un pipeline RAG completo** es clave para cualquier sistema de IA moderno sobre texto libre (notas, documentos, PDFs o soporte).

---

## 2. Qué es un *Embedding* (sin matemáticas complicadas)

Un **embedding** es una lista de números (un vector, por ejemplo de 768 dimensiones) que representa el **significado** de una frase.

* Frases con significado parecido (*"ecuaciones de segundo grado"* y *"fórmula cuadrática"*) producen vectores que apuntan en direcciones muy cercanas, aunque no compartan ni una sola palabra.
* Para medir qué tan parecidas son dos frases, se calcula la **similitud coseno** (el ángulo entre ambos vectores):
  * `1.0` = significan exactamente lo mismo.
  * `0.0` = no tienen nada que ver.

---

## 3. Los 4 pasos del pipeline en `rag_demo.py`

### Paso 1: *Chunking* con solapamiento y metadatos (`chunk_text`, `build_chunks`)
Los documentos largos no se meten enteros; se dividen en fragmentos (*chunks*):
* **Por qué con solapamiento (`overlap_words`):** si cortas justo en mitad de una frase importante, pierdes el contexto. Repetir las últimas 5–6 palabras al inicio del siguiente fragmento evita cortar ideas.
* **Por qué guardar metadatos (`student`, `subject`, `date`):** permite filtrar por alumno o fecha antes de buscar por significado.

### Paso 2: Pre-filtrado por metadatos (`student_filter`)
Antes de comparar vectores, se descartan los fragmentos de otros alumnos.
* **Por qué es crítico:** si preguntas *"¿qué problemas tuvo Darío?"*, filtrar primero `student == "Darío"` garantiza privacidad y evita mezclar notas de Ana aunque ambos tengan notas de *"ecuaciones"*.

### Paso 3: Búsqueda híbrida (`hybrid_search`)
Combinar dos señales funciona mejor que usar una sola:
1. **Similitud vectorial (semántica):** encuentra sinónimos e ideas parecidas.
2. **Coincidencia léxica (palabras exactas):** evita que se pierdan nombres propios, códigos o términos técnicos exactos.

En `rag_demo.py` se combinan ambas puntuaciones en un único *score* (`0.65 * sem + 0.35 * lex` en modo offline, `0.75 * sem + 0.25 * lex` en modo `--live`).

### Paso 4: Prompt con citas y abstención (`build_grounded_prompt`)
* Si ningún fragmento supera el umbral mínimo (`min_score`), el sistema **no llama al modelo con ruido**: devuelve `NO_CONTEXT_FOUND`.
* Si hay fragmentos válidos, se inyectan con su `id` y `fecha` y se instruye al modelo para que **cite la fuente** y responda *"No tengo datos suficientes en las notas"* si la respuesta no está ahí.

---

## 4. Modo Offline vs. Modo `--live`

| Modo | Qué usa por debajo | Para qué sirve |
| :--- | :--- | :--- |
| **Offline (por defecto)** | Vectores de n-gramas de caracteres + tokens | Entender la mecánica del pipeline (chunking, coseno, filtros, umbral) con **0 dependencias y 0 coste**. |
| **`--live`** | `text-embedding-004` de Gemini (`GEMINI_API_KEY`) | Ver en acción **embeddings neuronales reales** que entienden sinónimos sin palabras compartidas. |

---

## 5. Cómo probarlo

```bash
# Modo offline (sin API key)
agents_dev/.venv_agente/bin/python agents_dev/rag_lab/rag_demo.py --student Darío "¿Qué problemas tuvo con ecuaciones?"

# Probar un tema inexistente (ver la abstención)
agents_dev/.venv_agente/bin/python agents_dev/rag_lab/rag_demo.py "receta de paella valenciana"

# Modo real con embeddings de Gemini (requiere export GEMINI_API_KEY="...")
agents_dev/.venv_agente/bin/python agents_dev/rag_lab/rag_demo.py --live --student Darío "¿En qué falló con la fórmula cuadrática?"

# Ejecutar los tests del laboratorio
agents_dev/.venv_agente/bin/python -m unittest agents_dev/tests/test_rag_lab.py
```
