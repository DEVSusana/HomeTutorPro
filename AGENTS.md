# Comprehensive Android Studio Coding Guide

**Objective:** This document establishes strict and explicit guidelines for generating code in Android projects, ensuring adherence to Kotlin, Jetpack Compose, Clean Architecture, and professional development best practices.

---

## 1. Mandatory Technological Foundations

* **Primary Language:** Kotlin (exclusive for all application and test code).
* **UI Framework:** Jetpack Compose (exclusive for building user interfaces).
* **Concurrency Management:** Kotlin Coroutines (exclusive for asynchronous and concurrent operations).
* **Local Database:** Room Database (preferred for local data persistence).
* **Cloud Database:** Firebase Cloud Firestore (preferred for data synchronization and cloud database).
* **REST API Integration:** Retrofit (preferred for external REST API connections and consumption).

---

## 2. Mandatory Software Architecture: Clean Architecture with MVVM

Strictly implement **Clean Architecture** with **MVVM (Model-View-ViewModel)** in the presentation layer. The separation of concerns between layers must be absolute.

* **Domain Layer (`domain`):**
    * **Purpose:** Contains the core business logic of the application. It is completely independent of any framework, database, or UI.
    * **Content:** Entities, use case interfaces (Use Cases), repository interfaces.
    * **Principle:** Pure business logic, with no dependencies on the Android SDK or external frameworks (except standard Kotlin).
    * **Mandatory Business Rules:** All logic and use case implementations in this layer must inflexibly comply with the specifications in the `BUSINESS_RULES.md` file at the root of the project.

* **Data Layer (`data`):**
    * **Purpose:** Responsible for data retrieval, transformation, and persistence. Implements the repository interfaces defined in the Domain layer.
    * **Content:** Concrete repository implementations, Data Sources (local and remote), layer-specific data models (`data models`), mappers to convert between `data models` and `domain` entities.
    * **Principle:** Abstracts the underlying data source from the business logic.

* **Presentation Layer (`presentation` / `ui`):**
    * **Purpose:** Manages the user interface and user interaction. Observes states and dispatches events to business logic.
    * **Content:** Composable Screens, Composable Components, ViewModels, navigation logic.
    * **Principle:** The UI only knows how to display data and react to user input; it does not contain business logic directly.

---

## 3. Strict Naming Conventions and Code Style

* **Inflexible Adherence to Compose API Guidelines:** [https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-api-guidelines.md](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-api-guidelines.md)
* **Descriptive and Unambiguous Names:** All code entities (functions, variables, classes, packages, modules) must have names that clearly reflect their purpose and responsibility. Avoid ambiguous abbreviations.
* **Case Convention:**
    * **`camelCase`:** For variable, function, and parameter names (e.g., `myLocalVariable`, `calculateTotalPrice()`).
    * **`PascalCase`:** For class, interface, object, and Kotlin file names (e.g., `MyAwesomeClass`, `HomeScreen.kt`).
    * **`SCREAMING_SNAKE_CASE`:** For constants (e.g., `API_KEY`).
* **Language:** **All code (names, comments, messages, etc.) must be in English.** This includes package names, classes, functions, and variables.
* **Clean Code:**
    * **SOLID Principles:** The implementation of all components must strictly adhere to SOLID principles (Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, Dependency Inversion).
    * **Small Functions and Classes:** Each function must have a single responsibility. Classes must be cohesive and have well-defined responsibilities.
    * **DRY (Don't Repeat Yourself):** Avoid code duplication at all costs. Refactor and reuse components.
    * **Readability and Maintainability:** Code must be easily understandable by any developer, even without excessive comments. Prioritize clarity over brevity.
    * **Testability:** Generated code must be inherently easy to unit and integration test (see section 8). This implies low coupling and high cohesion.

---

## 4. UI Components with Jetpack Compose

* **Mandatory Libraries:** Use `material3` and Compose extended icons.
* **Mandatory Previews:** Each `Composable` representing significant visual UI (especially `Screen` and `Content`) must include at least one `@Preview` function.
* **Strict Composable Structure for Screens:**
    * **`Screen` Composable (Main):** Each application screen MUST have a main `Composable` with the `Screen` suffix (e.g., `HomeScreen`). This `Composable` will be the screen's entry point, responsible for:
        * Instantiating and managing the associated ViewModel (generally via dependency injection).
        * Collecting ViewModel states.
        * Passing states and event callbacks to the `Content` Composable.
        * Managing top-level components such as `Scaffold`, `SnackbarHost`, etc.
    * **`Content` Composable (Pure Presentation):** For each `Screen`, there MUST be a separate `Composable` with the `Content` suffix (e.g., `HomeContent`). This `Composable` is purely presentational:
        * Does **NOT** interact directly with ViewModels or business logic.
        * Receives **ALL** necessary states and event callbacks as function parameters (State Hoisting).
        * Its sole responsibility is to render the UI based on received parameters.
        * Must be easily reusable and independently testable.
* **Common and Reusable Components:** Create a `presentation.ui.components` package to host generic and reusable Composables across multiple screens (e.g., `AppToolbar.kt`, `LoadingIndicator.kt`, `ErrorDialog.kt`).
* **Stateless Composables:** Prioritize creating `Stateless Composables` whenever possible. State must be hoisted to parent Composables or, preferably, to ViewModels.

---

## 5. Layout Design in Compose: Recommended and Clear Rules

* **Use of `ConstraintLayout`:** Recommended for complex screens or `Content` with multiple relationships, cross-alignments, or visual dependencies. Not mandatory if a simple layout is better resolved with `Row`, `Column`, or `Box` without excessive nesting.
* **Use of `Row`, `Column`, `Box`:** Valid and preferred for simple or hierarchical layouts that read clearly. Avoid deep nesting; if the layout begins to be difficult to maintain, consider `ConstraintLayout`.
* **UI Tree Efficiency:** Prioritize flat hierarchies, `Lazy*` where appropriate, and avoid unnecessary containers.
* **State Hoisting in Compose:** `Content` should be stateless whenever reasonable. Screen UI state lives in the `Screen` and uses `rememberSaveable` when state must survive recreations.

---

## 6. Strict Class Organization by Layer and Folder

Gemini MUST generate classes in the following specific locations, respecting packages and directory structure:

* **`app/src/main/java/com/yourpackage/domain/`**
    * `entities/`: (e.g., `User.kt`, `Product.kt`) - Immutable data classes (preferably `data class`).
    * `usecases/`:
        * `interfaces/`: (e.g., `GetUserUseCase.kt`, `AddProductUseCase.kt`) - Interfaces for each use case.
        * `implementations/`: (e.g., `GetUserUseCaseImpl.kt`) - Concrete implementations of use case interfaces.
    * `repositories/`: (e.g., `UserRepository.kt`, `ProductRepository.kt`) - Interfaces defining contracts for interaction with the data layer.
    * **`di/` (Optional, for Hilt/Koin modules providing Domain dependencies):**
        * (e.g., `DomainModule.kt`) - Contains injection configuration for use cases or any pure domain dependency.

* **`app/src/main/java/com/yourpackage/data/`**
    * `repositories/impl/`: (e.g., `UserRepositoryImpl.kt`) - Implementations of `domain` repository interfaces.
    * `datasources/`:
        * `local/`: (e.g., `UserLocalDataSource.kt`) - Classes to access local data sources (Room, SharedPreferences).
        * `remote/`: (e.g., `UserRemoteDataSource.kt`) - Classes to access remote APIs (Retrofit, Ktor Client).
    * `models/`: (e.g., `UserApiModel.kt`, `UserDbModel.kt`) - Layer-specific data classes (`data` layer) (JSON, DB schemas).
    * `mappers/`: (e.g., `UserMapper.kt`) - Classes or functions to map between `data models` and `domain entities` and vice versa.
    * **`di/` (Mandatory, for Hilt/Koin modules providing Data dependencies):**
        * (e.g., `DataModule.kt`, `NetworkModule.kt`, `DatabaseModule.kt`) - Contains injection configuration for repositories, data sources, network services, databases.

* **`app/src/main/java/com/yourpackage/presentation/`**
    * `ui/screens/<feature_name>/`: (e.g., `home/HomeScreen.kt`, `home/HomeContent.kt`) - One package per screen or primary feature, containing `Screen` and `Content` Composables.
    * `ui/components/`: (e.g., `AppToolbar.kt`, `LoadingIndicator.kt`) - Reusable and generic Composable components.
    * `viewmodels/`: (e.g., `HomeViewModel.kt`) - ViewModels associated with each screen or UI section.
    * **`navigation/`:** (e.g., `AppNavGraph.kt`, `NavRoutes.kt`) - Navigation logic. When generating navigation routes for Jetpack Compose Navigation, Gemini MUST use the "type-safe navigation" or "navigation with arguments" approach via route objects/sealed classes encapsulating the route and its arguments (e.g., `object Home : NavRoute`, `data object Detail : NavRoute { val argument: String }`). Manual route construction with raw strings and concatenated arguments directly in the URL is NOT permitted.
    * **`di/` (Mandatory, for Hilt/Koin modules providing Presentation or application-level dependencies):**
        * (e.g., `AppModule.kt`, `ViewModelModule.kt`) - Contains injection configuration for ViewModels, UI-level utilities, or any dependency configured in the main application layer (e.g., Context, Application).

---

## 7. Dependency Injection (DI): Hilt or Koin

* **Mandatory Dependency Injection:** All dependencies must be injected, preferably via **Constructor Injection**. Avoid Service Locators or manual global Singletons.
* **DI Framework Choice:**
    * **Hilt (Recommended by default for Android projects):**
        * **Usage:** Priority for large, long-term projects, or when seeking maximum integration with Android components and compile-time verification.
        * **Syntax:** Uses `@AndroidEntryPoint` for Android components, `@HiltViewModel` for ViewModels. Defines Hilt modules (`@Module`, `@InstallIn`) to provide dependencies (e.g., `DataModule`, `DomainModule`, `AppModule`).
    * **Koin (Alternative for specific projects):**
        * **Usage:** Consider for MVPs, smaller projects, or when simplicity of configuration and absence of code generation are prioritized.
        * **Syntax:** Defines Koin modules (`module { ... }`) and declares types to inject (e.g., `factory`, `single`, `viewModel`).
* **Consistency:** Once a DI framework is selected for the project, Gemini must adhere exclusively to it.

---

## 8. Mandatory Testing Strategy: Full Testing Pyramid

**Gemini MUST generate appropriate and high-quality tests for each component of the generated code.** Follow the testing pyramid: more unit tests, fewer integration tests, minimal UI tests.

* **Unit Tests:**
    * **Objective:** Verify the logic of an isolated unit of code.
    * **Scope:**
        * **Domain Layer:** Use cases (interfaces and implementations) and entities MUST have comprehensive Unit Tests. **Mandatory to mock any dependency (e.g., repositories).**
        * **Presentation Layer:** ViewModels MUST be unit tested. **Mandatory to mock use cases** to simulate different business scenarios.
    * **Tools:** JUnit 5 (preferably), Mockito/MockK for mocking, Turbine for testing Kotlin Flows.
    * **Location:** `app/src/test/java/com/yourpackage/` (mirroring the package structure of `main` for easy navigation).
    * **Principles:** Determinism, speed, isolation.

* **Integration Tests:**
    * **Objective:** Verify the correct interaction between two or more components from different layers or subsystems.
    * **Scope:**
        * e.g., Testing a ViewModel with a REAL use case, but with a MOCKED repository.
        * e.g., Testing the interaction between a repository and a real DataSource (e.g., an in-memory Room database for tests).
    * **Tools:** JUnit, DI frameworks to construct the test dependency graph.
    * **Location:** `app/src/test/java/com/yourpackage/` (for tests not requiring an emulator) or `app/src/androidTest/java/com/yourpackage/` (for tests that do require an emulator, such as Room).

* **UI Tests (Instrumented Tests):**
    * **Objective:** Verify user interaction with the UI, visual behavior, and presentation layer integration with the ViewModel.
    * **Scope:** Test `Composable Screens` and `Composable Content`.
    * **Tools:** JUnit, `androidx.compose.ui.test` (Compose Test Rule, findNodeWithTag, performClick, assertIsDisplayed, etc.).
    * **Location:** `app/src/androidTest/java/com/yourpackage/` (mirroring the package structure of `main`).
    * **Principles:** Must be representative of key user flows.

---

## 9. Advanced Scalability and Maintainability

* **Robust Modularization:**
    * Encourage the creation of separate Gradle modules for each layer (`:domain`, `:data`, `:presentation`) or by feature (`:feature:home`, `:feature:profile`).
    * This separation significantly improves build times, reinforces separation of concerns, and facilitates team allocation on large projects.
* **Consistent State Management and UDF:**
    * Strictly implement the **Unidirectional Data Flow (UDF)** pattern for state management in Compose. State flows down (from ViewModel to UI) and events flow up (from UI to ViewModel).
    * ViewModels MUST expose UI state as a single immutable `StateFlow` of the screen's specific state type (e.g., `StateFlow<MyUiState>`), and single-use actions (events like navigation or Toasts) as `SharedFlow` or `Channel`.
    * Standardize the representation of asynchronous states and loads using a common sealed interface (`sealed interface` or `sealed class`) for UI states:
      ```kotlin
      sealed interface UiState<out T> {
          object Loading : UiState<Nothing>
          data class Success<out T>(val data: T) : UiState<T>
          data class Error(val errorMessage: String) : UiState<Nothing>
      }
      ```
      Alternatively, a state data class with explicit flags (`isLoading`, `error`, `data`) can be used if the screen requires maintaining previews or mixed states, but unidirectional flow must be strictly respected.
* **Global Error Handling:**
    * Define and apply a uniform error handling strategy across all layers. Errors must be caught as close to their source as possible (e.g., `data sources`), transformed into domain errors, and handled in the presentation layer to notify the user.
* **Concurrency Control with Coroutines:**
    * Exclusively use **Kotlin Coroutines** for all asynchronous and concurrent operation management.
    * Ensure the proper use of `Dispatchers` to execute work on the appropriate thread (e.g., `Dispatchers.IO` for network/DB operations, `Dispatchers.Main` for UI updates, `Dispatchers.Default` for CPU-intensive operations).
* **Essential Code Documentation (KDoc):**
    * Add **KDoc** comments for classes, interfaces, public functions, and any complex component requiring clear explanation of its purpose, parameters, return values, and potential exceptions.
    * Avoid redundant comments that merely restate what the code already says.

---

## 10. Development Processes and Tools

* **Mandatory Build Verification:** After EVERY significant code change (e.g., adding a function, class, screen, refactoring, integrating a new dependency), **a full project build MUST be executed** to identify and fix compilation errors immediately.
* **Mandatory Type-Safe Navigation:** Always use the Jetpack Compose navigation component with a **type-safe** approach. This implies defining routes and arguments using Kotlin objects (sealed classes or data objects) and passing data through the navigation graph safely. **Manual route construction with raw strings and concatenated arguments is NOT permitted.**
* **Centralized Dependency Management:** All external dependencies MUST be added and managed using the **version catalog in the `libs.versions.toml` file**.
* **Project Synchronization:** After any modification in `build.gradle` or `libs.versions.toml` files, a project synchronization MUST be performed.

---

## 11. Local Verification via Agents and Skills

You have a local audit agent orchestrator on your machine that you can run **optionally** to check code before pushing. Since the remote CI pipeline on GitHub executes these exact same automated validations upon opening or updating a Pull Request, local verification is purely diagnostic and is not part of the assistant's mandatory daily flow.

### Prerequisites and Configuration
The agent scripts and skills reside in the `agents_dev/` directory. They require setting the Gemini API key:
1. Configure the Gemini API key in your terminal:
   ```bash
   export GEMINI_API_KEY="your-api-key-here"
   ```

### Running Audits in the Terminal
To run local analysis on modified Kotlin files:
1. **Activate the Python virtual environment** of the agents project:
   ```bash
   source agents_dev/.venv_agente/bin/activate
   ```
2. **Run the agent orchestrator**:
   ```bash
   python agents_dev/skills/agent_orchestrator.py
   ```
   *Note:* By default, if no parameters are passed, the script automatically detects modified Kotlin files in your workspace (via `git status` / `git diff`). You can also force analysis of a specific file by passing it as an argument:
   ```bash
   python agents_dev/skills/agent_orchestrator.py app/src/main/java/com/devsusana/hometutorpro/data/repository/AuthRepositoryImpl.kt
   ```

The orchestrator will sequentially run the 5 audit agents (**Architecture**, **Security**, **Testing**, **KDocs**, and **Refactor**), applying 35-second pauses to respect the free API tier rate limits.

### Local Productivity Tools (Support Skills)
Three local scripts are available in `agents_dev/skills/` to automate repetitive development tasks:

1. **Test Skeleton Generator (`test_scaffolder.py`):**
   Creates the file and base skeleton for unit tests (with JUnit, MockK, and Coroutines) from a production Kotlin class.
   ```bash
   python agents_dev/skills/test_scaffolder.py app/src/main/java/com/devsusana/hometutorpro/domain/usecases/implementations/MyNewUseCase.kt
   ```
   *Note:* It generates the test in the corresponding path under `app/src/test/java/...` ready to be populated.

2. **Hilt Injection Validator (`hilt_checker.py`):**
   Quickly checks that new Use Case or Repository interfaces are declared in the project's Hilt DI modules.
   ```bash
   python agents_dev/skills/hilt_checker.py
   ```
   *Note:* If no arguments are specified, it automatically scans all modified Kotlin files in Git.

3. **KDocs Autocompleter (`kdoc_autocompleter.py`):**
   Identifies undocumented public classes and functions and automatically adds well-structured KDoc comments, overwriting the file.
   ```bash
   python agents_dev/skills/kdoc_autocompleter.py app/src/main/java/com/devsusana/hometutorpro/data/repository/MyRepositoryImpl.kt
   ```

---

**Note for Gemini:** The actual root package of this project is `com.devsusana.hometutorpro`. Any reference to the `com.yourpackage` placeholder in this guide must be replaced with this correct package in generated code.
