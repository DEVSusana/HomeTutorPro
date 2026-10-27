# HomeTutorPro: Universal Engineering Standards & Review Rules

This document establishes the official architectural, security, quality, and documentation standards for HomeTutorPro. All automated reviewers, agent skills, and developers must strictly adhere to these boundaries.

---

## 1. Component Responsibility Matrix (Separation of Concerns & SRP)

To prevent false positives and hallucinated violations, each file must only be evaluated against the strict scope of its component type.

### A. Room Data Access Objects (`data/local/dao/` - `@Dao`)
* **IN SCOPE:**
  - Correct Room annotations (`@Query`, `@Insert`, `@Update`, `@Delete`).
  - Strict parameter binding with `:paramName` to guarantee SQL injection immunity.
  - Reactive returns with `Flow<T>` or asynchronous one-shot queries with `suspend fun`.
  - Filtering by `pendingDelete = 0` and multi-tenant security (`professorId`).
* **STRICTLY OUT OF SCOPE (Do NOT flag as violations):**
  - Whole-database file encryption (e.g. SQLCipher). A DAO is an interface; encryption is configured at the database level in `DatabaseModule`/`AppDatabase`.
  - Business rule omissions that belong to sibling DAOs (e.g. `ScheduleDao` handles base recurring slots; `ScheduleExceptionDao` handles date exceptions and cancellations).

### B. Room Entities (`data/local/entities/` - `@Entity`)
* **IN SCOPE:**
  - `@Entity(tableName = "...")` and `@PrimaryKey`.
  - Supported SQLite data types or TypeConverters.
  - Proper indexing on foreign keys and frequently queried fields.
* **STRICTLY OUT OF SCOPE:**
  - Business logic or domain methods (entities are data holders).
  - Framework encryption of columns (unless an explicit `@TypeConverter` or cipher field is required).

### C. Domain Entities (`domain/entities/`)
* **IN SCOPE:**
  - Pure Kotlin immutable data classes (`data class`).
  - Zero dependencies on Android SDK (`android.*`, `androidx.*`), Room, Firebase, or Presentation layers.
* **STRICTLY OUT OF SCOPE:**
  - UI state or Room annotations.

### D. Use Cases (`domain/usecases/`)
* **IN SCOPE:**
  - Single public `operator fun invoke(...)`.
  - Pure Kotlin domain logic and orchestration.
  - Explicit business validation and domain error returns (e.g. `Result<T, DomainError>`).
  - Constructor injection with `@Inject constructor(...)`.
* **STRICTLY OUT OF SCOPE:**
  - Direct persistence calls without a repository contract.
  - Android framework dependencies (Context, Views, ViewModels).

### E. Repositories (`data/repository/` and `domain/repository/`)
* **IN SCOPE:**
  - Data layer implementations must implement the pure Kotlin contract in `domain/repository/`.
  - Mediation between data models/entities and domain entities using mappers.
  - Safe exception handling using `safeIoCall` or typed domain error results.
  - Thread safety using `Dispatchers.IO` for disk/network operations.
* **STRICTLY OUT OF SCOPE:**
  - UI presentation dependencies.

### F. ViewModels (`presentation/.../viewmodels/` or `presentation/.../`)
* **IN SCOPE:**
  - Expose immutable `StateFlow<UiState>` to the UI.
  - Keep `MutableStateFlow` strictly private (`private val _uiState`).
  - Single-use UI events (toasts, navigation) mediated by Channels or SharedFlows.
  - Business orchestration through Domain UseCases, never direct DAO/DataSource access.
  - Annotated with `@HiltViewModel`.
* **STRICTLY OUT OF SCOPE:**
  - Storing Views, Activities, or raw Activity Contexts in properties.

### G. Jetpack Compose UI (`presentation/ui/` or `presentation/.../`)
* **IN SCOPE:**
  - Separation into `*Screen` (ViewModel collector & Scaffold) and `*Content` (stateless presentation with state hoisting).
  - Material 3 components and extended icons.
* **STRICTLY OUT OF SCOPE:**
  - Direct database or repository access.

---

## 2. Security Standards (OWASP Mobile Top 10)

1. **No Hardcoded Secrets (OWASP M1):** Zero API keys, private tokens, passwords, or static symmetric keys in code.
2. **Cryptographic Integrity (OWASP M2/M4):**
   - Sensitive user tokens/credentials must use AndroidKeyStore via `SecureAuthManager` / `AndroidCryptographyProvider` (AES/GCM/NoPadding).
   - Passwords must be hashed using PBKDF2WithHmacSHA256 with cryptographically random salts (managed by `Pbkdf2PasswordHasher`).
3. **Safe Concurrency (OWASP M6):** Zero detached `GlobalScope.launch`. Coroutines must be launched in structured scopes (`viewModelScope`, `ApplicationScope`).
4. **Information Disclosure (OWASP M7):** Never log plaintext passwords, sensitive student PII, or security tokens with `Log.*` or `println`. Use `SafeLogger`.

---

## 3. KDoc Documentation Standards

1. **Public API Coverage:** All public interfaces, classes, and public member functions must have a KDoc comment.
2. **Tag Hygiene:** 
   - Every parameter must have an `@param` tag.
   - Every non-Unit returning function must have an `@return` tag explaining the output.
3. **Language:** English only for all KDoc comments and code identifiers.

---

## 4. Code Quality & Hygiene (SOLID & Clean Code)

1. **Strict English:** All names (classes, functions, parameters, variables) and comments must be in English.
2. **Single Responsibility:** Classes must have a clear, cohesive purpose.
3. **DRY & Dead Code:** No commented-out legacy code, unused imports, or duplicate logic.
