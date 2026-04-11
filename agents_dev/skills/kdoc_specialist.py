from config import get_client, MODEL_ID

def run_kdoc_audit(file_path):
    client = get_client()
    with open('AGENTS.md', 'r') as f: rules = f.read()
    with open(file_path, 'r') as f: code = f.read()

    prompt = f"""
    Act as a Technical Writer. 
    Review the documentation style required in AGENTS.md and identify missing KDocs.
    
    RULES:
    {rules}
    
    CODE:
    {code}
    """
    response = client.models.generate_content(model=MODEL_ID, contents=prompt)
    return response.text