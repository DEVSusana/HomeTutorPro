# AGENT INSTRUCTIONS: CORE ANDROID ARCHITECTURE (STATIC RULES)

## 1. FUNDAMENTALS
- Language: **Kotlin** exclusively.
- UI Framework: **Jetpack Compose** exclusively.
- Concurrency: **Kotlin Coroutines** exclusively (Dispatchers.IO for I/O operations).

## 2. ARCHITECTURE AND FLOW
- Pattern: Strict **Clean Architecture / MVVM**.
- Data Flow: **Unidirectional Data Flow (UDF)**. ViewModels expose **StateFlow** (or SharedFlow) and accept **UiEvent**.
- Use Cases: **Single-Action Use Cases** (operator fun invoke()).
- Errors: Data Sources **MUST** transform exceptions into a `sealed class` (DomainError). No raw exceptions in presentation layer.

## 3. STYLE AND TESTING REQUIREMENTS
- **Language:** All code, function names, and KDoc **MUST be in English**.
- **Test Tags:** All interactive Composables **MUST** include a `Modifier.testTag("tag_descriptivo")`.
- **UI Tests:** Aserciones must be **semantic** (not based on raw text).
- **DI:** **Hilt** is the default and preferred framework.