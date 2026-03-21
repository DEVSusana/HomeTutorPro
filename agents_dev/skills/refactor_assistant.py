from config import get_client, MODEL_ID

def run_refactor_analysis(file_path):
    client = get_client()
    with open('AGENTS.md', 'r') as f: rules = f.read()
    with open(file_path, 'r') as f: code = f.read()

    prompt = f"""
    Act as a Code Quality Expert. 
    Suggest refactorings that align with the Clean Architecture and patterns defined in AGENTS.md.
    
    RULES:
    {rules}
    
    CODE:
    {code}
    """
    response = client.models.generate_content(model=MODEL_ID, contents=prompt)
    return response.text