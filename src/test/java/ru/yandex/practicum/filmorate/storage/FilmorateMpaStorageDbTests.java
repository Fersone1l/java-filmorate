package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.db.MpaDbStorage;

import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import(MpaDbStorage.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FilmorateMpaStorageDbTests {

    private final MpaDbStorage mpaStorage;

    // ==================== TESTS FOR GET ALL MPA ====================

    @Test
    void testGetAllMpa() {
        Collection<Mpa> allMpa = mpaStorage.getAllMpa();

        assertThat(allMpa).isNotEmpty();
        assertThat(allMpa).hasSize(5); // G, PG, PG-13, R, NC-17
    }

    @Test
    void testGetAllMpaOrderedById() {
        Collection<Mpa> allMpa = mpaStorage.getAllMpa();

        // Проверяем, что рейтинги отсортированы по id
        Long previousId = 0L;
        for (Mpa mpa : allMpa) {
            assertThat(mpa.getId()).isGreaterThan(previousId);
            previousId = mpa.getId();
        }
    }

    @Test
    void testGetAllMpaContainsCorrectValues() {
        Collection<Mpa> allMpa = mpaStorage.getAllMpa();

        // Проверяем, что все стандартные рейтинги присутствуют
        assertThat(allMpa)
                .extracting(Mpa::getId)
                .containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L);

        assertThat(allMpa)
                .extracting(Mpa::getName)
                .containsExactlyInAnyOrder("G", "PG", "PG-13", "R", "NC-17");
    }

    @Test
    void testGetAllMpaVerifyEachMpa() {
        Collection<Mpa> allMpa = mpaStorage.getAllMpa();

        // Создаем мапу для удобной проверки
        java.util.Map<Long, String> expectedMpa = new java.util.HashMap<>();
        expectedMpa.put(1L, "G");
        expectedMpa.put(2L, "PG");
        expectedMpa.put(3L, "PG-13");
        expectedMpa.put(4L, "R");
        expectedMpa.put(5L, "NC-17");

        for (Mpa mpa : allMpa) {
            assertThat(expectedMpa).containsEntry(mpa.getId(), mpa.getName());
        }
    }

    // ==================== TESTS FOR GET MPA BY ID ====================

    @Test
    void testGetMpaById() {
        Optional<Mpa> mpaOptional = Optional.ofNullable(mpaStorage.getMpaById(1L));

        assertThat(mpaOptional)
                .isPresent()
                .hasValueSatisfying(mpa -> {
                    assertThat(mpa.getId()).isEqualTo(1L);
                    assertThat(mpa.getName()).isEqualTo("G");
                });
    }

    @Test
    void testGetMpaByIdForAllExistingIds() {
        Long[] ids = {1L, 2L, 3L, 4L, 5L};
        String[] expectedNames = {"G", "PG", "PG-13", "R", "NC-17"};

        for (int i = 0; i < ids.length; i++) {
            Mpa mpa = mpaStorage.getMpaById(ids[i]);
            assertThat(mpa.getId()).isEqualTo(ids[i]);
            assertThat(mpa.getName()).isEqualTo(expectedNames[i]);
        }
    }

    @Test
    void testGetMpaByIdForId1() {
        Mpa mpa = mpaStorage.getMpaById(1L);

        assertThat(mpa.getId()).isEqualTo(1L);
        assertThat(mpa.getName()).isEqualTo("G");
    }

    @Test
    void testGetMpaByIdForId2() {
        Mpa mpa = mpaStorage.getMpaById(2L);

        assertThat(mpa.getId()).isEqualTo(2L);
        assertThat(mpa.getName()).isEqualTo("PG");
    }

    @Test
    void testGetMpaByIdForId3() {
        Mpa mpa = mpaStorage.getMpaById(3L);

        assertThat(mpa.getId()).isEqualTo(3L);
        assertThat(mpa.getName()).isEqualTo("PG-13");
    }

    @Test
    void testGetMpaByIdForId4() {
        Mpa mpa = mpaStorage.getMpaById(4L);

        assertThat(mpa.getId()).isEqualTo(4L);
        assertThat(mpa.getName()).isEqualTo("R");
    }

    @Test
    void testGetMpaByIdForId5() {
        Mpa mpa = mpaStorage.getMpaById(5L);

        assertThat(mpa.getId()).isEqualTo(5L);
        assertThat(mpa.getName()).isEqualTo("NC-17");
    }

    @Test
    void testGetMpaByIdNotFound() {
        assertThatThrownBy(() -> mpaStorage.getMpaById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("MPA с id: 999 не найден");
    }

    @Test
    void testGetMpaByIdWithNegativeId() {
        assertThatThrownBy(() -> mpaStorage.getMpaById(-1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("MPA с id: -1 не найден");
    }

    @Test
    void testGetMpaByIdWithZeroId() {
        assertThatThrownBy(() -> mpaStorage.getMpaById(0L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("MPA с id: 0 не найден");
    }

    // ==================== TESTS FOR DATA INTEGRITY ====================

    @Test
    void testMpaDataIntegrity() {
        Collection<Mpa> allMpa = mpaStorage.getAllMpa();

        for (Mpa mpa : allMpa) {
            assertThat(mpa.getId()).isPositive();
            assertThat(mpa.getName()).isNotBlank();
            assertThat(mpa.getName()).matches("^[A-Z0-9\\-]+$"); // Должно содержать только буквы, цифры и дефис
        }
    }

    @Test
    void testMpaNamesAreUnique() {
        Collection<Mpa> allMpa = mpaStorage.getAllMpa();

        long uniqueNamesCount = allMpa.stream()
                .map(Mpa::getName)
                .distinct()
                .count();

        assertThat(uniqueNamesCount).isEqualTo(allMpa.size());
    }

    @Test
    void testMpaIdsAreUnique() {
        Collection<Mpa> allMpa = mpaStorage.getAllMpa();

        long uniqueIdsCount = allMpa.stream()
                .map(Mpa::getId)
                .distinct()
                .count();

        assertThat(uniqueIdsCount).isEqualTo(allMpa.size());
    }

    // ==================== TESTS FOR CONSISTENCY ====================

    @Test
    void testGetAllMpaAndGetByIdConsistency() {
        Collection<Mpa> allMpa = mpaStorage.getAllMpa();

        for (Mpa mpaFromList : allMpa) {
            Mpa mpaById = mpaStorage.getMpaById(mpaFromList.getId());
            assertThat(mpaById).isEqualTo(mpaFromList);
            assertThat(mpaById.getId()).isEqualTo(mpaFromList.getId());
            assertThat(mpaById.getName()).isEqualTo(mpaFromList.getName());
        }
    }

    @Test
    void testGetMpaByIdReturnsNewInstance() {
        Mpa mpa1 = mpaStorage.getMpaById(1L);
        Mpa mpa2 = mpaStorage.getMpaById(1L);

        // Должны быть разными объектами, но с одинаковыми значениями
        assertThat(mpa1).isNotSameAs(mpa2);
        assertThat(mpa1).isEqualTo(mpa2);
    }
}