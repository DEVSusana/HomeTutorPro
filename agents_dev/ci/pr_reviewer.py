"""
Fase 1: PR Reviewer
Ejecuta las skills de agente sobre los archivos modificados y publica un comentario en el PR.
"""
import os
import sys
import json
import time
from github import Github

# Asegurar que Python encuentra las skills
sys.path.append(os.path.join(os.path.dirname(__file__), "..", "skills"))

import pr_analyzer
import architecture_auditor
import security_scanner
import test_generator
import kdoc_specialist
import refactor_assistant

def run_reviews():
    files = pr_analyzer.get_modified_kt_files()
    if not files:
        print("No hay archivos .kt modificados para analizar.")
        return

    all_findings = []
    
    # Skills a ejecutar
    skills = [
        ("Architecture", architecture_auditor.run_architecture_audit),
        ("Security", security_scanner.run_security_scan),
        ("Testing", test_generator.run_test_design),
        ("KDoc", kdoc_specialist.run_kdoc_audit),
        ("Refactor", refactor_assistant.run_refactor_analysis)
    ]

    for file_path in files:
        print(f"Analizando: {file_path}")
        if not os.path.exists(file_path):
            print(f"Archivo no encontrado localmente: {file_path}")
            continue

        for name, skill_func in skills:
            print(f"  Ejecutando {name}...")
            try:
                response = skill_func(file_path, output_format="json")
                # Limpiar la respuesta (Gemini a veces envuelve en markdown ```json)
                response = response.strip()
                if response.startswith("```json"):
                    response = response[7:]
                if response.endswith("```"):
                    response = response[:-3]
                
                result_data = json.loads(response)
                if "findings" in result_data:
                    all_findings.extend(result_data["findings"])
                    
            except Exception as e:
                print(f"  Error en {name} para {file_path}: {e}")
            
            # Pausa para no saturar la API gratuita
            time.sleep(5)

    if all_findings:
        post_comment_to_pr(all_findings)
        
        # Guardar findings para la Fase 2 y 3
        with open("agent_findings.json", "w") as f:
            json.dump(all_findings, f)
    else:
        print("No se encontraron problemas.")

def post_comment_to_pr(findings):
    event_path = os.environ.get("GITHUB_EVENT_PATH")
    if not event_path:
        return

    with open(event_path, 'r') as f:
        event_data = json.load(f)

    if "pull_request" not in event_data:
        return

    pr_number = event_data["pull_request"]["number"]
    repo_name = os.environ.get("GITHUB_REPOSITORY")
    github_token = os.environ.get("GITHUB_TOKEN")

    g = Github(github_token)
    repo = g.get_repo(repo_name)
    pr = repo.get_pull(pr_number)

    # Agrupar findings por archivo
    findings_by_file = {}
    for f in findings:
        file = f.get("file", "General")
        if file not in findings_by_file:
            findings_by_file[file] = []
        findings_by_file[file].append(f)

    comment_body = "## 🤖 Agent Code Review\n\n"
    
    emoji_map = {"critical": "🔴", "warning": "🟡", "info": "🟢"}
    
    for file, file_findings in findings_by_file.items():
        comment_body += f"### 📄 `{file}`\n"
        comment_body += "| Sev | Categoría | Hallazgo | Sugerencia |\n"
        comment_body += "|-----|-----------|----------|------------|\n"
        
        for finding in file_findings:
            sev = finding.get("severity", "info")
            emoji = emoji_map.get(sev, "🟢")
            cat = finding.get("category", "-").capitalize()
            msg = finding.get("message", "-").replace("|", "-")
            sug = finding.get("suggestion", "-").replace("|", "-")
            line = finding.get("line", 0)
            
            line_str = f"L{line}" if line else ""
            
            comment_body += f"| {emoji} | **{cat}** {line_str} | {msg} | {sug} |\n"
        
        comment_body += "\n"

    # Publicar el comentario
    pr.create_issue_comment(comment_body)
    print("Comentario publicado en el PR.")

if __name__ == "__main__":
    run_reviews()
