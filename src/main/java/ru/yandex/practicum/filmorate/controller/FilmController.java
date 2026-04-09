package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController {
    private final Map<Long, Film> films = new HashMap<>();

    @GetMapping
    public Collection<Film> findAll() {
        return films.values();
    }

    @PostMapping
    public Film create(@Valid @RequestBody Film film) {
        // проверяем выполнение необходимых условий
        checkConditions(film);

        film.setId(getNextId());
        films.put(film.getId(), film);
        log.info("Фильм {} с id = {} создан", film.getName(), film.getId());
        return film;
    }

    private long getNextId() {
        long currentMaxId = films.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }

    @PutMapping
    public Film update(@Valid @RequestBody Film newFilm) {
        // проверяем необходимые условия
        if (newFilm.getId() == null) {
            log.warn("Id не был указан");
            throw new ConditionsNotMetException("Id должен быть указан");
        }
        if (films.containsKey(newFilm.getId())) {
            Film oldFilm = films.get(newFilm.getId());
            checkConditions(newFilm);

            oldFilm.setDescription(newFilm.getDescription());
            oldFilm.setName(newFilm.getName());
            oldFilm.setReleaseDate(newFilm.getReleaseDate());
            oldFilm.setDuration(newFilm.getDuration());
            log.info("Фильм с id = {} обновлен", newFilm.getId());
            return oldFilm;
        }
        log.warn("Фильм с id = {} не найден", newFilm.getId());
        throw new NotFoundException("Фильм с id = " + newFilm.getId() + " не найден");
    }

    private void checkConditions(Film film) {

        LocalDate cinemasBirthday = LocalDate.of(1895, 12, 28);
        if (film.getReleaseDate().isBefore(cinemasBirthday)) {

            log.warn("Передана некорректная дата релиза: {}", film.getReleaseDate());
            throw new ConditionsNotMetException("дата релиза — не раньше 28 декабря 1895 года");
        }

    }
}
