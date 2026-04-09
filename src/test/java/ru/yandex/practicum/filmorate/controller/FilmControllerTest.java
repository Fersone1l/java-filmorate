package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FilmControllerTest {

    private FilmController controller;

    @BeforeEach
    void setUp() {
        controller = new FilmController();
    }

    @Test
    void shouldCreateFilmSuccessfully() {
        Film film = new Film();
        film.setName("Film");
        film.setDescription("Desc");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);

        Film created = controller.create(film);

        assertNotNull(created.getId());
        assertEquals("Film", created.getName());
    }

    @Test
    void shouldThrowWhenReleaseDateTooEarly() {
        Film film = new Film();
        film.setName("Film");
        film.setDescription("Desc");
        film.setReleaseDate(LocalDate.of(1800, 1, 1));
        film.setDuration(100);

        assertThrows(ConditionsNotMetException.class,
                () -> controller.create(film));
    }

    @Test
    void shouldThrowWhenUpdateWithoutId() {
        Film film = new Film();
        film.setName("Film");

        assertThrows(ConditionsNotMetException.class,
                () -> controller.update(film));
    }

    @Test
    void shouldThrowWhenFilmNotFound() {
        Film film = new Film();
        film.setId(999L);
        film.setName("Film");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);

        assertThrows(RuntimeException.class,
                () -> controller.update(film));
    }

    @Test
    void shouldAllowExactCinemaBirthday() {
        Film film = new Film();
        film.setName("Film");
        film.setDescription("Desc");
        film.setReleaseDate(LocalDate.of(1895, 12, 28));
        film.setDuration(100);

        Film created = controller.create(film);

        assertNotNull(created.getId());
    }

    @Test
    void shouldFailWhenDurationIsZero() {
        Film film = new Film();
        film.setName("Film");
        film.setDescription("Desc");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(0);

        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        var violations = validator.validate(film);

        assertFalse(violations.isEmpty());
    }
}