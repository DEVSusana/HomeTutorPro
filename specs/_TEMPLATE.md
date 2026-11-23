# SPEC: [Título corto de la funcionalidad]

- **Estado:** DRAFT | APPROVED
- **Rama / Worktree:** `feat/<slug>`

## 1. Objetivo
Descripción clara en 2-3 líneas del problema que resuelve y del comportamiento esperado.

## 2. Alcance (Scope) — Archivos permitidos
- **Modificar:**
  - `app/src/main/java/com/devsusana/hometutorpro/...`
- **Crear:**
  - `app/src/test/java/com/devsusana/hometutorpro/...`
- **PROHIBIDO modificar:**
  - `gradle/*`, `AGENTS.md`, `BUSINESS_RULES.md`
  - Ficheros fuera de la lista anterior sin aprobación previa.

## 3. Reglas de negocio aplicables
- Cumplir `BUSINESS_RULES.md` (indicar sección concreta si aplica).
- [Regla específica 1]

## 4. Fuera de alcance (Out of Scope)
- [Lo que NO se debe implementar en esta tarea]

## 5. Criterios de aceptación (Verificables)
- [ ] **AC1:** Dado [contexto], cuando [acción], entonces [resultado observable].
- [ ] **AC2:** Dado [caso límite], cuando [acción], entonces [resultado observable].
- [ ] **AC3:** Tests unitarios / UI añadidos y pasando para AC1 y AC2.

## 6. Comandos de verificación obligatorios
```bash
./gradlew compileDebugKotlin
./gradlew testDebugUnitTest --tests "*<NombreTest>*"
```
