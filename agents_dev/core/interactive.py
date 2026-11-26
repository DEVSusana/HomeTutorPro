import subprocess
from typing import Optional, List, Dict, Any
from core.system_tools import PROJECT_ROOT


def ask_human(question: str, options: Optional[List[str]] = None) -> str:
    """
    Pauses the agentic loop to consult the developer via the terminal.
    """
    print("\n" + "━" * 65)
    print("❓ [AGENT HAS A DESIGN OR SPECIFICATION QUESTION]")
    print(f"\n{question}\n")
    if options:
        print("Suggested options:")
        for idx, opt in enumerate(options, 1):
            print(f"  [{idx}] {opt}")
    print("━" * 65)

    try:
        reply = input("\n👉 Your response (type your decision or pick an option): ").strip()
        while not reply:
            reply = input("👉 Please enter a response: ").strip()
        return reply
    except (EOFError, KeyboardInterrupt):
        return "USER CANCELLED OR PROVIDED EMPTY INPUT."


def show_diff_and_confirm(files: Optional[List[str]] = None) -> bool:
    """
    Displays an interactive git diff and prompts the developer for final approval before committing.
    If rejected, automatically reverts modified files using git checkout.
    """
    print("\n" + "═" * 70)
    print("🔍 [FINAL REVIEW: PROPOSED CHANGES (Git Diff)]")
    print("═" * 70)

    cmd = ["git", "diff"]
    if files:
        cmd.extend(files)

    try:
        subprocess.run(cmd, cwd=PROJECT_ROOT)
    except Exception as e:
        print(f"Error displaying git diff: {e}")

    print("═" * 70)
    try:
        decision = input("\n🤔 Do you want to APPROVE and KEEP these changes? (y/n): ").strip().lower()
        if decision == "y":
            print("✅ Changes approved and kept in workspace.")
            return True
        else:
            print("❌ Changes rejected. Reverting modified files...")
            revert_cmd = ["git", "checkout", "--"]
            if files:
                revert_cmd.extend(files)
            else:
                revert_cmd.append(".")
            subprocess.run(revert_cmd, cwd=PROJECT_ROOT)
            print("⏪ Files reverted to original state.")
            return False
    except (EOFError, KeyboardInterrupt):
        print("\nCancelled by user. Reverting for safety...")
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
    """Dispatches execution for interactive tools."""
    if name == "ask_human":
        return ask_human(args.get("question", ""), args.get("options"))
    return f"ERROR: Unknown interactive tool '{name}'."
