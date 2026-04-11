from config import get_client, MODEL_ID

def run_architecture_audit(file_path):
    client = get_client()

    with open('AGENTS.md', 'r') as f:
        rules = f.read()
    with open(file_path, 'r') as f:
        code = f.read()

    prompt = f"Review this Kotlin code based on these rules: {rules}\n\nCODE:\n{code}"

    # Nueva forma de llamar al modelo
    response = client.models.generate_content(
        model=MODEL_ID,
        contents=prompt
    )
    return response.text