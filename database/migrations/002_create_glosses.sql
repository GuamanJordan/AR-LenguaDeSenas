-- Migration 002: Crear tabla de glosas LSEC
CREATE TABLE glosses (
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
    FOREIGN KEY (category_id) REFERENCES categories(id)
);
