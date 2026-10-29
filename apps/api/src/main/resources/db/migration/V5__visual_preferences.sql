CREATE TABLE visual_preferences (
    scope TEXT NOT NULL CHECK(scope IN ('institution','recurrence')),
    target_id TEXT NOT NULL,
    alias TEXT,
    image TEXT,
    PRIMARY KEY(scope,target_id)
);
