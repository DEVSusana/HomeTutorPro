import os
import sys
import argparse

# Ensure relative imports
AGENTS_DEV_DIR = os.path.abspath(os.path.dirname(__file__))
if AGENTS_DEV_DIR not in sys.path:
    sys.path.insert(0, AGENTS_DEV_DIR)

from providers.base import BaseLLMProvider
from core.engine import AgenticEngine
from core.orchestrator import FeatureOrchestrator
from planner.spec_interviewer import SpecInterviewer


def get_default_provider(provider_name: str = None, model: str = None) -> BaseLLMProvider:
    """Selects the LLM provider based on argument or available environment variables."""
    name = (provider_name or "").lower()

    if name == "claude" or (not name and os.environ.get("ANTHROPIC_API_KEY")):
        from providers.anthropic_provider import AnthropicProvider
        return AnthropicProvider(model=model)
    elif name == "gemini" or (not name and os.environ.get("GEMINI_API_KEY")):
        from providers.gemini_provider import GeminiProvider
        return GeminiProvider(model=model)
    else:
        # Default fallback: attempt Gemini or guide the user
        print("ℹ️ No explicit provider detected. Attempting to initialize with Gemini...")
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
        help="LLM Provider to use (claude = Anthropic Claude 3.5 Sonnet, gemini = Google Gemini 2.5 Flash / 3.1 Flash-Lite)."
    )
    parser.add_argument(
        "--model",
        type=str,
        default=None,
        help="Specific model to use (e.g. gemini-3.1-flash-lite, gemini-2.5-pro, claude-3-5-sonnet-20241022)."
    )

    subparsers = parser.add_subparsers(dest="subcommand", help="Operating mode")

    # 1. Planning / Interview Mode
    plan_parser = subparsers.add_parser("plan", help="Phase 1: Interactive interview to define and lock the technical Spec (.md).")
    plan_parser.add_argument("idea", type=str, help="Initial idea or description of the feature or change.")

    # 2. Agentic Run Mode
    run_parser = subparsers.add_parser("run", help="Phase 2: Autonomous agentic loop with surgical precision and Gradle self-healing.")
    run_parser.add_argument("goal", nargs="?", default="", help="Direct objective to implement or refactor.")
    run_parser.add_argument("--spec", type=str, help="Path to the approved technical specification (.md) file.")
    run_parser.add_argument("--iterations", type=int, default=25, help="Maximum number of ReAct loop iterations (default: 25).")
    run_parser.add_argument("--no-blind-review", action="store_true", help="Skip the independent Blind Reviewer step after Builder finishes.")

    # 3. Skills Review Mode (Local CI)
    review_parser = subparsers.add_parser("review", help="Execute the 5 review skills (Architecture, Security, Tests, KDoc, Refactor) locally.")
    review_parser.add_argument("files", nargs="*", default=[], help="Specific Kotlin files to audit (optional).")

    # 4. End-to-End Feature Orchestrator Mode
    orch_parser = subparsers.add_parser("orchestrate", help="Phase 3: Run spec -> worktree -> Builder + BlindReviewer -> optional PR.")
    orch_parser.add_argument("--spec", required=True, type=str, help="Path to the approved specification (.md).")
    orch_parser.add_argument("--slug", type=str, default=None, help="Optional worktree/branch slug.")
    orch_parser.add_argument("--no-worktree", action="store_true", help="Run in current workspace instead of creating a worktree.")
    orch_parser.add_argument("--create-pr", action="store_true", help="Ask to push and open a GitHub PR after approval.")
    orch_parser.add_argument("--iterations", type=int, default=25, help="Maximum Builder iterations (default: 25).")

    args = parser.parse_args()

    if not args.subcommand:
        parser.print_help()
        sys.exit(1)

    if args.subcommand == "review":
        from ci.pr_review import run_pr_review_and_guardrails
        target_files = args.files if args.files else None
        run_pr_review_and_guardrails(target_files)
        return

    provider = get_default_provider(args.provider, model=args.model)
    print(f"🤖 Active Provider: {provider.__class__.__name__} | Model: {provider.model}")

    if args.subcommand == "plan":
        interviewer = SpecInterviewer(provider=provider)
        spec_path = interviewer.interview(args.idea)
        if spec_path:
            print(f"✅ Specification process completed successfully.")
        else:
            print("⚠️ Specification process cancelled.")

    elif args.subcommand == "run":
        if not args.goal and not args.spec:
            print("❌ Error: You must provide a goal in quotes or a spec with --spec <path>.")
            sys.exit(1)

        goal = args.goal or f"Fully implement and verify the specification in '{args.spec}'."
        engine = AgenticEngine(
            provider=provider,
            max_iterations=args.iterations,
            enable_blind_review=not args.no_blind_review,
        )
        success = engine.run(goal=goal, spec_file=args.spec)

        if success:
            print("\n🎉 Agentic mission completed and approved successfully!")
        else:
            print("\n❌ Agentic mission finished or changes reverted.")

    elif args.subcommand == "orchestrate":
        orchestrator = FeatureOrchestrator(provider=provider, max_iterations=args.iterations)
        success = orchestrator.run(
            spec_file=args.spec,
            slug=args.slug,
            use_worktree=not args.no_worktree,
            create_pr=args.create_pr,
        )
        if success:
            print("\n🎉 Feature orchestration completed!")
        else:
            print("\n❌ Feature orchestration stopped or cancelled.")


if __name__ == "__main__":
    main()
