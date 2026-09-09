# Database ER Diagram

This Mermaid diagram mirrors `src/main/resources/db/schema.sql`. SQLite uses `INTEGER` and `TEXT`; nullable columns are marked in their descriptions.

```mermaid
erDiagram
    TEAMS {
        INTEGER id PK "AUTOINCREMENT"
        TEXT name UK "NOT NULL, COLLATE NOCASE"
        TEXT short_code UK "NOT NULL, COLLATE NOCASE"
        TEXT coach_name "nullable"
        TEXT created_at "NOT NULL, default current timestamp"
    }

    PLAYERS {
        INTEGER id PK "AUTOINCREMENT"
        INTEGER team_id FK "NOT NULL"
        TEXT full_name "NOT NULL, COLLATE NOCASE"
        INTEGER shirt_number "NOT NULL, 1 through 99"
        TEXT position "nullable"
        TEXT created_at "NOT NULL, default current timestamp"
    }

    TOURNAMENTS {
        INTEGER id PK "AUTOINCREMENT"
        TEXT name UK "NOT NULL, COLLATE NOCASE"
        TEXT format "NOT NULL"
        TEXT status "NOT NULL, default DRAFT"
        TEXT start_date "NOT NULL, ISO date"
        TEXT created_at "NOT NULL, default current timestamp"
    }

    TOURNAMENT_TEAMS {
        INTEGER tournament_id PK, FK "composite primary key"
        INTEGER team_id PK, FK "composite primary key"
        INTEGER seed_number UK "NOT NULL within tournament"
    }

    MATCHES {
        INTEGER id PK "AUTOINCREMENT"
        INTEGER tournament_id FK "NOT NULL"
        INTEGER round_number "NOT NULL, positive"
        INTEGER home_team_id FK "nullable"
        INTEGER away_team_id FK "nullable"
        INTEGER home_score "nullable, non-negative"
        INTEGER away_score "nullable, non-negative"
        TEXT status "NOT NULL, default SCHEDULED"
        TEXT scheduled_at "nullable"
        INTEGER next_match_id FK "nullable, self-reference"
        TEXT next_match_slot "nullable"
    }

    TEAMS ||--o{ PLAYERS : has
    TOURNAMENTS ||--o{ TOURNAMENT_TEAMS : contains
    TEAMS ||--o{ TOURNAMENT_TEAMS : registers
    TOURNAMENTS ||--o{ MATCHES : schedules
    TEAMS o|--o{ MATCHES : home_team
    TEAMS o|--o{ MATCHES : away_team
    MATCHES o|--o{ MATCHES : feeds_winner_to
```

## Relationship and constraint notes

- `players.team_id` references `teams.id` with `ON DELETE RESTRICT`.
- `tournament_teams.tournament_id` references `tournaments.id` with `ON DELETE CASCADE`.
- `tournament_teams.team_id` references `teams.id` with `ON DELETE RESTRICT`.
- `matches.tournament_id` references `tournaments.id` with `ON DELETE CASCADE`.
- `matches.home_team_id` and `matches.away_team_id` reference `teams.id` with `ON DELETE RESTRICT`.
- `matches.next_match_id` references `matches.id` with `ON DELETE SET NULL`. At most two feeder matches are created for one downstream knockout match by the scheduling algorithm.
- `(team_id, shirt_number)` is unique in `players`.
- `(tournament_id, seed_number)` is unique in `tournament_teams`; `(tournament_id, team_id)` is its composite primary key.
- A match cannot contain the same known team in both slots.
- `next_match_id` and `next_match_slot` must both be null or both be present.
- Pending matches have an unknown participant and no scores. Scheduled matches have both participants and no scores. Completed matches have both participants and both scores.

The schema also indexes player-team lookup, team-registration lookup, and tournament/round match lookup.
