"""
Context Engine for Universal Agent Review Loop
Determines the architectural role of each Kotlin file, isolates its strict
responsibilities, gathers sibling context, and produces focused, hallucination-free prompts.
"""
import os
import re

COMPONENT_DAO = "ROOM_DAO"
COMPONENT_ROOM_ENTITY = "ROOM_ENTITY"
COMPONENT_DOMAIN_ENTITY = "DOMAIN_ENTITY"
COMPONENT_USECASE = "USECASE"
COMPONENT_REPO_INTERFACE = "REPOSITORY_INTERFACE"
COMPONENT_REPO_IMPL = "REPOSITORY_IMPL"
COMPONENT_VIEWMODEL = "VIEWMODEL"
COMPONENT_UI = "COMPOSE_UI"
COMPONENT_MAPPER = "MAPPER"
COMPONENT_SYNC = "SYNC_WORKER"
COMPONENT_GENERIC = "GENERIC_KOTLIN"


def classify_component(file_path: str, code: str = "") -> str:
    """Classifies a Kotlin file into an architectural component type."""
    norm_path = file_path.replace("\\", "/")

    if "data/local/dao/" in norm_path or "@Dao" in code:
        return COMPONENT_DAO
    if "data/local/entities/" in norm_path or "@Entity" in code:
        return COMPONENT_ROOM_ENTITY
    if "domain/entities/" in norm_path:
        return COMPONENT_DOMAIN_ENTITY
    if "domain/usecases/" in norm_path or re.search(r"operator\s+fun\s+invoke", code):
        return COMPONENT_USECASE
    if "domain/repository/" in norm_path:
        return COMPONENT_REPO_INTERFACE
    if "data/repository/" in norm_path or ("Repository" in norm_path and "Impl" in norm_path):
        return COMPONENT_REPO_IMPL
    if "viewmodels/" in norm_path or "ViewModel" in norm_path:
        return COMPONENT_VIEWMODEL
    if "presentation/ui/" in norm_path or "Screen.kt" in norm_path or "Content.kt" in norm_path:
        return COMPONENT_UI
    if "mappers/" in norm_path or "Mapper.kt" in norm_path:
        return COMPONENT_MAPPER
    if "sync/" in norm_path:
        return COMPONENT_SYNC

    return COMPONENT_GENERIC


def get_component_boundaries(component_type: str) -> dict:
    """Returns the in-scope checklist and strict anti-hallucination out-of-scope boundaries."""
    boundaries = {
        COMPONENT_DAO: {
            "title": "Room Data Access Object (@Dao)",
            "in_scope": [
                "Parameterized queries (:param) to prevent SQL injection",
                "Reactive Flow<T> or suspend functions for queries",
                "Filtering by pendingDelete = 0 and multi-tenant professorId",
                "KDoc coverage on public query and mutation methods (@param, @return)"
            ],
            "out_of_scope": [
                "Database-level encryption (SQLCipher) - Configured in DatabaseModule, NEVER inside a DAO",
                "Omission of rules that belong to sibling DAOs (e.g. ScheduleDao base vs ScheduleExceptionDao exceptions)"
            ]
        },
        COMPONENT_ROOM_ENTITY: {
            "title": "Room Database Entity (@Entity)",
            "in_scope": [
                "@Entity and primary key definitions",
                "Supported SQLite types or registered TypeConverters",
                "Proper indexes on foreign keys and search columns"
            ],
            "out_of_scope": [
                "Business rules or complex methods (entities are data carriers)",
                "Direct cryptographic operations inside entity fields"
            ]
        },
        COMPONENT_DOMAIN_ENTITY: {
            "title": "Pure Domain Entity",
            "in_scope": [
                "Pure Kotlin immutable data class",
                "Zero dependencies on Android SDK, Room, Firebase, or UI"
            ],
            "out_of_scope": [
                "Database schema details, Room annotations, or UI lifecycle"
            ]
        },
        COMPONENT_USECASE: {
            "title": "Domain Use Case",
            "in_scope": [
                "Single public operator fun invoke(...)",
                "Pure Kotlin business validations and explicit domain error returns",
                "Hilt constructor injection (@Inject constructor)",
                "Zero Android framework dependencies (Context, Views, ViewModels)"
            ],
            "out_of_scope": [
                "Direct database queries without repository contract",
                "UI rendering logic"
            ]
        },
        COMPONENT_REPO_IMPL: {
            "title": "Repository Implementation (Data Layer)",
            "in_scope": [
                "Implements pure domain repository contract",
                "Maps between data models and domain entities",
                "Thread safety: Dispatchers.IO for storage/network operations",
                "Safe error translation into domain results or safeIoCall"
            ],
            "out_of_scope": [
                "UI Presentation imports or View state management"
            ]
        },
        COMPONENT_VIEWMODEL: {
            "title": "Jetpack Compose ViewModel",
            "in_scope": [
                "Immutable StateFlow<UiState> exposed to UI",
                "Private MutableStateFlow (_uiState)",
                "Single-use UI events with Channel or SharedFlow",
                "Orchestrates UseCases, never accesses DAOs directly",
                "@HiltViewModel annotation and constructor injection"
            ],
            "out_of_scope": [
                "Storing Views or Context instances in ViewModel properties"
            ]
        },
        COMPONENT_UI: {
            "title": "Jetpack Compose Screen / Component",
            "in_scope": [
                "Screen (stateful with ViewModel) vs Content (pure presentation with state hoisting)",
                "Material 3 design guidelines and extended icons",
                "Zero direct database or repository calls"
            ],
            "out_of_scope": [
                "Backend data storage or network protocol definitions"
            ]
        }
    }

    return boundaries.get(component_type, {
        "title": "General Kotlin Architecture Component",
        "in_scope": [
            "Clean Architecture layer boundaries",
            "No hardcoded credentials or plaintext sensitive logs",
            "Public KDoc documentation with @param and @return",
            "Strict English naming and SOLID principles"
        ],
        "out_of_scope": [
            "Demanding responsibilities that belong to other layers"
        ]
    })


def get_related_project_context(file_path: str) -> str:
    """Discovers sibling files and architectural layer contracts to provide grounding."""
    context_lines = []
    abs_path = os.path.abspath(file_path)
    file_dir = os.path.dirname(abs_path)
    filename = os.path.basename(file_path)

    # 1. Sibling classes in the same directory
    if os.path.exists(file_dir):
        siblings = [f for f in sorted(os.listdir(file_dir)) if f.endswith(".kt") and f != filename]
        if siblings:
            context_lines.append(f"Sibling classes in same package ({os.path.basename(file_dir)}):")
            for s in siblings[:8]:
                context_lines.append(f"  - {s}")
            if len(siblings) > 8:
                context_lines.append(f"  ... and {len(siblings) - 8} more.")

    # 2. Related directory mapping in Clean Architecture
    root_pkg = "app/src/main/java/com/devsusana/hometutorpro"
    if "data/local/dao" in file_path:
        entities_dir = os.path.join(root_pkg, "data/local/entities")
        if os.path.exists(entities_dir):
            entities = [f for f in sorted(os.listdir(entities_dir)) if f.endswith(".kt")]
            context_lines.append("\nRelated Room entities (data/local/entities):")
            for e in entities[:6]:
                context_lines.append(f"  - {e}")
    elif "data/repository" in file_path:
        repo_interfaces_dir = os.path.join(root_pkg, "domain/repository")
        if os.path.exists(repo_interfaces_dir):
            repos = [f for f in sorted(os.listdir(repo_interfaces_dir)) if f.endswith(".kt")]
            context_lines.append("\nRelated domain repository contracts (domain/repository):")
            for r in repos[:6]:
                context_lines.append(f"  - {r}")

    return "\n".join(context_lines) if context_lines else "No direct sibling dependencies found."
