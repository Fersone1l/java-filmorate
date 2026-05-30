package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.db.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.db.UserDbStorage;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({FilmDbStorage.class, UserDbStorage.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FilmorateFilmStorageDbTests {

    private final FilmDbStorage filmStorage;
    private final UserDbStorage userStorage;
    private final JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM film_likes");
        jdbcTemplate.execute("DELETE FROM film_genres");
        jdbcTemplate.execute("DELETE FROM films");
        jdbcTemplate.execute("DELETE FROM friendships");
        jdbcTemplate.execute("DELETE FROM users");
    }

    private Film createTestFilm() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2020, 1, 1));
        film.setDuration(120);

        Mpa mpa = new Mpa();
        mpa.setId(1L); // G
        film.setMpa(mpa);

        Set<Genre> genres = new LinkedHashSet<>();
        Genre genre = new Genre();
        genre.setId(1L); // Комедия
        genres.add(genre);
        film.setGenres(genres);

        film.setLikes(new HashSet<>());

        return film;
    }

    private Film createAnotherTestFilm() {
        Film film = new Film();
        film.setName("Another Film");
        film.setDescription("Another Description");
        film.setReleaseDate(LocalDate.of(2021, 2, 2));
        film.setDuration(130);

        Mpa mpa = new Mpa();
        mpa.setId(2L); // PG
        film.setMpa(mpa);

        Set<Genre> genres = new LinkedHashSet<>();
        Genre genre = new Genre();
        genre.setId(2L); // Драма
        genres.add(genre);
        film.setGenres(genres);

        film.setLikes(new HashSet<>());

        return film;
    }

    private User createTestUser() {
        User user = new User();
        user.setEmail("test@test.ru");
        user.setLogin("testuser");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return userStorage.create(user);
    }

    private User createAnotherTestUser() {
        User user = new User();
        user.setEmail("test2@test.ru");
        user.setLogin("testuser2");
        user.setName("Test User 2");
        user.setBirthday(LocalDate.of(1991, 2, 2));
        return userStorage.create(user);
    }

    // ==================== TESTS FOR CREATE ====================

    @Test
    void testCreateFilm() {
        Film film = createTestFilm();

        Film created = filmStorage.create(film);

        assertThat(created.getId()).isPositive();
        assertThat(created.getName()).isEqualTo("Test Film");
        assertThat(created.getDescription()).isEqualTo("Test Description");
        assertThat(created.getReleaseDate()).isEqualTo(LocalDate.of(2020, 1, 1));
        assertThat(created.getDuration()).isEqualTo(120);
        assertThat(created.getMpa()).isNotNull();
        assertThat(created.getMpa().getId()).isEqualTo(1L);
        assertThat(created.getGenres()).hasSize(1);
        assertThat(created.getLikes()).isEmpty();
    }

    @Test
    void testCreateFilmWithMultipleGenres() {
        Film film = createTestFilm();
        Set<Genre> genres = new LinkedHashSet<>();
        Genre genre1 = new Genre();
        Genre genre2 = new Genre();
        genre1.setName("Комедия");
        genre1.setId(1L);
        genre2.setName("Драма");
        genre2.setId(2L);

        genres.add(genre1);
        genres.add(genre2);
        film.setGenres(genres);

        Film created = filmStorage.create(film);

        assertThat(created.getGenres()).hasSize(2);
        assertThat(created.getGenres()).extracting(Genre::getId).containsExactly(1L, 2L);
    }

    @Test
    void testCreateFilmWithNoGenres() {
        Film film = createTestFilm();
        film.setGenres(new LinkedHashSet<>());

        Film created = filmStorage.create(film);

        assertThat(created.getGenres()).isEmpty();
    }

    @Test
    void testCreateFilmWithInvalidMpaShouldThrowException() {
        Film film = createTestFilm();
        film.getMpa().setId(999L);

        assertThatThrownBy(() -> filmStorage.create(film))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Рейтинг MPA с id = 999 не существует");
    }

    @Test
    void testCreateFilmWithInvalidGenreShouldThrowException() {
        Film film = createTestFilm();
        Set<Genre> genres = new LinkedHashSet<>();
        Genre genre = new Genre();
        genre.setName("Invalid");
        genre.setId(999L);
        genres.add(genre);
        film.setGenres(genres);

        assertThatThrownBy(() -> filmStorage.create(film))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Жанра с id = 999 не существует");
    }

    // ==================== TESTS FOR GET BY ID ====================

    @Test
    void testGetFilmById() {
        Film film = createTestFilm();
        Film created = filmStorage.create(film);

        Optional<Film> found = Optional.ofNullable(filmStorage.getFilmById(created.getId()));

        assertThat(found)
                .isPresent()
                .hasValueSatisfying(f ->
                        assertThat(f.getId()).isEqualTo(created.getId())
                );
    }

    @Test
    void testGetFilmByIdNotFound() {
        assertThatThrownBy(() -> filmStorage.getFilmById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Фильм с id: 999 не найден");
    }

    // ==================== TESTS FOR UPDATE ====================

    @Test
    void testUpdateFilm() {
        Film film = createTestFilm();
        Film created = filmStorage.create(film);

        created.setName("Updated Film Name");
        created.setDescription("Updated Description");
        created.setDuration(150);

        Film updated = filmStorage.update(created);

        assertThat(updated.getName()).isEqualTo("Updated Film Name");
        assertThat(updated.getDescription()).isEqualTo("Updated Description");
        assertThat(updated.getDuration()).isEqualTo(150);
        assertThat(updated.getId()).isEqualTo(created.getId());
    }

    @Test
    void testUpdateFilmWithNewGenres() {
        Film film = createTestFilm();
        Film created = filmStorage.create(film);

        Set<Genre> newGenres = new LinkedHashSet<>();
        Genre genre1 = new Genre();
        Genre genre2 = new Genre();
        genre1.setName("Драма");
        genre1.setId(2L);
        genre2.setName("Мультфильм");
        genre2.setId(3L);

        newGenres.add(genre1);
        newGenres.add(genre2);
        created.setGenres(newGenres);

        Film updated = filmStorage.update(created);

        assertThat(updated.getGenres()).hasSize(2);
        assertThat(updated.getGenres()).extracting(Genre::getId).containsExactly(2L, 3L);
    }

    @Test
    void testUpdateFilmWithEmptyGenres() {
        Film film = createTestFilm();
        Film created = filmStorage.create(film);

        created.setGenres(new LinkedHashSet<>());

        Film updated = filmStorage.update(created);

        assertThat(updated.getGenres()).isEmpty();
    }

    @Test
    void testUpdateNonExistentFilmShouldThrowException() {
        Film film = createTestFilm();
        film.setId(999L);

        assertThatThrownBy(() -> filmStorage.update(film))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Фильм с id = 999 не найден");
    }

    // ==================== TESTS FOR FIND ALL ====================

    @Test
    void testFindAllFilms() {
        Film film1 = createTestFilm();
        Film film2 = createAnotherTestFilm();

        filmStorage.create(film1);
        filmStorage.create(film2);

        Collection<Film> films = filmStorage.findAll();

        assertThat(films).hasSize(2);
        assertThat(films).extracting(Film::getName).containsExactlyInAnyOrder("Test Film", "Another Film");
    }

    @Test
    void testFindAllFilmsWhenEmpty() {
        Collection<Film> films = filmStorage.findAll();

        assertThat(films).isEmpty();
    }

    @Test
    void testFindAllFilmsWithGenresAndLikes() {
        Film film = createTestFilm();
        Film created = filmStorage.create(film);
        User user = createTestUser();

        filmStorage.addLike(created.getId(), user.getId());

        Collection<Film> films = filmStorage.findAll();

        assertThat(films).hasSize(1);
        Film foundFilm = films.iterator().next();
        assertThat(foundFilm.getGenres()).isNotEmpty();
        assertThat(foundFilm.getLikes()).contains(user.getId());
    }

    // ==================== TESTS FOR CONTAINS FILM ====================

    @Test
    void testContainsFilm() {
        Film film = createTestFilm();
        Film created = filmStorage.create(film);

        assertTrue(filmStorage.containsFilm(created));
    }

    @Test
    void testContainsFilmNotFound() {
        Film film = createTestFilm();
        film.setId(999L);

        assertFalse(filmStorage.containsFilm(film));
    }

    // ==================== TESTS FOR ADD LIKE ====================

    @Test
    void testAddLike() {
        Film film = filmStorage.create(createTestFilm());
        User user = createTestUser();

        filmStorage.addLike(film.getId(), user.getId());

        Film likedFilm = filmStorage.getFilmById(film.getId());
        assertThat(likedFilm.getLikes()).contains(user.getId());
        assertThat(likedFilm.getLikes()).hasSize(1);
    }

    @Test
    void testAddMultipleLikes() {
        Film film = filmStorage.create(createTestFilm());
        User user1 = createTestUser();
        User user2 = createAnotherTestUser();

        filmStorage.addLike(film.getId(), user1.getId());
        filmStorage.addLike(film.getId(), user2.getId());

        Film likedFilm = filmStorage.getFilmById(film.getId());
        assertThat(likedFilm.getLikes()).hasSize(2);
        assertThat(likedFilm.getLikes()).contains(user1.getId(), user2.getId());
    }

    @Test
    void testAddDuplicateLikeShouldThrowException() {
        Film film = filmStorage.create(createTestFilm());
        User user = createTestUser();

        filmStorage.addLike(film.getId(), user.getId());

        assertThatThrownBy(() -> filmStorage.addLike(film.getId(), user.getId()))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void testAddLikeToNonExistentFilm() {
        User user = createTestUser();

        assertThatThrownBy(() -> filmStorage.addLike(999L, user.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ==================== TESTS FOR REMOVE LIKE ====================

    @Test
    void testRemoveLike() {
        Film film = filmStorage.create(createTestFilm());
        User user = createTestUser();

        filmStorage.addLike(film.getId(), user.getId());

        Film beforeRemove = filmStorage.getFilmById(film.getId());
        assertThat(beforeRemove.getLikes()).contains(user.getId());

        filmStorage.removeLike(film.getId(), user.getId());

        Film afterRemove = filmStorage.getFilmById(film.getId());
        assertThat(afterRemove.getLikes()).doesNotContain(user.getId());
        assertThat(afterRemove.getLikes()).isEmpty();
    }

    @Test
    void testRemoveNonExistentLike() {
        Film film = filmStorage.create(createTestFilm());
        User user = createTestUser();

        // Удаление несуществующего лайка не должно вызывать ошибку
        filmStorage.removeLike(film.getId(), user.getId());

        Film filmAfter = filmStorage.getFilmById(film.getId());
        assertThat(filmAfter.getLikes()).isEmpty();
    }

    // ==================== TESTS FOR GET TOP FILMS ====================

    @Test
    void testGetTopFilms() {
        Film film1 = filmStorage.create(createTestFilm());
        Film film2 = filmStorage.create(createAnotherTestFilm());
        User user1 = createTestUser();
        User user2 = createAnotherTestUser();

        filmStorage.addLike(film1.getId(), user1.getId());
        filmStorage.addLike(film1.getId(), user2.getId());
        filmStorage.addLike(film2.getId(), user1.getId());

        List<Film> topFilms = filmStorage.getTopFilms(2);

        assertThat(topFilms).hasSize(2);
        assertThat(topFilms.get(0).getId()).isEqualTo(film1.getId()); // Фильм с 2 лайками первый
        assertThat(topFilms.get(1).getId()).isEqualTo(film2.getId()); // Фильм с 1 лайком второй
    }

    @Test
    void testGetTopFilmsWithLimit() {
        Film film1 = filmStorage.create(createTestFilm());
        Film film2 = filmStorage.create(createAnotherTestFilm());
        User user = createTestUser();

        filmStorage.addLike(film1.getId(), user.getId());

        List<Film> topFilms = filmStorage.getTopFilms(1);

        assertThat(topFilms).hasSize(1);
        assertThat(topFilms.get(0).getId()).isEqualTo(film1.getId());
    }

    @Test
    void testGetTopFilmsWhenNoLikes() {
        filmStorage.create(createTestFilm());
        filmStorage.create(createAnotherTestFilm());

        List<Film> topFilms = filmStorage.getTopFilms(2);

        assertThat(topFilms).hasSize(2);
        // Фильмы должны быть отсортированы по id при одинаковом количестве лайков
        assertThat(topFilms.get(0).getId()).isLessThan(topFilms.get(1).getId());
        assertThat(topFilms.get(0).getLikes()).isEmpty();
        assertThat(topFilms.get(1).getLikes()).isEmpty();
    }

    @Test
    void testGetTopFilmsWhenCountGreaterThanAvailable() {
        filmStorage.create(createTestFilm());

        List<Film> topFilms = filmStorage.getTopFilms(5);

        assertThat(topFilms).hasSize(1);
    }

    @Test
    void testGetTopFilmsWithZeroCount() {
        filmStorage.create(createTestFilm());

        List<Film> topFilms = filmStorage.getTopFilms(0);

        assertThat(topFilms).isEmpty();
    }

    @Test
    void testGetTopFilmsPreservesGenresAndLikes() {
        Film film = filmStorage.create(createTestFilm());
        User user = createTestUser();

        filmStorage.addLike(film.getId(), user.getId());

        List<Film> topFilms = filmStorage.getTopFilms(1);

        assertThat(topFilms).hasSize(1);
        Film topFilm = topFilms.get(0);
        assertThat(topFilm.getGenres()).isNotEmpty();
        assertThat(topFilm.getLikes()).contains(user.getId());
    }

    // ==================== TESTS FOR LOADING RELATED DATA ====================

    @Test
    void testFilmGenresAreLoadedCorrectly() {
        Film film = createTestFilm();
        Set<Genre> genres = new LinkedHashSet<>();
        Genre genre1 = new Genre();
        Genre genre2 = new Genre();
        genre1.setName("Комедия");
        genre1.setId(1L);
        genre2.setName("Драма");
        genre2.setId(2L);

        genres.add(genre1);
        genres.add(genre2);
        film.setGenres(genres);

        Film created = filmStorage.create(film);

        Film loaded = filmStorage.getFilmById(created.getId());
        assertThat(loaded.getGenres()).hasSize(2);
        assertThat(loaded.getGenres()).extracting(Genre::getId).containsExactly(1L, 2L);
    }

    @Test
    void testFilmLikesAreLoadedCorrectly() {
        Film film = filmStorage.create(createTestFilm());
        User user1 = createTestUser();
        User user2 = createAnotherTestUser();

        filmStorage.addLike(film.getId(), user1.getId());
        filmStorage.addLike(film.getId(), user2.getId());

        Film loaded = filmStorage.getFilmById(film.getId());
        assertThat(loaded.getLikes()).hasSize(2);
        assertThat(loaded.getLikes()).contains(user1.getId(), user2.getId());
    }
}