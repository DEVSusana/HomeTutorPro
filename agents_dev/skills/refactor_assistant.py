import json
from google.genai import types
from config import get_client, MODEL_ID

def run_refactor_analysis(file_path):
    """Solicita a Gemini sugerencias de refactorización y parches quirúrgicos en formato JSON."""
    client = get_client()
    # Si es el archivo de prueba, usamos reglas compactas para evitar consumir cuota de tokens.
    if "_temp.kt" in file_path:
        rules = "1. Use camelCase for functions and variables. 2. Code must be in English. 3. Clean unused variables."
    else:
        with open('AGENTS.md', 'r') as f:
            rules = f.read()
    with open(file_path, 'r') as f:
        code = f.read()

    prompt = f"""
    Act as a Code Quality Expert for Android Kotlin development. 
    Analyze this code and suggest refactoring improvements based on the rules in AGENTS.md.
    
    You must propose surgical Search & Replace blocks to refactor the code.
    Only make changes that clean up the code, improve architecture or fix code smells.
    Do NOT change the logical behavior of the program.
    
    RULES:
    {rules}
    
    CODE:
    {code}
    
    You MUST return a JSON object with this exact schema:
    {{
      "replacements": [
        {{
          "search": "exact code block to find including all whitespace, indentations, and punctuation",
          "replace": "new code block to replace the search block with"
        }}
      ]
    }}
    
    If no refactorings are needed, return an empty array for "replacements".
    Ensure the "search" text matches EXACTLY what is in the file, line by line.
    """
    
    import config
    response = config.generate_content_with_retry(
        client=client,
        contents=prompt,
        config_args=types.GenerateContentConfig(response_mime_type="application/json"),
        max_retries=3,
        initial_delay=35
    )
    return response.text

def apply_patches(file_path, patch_json_str):
    """Aplica los parches de reemplazo descritos en el JSON al archivo destino."""
    try:
        data = json.loads(patch_json_str)
        replacements = data.get("replacements", [])
    except Exception as e:
        return False, f"Error decodificando el JSON del agente: {e}"

    if not replacements:
        return True, "No se propusieron refactorizaciones."

    with open(file_path, 'r') as f:
        code = f.read()

    modified_code = code
    for i, rep in enumerate(replacements):
        search_block = rep.get("search", "")
        replace_block = rep.get("replace", "")

        if not search_block:
            continue

        occurrences = code.count(search_block)
        if occurrences == 0:
            return False, f"Bloque {i+1} no encontrado en el archivo:\n--- SEARCH ---\n{search_block}\n--------------"
        elif occurrences > 1:
            return False, f"El bloque {i+1} es ambiguo, aparece {occurrences} veces en el archivo."

        modified_code = modified_code.replace(search_block, replace_block)

    with open(file_path, 'w') as f:
        f.write(modified_code)

    return True, f"Se aplicaron con éxito {len(replacements)} parches quirúrgicos."