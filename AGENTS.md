# Guía Exhaustiva de Codificación en Android Studio

**Objetivo:** Este documento establece directrices estrictas y explícitas para generar código en proyectos Android, asegurando la adhesión a Kotlin, Jetpack Compose, Arquitectura Limpia y las mejores prácticas de desarrollo profesional.

---

## 1. Fundamentos Tecnológicos Obligatorios

* **Lenguaje Principal:** Kotlin (exclusivo para todo el código de aplicación y pruebas).
* **Framework de UI:** Jetpack Compose (exclusivo para la construcción de interfaces de usuario).
* **Gestión de Concurrencia:** Kotlin Coroutines (exclusivo para operaciones asíncronas y concurrentes).

---

## 2. Arquitectura de Software Obligatoria: Arquitectura Limpia con MVVM

Implementar rigurosamente la **Arquitectura Limpia (Clean Architecture)** con **MVVM (Model-View-ViewModel)** en la capa de presentación. La separación de responsabilidades entre capas debe ser absoluta.

* **Capa de Dominio (`domain`):**
    * **Propósito:** Contiene la lógica de negocio central de la aplicación. Es completamente independiente de cualquier framework, base de datos o UI.
    * **Contenido:** Entidades, interfaces de casos de uso (Use Cases), interfaces de repositorios.
    * **Principio:** Pura lógica de negocio, sin dependencias de Android SDK ni frameworks externos (excepto Kotlin estándar).

* **Capa de Datos (`data`):**
    * **Propósito:** Responsable de la obtención, transformación y persistencia de datos. Implementa las interfaces de repositorio definidas en la capa de Dominio.
    * **Contenido:** Implementaciones concretas de repositorios, Data Sources (locales y remotos), modelos de datos específicos de la capa (`data models`), mappers para convertir entre `data models` y entidades de `domain`.
    * **Principio:** Abstracta la fuente de datos subyacente de la lógica de negocio.

* **Capa de Presentación (`presentation` / `ui`):**
    * **Propósito:** Gestiona la interfaz de usuario y la interacción del usuario. Observa estados y envía eventos a la lógica de negocio.
    * **Contenido:** Composable Screens, Composable Components, ViewModels, lógica de navegación.
    * **Principio:** La UI solo sabe cómo mostrar datos y reaccionar a la entrada del usuario; no contiene lógica de negocio directamente.

---

## 3. Convenciones de Nomenclatura y Estilo de Código Estrictas

* **Adherencia Inflexible a la Guía de API de Compose:** [https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-api-guidelines.md](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-api-guidelines.md)
* **Nombres Descriptivos y Unívocos:** Todas las entidades de código (funciones, variables, clases, paquetes, módulos) deben tener nombres que reflejen claramente su propósito y responsabilidad. Evitar abreviaturas ambiguas.
* **Case Convention:**
    * **`camelCase`:** Para nombres de variables, funciones y parámetros (ej. `myLocalVariable`, `calculateTotalPrice()`).
    * **`PascalCase`:** Para nombres de clases, interfaces, objetos y archivos Kotlin (ej. `MyAwesomeClass`, `HomeScreen.kt`).
    * **`SCREAMING_SNAKE_CASE`:** Para constantes (ej. `API_KEY`).
* **Idioma:** **Todo el código (nombres, comentarios, mensajes, etc.) debe estar en inglés.** Esto incluye nombres de paquetes, clases, funciones y variables.
* **Código Limpio (Clean Code):**
    * **Principios SOLID:** La implementación de todos los componentes debe adherirse estrictamente a los principios SOLID (Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, Dependency Inversion).
    * **Funciones y Clases Pequeñas:** Cada función debe tener una única responsabilidad. Las clases deben ser cohesivas y tener responsabilidades bien definidas.
    * **DRY (Don't Repeat Yourself):** Evitar la duplicación de código a toda costa. Refactorizar y reutilizar componentes.
    * **Legibilidad y Mantenibilidad:** El código debe ser fácilmente comprensible por cualquier desarrollador, incluso sin comentarios excesivos. Priorizar la claridad sobre la concisión.
    * **Testabilidad:** El código generado debe ser intrínsecamente fácil de testear unitaria e integradamente (ver punto 8). Esto implica un bajo acoplamiento y alta cohesión.

---

## 4. Componentes de UI con Jetpack Compose

* **Librerías Obligatorias:** Utilizar `material3` y los iconos extendidos de Compose.
* **Previews Obligatorios:** Cada `Composable` que represente una UI visual significativa (especialmente `Screen` y `Content`) debe incluir al menos una función `@Preview`.
* **Estructura Estricta de Composable para Pantallas:**
    * **`Screen` Composable (Principal):** Cada pantalla de la aplicación DEBE tener un `Composable` principal con el sufijo `Screen` (ej. `HomeScreen`). Este `Composable` será el punto de entrada de la pantalla, se encargará de:
        * Instanciar y gestionar el ViewModel asociado (generalmente a través de inyección de dependencias).
        * Recolectar los estados del ViewModel.
        * Pasar los estados y los callbacks de eventos al `Content` Composable.
        * Gestionar componentes de alto nivel como `Scaffold`, `SnackbarHost`, etc.
    * **`Content` Composable (Presentación Pura):** Para cada `Screen`, DEBE existir un `Composable` separado con el sufijo `Content` (ej. `HomeContent`). Este `Composable` es puramente de presentación:
        * **NO** interactúa directamente con ViewModels ni lógica de negocio.
        * Recibe **TODOS** los estados y los callbacks de eventos necesarios como parámetros de función (State Hoisting).
        * Su única responsabilidad es renderizar la UI basándose en los parámetros recibidos.
        * Debe ser fácilmente reutilizable y testeable de forma independiente.
* **Componentes Comunes y Reutilizables:** Crear un paquete `presentation.ui.components` para alojar Composable genéricos y reutilizables en múltiples pantallas (ej. `AppToolbar.kt`, `LoadingIndicator.kt`, `ErrorDialog.kt`).
* **Stateless Composables:** Priorizar la creación de `Stateless Composables` siempre que sea posible. El estado debe ser "levantado" (hoisted) a los Composable padres o, preferiblemente, a los ViewModels.

---

## 5. Diseño de Layout en Compose: Reglas Recomendadas y Claras

* **Uso de `ConstraintLayout`:** Recomendado para pantallas o `Content` complejos con múltiples relaciones, alineaciones cruzadas o dependencias visuales. No es obligatorio si un layout simple se resuelve mejor con `Row`, `Column` o `Box` sin anidamiento excesivo.
* **Uso de `Row`, `Column`, `Box`:** Válidos y preferibles para layouts simples o jerárquicos que se leen claramente. Evitar anidamiento profundo; si el layout empieza a ser difícil de mantener, considerar `ConstraintLayout`.
* **Eficiencia de árbol UI:** Priorizar jerarquías planas, `Lazy*` cuando corresponda y evitar contenedores innecesarios.
* **State hoisting en Compose:** Los `Content` deben ser stateless siempre que sea razonable. El estado UI de pantalla vive en el `Screen` y usa `rememberSaveable` cuando el estado deba sobrevivir a recreaciones.

---

## 6. Organización Estricta de Clases por Capa y Carpeta

Gemini DEBE generar las clases en las siguientes ubicaciones específicas, respetando los paquetes y la estructura de directorios:

* **`app/src/main/java/com/yourpackage/domain/`**
    * `entities/`: (Ej. `User.kt`, `Product.kt`) - Clases de datos inmutables (preferiblemente `data class`).
    * `usecases/`:
        * `interfaces/`: (Ej. `GetUserUseCase.kt`, `AddProductUseCase.kt`) - Interfaces para cada caso de uso.
        * `implementations/`: (Ej. `GetUserUseCaseImpl.kt`) - Implementaciones concretas de las interfaces de casos de uso.
    * `repositories/`: (Ej. `UserRepository.kt`, `ProductRepository.kt`) - Interfaces que definen los contratos para la interacción con la capa de datos.
    * **`di/` (Opcional, para módulos de Hilt/Koin que provean dependencias de Dominio):**
        * (Ej. `DomainModule.kt`) - Contiene la configuración de inyección para casos de uso o cualquier otra dependencia puramente de dominio.

* **`app/src/main/java/com/yourpackage/data/`**
    * `repositories/impl/`: (Ej. `UserRepositoryImpl.kt`) - Implementaciones de las interfaces de repositorio de `domain`.
    * `datasources/`:
        * `local/`: (Ej. `UserLocalDataSource.kt`) - Clases para acceder a fuentes de datos locales (Room, SharedPreferences).
        * `remote/`: (Ej. `UserRemoteDataSource.kt`) - Clases para acceder a APIs remotas (Retrofit, Ktor Client).
    * `models/`: (Ej. `UserApiModel.kt`, `UserDbModel.kt`) - Clases de datos específicas de la capa `data` (JSON, DB schemas).
    * `mappers/`: (Ej. `UserMapper.kt`) - Clases o funciones para mapear entre `data models` y `domain entities` y viceversa.
    * **`di/` (Obligatorio, para módulos de Hilt/Koin que provean dependencias de Datos):**
        * (Ej. `DataModule.kt`, `NetworkModule.kt`, `DatabaseModule.kt`) - Contiene la configuración de inyección para repositorios, data sources, servicios de red, bases de datos.

* **`app/src/main/java/com/yourpackage/presentation/`**
    * `ui/screens/<feature_name>/`: (Ej. `home/HomeScreen.kt`, `home/HomeContent.kt`) - Un paquete por cada pantalla o feature principal, conteniendo los Composables `Screen` y `Content`.
    * `ui/components/`: (Ej. `AppToolbar.kt`, `LoadingIndicator.kt`) - Componentes Composable reutilizables y genéricos.
    * `viewmodels/`: (Ej. `HomeViewModel.kt`) - ViewModels asociados a cada pantalla o sección de UI.
    * **`navigation/`:** (Ej. `AppNavGraph.kt`, `NavRoutes.kt`) - Lógica de navegación. Al generar rutas de navegación para Jetpack Compose Navigation, Gemini DEBE utilizar el enfoque de "type-safe navigation" o "navigation with arguments" a través de objetos de ruta/sealed classes que encapsulen la ruta y sus argumentos (ej. `object Home : NavRoute`, `data object Detail : NavRoute { val argument: String }`). No se permite la construcción manual de rutas con strings crudas y argumentos directamente en la URL.
    * **`di/` (Obligatorio, para módulos de Hilt/Koin que provean dependencias de Presentación o a nivel de aplicación):**
        * (Ej. `AppModule.kt`, `ViewModelModule.kt`) - Contiene la configuración de inyección para ViewModels, utilidades a nivel de UI, o cualquier otra dependencia que necesite ser configurada en la capa de aplicación principal (ej. Context, Application).

---

## 7. Inyección de Dependencias (DI): Hilt o Koin

* **Inyección de Dependencias Obligatoria:** Todas las dependencias deben ser inyectadas, preferiblemente a través de **inyección de constructor (Constructor Injection)**. Evitar Service Locators o Singletons globales manuales.
* **Elección de Framework de DI:**
    * **Hilt (Recomendado por defecto para proyectos Android):**
        * **Uso:** Prioritario para proyectos grandes, a largo plazo, o cuando se busca la máxima integración con los componentes de Android y la verificación en tiempo de compilación.
        * **Sintaxis:** Utiliza `@AndroidEntryPoint` para componentes Android, `@HiltViewModel` para ViewModels. Define módulos de Hilt (`@Module`, `@InstallIn`) para proporcionar dependencias (ej. `DataModule`, `DomainModule`, `AppModule`).
    * **Koin (Alternativa para proyectos específicos):**
        * **Uso:** Considerar para MVPs, proyectos más pequeños, o cuando la simplicidad de configuración y la ausencia de generación de código sean prioritarias.
        * **Sintaxis:** Define módulos de Koin (`module { ... }`) y declara los tipos a inyectar (ej. `factory`, `single`, `viewModel`).
* **Coherencia:** Una vez seleccionado un framework de DI para el proyecto, Gemini debe adherirse exclusivamente a él.

---

## 8. Estrategia de Pruebas Obligatoria: Pirámide de Pruebas Completa

**Gemini DEBE generar los tests apropiados y de alta calidad para cada componente del código generado.** Sigue la pirámide de pruebas: más tests unitarios, menos tests de integración, mínimos tests de UI.

* **Unit Tests:**
    * **Objetivo:** Verificar la lógica de una unidad de código aislada.
    * **Alcance:**
        * **Capa de Dominio:** Los casos de uso (interfaces e implementaciones) y las entidades DEBEN tener Unit Tests exhaustivos. **Obligatorio mockear cualquier dependencia (ej. repositorios).**
        * **Capa de Presentación:** Los ViewModels DEBEN ser testeados unitariamente. **Obligatorio mockear los casos de uso** para simular diferentes escenarios de negocio.
    * **Herramientas:** JUnit 5 (preferiblemente), Mockito/MockK para mocking, Turbine para probar Kotlin Flows.
    * **Ubicación:** `app/src/test/java/com/yourpackage/` (reflejando la estructura de paquetes del `main` para facilitar la navegación).
    * **Principios:** Determinismo, rapidez, aislamiento.

* **Integration Tests:**
    * **Objetivo:** Verificar la interacción correcta entre dos o más componentes de diferentes capas o subsistemas.
    * **Alcance:**
        * Ej. Probar un ViewModel con un caso de uso REAL, pero con el repositorio MOCKEADO.
        * Ej. Probar la interacción entre un repositorio y un DataSource real (ej. una base de datos Room en memoria para pruebas).
    * **Herramientas:** JUnit, frameworks de DI para construir el grafo de dependencias de prueba.
    * **Ubicación:** `app/src/test/java/com/yourpackage/` (para tests que no requieren un emulador) o `app/src/androidTest/java/com/yourpackage/` (para tests que sí lo requieren, como Room).

* **UI Tests (Instrumented Tests):**
    * **Objetivo:** Verificar la interacción del usuario con la interfaz de usuario, el comportamiento visual y la integración de la capa de presentación con el ViewModel.
    * **Alcance:** Probar `Composable Screens` y `Composable Content`.
    * **Herramientas:** JUnit, `androidx.compose.ui.test` (Compose Test Rule, findNodeWithTag, performClick, assertIsDisplayed, etc.).
    * **Ubicación:** `app/src/androidTest/java/com/yourpackage/` (reflejando la estructura de paquetes del `main`).
    * **Principios:** Deben ser representativos de los flujos de usuario clave.

---

## 9. Escalabilidad y Mantenibilidad Avanzadas

* **Modularización Robusta:**
    * Fomentar la creación de módulos Gradle separados para cada capa (`:domain`, `:data`, `:presentation`) o por característica/feature (`:feature:home`, `:feature:profile`).
    * Esta separación mejora significativamente los tiempos de compilación, refuerza la separación de preocupaciones y facilita la asignación de equipos en proyectos grandes.
* **Manejo de Estados Consistente:**
    * Implementar un enfoque claro y consistente para el manejo de estados en Compose, preferiblemente **Unidirectional Data Flow (UDF)**.
    * Los ViewModels DEBEN exponer el estado de la UI como `StateFlow` o `LiveData` (preferiblemente `StateFlow`) y los eventos como `SharedFlow` o `Channel`.
* **Manejo de Errores Global:**
    * Definir y aplicar una estrategia de manejo de errores uniforme en todas las capas. Los errores deben ser capturados lo más cerca posible de su origen (ej. `data sources`), transformados en errores de dominio y manejados en la capa de presentación para notificar al usuario.
* **Control de Concurrencia con Coroutines:**
    * Utilizar exclusivamente **Kotlin Coroutines** para toda la gestión de operaciones asíncronas y concurrentes.
    * Asegurar el uso correcto de `Dispatchers` para ejecutar el trabajo en el hilo adecuado (ej. `Dispatchers.IO` para operaciones de red/DB, `Dispatchers.Main` para actualizaciones de UI, `Dispatchers.Default` para operaciones intensivas en CPU).
* **Documentación de Código Esencial (KDoc):**
    * Agregar comentarios **KDoc** para clases, interfaces, funciones públicas y cualquier componente complejo que requiera una explicación clara de su propósito, parámetros, valores de retorno y posibles excepciones.
    * Evitar comentarios redundantes que solo describan lo que el código ya dice.

---

## 10. Procesos de Desarrollo y Herramientas

* **Verificación de Compilación Obligatoria:** Después de CADA cambio significativo en el código (ej. adición de una función, clase, pantalla, refactorización, integración de una nueva dependencia), **se DEBE realizar una compilación completa del proyecto** para identificar y corregir errores de compilación inmediatamente.
* **Navegación de Tipos Seguros Obligatoria:** Utilizar siempre el componente de navegación de Jetpack Compose con un enfoque de **tipos seguros**. Esto implica definir rutas y argumentos usando objetos Kotlin (sealed classes o data objects) y pasar datos a través del grafo de navegación de forma segura. **NO se permite la construcción manual de rutas con strings crudas y argumentos concatenados.**
* **Gestión de Dependencias Centralizada:** Todas las dependencias externas DEBEN ser agregadas y gestionadas utilizando el **catálogo de versiones en el fichero `libs.versions.toml`**.
* **Sincronización del Proyecto:** Después de cualquier modificación en los archivos `build.gradle` o `libs.versions.toml`, se DEBE realizar una sincronización del proyecto.

---

**Nota para Gemini:** `com.yourpackage` SIEMPRE debe ser reemplazado por el package raíz real del proyecto (ej. `com.example.myapp`). Cualquier referencia a este placeholder debe ser sustituida por el valor correcto.
