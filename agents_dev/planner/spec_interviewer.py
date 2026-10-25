import os
import re
from typing import List, Dict, Any
from providers.base import BaseLLMProvider
from core.system_tools import PROJECT_ROOT


class SpecInterviewer:
    """
    Motor de Entrevista de Especificaciones (Fase 1: Pensar y Acordar).
    Dialoga interactivamente con el desarrollador hasta que todos los requisitos
    estén perfectamente definidos y confirmados explícitamente.
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
   - You MUST NOT consider the spec final until the developer explicitly states they are 100% satisfied and approves it (e.g. typing 'OK', 'Aprobado', 'Sí').
   - In each turn, remind the developer that they can keep refining or approve with 'OK' when satisfied.
"""

    def interview(self, initial_idea: str) -> str:
        """
        Sostiene la entrevista interactiva con el desarrollador hasta la aprobación final.
        """
        system_prompt = self._build_interviewer_prompt()
        messages: List[Dict[str, Any]] = [
            {"role": "user", "content": f"Quiero definir la especificación para esta feature/tarea: '{initial_idea}'"}
        ]

        print("\n" + "📋" + "═" * 70)
        print("🧠 [FASE 1: MODO ENTREVISTA DE ESPECIFICACIONES]")
        print(f"💡 Idea inicial: {initial_idea}")
        print("═" * 72)
        print("El agente revisará las reglas de arquitectura y te entrevistará para cerrar la spec.")
        print("Nota: El proceso NO terminará hasta que tú confirmes que todo está 100% claro.\n")

        latest_spec_draft = ""
        turn = 1

        while True:
            print(f"⏳ [Turno {turn}] El arquitecto está analizando...")
            try:
                response = self.provider.chat(
                    messages=messages,
                    tools=[],  # En la entrevista dialogamos directamente sin tools
                    system_prompt=system_prompt
                )
            except Exception as e:
                print(f"❌ Error conectando con el modelo: {e}")
                return ""

            agent_reply = response.content
            latest_spec_draft = agent_reply

            print("\n" + "━" * 70)
            print("🤖 [RESPUESTA DEL ARQUITECTO]")
            print(agent_reply)
            print("━" * 70)

            # Gate de confirmación explícito
            print("\n👉 ¿Qué deseas hacer?")
            print("   - Escribe 'OK' o 'APROBADO' si la spec está 100% clara y lista para guardar.")
            print("   - O escribe tus dudas, ajustes o respuestas a las preguntas anteriores:")

            try:
                user_feedback = input("\nTu respuesta: ").strip()
                while not user_feedback:
                    user_feedback = input("Por favor ingresa una respuesta: ").strip()
            except (EOFError, KeyboardInterrupt):
                print("\nEntrevista cancelada por el usuario.")
                return ""

            # Verificación del Gate
            approval_keywords = {"ok", "aprobado", "aprobar", "sí", "si", "yes", "listo"}
            if user_feedback.lower() in approval_keywords:
                print("\n🎉 ¡Especificación aprobada al 100% por el desarrollador!")
                break

            # Agregar turno al historial y continuar
            messages.append({"role": "assistant", "content": agent_reply})
            messages.append({"role": "user", "content": user_feedback})
            turn += 1

        # Extraer y guardar la spec en specs/<slug>.md
        slug = re.sub(r"[^a-zA-Z0-9]+", "_", initial_idea.lower()).strip("_")[:30] or "feature_spec"
        spec_filename = f"{slug}.md"
        spec_path = os.path.join(self.specs_dir, spec_filename)

        with open(spec_path, "w", encoding="utf-8") as f:
            f.write(f"# SPECIFICATION: {initial_idea.upper()}\n\n{latest_spec_draft}\n")

        rel_spec_path = os.path.relpath(spec_path, PROJECT_ROOT)
        print("\n" + "💾" + "═" * 70)
        print(f"✅ Spec guardada exitosamente en: {rel_spec_path}")
        print(f"🚀 Ahora puedes lanzar el bucle autónomo cuando desees:")
        print(f"   python -m agents_dev run --spec {rel_spec_path}")
        print("═" * 72 + "\n")

        return rel_spec_path
