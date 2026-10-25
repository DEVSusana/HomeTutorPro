import os
import fnmatch
import subprocess
from typing import Dict, Any, Optional, Tuple

# Raíz del proyecto Android (2 niveles arriba de agents_dev/core)
PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "../.."))


def _resolve_path(path: str) -> str:
    """Resuelve la ruta para que siempre apunte al proyecto de forma segura."""
    if os.path.isabs(path):
        return path
    return os.path.abspath(os.path.join(PROJECT_ROOT, path))


def read_file(path: str, start_line: Optional[int] = None, end_line: Optional[int] = None) -> str:
    """
    Lee el contenido de un archivo, con soporte opcional de rango de líneas (1-indexed).
    """
    full_path = _resolve_path(path)
    if not os.path.exists(full_path):
        return f"ERROR: File not found at '{path}'."

    try:
        with open(full_path, "r", encoding="utf-8") as f:
            lines = f.readlines()

        total_lines = len(lines)
        if start_line is not None or end_line is not None:
            s = max(1, start_line or 1) - 1
            e = min(total_lines, end_line or total_lines)
            selected_lines = lines[s:e]
            header = f"=== File: {path} (Lines {s+1}-{e} of {total_lines}) ===\n"
            numbered_content = "".join([f"{i+s+1:4d}: {line}" for i, line in enumerate(selected_lines)])
            return header + numbered_content
        else:
            header = f"=== File: {path} ({total_lines} lines) ===\n"
            numbered_content = "".join([f"{i+1:4d}: {line}" for i, line in enumerate(lines)])
            return header + numbered_content
    except Exception as e:
        return f"ERROR: Could not read file '{path}': {str(e)}"


def patch_file(path: str, search: str, replace: str) -> str:
    """
    Aplica una cirugía de precisión (Search & Replace) sobre un archivo.
    Garantiza que el bloque 'search' exista exactamente una única vez.
    """
    full_path = _resolve_path(path)
    if not os.path.exists(full_path):
        return f"ERROR: File not found at '{path}'."

    if not search:
        return "ERROR: 'search' block cannot be empty."

    try:
        with open(full_path, "r", encoding="utf-8") as f:
            content = f.read()

        occurrences = content.count(search)
        if occurrences == 0:
            return (
                f"ERROR: Target block not found in '{path}'. "
                "Ensure exact whitespace, indentation, and punctuation match the source file."
            )
        elif occurrences > 1:
            return (
                f"ERROR: Target block found {occurrences} times in '{path}'. "
                "Expand the search block to include more surrounding context so it is strictly unique."
            )

        new_content = content.replace(search, replace, 1)
        with open(full_path, "w", encoding="utf-8") as f:
            f.write(new_content)

        return f"SUCCESS: Surgical patch successfully applied to '{path}'."
    except Exception as e:
        return f"ERROR: Could not patch file '{path}': {str(e)}"


def write_new_file(path: str, content: str) -> str:
    """
    Crea un nuevo archivo con el contenido especificado. Falla si ya existe para evitar sobreescrituras accidentales.
    """
    full_path = _resolve_path(path)
    if os.path.exists(full_path):
        return f"ERROR: File already exists at '{path}'. Use 'patch_file' to modify existing files."

    try:
        os.makedirs(os.path.dirname(full_path), exist_ok=True)
        with open(full_path, "w", encoding="utf-8") as f:
            f.write(content)
        return f"SUCCESS: Created new file at '{path}'."
    except Exception as e:
        return f"ERROR: Could not create file '{path}': {str(e)}"


def run_command(command: str) -> str:
    """
    Ejecuta un comando en la raíz del proyecto (útil para Gradle, git, etc.).
    """
    try:
        # Si se invoca gradlew, asegurar ruta ejecutable
        gradle_bin = "./gradlew" if os.name != "nt" else "gradlew.bat"
        if command.startswith("./gradlew") or command.startswith("gradlew"):
            parts = command.split()
            parts[0] = os.path.join(PROJECT_ROOT, gradle_bin)
            exec_cmd = " ".join(parts)
        else:
            exec_cmd = command

        result = subprocess.run(
            exec_cmd,
            shell=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            cwd=PROJECT_ROOT,
            timeout=300
        )

        output = (result.stdout or "") + ("\n" + result.stderr if result.stderr else "")
        status = "SUCCESS" if result.returncode == 0 else f"FAILED (Exit Code {result.returncode})"
        return f"=== Command: {command} [{status}] ===\n{output.strip()}"
    except subprocess.TimeoutExpired:
        return f"ERROR: Command '{command}' timed out after 300 seconds."
    except Exception as e:
        return f"ERROR: Failed executing command '{command}': {str(e)}"


def list_files(directory: str = ".", pattern: Optional[str] = None) -> str:
    """
    Lista archivos y carpetas recursivamente o por patrón desde el directorio indicado.
    """
    full_dir = _resolve_path(directory)
    if not os.path.exists(full_dir):
        return f"ERROR: Directory '{directory}' not found."

    results = []
    # Exclusiones típicas para no saturar contexto
    ignored_dirs = {".git", ".gradle", "build", ".idea", ".venv_agente", "__pycache__"}

    for root, dirs, files in os.walk(full_dir):
        dirs[:] = [d for d in dirs if d not in ignored_dirs]
        rel_root = os.path.relpath(root, PROJECT_ROOT)
        for f in files:
            if pattern and not fnmatch.fnmatch(f, pattern):
                continue
            rel_file = os.path.normpath(os.path.join(rel_root, f))
            results.append(rel_file)

    if not results:
        return f"No files matched in '{directory}' with pattern '{pattern or '*'}'."

    # Limitar a máximo 150 archivos para no desbordar
    clipped = results[:150]
    out = "\n".join(clipped)
    if len(results) > 150:
        out += f"\n... and {len(results) - 150} more files."
    return out


# Definiciones de herramientas para el LLM (JSON Schema universal)
SYSTEM_TOOLS = [
    {
        "name": "read_file",
        "description": "Reads file contents with line numbers. Use start_line and end_line to inspect specific sections without loading entire files.",
        "parameters": {
            "type": "object",
            "properties": {
                "path": {"type": "string", "description": "Relative path to file from project root"},
                "start_line": {"type": "integer", "description": "Optional start line number (1-based)"},
                "end_line": {"type": "integer", "description": "Optional end line number (inclusive)"}
            },
            "required": ["path"]
        }
    },
    {
        "name": "patch_file",
        "description": "Surgically replaces an exact block of code inside an existing file. Must provide exact text including indentation and whitespace. Only changes the target block.",
        "parameters": {
            "type": "object",
            "properties": {
                "path": {"type": "string", "description": "Relative path to file"},
                "search": {"type": "string", "description": "Exact text block to replace. Must exist uniquely in the file."},
                "replace": {"type": "string", "description": "New replacement code block."}
            },
            "required": ["path", "search", "replace"]
        }
    },
    {
        "name": "write_new_file",
        "description": "Creates a new file with the specified content. Fails if the file already exists.",
        "parameters": {
            "type": "object",
            "properties": {
                "path": {"type": "string", "description": "Relative path for the new file"},
                "content": {"type": "string", "description": "Full content of the new file"}
            },
            "required": ["path", "content"]
        }
    },
    {
        "name": "run_command",
        "description": "Runs a shell command in project root. Use for gradle builds ('./gradlew compileDebugKotlin'), running tests ('./gradlew test'), or git inspection.",
        "parameters": {
            "type": "object",
            "properties": {
                "command": {"type": "string", "description": "Command line to execute (e.g. './gradlew testDebugUnitTest')"}
            },
            "required": ["command"]
        }
    },
    {
        "name": "list_files",
        "description": "Lists files in a project directory matching an optional glob pattern (e.g. '*.kt', '*UseCase*').",
        "parameters": {
            "type": "object",
            "properties": {
                "directory": {"type": "string", "description": "Directory relative to project root. Defaults to '.'"},
                "pattern": {"type": "string", "description": "Glob filter pattern (e.g. '*Schedule*')"}
            }
        }
    }
]


def execute_system_tool(name: str, args: Dict[str, Any]) -> str:
    """Despacha la ejecución de herramientas del sistema."""
    if name == "read_file":
        return read_file(args.get("path", ""), args.get("start_line"), args.get("end_line"))
    elif name == "patch_file":
        return patch_file(args.get("path", ""), args.get("search", ""), args.get("replace", ""))
    elif name == "write_new_file":
        return write_new_file(args.get("path", ""), args.get("content", ""))
    elif name == "run_command":
        return run_command(args.get("command", ""))
    elif name == "list_files":
        return list_files(args.get("directory", "."), args.get("pattern"))
    else:
        return f"ERROR: System tool '{name}' not found."
