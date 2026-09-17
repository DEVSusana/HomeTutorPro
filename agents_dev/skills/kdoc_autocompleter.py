import os
import sys
import config

def autocomplete_kdocs(file_path):
    if not os.path.exists(file_path):
        print(f"❌ File {file_path} not found.")
        return

    print(f"✍️ Autocompleting KDocs for: {os.path.basename(file_path)}")
    
    with open(file_path, 'r') as f:
        code = f.read()

    prompt = f"""
    Act as a Senior Android Technical Writer and Developer.
    Add professional, concise KDoc comments to all undocumented public classes, interfaces, and public functions in the following Kotlin code.
    Follow these guidelines:
    - Add @param, @return, or @throws tags where necessary.
    - Avoid redundant comments that just repeat function names (e.g., do not write 'Get user' for a function named 'getUser').
    - Preserve all original code, logic, package, and imports exactly as they are.
    - Do not modify any code behavior.
    
    ORIGINAL CODE:
    {code}
    
    Return ONLY the complete modified Kotlin code containing the added KDoc comments. No explanation, no markdown backticks (```).
    """

    client = config.get_client()
    try:
        response = config.generate_content_with_retry(client, prompt)
        documented_code = response.text.strip()
        
        # Remove markdown code block wrappers
        if documented_code.startswith("```kotlin"):
            documented_code = documented_code[9:]
        elif documented_code.startswith("```"):
            documented_code = documented_code[3:]
        if documented_code.endswith("```"):
            documented_code = documented_code[:-3]
        documented_code = documented_code.strip()

        with open(file_path, 'w') as f:
            f.write(documented_code)
            
        print(f"✅ KDocs successfully generated and added to: {file_path}")
    except Exception as e:
        print(f"❌ Error autocompleting KDocs: {e}")

if __name__ == "__main__":
    if len(sys.argv) > 1:
        autocomplete_kdocs(sys.argv[1])
    else:
        print("Usage: python kdoc_autocompleter.py <path_to_kotlin_file.kt>")
