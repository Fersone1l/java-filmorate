package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Validation;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.db.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.db.UserDbStorage;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({FilmDbStorage.class, UserDbStorage.class, FilmService.class, UserService.class, FilmController.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FilmControllerTest {

    private final FilmController controller;
    private final JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM film_likes");
        jdbcTemplate.execute("DELETE FROM film_genres");
        jdbcTemplate.execute("DELETE FROM films");
        jdbcTemplate.execute("DELETE FROM friendships");
        jdbcTemplate.execute("DELETE FROM users");
    }

    private Film createValidFilm() {
        Film film = new Film();
        film.setName("Film");
        film.setDescription("Desc");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);

        Mpa mpa = new Mpa();
        mpa.setId(1L);
        film.setMpa(mpa);

        return film;
    }

    @Test
    void shouldCreateFilmSuccessfully() {
        Film film = createValidFilm();

        Film created = controller.create(film);

        assertNotNull(created.getId());
        assertEquals("Film", created.getName());
    }

    @Test
    void shouldThrowWhenReleaseDateTooEarly() {
        Film film = createValidFilm();
        film.setReleaseDate(LocalDate.of(1800, 1, 1));

        assertThrows(ValidationException.class,
                () -> controller.create(film));
    }

    @Test
    void shouldThrowWhenUpdateWithoutId() {
        Film film = new Film();
        film.setName("Film");

        assertThrows(ValidationException.class,
                () -> controller.update(film));
    }

    @Test
    void shouldThrowWhenFilmNotFound() {
        Film film = createValidFilm();
        film.setId(999L);

        assertThrows(RuntimeException.class,
                () -> controller.update(film));
    }

    @Test
    void shouldAllowExactCinemaBirthday() {
        Film film = createValidFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 28));

        Film created = controller.create(film);

        assertNotNull(created.getId());
    }

    @Test
    void shouldFailWhenDurationIsZero() {
        Film film = createValidFilm();
        film.setDuration(0);

        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        var violations = validator.validate(film);

        assertFalse(violations.isEmpty());
    }
}