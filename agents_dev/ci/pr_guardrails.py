"""
Fase 2: Guardrails
Evalúa los hallazgos del agente y bloquea el PR si hay violaciones críticas.
"""
import json
import sys
import os

def evaluate_guardrails():
    if not os.path.exists("agent_findings.json"):
        print("No se encontraron hallazgos del agente (agent_findings.json no existe).")
        sys.exit(0)
        
    with open("agent_findings.json", "r") as f:
        findings = json.load(f)
        
    if not findings:
        print("✅ Guardrails pass: No hay hallazgos.")
        sys.exit(0)

    critical_violations = []
    
    for finding in findings:
        severity = finding.get("severity", "info").lower()
        category = finding.get("category", "").lower()
        
        # Reglas de bloqueo: Críticas en Seguridad o Arquitectura
        if severity == "critical" and category in ["security", "architecture"]:
            critical_violations.append(finding)
            
    if critical_violations:
        print("\n❌ GUARDRAIL TRIGGERED: Se detectaron violaciones críticas.\n")
        for v in critical_violations:
            file = v.get("file", "Unknown")
            cat = v.get("category", "").upper()
            msg = v.get("message", "")
            print(f"[{cat}] {file}: {msg}")
            
        print("\nEl PR no puede mergearse hasta que se resuelvan estos problemas.")
        sys.exit(1)
    else:
        print("✅ Guardrails pass: No se detectaron violaciones críticas bloqueantes.")
        sys.exit(0)

if __name__ == "__main__":
    evaluate_guardrails()
