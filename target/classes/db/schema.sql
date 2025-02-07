CREATE TABLE IF NOT EXISTS backup_metadata (
    table_name TEXT PRIMARY KEY,
    backup_timestamp DATETIME
);