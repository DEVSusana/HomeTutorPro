import os
import sys
from typing import List, Dict, Any, Optional

from providers.base import BaseLLMProvider
from core.system_tools import SYSTEM_TOOLS, execute_system_tool, PROJECT_ROOT
from core.skill_tools import SKILL_TOOLS, execute_skill_tool
from core.interactive import INTERACTIVE_TOOLS, execute_interactive_tool, show_diff_and_confirm


class AgenticEngine:
    """
    Universal Autonomous Agentic Engine based on the ReAct (Reason + Act) loop.
    Governs autonomous execution, Gradle self-healing compilation/tests, and final human confirmation.
    """

    def __init__(self, provider: BaseLLMProvider, max_iterations: int = 30):
        self.provider = provider
        self.max_iterations = max_iterations
        self.tools = SYSTEM_TOOLS + SKILL_TOOLS + INTERACTIVE_TOOLS

    def _load_project_guidelines(self) -> str:
        """Loads project architecture and business rules."""
        guidelines = []
        agents_md = os.path.join(PROJECT_ROOT, "AGENTS.md")
        business_md = os.path.join(PROJECT_ROOT, "BUSINESS_RULES.md")

        if os.path.exists(agents_md):
            try:
                with open(agents_md, "r", encoding="utf-8") as f:
                    guidelines.append(f"### MANDATORY CODING & ARCHITECTURE GUIDELINES (AGENTS.md):\n{f.read()}")
            except Exception:
                pass

        if os.path.exists(business_md):
            try:
                with open(business_md, "r", encoding="utf-8") as f:
                    guidelines.append(f"### MANDATORY BUSINESS RULES & DOMAIN DIRECTIVES (BUSINESS_RULES.md):\n{f.read()}")
            except Exception:
                pass

        return "\n\n".join(guidelines)

    def _build_system_prompt(self, spec_content: Optional[str] = None) -> str:
        """Constructs the agent's behavior and system directives."""
        base_prompt = f"""You are Antigravity Core, a Senior Android Staff Engineer operating as an autonomous development agent.
You are running directly inside the user's project workspace.

{self._load_project_guidelines()}

### STRICT AGENTIC OPERATING PROTOCOL:
1. **Spec-Driven & ReAct Execution:**
   - Review the user's goal or specification thoroughly before taking any action.
   - Plan your steps, inspect files using `read_file` or `list_files`, apply changes, and verify.

2. **SURGICAL PRECISION & MINIMAL BLAST RADIUS (CRITICAL):**
   - NEVER regenerate or overwrite an entire class when refactoring or fixing errors.
   - Use `patch_file` to modify ONLY the exact faulty statement, function, or declaration and its directly affected references.
   - Leave all other working methods, imports, comments, and structure 100% untouched.
   - Only use `write_new_file` when creating an entirely brand new class or interface.

3. **HONESTY & NO GUESSWORK:**
   - If a business requirement or architectural decision is ambiguous, immediately call `ask_human` to consult the developer. Never guess or assume requirements.

4. **MANDATORY VERIFICATION & QUALITY GATES:**
   - **SECURITY AUDIT (NON-NEGOTIABLE):** After modifying or adding any Kotlin file, you MUST invoke `audit_security` on it. Ensure zero hardcoded secrets/API keys, zero unconfined coroutine scopes (e.g. no GlobalScope), and strict English naming. If security issues are found, fix them immediately.
   - **ARCHITECTURE AUDIT:** Run `audit_architecture` on modified files to verify compliance with Clean Architecture and BUSINESS_RULES.md.
   - **COMPILATION & SELF-HEALING:** Run `./gradlew compileDebugKotlin` using `run_command`. If it fails, locate the compiler error and patch it surgically.
   - **UNIT TESTS:** Run `./gradlew test` (or the specific test file) using `run_command`. If you created new production logic, invoke `scaffold_test` or `design_tests` to scaffold test coverage.
   - **REFACTORING & CODE SMELLS:** Run `audit_refactoring` on modified complex classes to verify maintainability.
   - **HILT BINDINGS:** Verify any new UseCase or Repository interface with `check_hilt_bindings`.

5. **TERMINATION CRITERIA (CRITICAL):**
   - When you have inspected the code, verified architecture, security, and tests, and applied any needed fixes:
   - STOP calling tools immediately.
   - Do NOT repeatedly call audit tools if you already obtained their output.
   - Output your comprehensive final evaluation report in plain text to the user explaining what was audited, the findings, and the verified status.
"""
        if spec_content:
            base_prompt += f"\n\n### ACTIVE FEATURE SPECIFICATION:\n{spec_content}"

        return base_prompt

    def _dispatch_tool(self, name: str, args: Dict[str, Any]) -> str:
        """Dispatches tool execution to the appropriate layer."""
        if any(t["name"] == name for t in SYSTEM_TOOLS):
            return execute_system_tool(name, args)
        elif any(t["name"] == name for t in SKILL_TOOLS):
            return execute_skill_tool(name, args)
        elif any(t["name"] == name for t in INTERACTIVE_TOOLS):
            return execute_interactive_tool(name, args)
        else:
            return f"ERROR: Tool '{name}' is not registered."

    def run(self, goal: str, spec_file: Optional[str] = None) -> bool:
        """
        Executes the autonomous agentic loop until the goal is achieved.
        """
        spec_content = None
        if spec_file:
            full_spec_path = os.path.join(PROJECT_ROOT, spec_file) if not os.path.isabs(spec_file) else spec_file
            if os.path.exists(full_spec_path):
                with open(full_spec_path, "r", encoding="utf-8") as f:
                    spec_content = f.read()
                print(f"📄 Spec loaded: {os.path.basename(full_spec_path)}")
            else:
                print(f"⚠️ Spec file '{spec_file}' not found. Proceeding with direct goal.")

        system_prompt = self._build_system_prompt(spec_content)
        messages: List[Dict[str, Any]] = [
            {"role": "user", "content": goal}
        ]

        print("\n" + "🚀" + "═" * 70)
        print(f"🤖 [STARTING AUTONOMOUS AGENTIC LOOP]")
        print(f"🎯 Goal: {goal}")
        print("═" * 72)

        iteration = 0
        current_limit = self.max_iterations

        while iteration < current_limit:
            print(f"\n🧠 [Iteration {iteration + 1}/{current_limit}] Thinking...")
            try:
                response = self.provider.chat(
                    messages=messages,
                    tools=self.tools,
                    system_prompt=system_prompt
                )
            except Exception as e:
                print(f"❌ Error in LLM call: {e}")
                return False

            # If the model requests no tools, it has completed its work
            if not response.has_tool_calls:
                print("\n" + "🎯" + "═" * 70)
                print("✅ [AGENT HAS COMPLETED THE TASK]")
                print("═" * 72)
                print(f"\n{response.content}\n")
                # Final Human-in-the-Loop phase
                return show_diff_and_confirm()

            # Append assistant message with its tool calls and raw content to history
            messages.append({
                "role": "assistant",
                "content": response.content,
                "tool_calls": [
                    {"id": tc.id, "name": tc.name, "args": tc.args} for tc in response.tool_calls
                ],
                "raw": response.raw
            })

            # If the LLM outputted explanatory text, display it
            if response.content:
                print(f"💬 {response.content}")

            # Execute each tool call and aggregate responses for the provider
            tool_responses = []
            for tool_call in response.tool_calls:
                print(f"🛠️  Executing: {tool_call.name}({tool_call.args})")
                tool_output = self._dispatch_tool(tool_call.name, tool_call.args)

                # Show a clean snippet of the result
                first_line = tool_output.strip().split("\n")[0] if tool_output else "Empty output"
                print(f"    ↳ Result: {first_line[:90]}")

                tool_responses.append({
                    "tool_call_id": tool_call.id,
                    "tool_name": tool_call.name,
                    "content": tool_output
                })

            messages.append({
                "role": "tool_results",
                "tools": tool_responses
            })

            iteration += 1

            # If iteration limit is reached but the agent is still requesting tools, offer interactive extension
            if iteration >= current_limit:
                print("\n" + "⚠️" + "═" * 70)
                print(f"⚠️ Reached limit of {current_limit} iterations, but the agent is still actively working.")
                print("═" * 72)
                try:
                    user_input = input("👉 Do you want to grant 10 additional steps to complete the task? (y/n) [y]: ").strip().lower()
                except (EOFError, KeyboardInterrupt):
                    user_input = "n"

                if user_input in ("", "y", "yes", "s", "si", "sí"):
                    current_limit += 10
                    print(f"⏩ Extension granted (+10 steps). New limit: {current_limit} iterations.")
                else:
                    break

        print(f"\n⚠️ Agentic loop finished after {iteration} iterations.")
        return show_diff_and_confirm()
