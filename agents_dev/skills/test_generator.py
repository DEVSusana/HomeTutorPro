from config import get_client, MODEL_ID

def run_test_design(file_path):
    client = get_client()
    # Leemos tus reglas sagradas
    with open('AGENTS.md', 'r') as f: rules = f.read()
    with open(file_path, 'r') as f: code = f.read()

    prompt = f"""
    Act as a Senior QA Engineer. 
    Based on the project rules in AGENTS.md, create a Unit Test plan.
    
    RULES:
    {rules}
    
    CODE TO TEST:
    {code}
    """
    response = client.models.generate_content(model=MODEL_ID, contents=prompt)
    return response.text