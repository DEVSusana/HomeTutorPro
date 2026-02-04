# Guía Paso a Paso: Configurar Codecov

## 🎯 ¿Qué hace Codecov?

Codecov te muestra **visualmente** qué partes de tu código están cubiertas por tests:

### Ejemplo Visual

**Sin Codecov** (lo que tienes ahora):
```
✅ Tests pasaron
✅ 32 tests ejecutados
❓ ¿Qué % del código está cubierto?
❓ ¿Qué archivos necesitan más tests?
```

**Con Codecov**:
```
📊 Cobertura: 67.5% (+2.3% vs main)
📁 Archivos:
   ✅ WeeklyScheduleViewModel.kt: 85%
   ⚠️  StudentRepository.kt: 45%
   ❌ PaymentService.kt: 0%
   
💬 Comentario automático en tu PR:
   "Esta PR aumentó la cobertura en 2.3% 🎉"
```

### Dashboard Web

Codecov te da un dashboard como este:

```
┌─────────────────────────────────────┐
│  HomeTutorPro                       │
│  Coverage: 67.5%  ▲ 2.3%           │
├─────────────────────────────────────┤
│  📊 Gráfico de evolución:           │
│     70% ┤     ╭─╮                   │
│     65% ┤   ╭─╯ ╰─╮                 │
│     60% ┤ ╭─╯     ╰─╮               │
│     55% ┼─╯         ╰─              │
│         └─────────────────→ tiempo  │
├─────────────────────────────────────┤
│  📁 Archivos con menos cobertura:   │
│  1. PaymentService.kt (0%)          │
│  2. AuthRepository.kt (35%)         │
│  3. StudentRepository.kt (45%)      │
└─────────────────────────────────────┘
```

---

## 🚀 Configuración en 5 Pasos

### Paso 1: Ir a Codecov

1. Abre tu navegador
2. Ve a: **https://codecov.io**
3. Haz clic en **"Sign up"** o **"Log in"**

### Paso 2: Login con GitHub

1. Haz clic en **"Sign up with GitHub"**
2. GitHub te pedirá permiso → **"Authorize Codecov"**
3. Codecov verá tus repositorios

### Paso 3: Agregar tu repositorio

1. En Codecov, verás una lista de tus repos
2. Busca **"HomeTutorPro"**
3. Haz clic en **"Setup repo"**

### Paso 4: Obtener el token (SOLO si tu repo es privado)

#### Si tu repo es PÚBLICO:
- ✅ **No necesitas token**
- ✅ Ya está configurado
- ✅ Salta al Paso 5

#### Si tu repo es PRIVADO:
1. Codecov te mostrará una pantalla con un token
2. Se ve así: `a1b2c3d4-e5f6-7890-abcd-ef1234567890`
3. **Cópialo** (lo necesitarás en el siguiente paso)

### Paso 5: Agregar el token a GitHub (SOLO repos privados)

1. Ve a tu repositorio en GitHub
2. **Settings** (arriba a la derecha)
3. En el menú izquierdo: **Secrets and variables** → **Actions**
4. Haz clic en **"New repository secret"**
5. Llena el formulario:
   - **Name**: `CODECOV_TOKEN`
   - **Secret**: Pega el token que copiaste
6. Haz clic en **"Add secret"**

### Paso 6: Actualizar el workflow (SOLO repos privados)

Si tu repo es privado, necesitas agregar una línea al workflow:

```bash
# Abre el archivo
code .github/workflows/android-ci.yml
```

Busca la sección de Codecov y agrega `token:`:

```yaml
- name: Upload coverage reports to Codecov (Optional)
  uses: codecov/codecov-action@v4
  continue-on-error: true
  with:
    files: ./app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml
    flags: unittests
    name: codecov-umbrella
    fail_ci_if_error: false
    token: ${{ secrets.CODECOV_TOKEN }}  # ← Agregar esta línea
```

---

## ✅ Verificar que funciona

### Después de configurar:

1. Haz un commit y push
2. Espera a que el CI termine
3. Ve a tu PR en GitHub
4. Deberías ver un comentario de **codecov-bot** como este:

```
📊 Codecov Report
Merging #123 (abc1234) into main (def5678) will increase coverage by 2.15%.
The diff coverage is 85.71%.

@@            Coverage Diff             @@
##             main     #123      +/-   ##
==========================================
+ Coverage   65.30%   67.45%   +2.15%     
==========================================
  Files          45       46       +1     
  Lines        1234     1289      +55     
==========================================
+ Hits          806      869      +63     
+ Misses        428      420       -8     
```

5. También puedes ir a **codecov.io** y ver el dashboard completo

---

## 🎨 Badge para tu README (Opcional)

Codecov te da un badge que puedes poner en tu README:

```markdown
[![codecov](https://codecov.io/gh/TU_USUARIO/HomeTutorPro/branch/main/graph/badge.svg)](https://codecov.io/gh/TU_USUARIO/HomeTutorPro)
```

Se ve así: ![codecov](https://img.shields.io/badge/coverage-67%25-brightgreen)

---

## 🤔 Preguntas Frecuentes

### ¿Mi repo es público o privado?

Para saberlo:
1. Ve a tu repo en GitHub
2. Mira arriba del nombre del repo
3. Si dice **"Public"** → No necesitas token
4. Si dice **"Private"** → Necesitas token

### ¿Cuánto cuesta?

- **Repos públicos**: 100% GRATIS ✅
- **Repos privados**: 
  - Gratis hasta 5 usuarios
  - Después: $10/mes

### ¿Puedo probarlo sin compromiso?

Sí, es gratis para repos públicos y puedes desactivarlo cuando quieras.

### ¿Qué pasa si no configuro Codecov?

Nada malo. Tu CI/CD funciona perfectamente sin él. Solo no tendrás:
- Dashboard web
- Comentarios automáticos en PRs
- Tracking histórico

Pero seguirás teniendo:
- ✅ Tests ejecutándose
- ✅ Reporte de cobertura (como artifact)
- ✅ Todo lo demás funciona

---

## 📊 Ejemplo Real

Aquí te muestro cómo se vería en tu proyecto:

### Antes (sin Codecov):
```
PR #123: "Fix schedule sorting"
✅ All checks passed
- Android CI: Success
```

### Después (con Codecov):
```
PR #123: "Fix schedule sorting"
✅ All checks passed
- Android CI: Success
- codecov/patch: 85.71% (+2.15%) ✅
- codecov/project: 67.45% (+2.15%) ✅

💬 codecov-bot commented:
📊 Coverage increased by 2.15% to 67.45%
✅ All files have acceptable coverage
🎉 Great work!

Files changed:
✅ WeeklyScheduleViewModel.kt: 85% (+5%)
✅ ScheduleRepository.kt: 78% (+3%)
```

---

## 🎯 Resumen

1. **Público**: Solo login en codecov.io → Ya está ✅
2. **Privado**: Login + copiar token + agregarlo a GitHub Secrets

**Tiempo total**: 5 minutos

**Beneficio**: Dashboard bonito + comentarios automáticos + tracking

---

¿Quieres que te ayude a configurarlo ahora o prefieres dejarlo para después?
