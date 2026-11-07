# -*- coding: utf-8 -*-
import sqlite3
import os
import json

class LsecDatabaseManager:
    """Gestor de base de datos local SQLite para el corpus LSEC"""
    
    def __init__(self, db_path="database/lsec_database.sqlite"):
        self.db_path = db_path
        os.makedirs(os.path.dirname(os.path.abspath(db_path)), exist_ok=True)
        self.init_database()

    def get_connection(self):
        conn = sqlite3.connect(self.db_path)
        conn.row_factory = sqlite3.Row
        return conn

    def init_database(self):
        conn = self.get_connection()
        cursor = conn.cursor()
        schema_path = os.path.join(os.path.dirname(__file__), "schema.sql")
        if os.path.exists(schema_path):
            with open(schema_path, "r", encoding="utf-8") as f:
                cursor.executescript(f.read())
        
        seed_path = os.path.join(os.path.dirname(__file__), "seed_lsec_corpus.sql")
        if os.path.exists(seed_path):
            with open(seed_path, "r", encoding="utf-8") as f:
                cursor.executescript(f.read())
        conn.commit()
        conn.close()

    def get_all_categories(self):
        with self.get_connection() as conn:
            return [dict(row) for row in conn.execute("SELECT * FROM categories ORDER BY id ASC").fetchall()]

    def search_gloss_by_text(self, term):
        query = "%" + term.strip().lower() + "%"
        with self.get_connection() as conn:
            cursor = conn.execute(
                "SELECT g.*, gt.animation_clip_name, gt.duration_ms FROM glosses g "
                "LEFT JOIN gestures gt ON g.id = gt.gloss_id "
                "WHERE LOWER(g.spanish_term) LIKE ? OR LOWER(g.lsec_gloss) LIKE ?",
                (query, query)
            )
            return [dict(row) for row in cursor.fetchall()]

    def get_gloss_by_code(self, code):
        with self.get_connection() as conn:
            cursor = conn.execute("SELECT * FROM glosses WHERE code = ?", (code,))
            row = cursor.fetchone()
            return dict(row) if row else None

    def record_history(self, direction, input_text, output_text, confidence=1.0):
        with self.get_connection() as conn:
            conn.execute(
                "INSERT INTO history (direction, input_content, output_content, confidence_score) "
                "VALUES (?, ?, ?, ?)",
                (direction, input_text, output_text, confidence)
            )
            conn.commit()

if __name__ == "__main__":
    db = LsecDatabaseManager()
    cats = db.get_all_categories()
    print(f"Base de datos LSEC inicializada exitosamente con {len(cats)} categorías.")
