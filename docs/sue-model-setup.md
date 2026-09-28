# 🤖 SUE — Guía de Configuración e Integración del Modelo On-Device (Gemma 3)

## 📌 Visión General

**SUE** (*Smart User Entity*) es la asistente inteligente local de **HomeTutorPro**. Funciona mediante inferencia de IA **100% en el propio dispositivo (Edge Computing / On-Device)** mediante **Google MediaPipe GenAI**, garantizando privacidad absoluta (RGPD), cero costes de servidores cloud y funcionamiento sin conexión a internet.

Dado que los pesos de los modelos cuantizados pesan entre **~550 MB y ~1.5 GB**, el modelo no se empaqueta dentro del APK base para mantener la aplicación ligera. 

Esta guía detalla cómo configurar, transferir y gestionar el modelo LLM tanto en entornos de desarrollo (mediante `adb`) como en producción.

---

## 🧠 Modelos Compatibles y Recomendados

El motor de inferencia de HomeTutorPro utiliza **MediaPipe Tasks GenAI** y soporta modelos basados en arquitecturas abiertas cuantizadas a 4 bits (**INT4**):

| Modelo | Tamaño en Disco | RAM Mínima / Recomendada | Idioma Español | Rendimiento / Uso |
|---|---|---|---|---|
| **Gemma 3 1B INT4** *(Recomendado)* | **~550 MB** | 4 GB / 6 GB | ⭐⭐⭐⭐⭐ (Excelente) | **Default:** Óptimo balance entre latencia, memoria y seguimiento de instrucciones en español. |
| **Gemma 2 2B INT4** | ~1.5 GB | 6 GB / 8 GB | ⭐⭐⭐⭐ (Muy bueno) | Mayor capacidad de razonamiento complejo en dispositivos de gama media-alta. |
| **Qwen 2.5 0.5B / 1.5B INT4** | ~260 MB - ~900 MB | 3 GB / 4 GB | ⭐⭐⭐ (Bueno) | Alternativa ultra-ligera para terminales con recursos muy limitados. |

> 📁 **Formatos de archivo admitidos:** `.bin` y `.task` (MediaPipe GenAI format).

### Dónde obtener los modelos
1. [Kaggle - Google Gemma Models](https://www.kaggle.com/models/google/gemma)
2. [Hugging Face - Gemma 3 / Gemma 2](https://huggingface.co/google)
3. [MediaPipe LLM Inference Guide & Model Conversions](https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference)

---

## 🔍 Detección Dinámica del Modelo en la App

La app cuenta con un escaneo automático dinámico implementado en `MediaPipeModelRepository`. Al inicializarse, busca en orden:

1. **Almacenamiento interno privado:** `context.filesDir/sue_model/`
2. **Almacenamiento externo privado de la app:** `context.getExternalFilesDir(null)/sue_model/`

> 💡 **Detección inteligente:** No es necesario que el archivo tenga un nombre exacto. La app detectará automáticamente el primer archivo con extensión `.bin` o `.task` presente en la carpeta `sue_model/`.

---

## 🛠️ Transferencia Manual del Modelo vía ADB (Desarrollo)

### Prerrequisitos
1. **USB Debugging activado** en tu dispositivo Android (`Ajustes > Opciones de desarrollador > Depuración USB`).
2. **Conexión ADB funcional** verificada desde la terminal del Mac:
   ```bash
   adb devices
   ```
   *(Si el comando no existe, asegúrate de tener en tu `~/.zshrc`: `export PATH="$PATH:$HOME/Library/Android/sdk/platform-tools"`)*

---

### Paso a Paso para la Instalación

#### 1. Conecta el dispositivo
Conecta el móvil al Mac por cable USB y autoriza la depuración en la pantalla del dispositivo si aparece el cuadro de diálogo.

#### 2. Transfiere el modelo con ADB

Ubícate en la terminal en la carpeta donde tengas descargado el modelo (por ejemplo `gemma-3-1b-it-int4.task` o `gemma-3-1b-it-int4.bin`):

**A) Para la versión de depuración (Debug / Android Studio - Recomendado):**
```bash
# 1. Crear el directorio de destino
adb shell mkdir -p /sdcard/Android/data/com.devsusana.hometutorpro.debug/files/sue_model/

# 2. Transferir el archivo del modelo (sustituye por el nombre de tu archivo .task o .bin)
adb push gemma-3-1b-it-int4.task /sdcard/Android/data/com.devsusana.hometutorpro.debug/files/sue_model/
```

**B) Para la versión de producción (Release):**
```bash
# 1. Crear el directorio de destino
adb shell mkdir -p /sdcard/Android/data/com.devsusana.hometutorpro/files/sue_model/

# 2. Transferir el archivo del modelo
adb push gemma-3-1b-it-int4.task /sdcard/Android/data/com.devsusana.hometutorpro/files/sue_model/
```

> ⏱️ **Tiempo estimado:** La transferencia suele tardar entre 15 y 45 segundos según la velocidad del puerto USB.

#### 3. Verificar en la App y en Logcat
1. Abre **HomeTutorPro** en el dispositivo.
2. Pulsa el **FAB de Sue** (micrófono flotante) en cualquier pantalla.
3. En el **Logcat** de Android Studio (filtrando por `MediaPipeModelRepository` o `SueViewModel`) verás:
   ```text
   MediaPipeModelRepository: Found model file: gemma-3-1b-it-int4.task
   MediaPipeModelRepository: Model loaded successfully in 412ms.
   ```

---

## ⚡ Ingeniería de Memoria y Ciclo de Vida (Android 15-17)

HomeTutorPro incorpora protecciones avanzadas para evitar que el LLM sature la memoria RAM del sistema operativo:

* **Carga Perezosa (Lazy Initialization):** El modelo no se carga al iniciar la app; únicamente se sube a memoria cuando el usuario pulsa el FAB para interactuar con Sue.
* **Descarga Automática por Inactividad:** Si el usuario no realiza preguntas durante **2 minutos** (o tras **30 segundos** con el overlay cerrado), la instancia de `LlmInference` se libera de la RAM automáticamente.
* **Blindaje ante Presión de Memoria:** Implementación de `ComponentCallbacks2.onTrimMemory(TRIM_MEMORY_RUNNING_CRITICAL / TRIM_MEMORY_COMPLETE)` que destruye la sesión del modelo inmediatamente si otra aplicación o el sistema requieren recursos.
* **Compatibilidad con Páginas de 16 KB:** Los binarios nativos de MediaPipe GenAI están alineados para los requisitos de Android 15 y Android 16/17.

---

## 🚀 Hoja de Ruta para Producción (Distribución OTA y Ajustes)

Para la versión comercial de Google Play Store, el usuario final no necesitará utilizar `adb`:

1. **Onboarding Interactivo (*User-Centric Opt-in*):**
   - En el primer inicio, un diálogo explicativo ofrece al profesor activar a Sue o continuar en modo clásico (sin IA y con FAB oculto).
2. **Descargador en la App (*In-App Model Downloader*):**
   - Descarga directa en segundo plano vía HTTPS/WiFi a `context.filesDir/sue_model/` con indicador visual de progreso en tiempo real.
3. **Panel de Control en Ajustes (`SettingsScreen`):**
   - Switch para activar/desactivar a Sue.
   - Switch para mostrar u ocultar el botón flotante (FAB).
   - Botón de gestión de almacenamiento: **"Eliminar modelo de IA (Liberar 550 MB)"**.

---

## ❓ Preguntas Frecuentes (FAQ)

### ¿Se borra el modelo si actualizo la app o hago un build nuevo?
**No.** El modelo reside en el almacenamiento persistente (`files/sue_model/`) y sobrevive a reinstalaciones desde Android Studio y a actualizaciones de versión de la app. Solo se elimina si:
- Se desinstala la aplicación por completo.
- El usuario pulsa "Borrar almacenamiento" en los Ajustes del sistema.
- El usuario pulsa "Eliminar modelo" en la pantalla de Ajustes de HomeTutorPro.

### ¿Cómo sé si una respuesta vino del LLM o de la capa determinista?
SUE implementa un **Fast-Path Determinista (0 ms)**:
- Si pides ver huecos libres, cobros pendientes, deudas de un alumno o cancelaciones, la app consulta directamente la base de datos Room y responde de forma instantánea sin invocar al LLM.
- Si haces una pregunta conversacional, de consejo pedagógico o abierta, la consulta pasa por el motor RAG y el modelo Gemma genera la respuesta.

### ¿Puedo tener varios modelos en la carpeta `sue_model/`?
La app tomará el primer archivo válido (`.bin` o `.task`) que encuentre. Para alternar entre modelos, se recomienda mantener únicamente el modelo que se desea utilizar en la carpeta.
