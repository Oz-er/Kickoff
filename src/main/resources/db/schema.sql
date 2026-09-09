CREATE TABLE IF NOT EXISTS teams (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL COLLATE NOCASE UNIQUE CHECK (length(trim(name)) > 0),
    short_code TEXT NOT NULL COLLATE NOCASE UNIQUE CHECK (length(short_code) BETWEEN 2 AND 5),
    coach_name TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS players (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    team_id INTEGER NOT NULL,
    full_name TEXT NOT NULL CHECK (length(trim(full_name)) > 0),
    shirt_number INTEGER NOT NULL CHECK (shirt_number BETWEEN 1 AND 99),
    position TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE RESTRICT,
    UNIQUE (team_id, shirt_number)
);

CREATE TABLE IF NOT EXISTS tournaments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL COLLATE NOCASE UNIQUE CHECK (length(trim(name)) > 0),
    format TEXT NOT NULL CHECK (format IN ('ROUND_ROBIN', 'KNOCKOUT')),
    status TEXT NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'REGISTRATION_CLOSED', 'FIXTURES_GENERATED', 'COMPLETED')),
    start_date TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tournament_teams (
    tournament_id INTEGER NOT NULL,
    team_id INTEGER NOT NULL,
    seed_number INTEGER NOT NULL CHECK (seed_number > 0),
    PRIMARY KEY (tournament_id, team_id),
    FOREIGN KEY (tournament_id) REFERENCES tournaments(id) ON DELETE CASCADE,
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE RESTRICT,
    UNIQUE (tournament_id, seed_number)
);

CREATE TABLE IF NOT EXISTS matches (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tournament_id INTEGER NOT NULL,
    round_number INTEGER NOT NULL CHECK (round_number > 0),
    home_team_id INTEGER,
    away_team_id INTEGER,
    home_score INTEGER CHECK (home_score IS NULL OR home_score >= 0),
    away_score INTEGER CHECK (away_score IS NULL OR away_score >= 0),
    status TEXT NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('PENDING', 'SCHEDULED', 'COMPLETED')),
    scheduled_at TEXT,
    next_match_id INTEGER,
    next_match_slot TEXT CHECK (next_match_slot IN ('HOME', 'AWAY')),
    FOREIGN KEY (tournament_id) REFERENCES tournaments(id) ON DELETE CASCADE,
    FOREIGN KEY (home_team_id) REFERENCES teams(id) ON DELETE RESTRICT,
    FOREIGN KEY (away_team_id) REFERENCES teams(id) ON DELETE RESTRICT,
    FOREIGN KEY (next_match_id) REFERENCES matches(id) ON DELETE SET NULL,
    CHECK (home_team_id IS NULL OR away_team_id IS NULL OR home_team_id <> away_team_id),
    CHECK ((next_match_id IS NULL AND next_match_slot IS NULL) OR (next_match_id IS NOT NULL AND next_match_slot IS NOT NULL)),
    CHECK (
        (status = 'PENDING' AND home_team_id IS NULL AND away_team_id IS NULL AND home_score IS NULL AND away_score IS NULL)
        OR (status = 'SCHEDULED' AND home_team_id IS NOT NULL AND away_team_id IS NOT NULL AND home_score IS NULL AND away_score IS NULL)
        OR (status = 'COMPLETED' AND home_team_id IS NOT NULL AND away_team_id IS NOT NULL AND home_score IS NOT NULL AND away_score IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_players_team ON players(team_id);
CREATE INDEX IF NOT EXISTS idx_matches_tournament ON matches(tournament_id, round_number);
