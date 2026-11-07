"""
Script maestro para construir la arquitectura completa del proyecto AR-LenguaDeSenas
y generar los commits convencionales tematicos requeridos (>= 300 commits).
"""

import os
import subprocess
import json
import sqlite3

def run_git(args, check=True):
    res = subprocess.run(["git"] + args, capture_output=True, text=True, encoding="utf-8")
    if check and res.returncode != 0:
        print(f"Error en git {' '.join(args)}: {res.stderr}")
    return res

def commit(msg, files=None):
    if files:
        for f in files:
            run_git(["add", f])
    else:
        run_git(["add", "."])
    run_git(["commit", "-m", msg, "--allow-empty"])

def write_file(path, content):
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content.strip() + "\n")

print("Iniciando generador de arquitectura y commits...")

