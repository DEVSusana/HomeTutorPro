"""
Analiza los archivos modificados en un Pull Request de GitHub.
Excluye los directorios configurados (como `di/` y `ui/theme/`).
"""
import os
import json
from github import Github

# Archivos o directorios a excluir
EXCLUDE_DIRS = ["di/", "ui/theme/"]
MAX_FILES = 10

def get_modified_kt_files():
    """Obtiene la lista de archivos .kt modificados en el PR."""
    # En GitHub Actions, GITHUB_EVENT_PATH apunta al JSON del evento
    event_path = os.environ.get("GITHUB_EVENT_PATH")
    if not event_path:
        print("No se encontró GITHUB_EVENT_PATH. Probablemente no estés en GitHub Actions.")
        return []

    with open(event_path, 'r') as f:
        event_data = json.load(f)

    if "pull_request" not in event_data:
        print("El evento no es un pull_request.")
        return []

    pr_number = event_data["pull_request"]["number"]
    repo_name = os.environ.get("GITHUB_REPOSITORY")
    github_token = os.environ.get("GITHUB_TOKEN")

    if not repo_name or not github_token:
        print("GITHUB_REPOSITORY o GITHUB_TOKEN no están configurados.")
        return []

    g = Github(github_token)
    repo = g.get_repo(repo_name)
    pr = repo.get_pull(pr_number)

    files = pr.get_files()
    target_files = []

    for file in files:
        if file.status in ["removed"]:
            continue
            
        filename = file.filename
        
        # Filtrar solo .kt
        if not filename.endswith(".kt"):
            continue
            
        # Aplicar exclusiones
        is_excluded = any(excl in filename for excl in EXCLUDE_DIRS)
        if is_excluded:
            continue
            
        target_files.append(filename)
        
        if len(target_files) >= MAX_FILES:
            print(f"Alcanzado el límite máximo de {MAX_FILES} archivos. Ignorando el resto.")
            break

    return target_files

if __name__ == "__main__":
    files = get_modified_kt_files()
    print("Archivos a analizar:")
    for f in files:
        print(f" - {f}")
