package ru.yandex.practicum.filmorate.storage.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.mapper.GenreMapper;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.GenreStorage;

import java.util.*;


@Component
@RequiredArgsConstructor
@Slf4j
public class GenreDbStorage implements GenreStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Collection<Genre> getAllGenre() {
        String sql = "SELECT * FROM genres ORDER BY id";

        return jdbcTemplate.query(sql, GenreMapper::mapRowToGenre);
    }

    @Override
    public Genre getGenreById(Long id) {
        String sql = "SELECT * FROM genres WHERE id = ?";

        Genre genre;
        try {
            genre = jdbcTemplate.queryForObject(sql, GenreMapper::mapRowToGenre, id);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("MPA с id: " + id + " не найден");
        }

        return genre;
    }
}
