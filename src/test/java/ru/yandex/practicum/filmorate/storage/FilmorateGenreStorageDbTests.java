package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.db.GenreDbStorage;

import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import(GenreDbStorage.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FilmorateGenreStorageDbTests {

    private final GenreDbStorage genreStorage;

    @Test
    void testGetAllGenres() {
        Collection<Genre> allGenres = genreStorage.getAllGenre();

        assertThat(allGenres).isNotEmpty();
        assertThat(allGenres).hasSize(6);
    }

    @Test
    void testGetAllGenresOrderedById() {
        Collection<Genre> allGenres = genreStorage.getAllGenre();

        Long previousId = 0L;
        for (Genre genre : allGenres) {
            assertThat(genre.getId()).isGreaterThan(previousId);
            previousId = genre.getId();
        }
    }

    @Test
    void testGetAllGenresContainsCorrectValues() {
        Collection<Genre> allGenres = genreStorage.getAllGenre();

        assertThat(allGenres)
                .extracting(Genre::getId)
                .containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L, 6L);

        assertThat(allGenres)
                .extracting(Genre::getName)
                .containsExactlyInAnyOrder(
                        "Комедия", "Драма", "Мультфильм",
                        "Триллер", "Документальный", "Боевик"
                );
    }

    @Test
    void testGetGenreById() {
        Optional<Genre> genreOptional = Optional.ofNullable(genreStorage.getGenreById(1L));

        assertThat(genreOptional)
                .isPresent()
                .hasValueSatisfying(genre -> {
                    assertThat(genre.getId()).isEqualTo(1L);
                    assertThat(genre.getName()).isEqualTo("Комедия");
                });
    }

    @Test
    void testGetGenreByIdForAllExistingIds() {
        Long[] ids = {1L, 2L, 3L, 4L, 5L, 6L};
        String[] expectedNames = {
                "Комедия", "Драма", "Мультфильм",
                "Триллер", "Документальный", "Боевик"
        };

        for (int i = 0; i < ids.length; i++) {
            Genre genre = genreStorage.getGenreById(ids[i]);
            assertThat(genre.getId()).isEqualTo(ids[i]);
            assertThat(genre.getName()).isEqualTo(expectedNames[i]);
        }
    }

    @Test
    void testGetGenreByIdNotFound() {
        assertThatThrownBy(() -> genreStorage.getGenreById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("MPA с id: 999 не найден"); // Здесь будет ваша опечатка
    }

    @Test
    void testGetGenreByIdWithNegativeId() {
        assertThatThrownBy(() -> genreStorage.getGenreById(-1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testGetGenreByIdWithZeroId() {
        assertThatThrownBy(() -> genreStorage.getGenreById(0L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testGenreDataIntegrity() {
        Collection<Genre> allGenres = genreStorage.getAllGenre();

        for (Genre genre : allGenres) {
            assertThat(genre.getId()).isPositive();
            assertThat(genre.getName()).isNotBlank();
        }
    }

    @Test
    void testGetAllGenresAndGetByIdConsistency() {
        Collection<Genre> allGenres = genreStorage.getAllGenre();

        for (Genre genreFromList : allGenres) {
            Genre genreById = genreStorage.getGenreById(genreFromList.getId());
            assertThat(genreById).isEqualTo(genreFromList);
        }
    }

    @Test
    void testGetGenreByIdReturnsNewInstance() {
        Genre genre1 = genreStorage.getGenreById(1L);
        Genre genre2 = genreStorage.getGenreById(1L);

        assertThat(genre1).isNotSameAs(genre2);
        assertThat(genre1).isEqualTo(genre2);
    }
}