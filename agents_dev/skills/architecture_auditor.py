import os
import json
import config

def get_related_project_context(file_path: str) -> str:
    """
    Scans the codebase surrounding the target file to provide architectural context
    and avoid false positives due to Single Responsibility Principle (SRP) separation.
    """
    context_lines = []
    abs_path = os.path.abspath(file_path)
    file_dir = os.path.dirname(abs_path)
    filename = os.path.basename(file_path)

    # 1. Sibling classes in the same directory
    if os.path.exists(file_dir):
        siblings = [f for f in sorted(os.listdir(file_dir)) if f.endswith(".kt") and f != filename]
        if siblings:
            context_lines.append(f"Sibling classes in same directory ({os.path.basename(file_dir)}):")
            for s in siblings:
                context_lines.append(f"  - {s}")

    # 2. Check for related directories in Clean Architecture layers
    root_pkg = "app/src/main/java/com/devsusana/hometutorpro"
    if "data/local/dao" in file_path:
        entities_dir = os.path.join(root_pkg, "data/local/entities")
        if os.path.exists(entities_dir):
            entities = [f for f in sorted(os.listdir(entities_dir)) if f.endswith(".kt")]
            context_lines.append(f"\nRelated local Room entities (data/local/entities):")
            for e in entities:
                context_lines.append(f"  - {e}")
    elif "data/repository" in file_path:
        repo_interfaces_dir = os.path.join(root_pkg, "domain/repository")
        if os.path.exists(repo_interfaces_dir):
            repos = [f for f in sorted(os.listdir(repo_interfaces_dir)) if f.endswith(".kt")]
            context_lines.append(f"\nRelated domain repository contracts (domain/repository):")
            for r in repos:
                context_lines.append(f"  - {r}")
    elif "domain/usecases" in file_path:
        domain_entities_dir = os.path.join(root_pkg, "domain/entities")
        if os.path.exists(domain_entities_dir):
            entities = [f for f in sorted(os.listdir(domain_entities_dir)) if f.endswith(".kt")]
            context_lines.append(f"\nRelated domain entities (domain/entities):")
            for e in entities:
                context_lines.append(f"  - {e}")

    return "\n".join(context_lines) if context_lines else "No related files identified."

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

    project_context = get_related_project_context(file_path)

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
       
    7. Business Rules Compliance & Context Awareness:
       - Inflexible compliance with domain logic specified in BUSINESS_RULES.md.
       - Single-file audit scope: DO NOT report that entities, DAOs, or methods mentioned in BUSINESS_RULES.md are missing from this specific file if they belong to other dedicated classes (e.g. ScheduleDao handles recurring weekly schedules, whereas ScheduleExceptionDao handles date-specific exceptions and cancellations; do not flag ScheduleDao for not defining exceptions).
       - Always inspect the provided "PROJECT CONTEXT & RELATED CLASSES" section before assuming a feature is missing or unaccounted for. Only report genuine architectural boundary violations within the code of the audited file.
    """

    if output_format == "json":
        prompt = f"""
        Act as a Principal Android Software Architect.
        Perform an exhaustive Clean Architecture audit on this Kotlin file:
        
        {architecture_checklist}
        
        PROJECT CONTEXT & RELATED CLASSES:
        {project_context}
        
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
        
        PROJECT CONTEXT & RELATED CLASSES:
        {project_context}
        
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
