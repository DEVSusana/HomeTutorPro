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

    def _build_interviewer_prompt(self) -> str:
        return f"""You are the Lead Android Architect for HomeTutorPro conducting a Spec Planning Interview.
Your goal is to help the developer convert their feature idea into a crystal-clear, unambiguous Technical Specification (.md).

{self._load_guidelines()}

### YOUR INTERVIEW PROTOCOL:
1. **Analyze the Feature Idea:** Compare it against existing Clean Architecture boundaries, Room persistence, and domain policies in BUSINESS_RULES.md.
2. **Identify Ambiguities & Edge Cases:** Ask 2-3 precise questions per turn focusing on:
   - UI vs Domain responsibilities.
   - Database entities / migrations needed.
   - Exception handling and business rule impacts (e.g. negative balances, calendar exceptions).
   - Test criteria.
3. **Progressive Drafting:** In each reply, provide:
   - Your questions or clarifications.
   - An updated DRAFT of the specification formatted in clean Markdown.
4. **STRICT CONFIRMATION GATE:**
   - You MUST NOT consider the spec final until the developer explicitly states they are 100% satisfied and approves it (e.g. typing 'OK', 'Approved', 'Yes').
   - In each turn, remind the developer that they can keep refining or approve with 'OK' when satisfied.
"""

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

        # Extract and save spec into specs/<slug>.md
        slug = re.sub(r"[^a-zA-Z0-9]+", "_", initial_idea.lower()).strip("_")[:30] or "feature_spec"
        spec_filename = f"{slug}.md"
        spec_path = os.path.join(self.specs_dir, spec_filename)

        with open(spec_path, "w", encoding="utf-8") as f:
            f.write(f"# SPECIFICATION: {initial_idea.upper()}\n\n{latest_spec_draft}\n")

        rel_spec_path = os.path.relpath(spec_path, PROJECT_ROOT)
        print("\n" + "💾" + "═" * 70)
        print(f"✅ Spec successfully saved to: {rel_spec_path}")
        print(f"🚀 You can now launch the autonomous loop whenever you want:")
        print(f"   python -m agents_dev run --spec {rel_spec_path}")
        print("═" * 72 + "\n")

        return rel_spec_path
