-- Migration 004: Crear tabla de historial de traducciones
CREATE TABLE history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    direction TEXT NOT NULL,
    input_content TEXT NOT NULL,
    output_content TEXT NOT NULL,
    confidence_score REAL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_favorite INTEGER DEFAULT 0
);
