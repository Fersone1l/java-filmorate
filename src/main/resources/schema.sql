-- USERS
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(50) NOT NULL UNIQUE,
    login VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    birthday DATE
);

-- MPA RATINGS
CREATE TABLE IF NOT EXISTS mpa_ratings (
    id INTEGER PRIMARY KEY,
    code VARCHAR(10) NOT NULL
);

-- FILMS
CREATE TABLE IF NOT EXISTS films (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    release_date DATE,
    duration INTEGER,
    mpa_rating_id INTEGER,
    CONSTRAINT fk_mpa FOREIGN KEY (mpa_rating_id) REFERENCES mpa_ratings(id)
);

-- GENRES
CREATE TABLE IF NOT EXISTS genres (
    id INTEGER PRIMARY KEY,
    name VARCHAR(50) NOT NULL
);

-- FILM_GENRES (many-to-many)
CREATE TABLE IF NOT EXISTS film_genres (
    film_id INTEGER,
    genre_id INTEGER,
    PRIMARY KEY (film_id, genre_id),
    CONSTRAINT fk_film FOREIGN KEY (film_id) REFERENCES films(id),
    CONSTRAINT fk_genre FOREIGN KEY (genre_id) REFERENCES genres(id)
);

-- FRIENDSHIPS
CREATE TABLE IF NOT EXISTS friendships (
    user_id INTEGER,
    friend_id INTEGER,
    status VARCHAR(20),
    PRIMARY KEY (user_id, friend_id),
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_friend FOREIGN KEY (friend_id) REFERENCES users(id)
);

-- FILM LIKES
CREATE TABLE IF NOT EXISTS film_likes (
    film_id INTEGER,
    user_id INTEGER,
    PRIMARY KEY (film_id, user_id),
    CONSTRAINT fk_like_film FOREIGN KEY (film_id) REFERENCES films(id),
    CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES users(id)
);