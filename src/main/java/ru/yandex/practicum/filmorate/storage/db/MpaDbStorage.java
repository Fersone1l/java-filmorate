package ru.yandex.practicum.filmorate.storage.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.mapper.MpaMapper;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.MpaStorage;

import java.util.*;


@Component
@RequiredArgsConstructor
@Slf4j
public class MpaDbStorage implements MpaStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Collection<Mpa> getAllMpa() {
        String sql = "SELECT * FROM mpa_ratings ORDER BY id";

        return jdbcTemplate.query(sql, MpaMapper::mapRowToMpa);
    }


    @Override
    public Mpa getMpaById(Long id) {
        String sql = "SELECT * FROM mpa_ratings WHERE id = ?";

        Mpa mpa;
        try {
            mpa = jdbcTemplate.queryForObject(sql, MpaMapper::mapRowToMpa, id);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("MPA с id: " + id + " не найден");
        }

        return mpa;
    }
}
