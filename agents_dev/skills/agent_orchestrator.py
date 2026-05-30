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
    if len(sys.argv) > 1:
        target = sys.argv[1]
    else:
        target = "app/src/main/java/com/devsusana/hometutorpro/data/repository/StudentRepositoryImpl.kt"
    start_mission(target)