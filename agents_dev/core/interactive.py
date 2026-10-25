import subprocess
from typing import Optional, List, Dict, Any
from core.system_tools import PROJECT_ROOT


def ask_human(question: str, options: Optional[List[str]] = None) -> str:
    """
    Pausa el bucle agéntico para consultar una duda al desarrollador en la terminal de Android Studio.
    """
    print("\n" + "━" * 65)
    print("❓ [EL AGENTE TIENE UNA DUDA DE DISEÑO O ESPECIFICACIÓN]")
    print(f"\n{question}\n")
    if options:
        print("Opciones sugeridas:")
        for idx, opt in enumerate(options, 1):
            print(f"  [{idx}] {opt}")
    print("━" * 65)

    try:
        reply = input("\n👉 Tu respuesta (escribe tu decisión o elige opción): ").strip()
        while not reply:
            reply = input("👉 Por favor ingresa una respuesta: ").strip()
        return reply
    except (EOFError, KeyboardInterrupt):
        return "USER CANCELLED OR PROVIDED EMPTY INPUT."


def show_diff_and_confirm(files: Optional[List[str]] = None) -> bool:
    """
    Presenta un git diff interactivo y solicita aprobación final del desarrollador antes de consolidar.
    Si se rechaza, revierte automáticamente con git checkout.
    """
    print("\n" + "═" * 70)
    print("🔍 [REVISIÓN FINAL: CAMBIOS PROPUESTOS POR EL AGENTE (Git Diff)]")
    print("═" * 70)

    cmd = ["git", "diff"]
    if files:
        cmd.extend(files)

    try:
        subprocess.run(cmd, cwd=PROJECT_ROOT)
    except Exception as e:
        print(f"Error mostrando git diff: {e}")

    print("═" * 70)
    try:
        decision = input("\n🤔 ¿Deseas APROBAR y CONSERVAR estos cambios? (y/n): ").strip().lower()
        if decision == "y":
            print("✅ Cambios aprobados y conservados en el espacio de trabajo.")
            return True
        else:
            print("❌ Cambios rechazados. Revirtiendo archivos modificados...")
            revert_cmd = ["git", "checkout", "--"]
            if files:
                revert_cmd.extend(files)
            else:
                revert_cmd.append(".")
            subprocess.run(revert_cmd, cwd=PROJECT_ROOT)
            print("⏪ Archivos revertidos al estado original.")
            return False
    except (EOFError, KeyboardInterrupt):
        print("\nCancelado por el usuario. Revirtiendo por seguridad...")
        subprocess.run(["git", "checkout", "--", "."], cwd=PROJECT_ROOT)
        return False


INTERACTIVE_TOOLS = [
    {
        "name": "ask_human",
        "description": "Pauses execution and asks the developer a clarification question when requirements are ambiguous or an architectural trade-off requires a decision. NEVER guess critical business or architecture logic; use this tool to ask.",
        "parameters": {
            "type": "object",
            "properties": {
                "question": {"type": "string", "description": "Clear explanation of the doubt and the decision needed"},
                "options": {
                    "type": "array",
                    "items": {"type": "string"},
                    "description": "Optional list of recommended choices for the developer"
                }
            },
            "required": ["question"]
        }
    }
]


def execute_interactive_tool(name: str, args: Dict[str, Any]) -> str:
    """Despacha la ejecución de herramientas interactivas."""
    if name == "ask_human":
        return ask_human(args.get("question", ""), args.get("options"))
    return f"ERROR: Unknown interactive tool '{name}'."
