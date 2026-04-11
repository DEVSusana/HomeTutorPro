import os
import time
import architecture_auditor
import security_scanner
import test_generator
import kdoc_specialist
import refactor_assistant
import config

# Configuración de rutas
TARGET_DIR = "app/src/main/java/com/devsusana/hometutorpro/"
EXCLUDE_DIRS = ["di", "ui/theme"]

# Nombres de los archivos de salida
REPORT_FILE = "project_reports.txt"
BACKLOG_FILE = "PENDING_TASKS.md"

def generate_unified_backlog(filename, reports):
    """Genera tareas técnicas basadas en los 5 reportes combinados."""
    client = config.get_client()
    # Unimos todos los reportes para que la IA decida las 5 tareas más críticas
    full_context = "\n".join(reports)

    prompt = f"""
    Based on the full audit (Arch, Sec, Test, KDoc, Refactor) for {filename}, 
    create a technical checklist [ ] with the 5 most important tasks.
    Be extremely specific so I can paste them into the chat later.
    
    CONTEXT:
    {full_context}
    """

    try:
        response = client.models.generate_content(model=config.MODEL_ID, contents=prompt)
        with open(BACKLOG_FILE, "a") as f:
            f.write(f"\n### 📋 Tasks for {filename}\n")
            f.write(response.text)
            f.write("\n" + "-"*30 + "\n")
    except Exception as e:
        print(f"⚠️ Backlog error for {filename}: {e}")

def save_detailed_report(filename, reports):
    """Guarda el análisis profundo de los 5 agentes."""
    names = ["ARCHITECTURE", "SECURITY", "TESTING", "KDOC", "REFACTOR"]
    with open(REPORT_FILE, "a") as f:
        f.write(f"\n\n{'='*30} FULL 360° REPORT: {filename} {'='*30}\n")
        for name, content in zip(names, reports):
            f.write(f"\n--- {name} ---\n{content}")
        f.write(f"\n{'-'*80}\n")

def scan_project():
    print(f"🚀 FULL 360° SCAN STARTING")

    with open(REPORT_FILE, "w") as f: f.write("--- FULL PROJECT MASTER ARCHIVE ---\n")
    with open(BACKLOG_FILE, "w") as f: f.write("# 🏗️ Project Technical Backlog\n")

    for root, dirs, files in os.walk(TARGET_DIR):
        if any(exclude in root for exclude in EXCLUDE_DIRS): continue

        for file in files:
            if file.endswith(".kt"):
                file_path = os.path.join(root, file)
                print(f"\n📄 ANALYZING: {file} with 5 Agents...")

                try:
                    # Ejecución secuencial de los 5 agentes
                    # 1. Arch
                    r1 = architecture_auditor.run_architecture_audit(file_path)
                    print("  [1/5] Arch ✅")
                    time.sleep(35)

                    # 2. Sec
                    r2 = security_scanner.run_security_scan(file_path)
                    print("  [2/5] Sec ✅")
                    time.sleep(35)

                    # 3. Test
                    r3 = test_generator.run_test_design(file_path)
                    print("  [3/5] Test ✅")
                    time.sleep(35)

                    # 4. KDoc
                    r4 = kdoc_specialist.run_kdoc_audit(file_path)
                    print("  [4/5] KDoc ✅")
                    time.sleep(35)

                    # 5. Refactor
                    r5 = refactor_assistant.run_refactor_analysis(file_path)
                    print("  [5/5] Refactor ✅")

                    # Guardar todo
                    all_reports = [r1, r2, r3, r4, r5]
                    save_detailed_report(file, all_reports)
                    generate_unified_backlog(file, all_reports)

                    print(f"✨ Finished {file}. Waiting 40s for next file...")
                    time.sleep(40)

                except Exception as e:
                    print(f"❌ Error in {file}: {e}")
                    time.sleep(60)

if __name__ == "__main__":
    scan_project()