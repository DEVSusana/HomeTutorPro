import os
import time
import subprocess
import architecture_auditor
import security_scanner
import test_generator
import kdoc_specialist
import refactor_assistant

def run_gradle_task(command_args):
    """Ejecuta una tarea de Gradle en el directorio raíz del proyecto."""
    root_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "../.."))
    # En macOS/Linux el script ejecutable es ./gradlew
    gradle_bin = "./gradlew" if os.name != "nt" else "gradlew.bat"
    full_command = [os.path.join(root_dir, gradle_bin)] + command_args
    
    print(f"⏳ Running Gradle command: {' '.join(full_command)} ...")
    try:
        result = subprocess.run(
            full_command,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            cwd=root_dir
        )
        success = result.returncode == 0
        output = result.stdout + "\n" + result.stderr
        return success, output
    except Exception as e:
        return False, str(e)


def heal_code(file_path, error_message):
    """Llama a Gemini para corregir quirúrgicamente un error de compilación/test en el archivo."""
    from config import get_client, MODEL_ID, generate_content_with_retry
    from google.genai import types
    
    client = get_client()
    with open(file_path, 'r') as f:
        code = f.read()

    # Si es el archivo de prueba, usamos reglas compactas de compilación
    if "_temp.kt" in file_path:
        rules_context = "Correct syntax errors or compiler issues. Match existing braces."
    else:
        with open('AGENTS.md', 'r') as f:
            rules_context = f.read()

    prompt = f"""
    Act as an Android Kotlin Expert. The following file has a compilation or test failure.
    You must propose surgical Search & Replace blocks to resolve the error.
    
    ERROR LOG:
    {error_message}
    
    RULES CONTEXT:
    {rules_context}
    
    CURRENT CODE:
    {code}
    
    You MUST return a JSON object with this exact schema:
    {{
      "replacements": [
        {{
          "search": "exact code block to find including all whitespace, indentation, and punctuation",
          "replace": "new code block to replace the search block with"
        }}
      ]
    }}
    
    Ensure the "search" text matches EXACTLY what is in the file.
    """
    try:
        response = generate_content_with_retry(
            client=client,
            contents=prompt,
            config_args=types.GenerateContentConfig(response_mime_type="application/json"),
            max_retries=1,
            initial_delay=1
        )
        success, message = refactor_assistant.apply_patches(file_path, response.text)
        return success, message
    except Exception as e:
        return False, str(e)


def start_mission(target_file):
    if not os.path.exists(target_file):
        print(f"❌ Error: File {target_file} not found.")
        return False

    print(f"\n🚀 MISSION START: Autonomous Refactoring & Verification of {os.path.basename(target_file)}")
    print("="*80)

    # 1. Ejecutar el Agente de Refactorización para aplicar mejoras iniciales
    print("\n[STEP 1] Running Refactor Agent to apply patches...")
    try:
        patch_json = refactor_assistant.run_refactor_analysis(target_file)
        success, message = refactor_assistant.apply_patches(target_file, patch_json)
        print(f"Resultado Refactor: {'Éxito' if success else 'Fallo'}")
        print(f"Detalle: {message}")
        if not success:
            print("⚠️ Procediendo con la compilación a pesar del fallo del parche inicial.")
    except Exception as e:
        print(f"⚠️ Error en Refactor Agent: {e}")

    # 2. Bucle de Compilación y Autocorrección (Self-Healing Loop)
    print("\n[STEP 2] Running Compilation & Self-Healing Loop...")
    max_attempts = 3
    compiled_successfully = False
    
    for attempt in range(max_attempts):
        success, output = run_gradle_task(["compileDebugKotlin"])
        if success:
            print(f"✅ Compilación exitosa en el intento {attempt + 1}.")
            compiled_successfully = True
            break
            
        print(f"❌ Falló la compilación (Intento {attempt + 1}/{max_attempts}). Iniciando autocorrección...")
        # Capturamos el log de error y llamamos al corrector
        heal_success, heal_msg = heal_code(target_file, output)
        if heal_success:
            print(f"🔧 Parche de corrección aplicado con éxito. Re-comprobando compilación...")
        else:
            print(f"⚠️ Falló la aplicación del parche de corrección: {heal_msg}")
            
    if not compiled_successfully:
        print("❌ No se pudo auto-corregir el error de compilación. Abortando misión.")
        return False

    # 3. Ejecución de Tests Unitarios
    print("\n[STEP 3] Running Unit Tests Verification...")
    success_tests, test_output = run_gradle_task(["test"])
    if success_tests:
        print("✅ ¡Todas las pruebas unitarias pasaron con éxito!")
    else:
        print("❌ Las pruebas unitarias fallaron. Iniciando autocorrección de lógica...")
        heal_success, heal_msg = heal_code(target_file, test_output)
        if heal_success:
            print("🔧 Parche de corrección de lógica aplicado. Re-comprobando pruebas...")
            success_tests_2, test_output_2 = run_gradle_task(["test"])
            if success_tests_2:
                print("✅ ¡Pruebas unitarias pasaron con éxito en la segunda ronda!")
            else:
                print("❌ Pruebas unitarias fallaron en la segunda ronda. Abortando.")
                return False
        else:
            print(f"⚠️ Falló la aplicación del parche de corrección de tests: {heal_msg}")
            return False

    print("\n" + "="*80)
    print("✅ MISSION COMPLETE: Code is refactored, compiled, and tested successfully!")
    
    # 4. Human-in-the-Loop (Aprobación humana final)
    print("\n🔍 CAMBIOS PROPUESTOS (Git Diff):")
    print("="*60)
    # Ejecutamos git diff sobre el archivo destino
    root_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "../.."))
    subprocess.run(["git", "diff", target_file], cwd=root_dir)
    print("="*60)
    
    try:
        response = input("\n🤔 ¿Desea APROBAR y CONSERVAR estos cambios en el archivo? (y/n): ").strip().lower()
        if response == 'y':
            print("✅ Cambios aprobados y conservados.")
            return True
        else:
            print("❌ Cambios rechazados. Revirtiendo archivo...")
            subprocess.run(["git", "checkout", "--", target_file], cwd=root_dir)
            return False
    except Exception as e:
        print(f"⚠️ Error al capturar la confirmación: {e}. Revirtiendo por seguridad...")
        subprocess.run(["git", "checkout", "--", target_file], cwd=root_dir)
        return False


if __name__ == "__main__":
    import sys
    
    def get_modified_kt_files():
        """Obtiene la lista de archivos Kotlin modificados usando git status/diff."""
        try:
            status_output = subprocess.check_output(
                ["git", "status", "--porcelain"], 
                stderr=subprocess.DEVNULL
            ).decode("utf-8")
            
            modified_files = []
            for line in status_output.splitlines():
                if line.strip() and len(line) >= 4:
                    filepath = line[3:].strip().strip('"')
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

    # Determinamos el archivo destino a procesar
    if len(sys.argv) > 1:
        targets = [sys.argv[1]]
    else:
        targets = get_modified_kt_files()
        
    if not targets:
        print("ℹ️ No modified Kotlin files detected via git. Nothing to refactor.")
        sys.exit(0)
        
    print(f"🔍 Files to refactor & verify: {targets}")
    for idx, target in enumerate(targets):
        success = start_mission(target)
        if not success:
            print(f"❌ Misión abortada o fallida en: {target}")
            
        if idx < len(targets) - 1:
            print("\n🧘 Resting 35s between files to keep API free...")
            time.sleep(35)