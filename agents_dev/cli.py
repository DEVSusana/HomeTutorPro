import os
import sys
import argparse

# Asegurar importaciones relativas
AGENTS_DEV_DIR = os.path.abspath(os.path.dirname(__file__))
if AGENTS_DEV_DIR not in sys.path:
    sys.path.insert(0, AGENTS_DEV_DIR)

from providers.base import BaseLLMProvider
from core.engine import AgenticEngine
from planner.spec_interviewer import SpecInterviewer


def get_default_provider(provider_name: str = None, model: str = None) -> BaseLLMProvider:
    """Selecciona el proveedor según el argumento o las variables de entorno disponibles."""
    name = (provider_name or "").lower()

    if name == "claude" or (not name and os.environ.get("ANTHROPIC_API_KEY")):
        from providers.anthropic_provider import AnthropicProvider
        return AnthropicProvider(model=model)
    elif name == "gemini" or (not name and os.environ.get("GEMINI_API_KEY")):
        from providers.gemini_provider import GeminiProvider
        return GeminiProvider(model=model)
    else:
        # Fallback por defecto: intentar Gemini si existe o guiar al usuario
        print("ℹ️ No se detectó proveedor explícito. Intentando inicializar con Gemini...")
        from providers.gemini_provider import GeminiProvider
        return GeminiProvider(model=model)


def main():
    parser = argparse.ArgumentParser(
        prog="agents_dev",
        description="Universal Agentic Loop & Spec-Driven Development Engine for Android Studio."
    )
    parser.add_argument(
        "--provider",
        choices=["claude", "gemini"],
        help="LLM Provider to use (claude = Anthropic Claude 3.5 Sonnet, gemini = Google Gemini 2.5 Flash)."
    )
    parser.add_argument(
        "--model",
        type=str,
        default=None,
        help="Modelo específico a usar (ej: gemini-3.1-flash-lite, gemini-2.5-pro, claude-3-5-sonnet-20241022)."
    )

    subparsers = parser.add_subparsers(dest="subcommand", help="Modo de operación")

    # 1. Modo Planificación / Entrevista
    plan_parser = subparsers.add_parser("plan", help="Fase 1: Entrevista interactiva para definir y cerrar la Spec (.md).")
    plan_parser.add_argument("idea", type=str, help="Idea o descripción inicial de la feature/cambio.")

    # 2. Modo Ejecución Agéntica
    run_parser = subparsers.add_parser("run", help="Fase 2: Bucle agéntico autónomo con cirugía de precisión y Gradle self-healing.")
    run_parser.add_argument("goal", nargs="?", default="", help="Objetivo directo a implementar o refactorizar.")
    run_parser.add_argument("--spec", type=str, help="Ruta al archivo de especificación .md aprobado.")
    run_parser.add_argument("--iterations", type=int, default=25, help="Límite máximo de iteraciones ReAct (default: 25).")

    args = parser.parse_args()

    if not args.subcommand:
        parser.print_help()
        sys.exit(1)

    provider = get_default_provider(args.provider, model=args.model)
    print(f"🤖 Proveedor activo: {provider.__class__.__name__} | Modelo: {provider.model}")

    if args.subcommand == "plan":
        interviewer = SpecInterviewer(provider=provider)
        spec_path = interviewer.interview(args.idea)
        if spec_path:
            print(f"✅ Proceso de especificación finalizado con éxito.")
        else:
            print("⚠️ Proceso de especificación cancelado.")

    elif args.subcommand == "run":
        if not args.goal and not args.spec:
            print("❌ Error: Debes proporcionar un objetivo entre comillas o una spec con --spec <ruta>.")
            sys.exit(1)

        goal = args.goal or f"Implementar y verificar completamente la especificación en '{args.spec}'."
        engine = AgenticEngine(provider=provider, max_iterations=args.iterations)
        success = engine.run(goal=goal, spec_file=args.spec)

        if success:
            print("\n🎉 ¡Misión agéntica completada y aprobada con éxito!")
        else:
            print("\n❌ Misión agéntica finalizada o cambios revertidos.")


if __name__ == "__main__":
    main()
