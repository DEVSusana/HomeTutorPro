import os
import sys

def check_hilt_bindings(file_path):
    if not os.path.exists(file_path):
        return
        
    filename = os.path.basename(file_path)
    if not filename.endswith(".kt"):
        return

    class_name = filename.replace(".kt", "")
    
    # Check if it is a Use Case or a Repository
    is_usecase = "UseCase" in class_name or (class_name.startswith("I") and "UseCase" in class_name)
    is_repository = "Repository" in class_name
    
    if not is_usecase and not is_repository:
        return

    print(f"🔍 Checking Hilt Dependency Injection status for: {class_name}")

    di_dir = "app/src/main/java/com/devsusana/hometutorpro/di"
    if not os.path.exists(di_dir):
        print("⚠️ Hilt DI directory not found.")
        return

    found = False
    checked_files = []
    
    # Iterate over all files in the DI folder to see if the interface is mentioned
    for root, dirs, files in os.walk(di_dir):
        for file in files:
            if file.endswith(".kt"):
                di_file_path = os.path.join(root, file)
                checked_files.append(file)
                with open(di_file_path, 'r') as f:
                    content = f.read()
                    if class_name in content:
                        print(f"✅ Found binding/definition for {class_name} in DI module: {file}")
                        found = True
                        break
        if found:
            break

    if not found:
        print(f"\n⚠️ WARNING: No Hilt binding found for '{class_name}' in any module inside '{di_dir}'.")
        print("   Please check that you have added the binding (e.g. @Binds or @Provides) so that Hilt can inject it.")
        print(f"   Modules scanned: {', '.join(checked_files)}")

if __name__ == "__main__":
    if len(sys.argv) > 1:
        check_hilt_bindings(sys.argv[1])
    else:
        # Check all modified files using git status
        import subprocess
        try:
            status_output = subprocess.check_output(
                ["git", "status", "--porcelain"], 
                stderr=subprocess.DEVNULL
            ).decode("utf-8")
            
            modified_found = False
            for line in status_output.splitlines():
                if line.strip() and len(line) >= 4:
                    filepath = line[3:].strip().strip('"')
                    if filepath.endswith(".kt"):
                        check_hilt_bindings(filepath)
                        modified_found = True
            if not modified_found:
                print("ℹ️ No modified files to check for Hilt bindings.")
        except Exception as e:
            print(f"❌ Error getting git status: {e}")
