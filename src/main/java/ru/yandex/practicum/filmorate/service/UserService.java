package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserStorage userStorage;

    public Collection<User> findAll() {
        return userStorage.findAll();
    }

    public User create(User user) {
        setLoginToName(user);
        return userStorage.create(user);
    }

    public User update(User newUser) {
        checkConditions(newUser.getId());
        setLoginToName(newUser);
        return userStorage.update(newUser);
    }

    public List<User> getFriends(Long id) {
        checkConditions(id);
        return userStorage.getFriends(id);
    }

    public User addFriend(Long userId, Long friendId) {
        validateNotSameUser(userId, friendId);

        User user = userStorage.getUserById(userId);

        if (user.getFriendsId().contains(friendId)) {
            log.warn("Пользователи уже являются друзьями");
            throw new ValidationException("Пользователи уже являются друзьями");
        }

        return userStorage.addFriend(userId, friendId);
    }

    public User removeFriend(Long userId, Long friendId) {
        validateNotSameUser(userId, friendId);

        return userStorage.removeFriend(userId, friendId);
    }

    public List<User> getCommonFriends(Long id, Long otherId) {
        validateNotSameUser(id,otherId);

       return userStorage.getCommonFriends(id, otherId);
    }

    public Boolean userExists(Long id) {
        return userStorage.containsUserId(id);
    }

    private void checkConditions(Long id) {
        if (id == null) {
            log.warn("Id не был указан");
            throw new ValidationException("Id должен быть указан");
        } else if (!userStorage.containsUserId(id)) {
            log.warn("Пользователь с id = {} не найден", id);
            throw new NotFoundException("Пользователь с id = " + id + " не найден");
        }
    }

    private void setLoginToName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.info("Поле name отсутствует или пустое, поэтому name установлено значение из login: {}", user.getLogin());
        }

    }

    private void validateNotSameUser(Long userId, Long friendId) {
        checkConditions(friendId);
        checkConditions(userId);

        if (userId.equals(friendId)) {
            log.warn("Пользователь не может находится в своем же списке друзей");
            throw new ValidationException("Пользователь не может находится в своем же списке друзей");
        }
    }
}