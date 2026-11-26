---
name: discovery
description: Conducts structured product and technical discovery before writing a spec or code, producing a closed contract based on specs/_TEMPLATE.md.
---

# Discovery Skill

Use this skill before writing any implementation code for a new feature or refactor.

## Protocol
1. **Inspect existing context first:** Check relevant `domain/` entities, `BUSINESS_RULES.md`, and `presentation/` screens so you do not ask obvious questions already answered by the codebase.
2. **Ask 3–5 focused questions per turn** covering:
   - Exact user problem and expected behavior.
   - Explicit **Out of Scope** boundaries.
   - Business rules & edge cases (zero, negative, null, offline, permissions).
   - Exact files/layers in **Scope**.
   - Verifiable Acceptance Criteria (`Given / When / Then`) and Gradle test commands.
3. **Draft cleanly:** Keep questions outside `<!-- SPEC_START -->` ... `<!-- SPEC_END -->` and maintain the contract inside following `specs/_TEMPLATE.md`.
4. **Approval gate:** Never mark `Estado: APPROVED` or start coding until the developer explicitly confirms.
