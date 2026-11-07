-- Migration 005: Crear tabla de retroalimentacion comunitaria
CREATE TABLE user_feedback (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    gloss_id INTEGER,
    rating INTEGER,
    comment TEXT,
    user_type TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (gloss_id) REFERENCES glosses(id)
);
