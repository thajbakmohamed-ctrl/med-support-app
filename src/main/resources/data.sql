-- Sample seed data for Bahrain Defence Force Military Hospital
INSERT INTO hospital
(name, location, phone, description, is_active, created_at, updated_at)
VALUES
    (
        'Bahrain Defence Force Military Hospital',
        'Riffa, Bahrain',
        '17000001',
        'Military hospital operated by Bahrain Defence Force Royal Medical Services',
        true,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
    ON CONFLICT DO NOTHING;


-- Sample seed data for Salmaniya Medical Complex
INSERT INTO hospital
(name, location, phone, description, is_active, created_at, updated_at)
VALUES
    (
        'Salmaniya Medical Complex',
        'Manama, Bahrain',
        '17000002',
        'Sample hospital data for demonstrating the Med Support application',
        true,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
    ON CONFLICT DO NOTHING;