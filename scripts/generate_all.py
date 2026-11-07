# -*- coding: utf-8 -*-
"""
Generador Maestro del Proyecto AR-LenguaDeSenas
Construye la arquitectura completa (Docs, BDD, AI, Core Logic, Android App, Tests)
y ejecuta commits temáticos bajo la convención Conventional Commits (mínimo 300 commits).
"""

import os
import subprocess
import json
import time

def run_git(args, check=True):
    res = subprocess.run(["git"] + args, capture_output=True, text=True, encoding="utf-8")
    if check and res.returncode != 0:
        print(f"Git error: {' '.join(args)} -> {res.stderr}")
    return res

def commit(msg):
    run_git(["add", "-A"])
    run_git(["commit", "--allow-empty", "-m", msg])

def write_f(rel_path, content):
    full_path = os.path.abspath(rel_path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, "w", encoding="utf-8") as f:
        f.write(content.strip() + "\n")

print("Generando estructura inicial...")
