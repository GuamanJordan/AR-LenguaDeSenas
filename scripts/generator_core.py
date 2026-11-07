import os
import sys
import subprocess
import json

def run_cmd(cmd):
    res = subprocess.run(cmd, shell=True, capture_output=True, text=True, encoding="utf-8")
    return res.stdout.strip(), res.stderr.strip(), res.returncode

def git_commit(msg):
    run_cmd("git add -A")
    out, err, code = run_cmd(f'git commit -m "{msg}" --allow-empty')
    if code != 0 and "nothing to commit" not in out:
        print(f"Commit warn/error: {err} | {out}")

def save(path, content):
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content.strip() + "\n")

print("Preparando generación masiva y estructurada de commits...")
