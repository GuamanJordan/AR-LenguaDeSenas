-- Migration 003: Crear tabla de configuracion de gestos y animaciones
CREATE TABLE gestures (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    gloss_id INTEGER UNIQUE NOT NULL,
    animation_clip_name TEXT NOT NULL,
    duration_ms INTEGER NOT NULL,
    framerate INTEGER DEFAULT 30,
    keyframes_json TEXT,
    mesh_anchor_point TEXT DEFAULT 'chest',
    FOREIGN KEY (gloss_id) REFERENCES glosses(id)
);
