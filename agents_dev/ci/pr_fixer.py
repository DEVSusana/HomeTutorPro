"""
Fase 3: Auto-Fixer
Genera correcciones para los hallazgos auto-fixables, crea una rama y abre un PR.
"""
import json
import sys
import os
import time
from github import Github
import agents_dev_config as config

FIXABLE_CATEGORIES = ["kdoc", "refactor", "testing"]

def create_fixes():
    if not os.path.exists("agent_findings.json"):
        print("No agent_findings.json found.")
        return

    with open("agent_findings.json", "r") as f:
        findings = json.load(f)

    # Filtrar solo findings fixables
    fixable_findings = [f for f in findings if f.get("category", "").lower() in FIXABLE_CATEGORIES]
    
    if not fixable_findings:
        print("No hay hallazgos auto-fixables.")
        return

    # Agrupar por archivo
    findings_by_file = {}
    for f in fixable_findings:
        file = f.get("file")
        if file and os.path.exists(file):
            if file not in findings_by_file:
                findings_by_file[file] = []
            findings_by_file[file].append(f)

    if not findings_by_file:
        print("No hay archivos válidos para fixear.")
        return

    client = config.get_client()
    with open('AGENTS.md', 'r') as f:
        rules = f.read()

    files_fixed = []

    for file_path, file_findings in findings_by_file.items():
        print(f"Generando fix para {file_path}...")
        
        with open(file_path, 'r') as f:
            code = f.read()

        findings_text = json.dumps(file_findings, indent=2)

        prompt = f"""
        Act as an Expert Kotlin Android Developer.
        You need to fix the following issues in the code based on the AGENTS.md rules.
        
        RULES:
        {rules}
        
        ISSUES TO FIX:
        {findings_text}
        
        ORIGINAL CODE:
        ```kotlin
        {code}
        ```
        
        IMPORTANT INSTRUCTIONS:
        1. Return ONLY the COMPLETE valid Kotlin code.
        2. Do not include markdown codeblocks (like ```kotlin). Just the raw text.
        3. Do not include any explanations.
        4. Apply the fixes required by the issues.
        """

        try:
            response = client.models.generate_content(
                model=config.MODEL_ID,
                contents=prompt
            )
            
            fixed_code = response.text.strip()
            # Clean up markdown if model ignored instructions
            if fixed_code.startswith("```kotlin"):
                fixed_code = fixed_code[9:]
            elif fixed_code.startswith("```"):
                fixed_code = fixed_code[3:]
            if fixed_code.endswith("```"):
                fixed_code = fixed_code[:-3]
                
            fixed_code = fixed_code.strip()

            # Basic validation
            if "package " in fixed_code or "import " in fixed_code:
                with open(file_path, 'w') as f:
                    f.write(fixed_code)
                files_fixed.append(file_path)
                print(f"Fix aplicado localmente a {file_path}")
            else:
                print(f"El código generado para {file_path} no parece válido. Omitiendo.")

        except Exception as e:
            print(f"Error generando fix para {file_path}: {e}")
            
        time.sleep(5)

    if files_fixed:
        create_pr_with_fixes(files_fixed)

def create_pr_with_fixes(files_fixed):
    event_path = os.environ.get("GITHUB_EVENT_PATH")
    if not event_path:
        return

    with open(event_path, 'r') as f:
        event_data = json.load(f)

    if "pull_request" not in event_data:
        return

    pr_number = event_data["pull_request"]["number"]
    base_branch = event_data["pull_request"]["head"]["ref"]  # Apuntamos a la rama de la PR original
    
    repo_name = os.environ.get("GITHUB_REPOSITORY")
    github_token = os.environ.get("GITHUB_TOKEN")

    g = Github(github_token)
    repo = g.get_repo(repo_name)
    
    timestamp = int(time.time())
    new_branch_name = f"agent/fix-pr-{pr_number}-{timestamp}"
    
    # Obtener el SHA de la rama base (head de la PR)
    base_ref = repo.get_git_ref(f"heads/{base_branch}")
    
    # Crear nueva rama
    repo.create_git_ref(ref=f"refs/heads/{new_branch_name}", sha=base_ref.object.sha)
    
    # Comitear archivos
    for file_path in files_fixed:
        with open(file_path, 'r') as f:
            content = f.read()
            
        # Obtener SHA del archivo en la rama nueva
        try:
            file_obj = repo.get_contents(file_path, ref=new_branch_name)
            repo.update_file(
                path=file_path,
                message=f"🤖 Auto-fix agent findings for {os.path.basename(file_path)}",
                content=content,
                sha=file_obj.sha,
                branch=new_branch_name
            )
        except Exception as e:
            print(f"No se pudo actualizar {file_path} en GitHub: {e}")

    # Crear PR apuntando a la rama del usuario, no a main
    try:
        new_pr = repo.create_pull(
            title=f"🤖 Auto-fixes for PR #{pr_number}",
            body="El agente ha corregido automáticamente algunos hallazgos (KDoc, naming, refactoring simple). Revisa los cambios y haz merge a tu rama si te parecen correctos.",
            head=new_branch_name,
            base=base_branch
        )
        print(f"PR de fixes creado: {new_pr.html_url}")
    except Exception as e:
        print(f"Error creando PR: {e}")

if __name__ == "__main__":
    create_fixes()
