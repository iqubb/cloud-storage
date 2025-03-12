CREATE TABLE users (
                       id SERIAL PRIMARY KEY,
                       username VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL
);

CREATE INDEX username_index ON users (username);