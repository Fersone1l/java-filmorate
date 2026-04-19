    package ru.yandex.practicum.filmorate.controller;

    import jakarta.validation.Valid;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.web.bind.annotation.*;
    import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
    import ru.yandex.practicum.filmorate.exception.NotFoundException;
    import ru.yandex.practicum.filmorate.model.User;

    import java.util.Collection;
    import java.util.HashMap;
    import java.util.Map;

    @RestController
    @RequestMapping("/users")
    @Slf4j
    public class UserController {
        private final Map<Long, User> users = new HashMap<>();

        @GetMapping
        public Collection<User> findAll() {
            return users.values();
        }

        @PostMapping
        public User create(@Valid @RequestBody User user) {
            // проверяем выполнение необходимых условий
            user.setId(getNextId());
            if (user.getName() == null || user.getName().isBlank()) {
                user.setName(user.getLogin());
                log.info("Поле name отсутствует или пустое, поэтому name установлено значение из login: {}", user.getLogin());
            }
            this.users.put(user.getId(), user);
            log.info("Пользователь {} с id = {} создан", user.getName(), user.getId());
            return user;
        }

        private long getNextId() {
            long currentMaxId = users.keySet()
                    .stream()
                    .mapToLong(id -> id)
                    .max()
                    .orElse(0);
            return ++currentMaxId;
        }

        @PutMapping
        public User update(@Valid @RequestBody User newUser) {
            // проверяем необходимые условия
            if (newUser.getId() == null) {
                log.warn("Id не был указан");
                throw new ConditionsNotMetException("Id должен быть указан");
            }
            if (users.containsKey(newUser.getId())) {
                User oldUser = users.get(newUser.getId());
                oldUser.setEmail(newUser.getEmail());
                oldUser.setLogin(newUser.getLogin());

                if (newUser.getName() != null && !newUser.getName().isBlank()) {
                    oldUser.setName(newUser.getName());
                }

                oldUser.setBirthday(newUser.getBirthday());
                log.info("Пользователь с id = {} обновлен", newUser.getId());
                return oldUser;
            }
            log.warn("Пользователь с id = {} не найден", newUser.getId());
            throw new NotFoundException("Пользователь с id = " + newUser.getId() + " не найден");
        }
    }
