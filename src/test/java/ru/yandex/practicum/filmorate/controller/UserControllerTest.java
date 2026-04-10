package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest {

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController();
    }

    @Test
    void shouldCreateUserSuccessfully() {
        User user = new User();
        user.setEmail("test@mail.com");
        user.setLogin("login");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        User created = controller.create(user);

        assertNotNull(created.getId());
        assertEquals("login", created.getName());
    }

    @Test
    void shouldSetNameFromLoginIfBlank() {
        User user = new User();
        user.setEmail("test@mail.com");
        user.setLogin("login");
        user.setName("");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        User created = controller.create(user);

        assertEquals("login", created.getName());
    }

    @Test
    void shouldThrowWhenBirthdayInFuture() {
        User user = new User();
        user.setEmail("test@mail.com");
        user.setLogin("login");
        user.setBirthday(LocalDate.now().plusDays(1)); // дата в будущем

        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        var violations = validator.validate(user);

        assertFalse(violations.isEmpty()); // проверяем, что есть ошибки валидации
    }

    @Test
    void shouldThrowWhenUpdateWithoutId() {
        User user = new User();
        assertThrows(ConditionsNotMetException.class,
                () -> controller.update(user));
    }

    @Test
    void shouldFailWhenLoginContainsSpaces() {
        User user = new User();
        user.setEmail("test@mail.com");
        user.setLogin("my login");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        var violations = validator.validate(user);

        assertFalse(violations.isEmpty());
    }
}