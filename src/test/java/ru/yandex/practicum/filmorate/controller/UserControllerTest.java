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
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.db.UserDbStorage;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({UserDbStorage.class, UserService.class, UserController.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserControllerTest {

    private final UserController controller;
    private final JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM friendships");
        jdbcTemplate.execute("DELETE FROM users");
    }

    private User createValidUser() {
        User user = new User();
        user.setEmail("test@mail.com");
        user.setLogin("login");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(2000, 1, 1));
        return user;
    }

    @Test
    void shouldCreateUserSuccessfully() {
        User user = createValidUser();

        User created = controller.create(user);

        assertNotNull(created.getId());
        assertEquals("login", created.getLogin());
    }

    @Test
    void shouldSetNameFromLoginIfBlank() {
        User user = createValidUser();
        user.setName("");

        User created = controller.create(user);

        assertEquals("login", created.getName());
    }

    @Test
    void shouldThrowWhenBirthdayInFuture() {
        User user = createValidUser();
        user.setBirthday(LocalDate.now().plusDays(1));

        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        var violations = validator.validate(user);

        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldThrowWhenUpdateWithoutId() {
        User user = new User();
        assertThrows(ValidationException.class,
                () -> controller.update(user));
    }

    @Test
    void shouldFailWhenLoginContainsSpaces() {
        User user = createValidUser();
        user.setLogin("my login");

        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        var violations = validator.validate(user);

        assertFalse(violations.isEmpty());
    }
}