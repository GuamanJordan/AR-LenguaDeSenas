-- Esquema Relacional de Base de Datos LSEC para SQLite / Room
PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    icon_name TEXT,
    display_order INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS glosses (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    category_id INTEGER NOT NULL,
    code TEXT UNIQUE NOT NULL,
    spanish_term TEXT NOT NULL,
    lsec_gloss TEXT NOT NULL,
    description TEXT,
    hand_shape TEXT,
    location TEXT,
    movement TEXT,
    orientation TEXT,
    facial_expression TEXT,
    is_two_handed INTEGER DEFAULT 0,
    is_dynamic INTEGER DEFAULT 0,
    difficulty_level INTEGER DEFAULT 1,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS gestures (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    gloss_id INTEGER UNIQUE NOT NULL,
    animation_clip_name TEXT NOT NULL,
    duration_ms INTEGER NOT NULL,
    framerate INTEGER DEFAULT 30,
    keyframes_json TEXT,
    mesh_anchor_point TEXT DEFAULT 'chest',
    FOREIGN KEY (gloss_id) REFERENCES glosses(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    direction TEXT NOT NULL CHECK(direction IN ('SIGN_TO_TEXT', 'TEXT_TO_SIGN')),
    input_content TEXT NOT NULL,
    output_content TEXT NOT NULL,
    confidence_score REAL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_favorite INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS user_feedback (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    gloss_id INTEGER,
    rating INTEGER CHECK(rating BETWEEN 1 AND 5),
    comment TEXT,
    user_type TEXT CHECK(user_type IN ('DEAF_USER', 'HEARING_USER', 'INTERPRETER')),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (gloss_id) REFERENCES glosses(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_glosses_spanish ON glosses(spanish_term);
CREATE INDEX IF NOT EXISTS idx_glosses_code ON glosses(code);
CREATE INDEX IF NOT EXISTS idx_history_timestamp ON history(timestamp DESC);
