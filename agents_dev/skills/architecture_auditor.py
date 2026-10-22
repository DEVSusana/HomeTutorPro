import os
import json
import config

def run_architecture_audit(file_path: str, output_format: str = "text") -> str:
    """
    Executes a strict Clean Architecture audit on a Kotlin file.
    Verifies boundary integrity between Domain, Data, and Presentation layers,
    UDF compliance, UseCase single-responsibility, Hilt constructor injection,
    data mappers, and domain business rules adherence.
    """
    client = config.get_client()

    rules = ""
    if os.path.exists("AGENTS.md"):
        with open("AGENTS.md", "r", encoding="utf-8") as f:
            rules = f.read()

    business_rules = ""
    if os.path.exists("BUSINESS_RULES.md"):
        try:
            with open("BUSINESS_RULES.md", "r", encoding="utf-8") as f:
                business_rules = f.read()
        except Exception:
            pass

    with open(file_path, "r", encoding="utf-8") as f:
        code = f.read()

    architecture_checklist = """
    Android Clean Architecture & Design Verification Checklist:
    
    1. Layer Boundary Enforcement:
       - Domain Layer (`domain/`): Must be pure Kotlin. ABSOLUTELY ZERO dependencies on Android SDK (`android.*`, `androidx.*`), Room annotations (`@Entity`, `@Dao`), Firebase, or Presentation/UI.
       - Data Layer (`data/`): Must implement repository interfaces defined in `domain/repository/`. Must NOT expose Room entities or Firebase models to Domain or Presentation.
       - Presentation Layer (`presentation/`): Must observe Domain UseCases via ViewModels. Must NOT access DAOs, DataSources, or Room database classes directly.
       
    2. UseCase & Single Responsibility (SOLID):
       - Each UseCase class must implement a single public `operator fun invoke(...)`.
       - Must contain genuine business rules or domain orchestration; do not bypass validation.
       
    3. Mappers & Data Separation:
       - Dedicated mapper functions (`toDomain()`, `toEntity()`, `toDataModel()`) must mediate all data transfers between Data and Domain layers.
       
    4. Unidirectional Data Flow (UDF) & State Management:
       - ViewModels must expose immutable `StateFlow<UiState>` to the UI.
       - Mutable state (`MutableStateFlow`) must remain strictly private (`private val _uiState`).
       - Single-use UI events (navigation, snackbars) must be mediated by Channels or SharedFlows.
       
    5. Dependency Injection (Hilt):
       - Mandatory constructor injection (`@Inject constructor(...)`) for ViewModels, UseCases, and Repositories.
       - No manual Singletons, Service Locators, or Global static state.
       
    6. Language & Naming Conventions:
       - Strict English naming for classes, functions, properties, and comments (no Spanish code tokens).
       
    7. Business Rules Compliance:
       - Inflexible compliance with domain logic specified in BUSINESS_RULES.md.
    """

    if output_format == "json":
        prompt = f"""
        Act as a Principal Android Software Architect.
        Perform an exhaustive Clean Architecture audit on this Kotlin file:
        
        {architecture_checklist}
        
        CODING RULES (AGENTS.md):
        {rules}
        
        BUSINESS RULES (BUSINESS_RULES.md):
        {business_rules}
        
        CODE TO AUDIT ({file_path}):
        {code}
        
        Return a strict JSON object matching this schema:
        {{
          "summary": {{
            "file": "{file_path}",
            "critical_count": <int>,
            "warning_count": <int>,
            "info_count": <int>
          }},
          "findings": [
            {{
              "severity": "critical" | "warning" | "info",
              "category": "architecture",
              "file": "{file_path}",
              "line": <line_number_or_0>,
              "title": "<Concise issue title>",
              "message": "<Detailed explanation of the architectural violation>",
              "suggestion": "<Concrete Kotlin architectural fix or code recommendation>",
              "remediation": "<Concrete Kotlin architectural fix or code recommendation>"
            }}
          ]
        }}
        If the code is 100% compliant with Clean Architecture and has zero violations, return findings as an empty list [].
        """
        from google.genai import types
        response = config.generate_content_with_retry(
            client=client,
            contents=prompt,
            config_args=types.GenerateContentConfig(response_mime_type="application/json")
        )
        return response.text
    else:
        prompt = f"""
        Act as a Principal Android Software Architect.
        Perform an exhaustive Clean Architecture audit on this Kotlin file:
        
        {architecture_checklist}
        
        CODING RULES (AGENTS.md):
        {rules}
        
        BUSINESS RULES (BUSINESS_RULES.md):
        {business_rules}
        
        CODE TO AUDIT ({file_path}):
        {code}
        
        Provide an executive architectural report with:
        1. Architectural Status (CERTIFIED CLEAN / ARCHITECTURAL WARNINGS / SEVERE VIOLATIONS).
        2. Detailed Layer Boundary, UDF, and SOLID analysis with line numbers.
        3. Step-by-step Kotlin refactoring guidance.
        """
        response = config.generate_content_with_retry(
            client=client,
            contents=prompt
        )
        return response.text
