"""
Configuración unificada para el entorno CI (GitHub Actions).
Lee la API key de los Secrets de GitHub a través de variables de entorno.
"""
import os
from google import genai
from google.genai import types

# Usaremos un modelo más potente en CI dado que tenemos acceso gratuito
MODEL_ID = "gemini-3.1-flash-lite" 

def get_client():
    """Crea y devuelve un cliente de Gemini configurado."""
    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        raise ValueError(
            "GEMINI_API_KEY no está configurado. "
            "Asegúrate de haber añadido el secret en GitHub."
        )
    return genai.Client(api_key=api_key)

def get_json_config():
    """Devuelve la configuración para forzar una respuesta en formato JSON."""
    return types.GenerateContentConfig(
        response_mime_type="application/json",
    )
