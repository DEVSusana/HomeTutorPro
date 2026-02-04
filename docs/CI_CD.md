# CI/CD Pipeline - HomeTutorPro

## 🚀 Estado del Build

![Android CI](https://github.com/TU_USUARIO/HomeTutorPro/workflows/Android%20CI/badge.svg)
[![codecov](https://codecov.io/gh/TU_USUARIO/HomeTutorPro/branch/main/graph/badge.svg)](https://codecov.io/gh/TU_USUARIO/HomeTutorPro)

## 📋 Qué hace el CI/CD

El pipeline de CI/CD se ejecuta automáticamente en cada:
- ✅ Push a `main` o `develop`
- ✅ Pull Request hacia `main` o `develop`

### Pasos del Pipeline

1. **Lint** - Análisis estático de código
2. **Unit Tests** - Tests unitarios (32+ tests)
3. **Coverage Report** - Reporte de cobertura con Jacoco
4. **Build APK** - Compilación de APK debug

## 📊 Reportes Generados

### Lint Results
- Ubicación: `app/build/reports/lint-results-debug.html`
- Se sube como artifact en cada ejecución

### Test Results
- Ubicación: `app/build/test-results/testDebugUnitTest/`
- Comentario automático en PRs con resultados

### Coverage Report
- Ubicación: `app/build/reports/jacoco/jacocoTestReport/`
- Integrado con Codecov para tracking histórico

## 🔧 Configuración Local

### Ejecutar todos los checks localmente:

```bash
# Lint
./gradlew lintDebug

# Unit tests
./gradlew testDebugUnitTest

# Coverage report
./gradlew jacocoTestReport

# Ver reporte de cobertura
open app/build/reports/jacoco/jacocoTestReport/html/index.html
```

### Verificar antes de hacer commit:

```bash
# Script completo de verificación
./gradlew clean lintDebug testDebugUnitTest jacocoTestReport
```

## 📈 Métricas de Calidad

### Objetivos
- ✅ Cobertura de tests: >80%
- ✅ 0 errores de lint críticos
- ✅ Todos los tests pasando
- ✅ Build exitoso

### Estado Actual
- **Tests Unitarios**: 32 tests
- **Tests UI**: 4 tests instrumentados
- **Cobertura**: ~65% (objetivo: >80%)

## 🔐 Seguridad

### ProGuard/R8
El proyecto usa ProGuard con configuración de seguridad:
- ✅ Ofuscación agresiva de código
- ✅ Optimización en 5 pases
- ✅ Eliminación de logs en release
- ✅ Protección de clases sensibles

### Verificar ofuscación:
```bash
./gradlew assembleRelease
# Revisar mapping file:
cat app/build/outputs/mapping/release/mapping.txt
```

## 🐛 Troubleshooting

### El CI falla pero local funciona
1. Limpiar cache de Gradle: `./gradlew clean`
2. Invalidar caches de Android Studio
3. Verificar versión de JDK (debe ser 17)

### Tests fallan en CI
1. Revisar logs en GitHub Actions
2. Ejecutar localmente: `./gradlew testDebugUnitTest --info`
3. Verificar que no haya tests que dependan del orden de ejecución

### Coverage report no se genera
1. Verificar que los tests pasen primero
2. Ejecutar: `./gradlew clean testDebugUnitTest jacocoTestReport`
3. Revisar exclusiones en `build.gradle.kts`

## 📚 Recursos

- [GitHub Actions Docs](https://docs.github.com/en/actions)
- [Jacoco Documentation](https://www.jacoco.org/jacoco/trunk/doc/)
- [ProGuard Manual](https://www.guardsquare.com/manual/home)
- [Codecov](https://about.codecov.io/)

## 🔄 Próximas Mejoras

- [ ] Tests de UI automatizados en CI
- [ ] Deploy automático a Firebase App Distribution
- [ ] Análisis de seguridad con MobSF
- [ ] Performance testing con Macrobenchmark
- [ ] Dependabot para actualizaciones automáticas
