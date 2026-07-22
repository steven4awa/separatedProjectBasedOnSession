CREATE TABLE IF NOT EXISTS persistent_logins (
     username VARCHAR(64) NOT NULL,
     series VARCHAR(64) NOT NULL,
     token VARCHAR(64) NOT NULL,
     last_used DATETIME NOT NULL,
     PRIMARY KEY (series)
);