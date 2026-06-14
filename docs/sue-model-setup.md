# Sue — Guía de Configuración del Modelo Gemma 2B

## ¿Qué es esto?

Sue necesita un modelo de IA (Gemma 2B) instalado en el dispositivo Android para funcionar.
Este modelo NO se incluye en el APK porque pesa ~1.5-2 GB.
Hay que transferirlo manualmente al móvil usando `adb` (Android Debug Bridge).

**Solo necesitas hacer esto UNA VEZ por dispositivo.** El modelo se queda permanentemente
en el almacenamiento privado de la app.

---

## Prerequisitos

1. **Modelo descargado:** Necesitas el archivo del modelo Gemma 2B en formato compatible
   con MediaPipe (`.bin`). Descárgalo desde:
   - [Kaggle - Google Gemma](https://www.kaggle.com/models/google/gemma)
   - [HuggingFace - Google Gemma](https://huggingface.co/google/gemma-2b)
   
   > Busca la versión cuantizada para móvil, normalmente llamada algo como:
   > `gemma-2b-it-gpu-int4.bin` o `gemma-2b-it-cpu-int4.bin`

2. **USB Debugging activado** en tu dispositivo Android:
   - Ve a `Ajustes > Acerca del teléfono > Número de compilación` (tócalo 7 veces)
   - Ve a `Ajustes > Opciones de desarrollador > Depuración USB` → Activar

3. **adb instalado** en tu Mac (ya viene con Android Studio):
   - Comprueba que funciona: abre Terminal y escribe `adb devices`
   - Si no lo encuentra, añade esto a tu `~/.zshrc`:
     ```bash
     export PATH="$PATH:$HOME/Library/Android/sdk/platform-tools"
     ```

---

## Pasos para transferir el modelo

### Paso 1: Conecta el móvil por USB

Conecta tu dispositivo Android al Mac con un cable USB.
Si te pide "¿Confiar en este ordenador?", acepta.

Verifica la conexión:
```bash
adb devices
```

Deberías ver algo como:
```
List of devices attached
XXXXXXXXX    device
```

Si dice `unauthorized`, desbloquea el móvil y acepta el diálogo de depuración USB.

### Paso 2: Transfiere el modelo al dispositivo

Navega en Terminal a la carpeta donde descargaste el modelo y ejecuta:

```bash
adb push gemma-2b-it-gpu-int4.bin /data/local/tmp/gemma-model.bin
```

> ⏱ Esto puede tardar 1-3 minutos dependiendo de la velocidad USB.
> Verás una barra de progreso:
> ```
> gemma-2b-it-gpu-int4.bin: 1 file pushed, 0 skipped. 45.2 MB/s (1536000000 bytes in 34.2s)
> ```

### Paso 3: Arranca la app

La app detectará automáticamente el modelo en `/data/local/tmp/gemma-model.bin` y lo
copiará a su almacenamiento privado interno (`context.filesDir/sue/`).

Una vez copiado, el archivo de `/data/local/tmp/` se puede borrar (la app lo hace
automáticamente).

### Paso 4: Verifica

Pulsa el FAB de Sue en cualquier pantalla y habla. Si el modelo está cargado
correctamente, Sue responderá.

---

## Preguntas Frecuentes

### ¿Tengo que repetir esto en cada build/reinstalación?

**No.** El modelo se guarda en el almacenamiento interno de la app y sobrevive a:
- Reinstalaciones de debug (`Run` desde Android Studio)
- Actualizaciones de la app

Solo se borra si:
- Desinstalas la app completamente
- Haces "Borrar datos" desde Ajustes del móvil

### ¿Y si cambio de dispositivo?

Repite el `adb push` en el nuevo dispositivo.

### ¿Qué modelo uso exactamente?

Para el MVP usamos **Gemma 2B** cuantizado (INT4). Es el modelo más ligero que
funciona bien en móviles con ≥6 GB de RAM.

| Variante | Tamaño | Requisito RAM | Velocidad |
|---|---|---|---|
| `gemma-2b-it-gpu-int4.bin` | ~1.5 GB | 4-6 GB | Rápido (GPU) |
| `gemma-2b-it-cpu-int4.bin` | ~1.5 GB | 4-6 GB | Más lento (CPU) |

> Recomendamos la versión **GPU** (`gpu-int4`) para tu dispositivo con 8 GB de RAM.

### ¿Puedo actualizar el modelo en el futuro?

Sí. Simplemente repite el `adb push` con el nuevo archivo `.bin` (por ejemplo,
Gemma 4 E2B cuando esté disponible en formato MediaPipe). La app detectará el nuevo
modelo al arrancar.

---

## Para producción (futuro)

Cuando publiques la app en Google Play, el modelo se distribuirá mediante
**Play Asset Delivery (on-demand)**:
- El usuario instala la app (ligera, ~50 MB)
- La app le ofrece "Activar Sue" → se descarga el modelo (~2 GB) vía WiFi
- No requiere adb ni conocimientos técnicos

Esta funcionalidad se implementará en una fase posterior.
