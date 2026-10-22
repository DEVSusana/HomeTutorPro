import os
import sys
from typing import List, Dict, Any, Optional

from providers.base import BaseLLMProvider
from core.system_tools import SYSTEM_TOOLS, execute_system_tool, PROJECT_ROOT
from core.skill_tools import SKILL_TOOLS, execute_skill_tool
from core.interactive import INTERACTIVE_TOOLS, execute_interactive_tool, show_diff_and_confirm


class AgenticEngine:
    """
    Motor Agéntico Autónomo universal basado en el bucle ReAct (Reason + Act).
    Gobierna la ejecución autónoma, la autocorrección mediante Gradle y la confirmación final.
    """

    def __init__(self, provider: BaseLLMProvider, max_iterations: int = 30):
        self.provider = provider
        self.max_iterations = max_iterations
        self.tools = SYSTEM_TOOLS + SKILL_TOOLS + INTERACTIVE_TOOLS

    def _load_project_guidelines(self) -> str:
        """Carga las reglas arquitectónicas y de negocio del proyecto."""
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
        """Construye las directivas de comportamiento del agente."""
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
        """Despacha la ejecución de la herramienta a la capa correspondiente."""
        # Herramientas del sistema
        if any(t["name"] == name for t in SYSTEM_TOOLS):
            return execute_system_tool(name, args)
        # Herramientas de skills
        elif any(t["name"] == name for t in SKILL_TOOLS):
            return execute_skill_tool(name, args)
        # Herramientas interactivas
        elif any(t["name"] == name for t in INTERACTIVE_TOOLS):
            return execute_interactive_tool(name, args)
        else:
            return f"ERROR: Tool '{name}' is not registered."

    def run(self, goal: str, spec_file: Optional[str] = None) -> bool:
        """
        Ejecuta el bucle agéntico autónomo hasta cumplir el objetivo.
        """
        spec_content = None
        if spec_file:
            full_spec_path = os.path.join(PROJECT_ROOT, spec_file) if not os.path.isabs(spec_file) else spec_file
            if os.path.exists(full_spec_path):
                with open(full_spec_path, "r", encoding="utf-8") as f:
                    spec_content = f.read()
                print(f"📄 Spec cargada: {os.path.basename(full_spec_path)}")
            else:
                print(f"⚠️ Archivo spec '{spec_file}' no encontrado. Procediendo con el objetivo directo.")

        system_prompt = self._build_system_prompt(spec_content)
        messages: List[Dict[str, Any]] = [
            {"role": "user", "content": goal}
        ]

        print("\n" + "🚀" + "═" * 70)
        print(f"🤖 [INICIANDO BUCLE AGÉNTICO AUTÓNOMO]")
        print(f"🎯 Objetivo: {goal}")
        print("═" * 72)

        for iteration in range(self.max_iterations):
            print(f"\n🧠 [Iteración {iteration + 1}/{self.max_iterations}] Pensando...")
            try:
                response = self.provider.chat(
                    messages=messages,
                    tools=self.tools,
                    system_prompt=system_prompt
                )
            except Exception as e:
                print(f"❌ Error en llamada al LLM: {e}")
                return False

            # Si el modelo no pide herramientas, ha terminado su trabajo
            if not response.has_tool_calls:
                print("\n" + "🎯" + "═" * 70)
                print("✅ [EL AGENTE HA COMPLETADO LA TAREA]")
                print("═" * 72)
                print(f"\n{response.content}\n")
                # Fase final Human-in-the-Loop
                return show_diff_and_confirm()

            # Añadimos la respuesta del asistente con sus tool calls y raw content al historial
            messages.append({
                "role": "assistant",
                "content": response.content,
                "tool_calls": [
                    {"id": tc.id, "name": tc.name, "args": tc.args} for tc in response.tool_calls
                ],
                "raw": response.raw
            })

            # Si el LLM emitió texto explicativo además de tools, lo mostramos
            if response.content:
                print(f"💬 {response.content}")

            # Ejecutamos cada tool call y agrupamos las respuestas para el proveedor
            tool_responses = []
            for tool_call in response.tool_calls:
                print(f"🛠️  Ejecutando: {tool_call.name}({tool_call.args})")
                tool_output = self._dispatch_tool(tool_call.name, tool_call.args)

                # Mostramos un extracto limpio del resultado
                first_line = tool_output.strip().split("\n")[0] if tool_output else "Empty output"
                print(f"    ↳ Resultado: {first_line[:90]}")

                tool_responses.append({
                    "tool_call_id": tool_call.id,
                    "tool_name": tool_call.name,
                    "content": tool_output
                })

            messages.append({
                "role": "tool_results",
                "tools": tool_responses
            })

        print(f"\n⚠️ Se alcanzó el límite máximo de iteraciones ({self.max_iterations}).")
        return show_diff_and_confirm()
