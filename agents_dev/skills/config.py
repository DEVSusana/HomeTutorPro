from google import genai

API_KEY = ""
MODEL_ID = "gemini-2.5-flash" # El modelo más actual y rápido

def get_client():
    client = genai.Client(api_key=API_KEY)
    return client
