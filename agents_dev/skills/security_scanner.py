import json
import config

def run_security_scan(file_path: str, output_format: str = "text") -> str:
    """
    Executes an in-depth Android security scan on a Kotlin file.
    Audits for OWASP Mobile Top 10 vulnerabilities, cryptographic weaknesses,
    insecure data persistence, concurrency risks, PII logging, and English naming compliance.
    """
    client = config.get_client()

    with open(file_path, "r", encoding="utf-8") as f:
        code = f.read()

    security_guidelines = """
    Exhaustive Android Security Audit Checklist (OWASP Mobile & Android Best Practices):
    
    1. Secrets & Sensitive Data in Code (OWASP M1):
       - Hardcoded API keys, JWT tokens, Firebase credentials, bearer tokens, passwords.
       - Hardcoded symmetric keys (AES, DES), static initialization vectors (IVs), or static salts.
       
    2. Cryptographic Weaknesses & Insecure Storage (OWASP M2 / M4):
       - Deprecated or broken algorithms: MD5, SHA-1 for passwords/signatures, DES, 3DES, RC4.
       - Insecure cipher modes: AES/ECB mode (MUST use AES/GCM/NoPadding or AES/CBC with random IV).
       - Weak key derivation: PBKDF2 with < 100,000 iterations, static salts, or missing AndroidKeyStore integration.
       - Plaintext persistence of sensitive data (using standard SharedPreferences instead of EncryptedSharedPreferences or DataStore).
       - Path Traversal vulnerabilities in file I/O or backup extraction (unsanitized '../' paths).
       
    3. Authentication, Authorization & Session Management (OWASP M3):
       - Client-side authorization bypasses or missing checks before privileged repository actions.
       - Insecure BiometricPrompt usage (omitting CryptoObject validation).
       - Retaining cleartext passwords as immutable Strings in memory rather than CharArray wiped after use.
       
    4. Network, IPC & Database Security (OWASP M5):
       - Insecure HTTP cleartext endpoints (must use HTTPS).
       - TrustManager or SSL certificate verification bypasses.
       - SQL Injection risks in Room @RawQuery or raw SQLite queries concatenated with dynamic strings instead of query parameters (?).
       - Exported Android components (Activities, Receivers, Services) without required permissions.
       
    5. Concurrency & Coroutine Safety (OWASP M6):
       - Unscoped or detached coroutines (GlobalScope.launch).
       - Race conditions or non-thread-safe mutation of shared state across coroutine contexts without Mutex or Dispatchers.
       - Unhandled exceptions in coroutines leading to silent failure or crashes.
       
    6. Information Disclosure & Logging (OWASP M7):
       - PII, student sensitive data, door codes, passwords, or tokens printed via Log.d, Log.i, Log.e, or println.
       
    7. Code Hygiene & Language Compliance:
       - Strict English naming for all classes, functions, variables, and comments as mandated by AGENTS.md.
    """

    if output_format == "json":
        prompt = f"""
        Act as a Principal Android Security Engineer and Penetration Tester.
        Thoroughly audit the following Kotlin file against these security guidelines:
        
        {security_guidelines}
        
        CODE TO SCAN ({file_path}):
        {code}
        
        Return a strict JSON object matching this schema:
        {{
          "summary": {{
            "file": "{file_path}",
            "critical_count": <int>,
            "high_count": <int>,
            "medium_count": <int>,
            "low_count": <int>,
            "info_count": <int>
          }},
          "findings": [
            {{
              "severity": "critical" | "high" | "medium" | "low" | "info",
              "category": "<OWASP category or Security domain>",
              "file": "{file_path}",
              "line": <line_number_or_0>,
              "title": "<Concise issue title>",
              "message": "<Detailed technical vulnerability description>",
              "suggestion": "<Concrete Kotlin code fix or recommendation>",
              "remediation": "<Concrete Kotlin code fix or recommendation>"
            }}
          ]
        }}
        If the code is completely clean and follows all security standards, return findings as an empty list [].
        """
        from google.genai import types
        response = config.generate_content_with_retry(
            client=client,
            contents=prompt,
            config_args=types.GenerateContentConfig(response_mime_type="application/json")
        )
        return response.text
    else:
        prompt = f"""
        Act as a Principal Android Security Engineer and Penetration Tester.
        Thoroughly audit the following Kotlin file against these security guidelines:
        
        {security_guidelines}
        
        CODE TO SCAN ({file_path}):
        {code}
        
        Provide an executive and technical security report with:
        1. Executive Security Status (CLEAN / WARNINGS / VULNERABLE).
        2. Detailed Findings (ordered by severity: CRITICAL, HIGH, MEDIUM, LOW, INFO) with line numbers, OWASP classification, vulnerability explanation, and exact Kotlin remediation code.
        3. Compliance with AGENTS.md (English language check).
        
        If the file has zero vulnerabilities, clearly state that it is CERTIFIED SECURE.
        """
        response = config.generate_content_with_retry(
            client=client,
            contents=prompt
        )
        return response.text
