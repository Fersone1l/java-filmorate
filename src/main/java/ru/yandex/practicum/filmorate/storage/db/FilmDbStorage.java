package ru.yandex.practicum.filmorate.storage.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.mapper.FilmMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.util.*;


@Component
@RequiredArgsConstructor
@Slf4j
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Collection<Film> findAll() {
        String sql = "SELECT f.*, m.id AS mpa_id, m.code AS code, " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id";

        List<Film> films = jdbcTemplate.query(sql, FilmMapper::mapRowToFilm);

        for (Film film : films) {
            film.setGenres(loadGenres(film.getId()));
            film.setLikes(loadUserLikes(film.getId()));
        }
        return films;
    }

    @Override
    public Film create(Film film) {
        String sql = """
                INSERT INTO films (name, description, release_date, duration, mpa_rating_id)
                VALUES (?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();

        checkMpa(film.getMpa().getId());

        for (Genre genre : film.getGenres()) {
            checkGenre(genre.getId());
        }

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, new String[]{"id"});

            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, Date.valueOf(film.getReleaseDate()));
            ps.setInt(4, film.getDuration());
            ps.setLong(5, film.getMpa() != null ? film.getMpa().getId() : null);
            return ps;
        }, keyHolder);

        long generatedId = keyHolder.getKey().longValue();
        film.setId(generatedId);

        saveGenres(generatedId, film.getGenres());
        log.info("Фильм '{}' успешно создан с id = {}", film.getName(), generatedId);

        return getFilmById(generatedId);
    }

    @Override
    public Film update(Film newFilm) {
        String sql = """
                UPDATE films
                SET name = ?,
                description = ?,
                release_date = ?,
                duration = ?,
                mpa_rating_id = ?
                WHERE id = ?
                """;

        int rowsUpdated = jdbcTemplate.update(sql,
                newFilm.getName(),
                newFilm.getDescription(),
                Date.valueOf(newFilm.getReleaseDate()),
                newFilm.getDuration(),
                newFilm.getMpa() != null ? newFilm.getMpa().getId() : null,
                newFilm.getId());

        if (rowsUpdated == 0) {
            throw new NotFoundException("Фильм с id = " + newFilm.getId() + " не найден");
        }

        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", newFilm.getId());
        saveGenres(newFilm.getId(), newFilm.getGenres());
        log.info("Фильм с id = {} успешно обновлён", newFilm.getId());

        return getFilmById(newFilm.getId());
    }

    @Override
    public Boolean containsFilm(Film film) {
        String sql = "SELECT COUNT(*) FROM films WHERE id = ?";

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, film.getId());
        return count > 0;
    }

    @Override
    public Film getFilmById(Long id) {
        String sql = "SELECT f.*, m.id AS mpa_id, m.code AS code, " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id " +
                "WHERE f.id = ?";

        Film film;
        try {
            film = jdbcTemplate.queryForObject(sql, FilmMapper::mapRowToFilm, id);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Фильм с id: " + id + " не найден");
        }

        film.setGenres(loadGenres(film.getId()));
        film.setLikes(loadUserLikes(film.getId()));

        return film;
    }

    @Override
    public List<Film> getTopFilms(int count) {
        String sql = """
        SELECT f.*,
               m.id AS mpa_id,
               m.code,
               COUNT(fl.user_id) AS likes_count
        FROM films f
        LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id
        LEFT JOIN film_likes fl ON f.id = fl.film_id
        GROUP BY f.id, m.id, m.code
        ORDER BY likes_count DESC, f.id ASC
        LIMIT ?
        """;

        List<Film> films = jdbcTemplate.query(sql, FilmMapper::mapRowToFilm, count);

        for (Film film : films) {
            film.setGenres(loadGenres(film.getId()));
            film.setLikes(loadUserLikes(film.getId()));
        }

        return films;
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        String sql = "INSERT INTO film_likes (film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        String sql = "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
    }

    private Set<Genre> loadGenres(Long filmId) {
        String sql = "SELECT g.id, g.name " +
                "FROM film_genres fg " +
                "JOIN genres g ON fg.genre_id = g.id " +
                "WHERE fg.film_id = ? " +
                "ORDER BY g.id ASC";

        List<Genre> genres = jdbcTemplate.query(sql, FilmMapper::mapGenre, filmId);

        return new LinkedHashSet<>(genres);
    }


    private Set<Long> loadUserLikes(Long filmId) {
        String sql = "SELECT user_id " +
                "FROM film_likes " +
                "WHERE film_id = ? " +
                "ORDER BY user_id";

        List<Long> usersLikes = jdbcTemplate.queryForList(sql, Long.class, filmId);

        return new HashSet<>(usersLikes);
    }

    private void saveGenres(long filmId, Set<Genre> genres) {
        if (genres == null || genres.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";

        for (Genre genre : genres) {
            jdbcTemplate.update(sql, filmId, genre.getId());
        }
    }

    private void checkMpa(Long mpaId) {
        String sql = "SELECT COUNT(*) FROM mpa_ratings WHERE id = ?";

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, mpaId);

        if (count == null || count == 0) {
            throw new NotFoundException("Рейтинг MPA с id = " + mpaId + " не существует");
        }
    }

    private void checkGenre(Long genreId) {
        String sql = "SELECT COUNT(*) FROM genres WHERE id = ?";

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, genreId);

        if (count == null || count == 0) {
            throw new NotFoundException("Жанра с id = " + genreId + " не существует");
        }
    }
}