from config import get_client, MODEL_ID

def run_security_scan(file_path):
    client = get_client()

    with open(file_path, 'r') as f:
        code = f.read()

    prompt = f"""
    Act as an Android Security Expert.
    Scan this code for:
    1. Hardcoded strings/keys.
    2. Unsafe Coroutine usage.
    3. Proper English naming (as per GEMINI.md requirements).
    
    CODE:
    {code}
    """

    # Nueva forma de llamar al cliente moderno
    response = client.models.generate_content(
        model=MODEL_ID,
        contents=prompt
    )
    return response.text