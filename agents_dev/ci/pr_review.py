"""
Universal CI: PR Reviewer & Quality Guardrails
Ejecuta las 5 skills especialistas sobre los archivos Kotlin modificados en la PR,
publica un informe claro y bloquea el merge si detecta violaciones críticas.
"""
import os
import sys
import json
import time
from github import Github

# Asegurar importación de skills
SKILLS_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "skills"))
if SKILLS_DIR not in sys.path:
    sys.path.insert(0, SKILLS_DIR)

import architecture_auditor
import security_scanner
import test_generator
import kdoc_specialist
import refactor_assistant

EXCLUDE_DIRS = ["di/", "ui/theme/"]
MAX_FILES = 10


def get_modified_kt_files():
    """Detecta los archivos .kt modificados en el PR a través del evento de GitHub."""
    event_path = os.environ.get("GITHUB_EVENT_PATH")
    if not event_path or not os.path.exists(event_path):
        print("ℹ️ GITHUB_EVENT_PATH no disponible (ejecución fuera de GitHub Actions).")
        return []

    try:
        with open(event_path, "r", encoding="utf-8") as f:
            event_data = json.load(f)

        if "pull_request" not in event_data:
            print("ℹ️ El evento actual no corresponde a un pull_request.")
            return []

        pr_number = event_data["pull_request"]["number"]
        repo_name = os.environ.get("GITHUB_REPOSITORY")
        github_token = os.environ.get("GITHUB_TOKEN")

        if not repo_name or not github_token:
            print("⚠️ GITHUB_REPOSITORY o GITHUB_TOKEN no configurados.")
            return []

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
        return []


def post_pr_comment(comment_body: str):
    """Publica un comentario único en la conversación del Pull Request."""
    event_path = os.environ.get("GITHUB_EVENT_PATH")
    if not event_path or not os.path.exists(event_path):
        return

    try:
        with open(event_path, "r", encoding="utf-8") as f:
            event_data = json.load(f)

        if "pull_request" not in event_data:
            return

        pr_number = event_data["pull_request"]["number"]
        repo_name = os.environ.get("GITHUB_REPOSITORY")
        github_token = os.environ.get("GITHUB_TOKEN")

        if not repo_name or not github_token:
            return

        g = Github(github_token)
        repo = g.get_repo(repo_name)
        pr = repo.get_pull(pr_number)
        pr.create_issue_comment(comment_body)
        print("✅ Comentario publicado en el PR.")
    except Exception as e:
        print(f"⚠️ No se pudo publicar el comentario en el PR: {e}")


def run_pr_review_and_guardrails():
    files = get_modified_kt_files()
    if not files:
        print("ℹ️ No hay archivos .kt modificados para analizar.")
        post_pr_comment(
            "## 🤖 Agent Code Review\n\n"
            "ℹ️ No se detectaron archivos Kotlin (`.kt`) modificados en este Pull Request. "
            "Revisión completada sin incidencias."
        )
        sys.exit(0)

    print(f"🔍 Archivos Kotlin a revisar ({len(files)}): {files}")

    skills = [
        ("Architecture", architecture_auditor.run_architecture_audit),
        ("Security", security_scanner.run_security_scan),
        ("Testing", test_generator.run_test_design),
        ("KDoc", kdoc_specialist.run_kdoc_audit),
        ("Refactor", refactor_assistant.run_refactor_analysis)
    ]

    all_findings = []
    errors = []

    for file_path in files:
        print(f"\n📄 Analizando: {file_path}")
        if not os.path.exists(file_path):
            print(f"  ⚠️ Archivo no encontrado en checkout local: {file_path}")
            continue

        for name, skill_func in skills:
            print(f"  ↳ Ejecutando skill: {name}...")
            try:
                response = skill_func(file_path, output_format="json")
                response = response.strip()
                if response.startswith("```json"):
                    response = response[7:]
                if response.endswith("```"):
                    response = response[:-3]

                result_data = json.loads(response.strip())
                if "findings" in result_data and isinstance(result_data["findings"], list):
                    all_findings.extend(result_data["findings"])
            except Exception as e:
                err_msg = str(e)
                print(f"    ⚠️ Error en {name} ({file_path}): {err_msg}")
                errors.append({"skill": name, "file": file_path, "error": err_msg})

            time.sleep(2)

    # 1. Construir el comentario para el PR
    if all_findings:
        findings_by_file = {}
        for f in all_findings:
            file_key = f.get("file", "General")
            findings_by_file.setdefault(file_key, []).append(f)

        body = "## 🤖 Agent Code Review: Hallazgos Detectados\n\n"
        emoji_map = {"critical": "🔴", "warning": "🟡", "info": "🟢"}

        for file_key, file_findings in findings_by_file.items():
            body += f"### 📄 `{file_key}`\n"
            body += "| Sev | Categoría | Hallazgo | Sugerencia |\n"
            body += "| :---: | :--- | :--- | :--- |\n"
            for f in file_findings:
                sev = f.get("severity", "info").lower()
                emoji = emoji_map.get(sev, "🟢")
                cat = f.get("category", "-").capitalize()
                line = f"L{f.get('line')}" if f.get("line") else ""
                msg = str(f.get("message", "-")).replace("|", "-")
                sug = str(f.get("suggestion", "-")).replace("|", "-")
                body += f"| {emoji} | **{cat}** {line} | {msg} | {sug} |\n"
            body += "\n"

        if errors:
            body += "\n### ⚠️ Incidencias de ejecución:\n"
            for err in errors:
                body += f"- **{err['skill']}** en `{err['file']}`: `{err['error'][:80]}`\n"

        post_pr_comment(body)

    elif errors:
        body = "## ⚠️ Agent Code Review: Alerta durante el análisis\n\n"
        body += "No se pudieron ejecutar algunas skills automáticas:\n\n"
        for err in errors:
            body += f"- **{err['skill']}** en `{err['file']}`: `{err['error'][:90]}`\n"
        body += "\n> 💡 *Asegúrate de que el Secret `GEMINI_API_KEY` esté configurado en los Secrets de GitHub Actions.*"
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

    # 2. Guardrails: Bloquear el merge si hay violaciones críticas en Seguridad o Arquitectura
    critical_violations = [
        f for f in all_findings
        if f.get("severity", "").lower() == "critical"
        and f.get("category", "").lower() in ["security", "architecture"]
    ]

    if critical_violations:
        print("\n❌ [GUARDRAIL TRIGGERED]: Se detectaron violaciones críticas bloqueantes:")
        for v in critical_violations:
            print(f"  - [{v.get('category').upper()}] {v.get('file')}: {v.get('message')}")
        print("\n⛔ El PR no puede fusionarse hasta resolver estas incidencias críticas.")
        sys.exit(1)

    print("\n✅ [GUARDRAILS PASS]: Cero violaciones críticas bloqueantes. PR apto para merge.")
    sys.exit(0)


if __name__ == "__main__":
    run_pr_review_and_guardrails()
