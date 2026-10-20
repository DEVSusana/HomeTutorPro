import os
import sys
from typing import Dict, Any

# Asegurar que agents_dev/skills esté en sys.path para importar los scripts existentes
SKILLS_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "../skills"))
if SKILLS_DIR not in sys.path:
    sys.path.insert(0, SKILLS_DIR)

from core.system_tools import _resolve_path


def check_hilt_bindings(file_path: str) -> str:
    """Verifica si una interfaz de UseCase o Repository está registrada en los módulos de Hilt."""
    full_path = _resolve_path(file_path)
    if not os.path.exists(full_path):
        return f"ERROR: File '{file_path}' does not exist."

    filename = os.path.basename(full_path)
    if not filename.endswith(".kt"):
        return f"File '{filename}' is not a Kotlin file."

    class_name = filename.replace(".kt", "")
    is_usecase = "UseCase" in class_name
    is_repository = "Repository" in class_name

    if not is_usecase and not is_repository:
        return f"Class '{class_name}' is neither a UseCase nor a Repository. No Hilt module binding required."

    di_dir = _resolve_path("app/src/main/java/com/devsusana/hometutorpro/di")
    if not os.path.exists(di_dir):
        return f"WARNING: Hilt DI directory '{di_dir}' not found."

    found = False
    matching_module = None
    checked_files = []

    for root, dirs, files in os.walk(di_dir):
        for file in files:
            if file.endswith(".kt"):
                di_file_path = os.path.join(root, file)
                checked_files.append(file)
                try:
                    with open(di_file_path, "r", encoding="utf-8") as f:
                        if class_name in f.read():
                            found = True
                            matching_module = file
                            break
                except Exception:
                    pass
        if found:
            break

    if found:
        return f"PASS: Hilt binding for '{class_name}' verified in module '{matching_module}'."
    else:
        return (
            f"FAIL: No Hilt binding found for '{class_name}' in '{di_dir}'. "
            f"Scanned modules: {', '.join(checked_files)}. "
            f"Action needed: Add @Binds or @Provides for this interface in the corresponding Hilt module."
        )


def audit_architecture(file_path: str) -> str:
    """Audita un archivo Kotlin contra AGENTS.md y BUSINESS_RULES.md usando architecture_auditor."""
    full_path = _resolve_path(file_path)
    if not os.path.exists(full_path):
        return f"ERROR: File '{file_path}' not found."

    try:
        import architecture_auditor
        report = architecture_auditor.run_architecture_audit(full_path, output_format="text")
        return f"=== Architecture Audit for {os.path.basename(file_path)} ===\n{report}"
    except Exception as e:
        return f"ERROR: Failed running architecture audit: {str(e)}"


def audit_security(file_path: str) -> str:
    """Escanea el archivo en busca de claves hardcodeadas, naming en español o corrutinas inseguras."""
    full_path = _resolve_path(file_path)
    if not os.path.exists(full_path):
        return f"ERROR: File '{file_path}' not found."

    try:
        import security_scanner
        report = security_scanner.run_security_scan(full_path)
        return f"=== Security Scan for {os.path.basename(file_path)} ===\n{report}"
    except Exception as e:
        return f"ERROR: Failed running security scan: {str(e)}"


def scaffold_test(file_path: str) -> str:
    """Genera el esqueleto de test unitario para una clase Kotlin en app/src/main/java."""
    full_path = _resolve_path(file_path)
    if not os.path.exists(full_path):
        return f"ERROR: File '{file_path}' not found."

    try:
        import test_scaffolder
        test_scaffolder.scaffold_test(full_path)
        test_path = full_path.replace("app/src/main/java/", "app/src/test/java/").replace(".kt", "Test.kt")
        if os.path.exists(test_path):
            return f"SUCCESS: Unit test scaffold created at '{os.path.relpath(test_path, os.path.dirname(SKILLS_DIR))}'."
        return "NOTICE: Test scaffolding executed (file might already exist)."
    except Exception as e:
        return f"ERROR: Failed running test scaffolder: {str(e)}"


def fix_kdocs(file_path: str) -> str:
    """Autocompleta los comentarios KDoc faltantes en clases y funciones públicas."""
    full_path = _resolve_path(file_path)
    if not os.path.exists(full_path):
        return f"ERROR: File '{file_path}' not found."

    try:
        import kdoc_autocompleter
        kdoc_autocompleter.autocomplete_kdocs(full_path)
        return f"SUCCESS: Completed KDoc documentation check for '{file_path}'."
    except Exception as e:
        return f"ERROR: Failed running KDoc autocompleter: {str(e)}"


# Definiciones JSON Schema de tus skills como Tools
SKILL_TOOLS = [
    {
        "name": "check_hilt_bindings",
        "description": "Checks whether a newly created UseCase or Repository interface has its Hilt dependency injection binding registered in app/src/main/java/com/devsusana/hometutorpro/di.",
        "parameters": {
            "type": "object",
            "properties": {
                "file_path": {"type": "string", "description": "Relative path to the Kotlin interface/class"}
            },
            "required": ["file_path"]
        }
    },
    {
        "name": "audit_architecture",
        "description": "Audits a Kotlin file against Clean Architecture guidelines in AGENTS.md and domain policies in BUSINESS_RULES.md.",
        "parameters": {
            "type": "object",
            "properties": {
                "file_path": {"type": "string", "description": "Relative path to the file to audit"}
            },
            "required": ["file_path"]
        }
    },
    {
        "name": "audit_security",
        "description": "Scans a file for hardcoded secrets, unsafe coroutine usage, and naming in English.",
        "parameters": {
            "type": "object",
            "properties": {
                "file_path": {"type": "string", "description": "Relative path to the file to scan"}
            },
            "required": ["file_path"]
        }
    },
    {
        "name": "scaffold_test",
        "description": "Generates a JUnit + MockK unit test skeleton for a production Kotlin file under app/src/main/java/.",
        "parameters": {
            "type": "object",
            "properties": {
                "file_path": {"type": "string", "description": "Relative path to the production file"}
            },
            "required": ["file_path"]
        }
    },
    {
        "name": "fix_kdocs",
        "description": "Automatically documents all public classes and functions in a file with KDoc standard comments.",
        "parameters": {
            "type": "object",
            "properties": {
                "file_path": {"type": "string", "description": "Relative path to the file"}
            },
            "required": ["file_path"]
        }
    }
]


def execute_skill_tool(name: str, args: Dict[str, Any]) -> str:
    """Despacha la ejecución de herramientas de skills."""
    if name == "check_hilt_bindings":
        return check_hilt_bindings(args.get("file_path", ""))
    elif name == "audit_architecture":
        return audit_architecture(args.get("file_path", ""))
    elif name == "audit_security":
        return audit_security(args.get("file_path", ""))
    elif name == "scaffold_test":
        return scaffold_test(args.get("file_path", ""))
    elif name == "fix_kdocs":
        return fix_kdocs(args.get("file_path", ""))
    else:
        return f"ERROR: Skill tool '{name}' not found."
