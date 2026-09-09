INSERT OR IGNORE INTO teams(name, short_code, coach_name) VALUES
    ('River City FC', 'RCF', 'Samir Khan'),
    ('Greenfield United', 'GFU', 'Nadia Rahman'),
    ('Harbor Athletic', 'HAT', 'Imran Ali'),
    ('Metro Stars', 'MST', 'Farah Ahmed');

INSERT OR IGNORE INTO players(team_id, full_name, shirt_number, position)
SELECT id, 'Arif Hossain', 10, 'Forward' FROM teams WHERE short_code = 'RCF';

INSERT OR IGNORE INTO players(team_id, full_name, shirt_number, position)
SELECT id, 'Tanvir Ahmed', 1, 'Goalkeeper' FROM teams WHERE short_code = 'GFU';

INSERT OR IGNORE INTO players(team_id, full_name, shirt_number, position)
SELECT id, 'Nabil Rahman', 8, 'Midfielder' FROM teams WHERE short_code = 'HAT';

INSERT OR IGNORE INTO players(team_id, full_name, shirt_number, position)
SELECT id, 'Fahim Islam', 4, 'Defender' FROM teams WHERE short_code = 'MST';
