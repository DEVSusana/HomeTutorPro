"""
Hilt Dependency Injection Binding Checker.
Verifies whether a UseCase or Repository interface has its Hilt dependency injection
binding registered in app/src/main/java/com/devsusana/hometutorpro/di.
"""
import os
import sys

# Project root (2 levels above agents_dev/skills)
PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "../.."))


def _resolve_path(path: str) -> str:
    if os.path.isabs(path):
        return path
    return os.path.abspath(os.path.join(PROJECT_ROOT, path))


def check_hilt_bindings(file_path: str) -> str:
    """Verifies whether a UseCase or Repository interface is properly bound in Hilt DI modules."""
    full_path = _resolve_path(file_path)
    if not os.path.exists(full_path):
        return f"ERROR: File '{file_path}' does not exist."

    filename = os.path.basename(full_path)
    if not filename.endswith(".kt"):
        return f"File '{filename}' is not a Kotlin file."

    class_name = filename.replace(".kt", "")
    is_usecase = "UseCase" in class_name or (class_name.startswith("I") and "UseCase" in class_name)
    is_repository = "Repository" in class_name

    if not is_usecase and not is_repository:
        return f"Class '{class_name}' is neither a UseCase nor a Repository. No Hilt module binding required."

    di_dir = _resolve_path("app/src/main/java/com/devsusana/hometutorpro/di")
    if not os.path.exists(di_dir):
        return f"WARNING: Hilt DI directory '{di_dir}' not found."

    found = False
    matching_module = None
    checked_files = []

    for root, dirs, files in os.walk(di_dir):
        for file in files:
            if file.endswith(".kt"):
                di_file_path = os.path.join(root, file)
                checked_files.append(file)
                try:
                    with open(di_file_path, "r", encoding="utf-8") as f:
                        if class_name in f.read():
                            found = True
                            matching_module = file
                            break
                except Exception:
                    pass
        if found:
            break

    if found:
        return f"PASS: Hilt binding for '{class_name}' verified in module '{matching_module}'."
    else:
        return (
            f"FAIL: No Hilt binding found for '{class_name}' in '{di_dir}'. "
            f"Scanned modules: {', '.join(checked_files)}. "
            f"Action needed: Add @Binds or @Provides for this interface in the corresponding Hilt module."
        )


if __name__ == "__main__":
    if len(sys.argv) > 1:
        print(check_hilt_bindings(sys.argv[1]))
    else:
        # Check all modified Kotlin files using git status
        import subprocess
        try:
            status_output = subprocess.check_output(
                ["git", "status", "--porcelain"],
                cwd=PROJECT_ROOT,
                stderr=subprocess.DEVNULL
            ).decode("utf-8")

            modified_found = False
            for line in status_output.splitlines():
                if line.strip() and len(line) >= 4:
                    filepath = line[3:].strip().strip('"')
                    if filepath.endswith(".kt"):
                        result = check_hilt_bindings(filepath)
                        if "No Hilt module binding required" not in result:
                            print(result)
                            modified_found = True
            if not modified_found:
                print("ℹ️ No modified UseCase or Repository files to check for Hilt bindings.")
        except Exception as e:
            print(f"❌ Error inspecting git status: {e}")
