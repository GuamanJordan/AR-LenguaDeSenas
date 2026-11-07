import os
import subprocess
import json
import sys

def run_cmd(cmd):
    res = subprocess.run(cmd, shell=True, capture_output=True, text=True, encoding="utf-8")
    return res.stdout.strip(), res.stderr.strip(), res.returncode

def commit(msg):
    run_cmd("git add -A")
    out, err, code = run_cmd(f'git commit -m "{msg}" --allow-empty')
    return out

def write_f(path, content):
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content.strip() + "\n")

print("Generando proyecto y commits...")

