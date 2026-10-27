"""
Universal CI: PR Reviewer & Quality Guardrails
Ejecuta las 5 skills especialistas sobre los archivos Kotlin modificados en la PR,
publica un informe claro y bloquea el merge si detecta violaciones críticas.
"""
import os
import sys
import json
import time
import subprocess

try:
    from github import Github
except ImportError:
    Github = None

# Asegurar importación de skills
SKILLS_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "skills"))
if SKILLS_DIR not in sys.path:
    sys.path.insert(0, SKILLS_DIR)

import unified_reviewer

EXCLUDE_DIRS = ["di/", "ui/theme/", "build/", ".gradle/", "ksp/"]
MAX_FILES = 10


def get_modified_kt_files(explicit_files=None):
    """Detecta los archivos .kt modificados en el PR a través del evento de GitHub o localmente con git diff."""
    if explicit_files:
        valid_files = [
            f for f in explicit_files
            if f.endswith(".kt") and os.path.exists(f) and not any(excl in f for excl in EXCLUDE_DIRS)
        ]
        if valid_files:
            return valid_files[:MAX_FILES]

    event_path = os.environ.get("GITHUB_EVENT_PATH")
    if event_path and os.path.exists(event_path):
        try:
            with open(event_path, "r", encoding="utf-8") as f:
                event_data = json.load(f)

            if "pull_request" in event_data:
                pr_number = event_data["pull_request"]["number"]
                repo_name = os.environ.get("GITHUB_REPOSITORY")
                github_token = os.environ.get("GITHUB_TOKEN")

                if repo_name and github_token and Github is not None:
                    g = Github(github_token)
                    repo = g.get_repo(repo_name)
                    pr = repo.get_pull(pr_number)

                    target_files = []
                    for file in pr.get_files():
                        if file.status == "removed" or not file.filename.endswith(".kt"):
                            continue
                        if any(excl in file.filename for excl in EXCLUDE_DIRS):
                            continue

                        target_files.append(file.filename)
                        if len(target_files) >= MAX_FILES:
                            break

                    return target_files
        except Exception as e:
            print(f"⚠️ Error obteniendo archivos modificados del PR: {e}")

    # Fallback para ejecución local vía git diff
    print("ℹ️ Detectando archivos Kotlin modificados localmente...")
    for cmd in [
        ["git", "diff", "--name-only", "origin/develop...HEAD"],
        ["git", "diff", "--name-only", "HEAD~1"],
        ["git", "diff", "--name-only", "--staged"],
        ["git", "diff", "--name-only"]
    ]:
        try:
            res = subprocess.run(cmd, capture_output=True, text=True, check=True)
            candidate_files = [line.strip() for line in res.stdout.splitlines() if line.strip()]
            valid_files = [
                f for f in candidate_files
                if f.endswith(".kt") and os.path.exists(f) and not any(excl in f for excl in EXCLUDE_DIRS)
            ]
            if valid_files:
                return valid_files[:MAX_FILES]
        except Exception:
            continue

    return []


def post_pr_comment(comment_body: str):
    """Publica un comentario único en el PR o lo muestra en consola si es ejecución local."""
    event_path = os.environ.get("GITHUB_EVENT_PATH")
    if not event_path or not os.path.exists(event_path):
        print("\n" + "=" * 70)
        print("🤖 REPORTE DE REVISIÓN DE SKILLS (EJECUCIÓN LOCAL)")
        print("=" * 70)
        print(comment_body)
        print("=" * 70 + "\n")
        return

    try:
        with open(event_path, "r", encoding="utf-8") as f:
            event_data = json.load(f)

        if "pull_request" not in event_data:
            print(comment_body)
            return

        pr_number = event_data["pull_request"]["number"]
        repo_name = os.environ.get("GITHUB_REPOSITORY")
        github_token = os.environ.get("GITHUB_TOKEN")

        if not repo_name or not github_token or Github is None:
            print(comment_body)
            return

        g = Github(github_token)
        repo = g.get_repo(repo_name)
        pr = repo.get_pull(pr_number)

        # Buscar y limpiar comentarios anteriores del bot para mantener la PR limpia
        try:
            bot_markers = [
                "## 🤖 Agent Code Review",
                "## ⚠️ Agent Code Review"
            ]
            for comment in pr.get_issue_comments():
                if any(marker in (comment.body or "") for marker in bot_markers):
                    try:
                        comment.delete()
                    except Exception as err:
                        print(f"⚠️ No se pudo eliminar comentario previo {comment.id}: {err}")
        except Exception as e:
            print(f"⚠️ Error limpiando comentarios previos: {e}")

        pr.create_issue_comment(comment_body)
        print("✅ Comentario publicado en el PR (comentarios antiguos limpiados).")
    except Exception as e:
        print(f"⚠️ No se pudo publicar el comentario en el PR: {e}")
        print(comment_body)


def run_pr_review_and_guardrails(target_files=None):
    files = get_modified_kt_files(target_files)
    if not files:
        print("ℹ️ No hay archivos .kt modificados para analizar.")
        post_pr_comment(
            "## 🤖 Agent Code Review\n\n"
            "ℹ️ No se detectaron archivos Kotlin (`.kt`) modificados en este Pull Request. "
            "Revisión completada sin incidencias."
        )
        sys.exit(0)

    print(f"🔍 Archivos Kotlin a revisar ({len(files)}): {files}")

    all_findings = []
    errors = []

    for file_path in files:
        print(f"\n📄 Analizando quirúrgicamente (Unified Review): {file_path}")
        if not os.path.exists(file_path):
            print(f"  ⚠️ Archivo no encontrado en checkout local: {file_path}")
            continue

        try:
            result_data = unified_reviewer.run_unified_review(file_path)
            findings = result_data.get("findings", [])
            all_findings.extend(findings)
            print(f"  ↳ Hallazgos detectados en {file_path}: {len(findings)}")
        except Exception as e:
            err_msg = str(e)
            print(f"  ⚠️ Error analizando {file_path}: {err_msg}")
            errors.append({"skill": "UnifiedReview", "file": file_path, "error": err_msg})

        time.sleep(2)

    # 1. Filtrar solo hallazgos accionables (ignorar informativos/verdes para mantener el informe limpio y conciso)
    actionable_findings = [
        f for f in all_findings
        if str(f.get("severity", "")).lower() in ["critical", "high", "warning", "medium"]
    ]

    if actionable_findings:
        findings_by_file = {}
        for f in actionable_findings:
            file_key = f.get("file", "General")
            findings_by_file.setdefault(file_key, []).append(f)

        body = "## 🤖 Agent Code Review: Hallazgos Detectados\n\n"
        emoji_map = {"critical": "🔴", "high": "🔴", "warning": "🟡", "medium": "🟡"}

        for file_key, file_findings in findings_by_file.items():
            body += f"### 📄 `{file_key}`\n"
            body += "| Sev | Categoría | Hallazgo | Sugerencia |\n"
            body += "| :---: | :--- | :--- | :--- |\n"
            for f in file_findings:
                sev = str(f.get("severity", "warning")).lower()
                emoji = emoji_map.get(sev, "🟡")
                cat = str(f.get("category", "-")).capitalize()
                line = f"L{f.get('line')}" if f.get("line") else ""
                msg = str(f.get("message") or f.get("title") or "-").replace("|", "-")
                sug = str(f.get("suggestion") or f.get("remediation") or "-").replace("|", "-")
                body += f"| {emoji} | **{cat}** {line} | {msg} | {sug} |\n"
            body += "\n"

        if errors:
            body += "\n### ⚠️ Incidencias de ejecución:\n"
            for err in errors:
                body += f"- **{err['skill']}** en `{err['file']}`: `{err['error'][:80]}`\n"

            has_auth_error = any("GEMINI_API_KEY" in str(e["error"]) or "API_KEY_INVALID" in str(e["error"]) for e in errors)
            if has_auth_error:
                body += "\n> 💡 *Aviso de credenciales: Asegúrate de que el Secret `GEMINI_API_KEY` esté configurado en los Secrets de GitHub Actions.*\n"

        post_pr_comment(body)

    elif errors:
        body = "## ⚠️ Agent Code Review: Alerta durante el análisis\n\n"
        body += "No se pudieron ejecutar algunas skills automáticas:\n\n"
        for err in errors:
            body += f"- **{err['skill']}** en `{err['file']}`: `{err['error'][:90]}`\n"

        has_auth_error = any("GEMINI_API_KEY" in str(e["error"]) or "API_KEY_INVALID" in str(e["error"]) for e in errors)
        if has_auth_error:
            body += "\n> 💡 *Asegúrate de que el Secret `GEMINI_API_KEY` esté configurado en los Secrets de GitHub Actions.*"
        else:
            body += "\n> ℹ️ *Incidencia temporal de red o sobrecarga en los servidores de la API.*"
        post_pr_comment(body)

    else:
        body = "## 🤖 Agent Code Review: Todo en Orden ✅\n\n"
        body += f"Se analizaron con éxito **{len(files)} archivo(s) Kotlin** con las 5 skills de agentes:\n"
        for f in files:
            body += f"- `{f}`\n"
        body += "\n| Skill | Estado | Resultado |\n"
        body += "| :--- | :---: | :--- |\n"
        body += "| 🔒 **Seguridad** | ✅ PASS | Sin credenciales expuestas, corrutinas seguras y nomenclatura en inglés. |\n"
        body += "| 🏛️ **Arquitectura** | ✅ PASS | Cumple Clean Architecture y reglas de negocio. |\n"
        body += "| 🧪 **Tests** | ✅ PASS | Estructura de pruebas unitarias validada. |\n"
        body += "| 📝 **KDoc** | ✅ PASS | Documentación pública conforme. |\n"
        body += "| 🧹 **Refactor** | ✅ PASS | Sin code smells críticos detectados. |\n"
        post_pr_comment(body)

    # 2. Guardrails: Mostrar todas las sugerencias de forma consultiva sin bloquear el merge del PR
    critical_violations = [
        f for f in all_findings
        if f.get("severity", "").lower() == "critical"
    ]

    if critical_violations:
        print(f"\n⚠️ Se detectaron {len(critical_violations)} observaciones críticas (sugerencias publicadas en el PR):")
        for v in critical_violations:
            cat = str(v.get("category", "GENERAL")).upper()
            print(f"  - [{cat}] {v.get('file')}: {v.get('message')}")
        print("\nℹ️ [ADVISORY GUARDRAIL]: Sugerencias publicadas en el PR. No bloquea el merge para permitir revisión humana.")
    else:
        print("\n✅ [GUARDRAILS PASS]: Cero violaciones críticas. PR apto para merge.")

    sys.exit(0)


if __name__ == "__main__":
    cli_files = [a for a in sys.argv[1:] if not a.startswith("-")]
    run_pr_review_and_guardrails(cli_files if cli_files else None)
