import os
import re
from typing import List, Dict, Any
from providers.base import BaseLLMProvider
from core.system_tools import PROJECT_ROOT


class SpecInterviewer:
    """
    Specification Interview Engine (Phase 1: Think and Agree).
    Interactively dialogues with the developer until all technical requirements
    and architecture boundaries are clearly defined and approved.
    """

    def __init__(self, provider: BaseLLMProvider):
        self.provider = provider
        self.specs_dir = os.path.join(PROJECT_ROOT, "specs")
        os.makedirs(self.specs_dir, exist_ok=True)

    def _load_guidelines(self) -> str:
        guidelines = []
        for file in ["AGENTS.md", "BUSINESS_RULES.md"]:
            path = os.path.join(PROJECT_ROOT, file)
            if os.path.exists(path):
                try:
                    with open(path, "r", encoding="utf-8") as f:
                        guidelines.append(f"### {file}:\n{f.read()}")
                except Exception:
                    pass
        return "\n\n".join(guidelines)

    def _load_template(self) -> str:
        """Loads specs/_TEMPLATE.md if present, returning a minimal contract fallback otherwise."""
        template_path = os.path.join(self.specs_dir, "_TEMPLATE.md")
        if os.path.exists(template_path):
            try:
                with open(template_path, "r", encoding="utf-8") as f:
                    return f.read()
            except Exception:
                pass
        return (
            "# SPEC: [Short title]\n"
            "- **Estado:** DRAFT | APPROVED\n"
            "- **Rama / Worktree:** `feat/<slug>`\n\n"
            "## 1. Objetivo\n...\n\n"
            "## 2. Alcance (Scope) — Archivos permitidos\n- **Modificar:**\n- **Crear:**\n- **PROHIBIDO modificar:**\n\n"
            "## 3. Reglas de negocio aplicables\n...\n\n"
            "## 4. Fuera de alcance (Out of Scope)\n...\n\n"
            "## 5. Criterios de aceptación (Verificables)\n- [ ] **AC1:** ...\n\n"
            "## 6. Comandos de verificación obligatorios\n```bash\n./gradlew compileDebugKotlin\n```"
        )

    def _build_interviewer_prompt(self) -> str:
        template = self._load_template()
        return f"""You are the Lead Android Architect for HomeTutorPro conducting a Spec Planning Interview.
Your goal is to help the developer convert their feature idea into a crystal-clear, unambiguous Technical Specification (.md) contract.

{self._load_guidelines()}

### MANDATORY SPEC CONTRACT TEMPLATE:
```markdown
{template}
```

### YOUR INTERVIEW PROTOCOL:
1. **Analyze the Feature Idea:** Compare it against existing Clean Architecture boundaries, Room persistence, and domain policies in BUSINESS_RULES.md.
2. **Identify Ambiguities & Edge Cases:** Ask 2-3 precise questions per turn focusing on:
   - UI vs Domain responsibilities and exact files/packages to touch (Scope).
   - Database entities / migrations needed (or explicitly mark them Out of Scope).
   - Exception handling and business rule impacts (e.g. negative balances, calendar exceptions).
   - Verifiable acceptance criteria (Given / When / Then) and Gradle verification commands.
3. **Progressive Contract Drafting (CRITICAL FORMAT):**
   - Put your conversational questions or clarifications OUTSIDE the spec markers.
   - ALWAYS wrap the updated specification draft strictly between `<!-- SPEC_START -->` and `<!-- SPEC_END -->` markers, following the exact structure of the template above.
   - Keep `Estado: DRAFT` while interviewing; only the final saved contract becomes `APPROVED`.
4. **STRICT CONFIRMATION GATE:**
   - You MUST NOT consider the spec final until the developer explicitly states they are 100% satisfied and approves it (e.g. typing 'OK', 'Approved', 'Yes').
   - In each turn, remind the developer that they can keep refining or approve with 'OK' when satisfied.
"""

    def _extract_clean_spec(self, latest_reply: str, messages: List[Dict[str, Any]], system_prompt: str) -> str:
        """Extracts the contract block between SPEC_START/SPEC_END or asks the model to consolidate it."""
        match = re.search(r"<!--\s*SPEC_START\s*-->(.*?)<!--\s*SPEC_END\s*-->", latest_reply, re.DOTALL)
        if match:
            clean_spec = match.group(1).strip()
        else:
            # Fallback: request a clean, chat-free contract matching _TEMPLATE.md
            consolidation_messages = list(messages) + [
                {"role": "assistant", "content": latest_reply},
                {
                    "role": "user",
                    "content": (
                        "The spec is approved. Output ONLY the final clean Markdown specification following the template, "
                        "with no greetings, no questions, and no extra commentary."
                    ),
                },
            ]
            try:
                response = self.provider.chat(
                    messages=consolidation_messages,
                    tools=[],
                    system_prompt=system_prompt,
                )
                clean_spec = response.content.strip()
            except Exception:
                clean_spec = latest_reply.strip()

        clean_spec = re.sub(
            r"(\*\*Estado:\*\*\s*)(DRAFT\s*\|\s*APPROVED|DRAFT)",
            r"\1APPROVED",
            clean_spec,
            flags=re.IGNORECASE,
        )
        return clean_spec

    def interview(self, initial_idea: str) -> str:
        """
        Conducts the interactive interview with the developer until final approval.
        """
        system_prompt = self._build_interviewer_prompt()
        messages: List[Dict[str, Any]] = [
            {"role": "user", "content": f"I want to define the technical specification for this feature/task: '{initial_idea}'"}
        ]

        print("\n" + "📋" + "═" * 70)
        print("🧠 [PHASE 1: SPECIFICATION INTERVIEW MODE]")
        print(f"💡 Initial idea: {initial_idea}")
        print("═" * 72)
        print("The agent will review architecture guidelines and interview you to finalize the specification.")
        print("Note: The process will NOT finish until you explicitly approve the spec.\n")

        latest_spec_draft = ""
        turn = 1

        while True:
            print(f"⏳ [Turn {turn}] Architect is analyzing...")
            try:
                response = self.provider.chat(
                    messages=messages,
                    tools=[],  # Direct dialogue without tools during planning interview
                    system_prompt=system_prompt
                )
            except Exception as e:
                print(f"❌ Error connecting to model: {e}")
                return ""

            agent_reply = response.content
            latest_spec_draft = agent_reply

            print("\n" + "━" * 70)
            print("🤖 [ARCHITECT RESPONSE]")
            print(agent_reply)
            print("━" * 70)

            # Explicit confirmation gate
            print("\n👉 What would you like to do?")
            print("   - Type 'OK' or 'APPROVED' if the spec is 100% clear and ready to save.")
            print("   - Or enter your feedback, adjustments, or answers to the questions above:")

            try:
                user_feedback = input("\nYour response: ").strip()
                while not user_feedback:
                    user_feedback = input("Please enter a response: ").strip()
            except (EOFError, KeyboardInterrupt):
                print("\nInterview cancelled by user.")
                return ""

            # Gate verification (supporting English & Spanish confirmation keywords)
            approval_keywords = {"ok", "approved", "approve", "yes", "y", "done", "aprobado", "aprobar", "sí", "si", "listo"}
            if user_feedback.lower() in approval_keywords:
                print("\n🎉 Specification 100% approved by the developer!")
                break

            # Add turn to history and continue
            messages.append({"role": "assistant", "content": agent_reply})
            messages.append({"role": "user", "content": user_feedback})
            turn += 1

        # Extract clean contract and save spec into specs/<slug>.md
        clean_spec = self._extract_clean_spec(latest_spec_draft, messages, system_prompt)
        slug = re.sub(r"[^a-zA-Z0-9]+", "_", initial_idea.lower()).strip("_")[:30] or "feature_spec"
        spec_filename = f"{slug}.md"
        spec_path = os.path.join(self.specs_dir, spec_filename)

        with open(spec_path, "w", encoding="utf-8") as f:
            if clean_spec.lstrip().startswith("# "):
                f.write(f"{clean_spec}\n")
            else:
                f.write(f"# SPEC: {initial_idea}\n\n{clean_spec}\n")

        rel_spec_path = os.path.relpath(spec_path, PROJECT_ROOT)
        print("\n" + "💾" + "═" * 70)
        print(f"✅ Spec successfully saved to: {rel_spec_path}")
        print(f"🚀 You can now launch the autonomous loop whenever you want:")
        print(f"   python -m agents_dev run --spec {rel_spec_path}")
        print("═" * 72 + "\n")

        return rel_spec_path
