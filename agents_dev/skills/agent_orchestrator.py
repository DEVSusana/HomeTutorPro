import os
import time
import architecture_auditor
import security_scanner
import test_generator
import kdoc_specialist
import refactor_assistant

def start_mission(target_file):
    if not os.path.exists(target_file):
        print(f"❌ Error: File {target_file} not found.")
        return

    print(f"\n🚀 MISSION START: Full Audit of {os.path.basename(target_file)}")
    print("="*80)

    # Lista de agentes a ejecutar
    agents = [
        ("Architecture", architecture_auditor.run_architecture_audit),
        ("Security", security_scanner.run_security_scan),
        ("Testing", test_generator.run_test_design),
        ("KDoc", kdoc_specialist.run_kdoc_audit),
        ("Refactor", refactor_assistant.run_refactor_analysis)
    ]

    for name, agent_function in agents:
        print(f"\n[STEP] Running {name} Agent...")
        try:
            report = agent_function(target_file)
            print(report)
        except Exception as e:
            print(f"⚠️ Error in {name}: {e}")

        # Pausa de Paz Mental entre agentes (Capa Gratuita)
        if name != agents[-1][0]: # No esperar después del último
            print(f"\n🧘 Resting 35s to keep API free...")
            time.sleep(35)
            print("-" * 40)

    print("\n" + "="*80)
    print("✅ MISSION COMPLETE: You now have a 360° view of your code.")

if __name__ == "__main__":
    import sys
    import subprocess
    
    def get_modified_kt_files():
        try:
            status_output = subprocess.check_output(
                ["git", "status", "--porcelain"], 
                stderr=subprocess.DEVNULL
            ).decode("utf-8")
            
            modified_files = []
            for line in status_output.splitlines():
                if line.strip():
                    # Format: XY filepath (e.g. M  app/src/main/.../File.kt)
                    # XY is 2 characters plus a space
                    filepath = line.strip()[3:].strip().strip('"')
                    if filepath.endswith(".kt") and os.path.exists(filepath):
                        modified_files.append(filepath)
            
            if not modified_files:
                diff_output = subprocess.check_output(
                    ["git", "diff", "--name-only", "HEAD~1", "HEAD"],
                    stderr=subprocess.DEVNULL
                ).decode("utf-8")
                for line in diff_output.splitlines():
                    filepath = line.strip().strip('"')
                    if filepath.endswith(".kt") and os.path.exists(filepath):
                        modified_files.append(filepath)
                        
            return list(set(modified_files))
        except Exception:
            return []

    if len(sys.argv) > 1:
        targets = [sys.argv[1]]
    else:
        targets = get_modified_kt_files()
        
    if not targets:
        print("ℹ️ No modified Kotlin files detected via git. Nothing to audit.")
        sys.exit(0)
        
    print(f"🔍 Files to audit: {targets}")
    for idx, target in enumerate(targets):
        start_mission(target)
        if idx < len(targets) - 1:
            print("\n🧘 Resting 35s between files to keep API free...")
            time.sleep(35)