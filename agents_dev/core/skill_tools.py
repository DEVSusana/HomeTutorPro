import os
import sys
from typing import Dict, Any, Callable

# Ensure agents_dev/skills is in sys.path to import existing skill modules
SKILLS_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "../skills"))
if SKILLS_DIR not in sys.path:
    sys.path.insert(0, SKILLS_DIR)

from core.system_tools import _resolve_path


def _run_audit_skill(file_path: str, module_name: str, func_name: str, title: str, output_format: str = "text") -> str:
    """
    Generic runner for cognitive audit skills to eliminate repetitive path resolution,
    validation, and error handling.
    """
    full_path = _resolve_path(file_path)
    if not os.path.exists(full_path):
        return f"ERROR: File '{file_path}' not found."

    try:
        module = __import__(module_name)
        audit_fn = getattr(module, func_name)
        report = audit_fn(full_path, output_format=output_format)
        return f"=== {title} for {os.path.basename(file_path)} ===\n{report}"
    except Exception as e:
        return f"ERROR: Failed running {title.lower()}: {str(e)}"


def check_hilt_bindings(file_path: str) -> str:
    """Verifies whether a UseCase or Repository interface is properly bound in Hilt DI modules."""
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
    """Audits a Kotlin file against Clean Architecture guidelines in AGENTS.md and domain policies in BUSINESS_RULES.md."""
    return _run_audit_skill(file_path, "architecture_auditor", "run_architecture_audit", "Architecture Audit")


def audit_security(file_path: str) -> str:
    """Scans a Kotlin file for hardcoded secrets, cryptographic flaws, insecure storage, and OWASP Mobile risks."""
    return _run_audit_skill(file_path, "security_scanner", "run_security_scan", "Security Scan")


def scaffold_test(file_path: str) -> str:
    """Generates a JUnit + MockK unit test skeleton for a production Kotlin class under app/src/main/java."""
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


def audit_kdoc(file_path: str) -> str:
    """Audits KDoc documentation completeness and style requirements."""
    return _run_audit_skill(file_path, "kdoc_specialist", "run_kdoc_audit", "KDoc Audit")


def audit_refactoring(file_path: str) -> str:
    """Analyzes a Kotlin file for code smells, high cyclomatic complexity, and suggests Clean Architecture refactorings."""
    return _run_audit_skill(file_path, "refactor_assistant", "run_refactor_analysis", "Refactor Analysis")


def design_tests(file_path: str) -> str:
    """Designs a comprehensive unit test specification and test plan based on the production Kotlin class."""
    return _run_audit_skill(file_path, "test_generator", "run_test_design", "Test Design Plan")


# JSON Schema definitions of cognitive skills exposed as LLM Tools
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
        "description": "Performs an exhaustive security audit scanning for hardcoded secrets, insecure cryptography, plaintext storage, OWASP Mobile risks, and strict English naming. MANDATORY before finishing code modifications.",
        "parameters": {
            "type": "object",
            "properties": {
                "file_path": {"type": "string", "description": "Relative path to the file to scan"}
            },
            "required": ["file_path"]
        }
    },
    {
        "name": "audit_refactoring",
        "description": "Analyzes a file for code smells, high complexity, and suggests Clean Architecture refactorings.",
        "parameters": {
            "type": "object",
            "properties": {
                "file_path": {"type": "string", "description": "Relative path to the file to analyze"}
            },
            "required": ["file_path"]
        }
    },
    {
        "name": "design_tests",
        "description": "Generates a detailed unit test specification and plan based on the production Kotlin class.",
        "parameters": {
            "type": "object",
            "properties": {
                "file_path": {"type": "string", "description": "Relative path to the file to design tests for"}
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
        "name": "audit_kdoc",
        "description": "Audits a Kotlin file for missing KDoc comments and documentation style requirements.",
        "parameters": {
            "type": "object",
            "properties": {
                "file_path": {"type": "string", "description": "Relative path to the file"}
            },
            "required": ["file_path"]
        }
    }
]


# Dispatch mapping to eliminate branching boilerplate and ensure single point of execution
SKILL_DISPATCH_MAP: Dict[str, Callable[[Dict[str, Any]], str]] = {
    "check_hilt_bindings": lambda args: check_hilt_bindings(args.get("file_path", "")),
    "audit_architecture": lambda args: audit_architecture(args.get("file_path", "")),
    "audit_security": lambda args: audit_security(args.get("file_path", "")),
    "audit_refactoring": lambda args: audit_refactoring(args.get("file_path", "")),
    "design_tests": lambda args: design_tests(args.get("file_path", "")),
    "scaffold_test": lambda args: scaffold_test(args.get("file_path", "")),
    "audit_kdoc": lambda args: audit_kdoc(args.get("file_path", "")),
}


def execute_skill_tool(name: str, args: Dict[str, Any]) -> str:
    """Dispatches execution for registered cognitive skill tools."""
    handler = SKILL_DISPATCH_MAP.get(name)
    if handler:
        return handler(args)
    return f"ERROR: Skill tool '{name}' not found."

