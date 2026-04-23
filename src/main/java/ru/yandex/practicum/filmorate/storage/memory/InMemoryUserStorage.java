package ru.yandex.practicum.filmorate.storage.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Component
@Slf4j
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new HashMap<>();

    @Override
    public Collection<User> findAll() {
        return users.values();
    }

    @Override
    public User create(User user) {
        user.setId(getNextId());
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.info("Поле name отсутствует или пустое, поэтому name установлено значение из login: {}", user.getLogin());
        }
        user.setFriendsId(new HashSet<>());
        this.users.put(user.getId(), user);
        log.info("Пользователь {} с id = {} создан", user.getName(), user.getId());

        return user;
    }

    @Override
    public User update(User newUser) {

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

    private long getNextId() {
        long currentMaxId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }

    @Override
    public Boolean containsUserId(Long id) {
        return users.containsKey(id);
    }

    @Override
    public User getUserById(Long id) {
        User user = users.get(id);
        if (user == null) {
            log.warn("Пользователь с id = {} не найден", id);
            throw new NotFoundException("Пользователь с id = " + id + " не найден");
        }
        return user;
    }
}
