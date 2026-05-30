package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    @Autowired
    @Qualifier("UserDbStorage")
    private UserStorage userStorage;

    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public Collection<User> findAll() {
        return userStorage.findAll();
    }

    public User create(User user) {
        return userStorage.create(user);
    }

    public User update(User newUser) {
        checkConditions(newUser.getId());
        return userStorage.update(newUser);
    }

    public List<User> getFriends(Long id) {
        return userStorage.getUserById(id).getFriendsId().stream()
                .map(userStorage::getUserById)
                .collect(Collectors.toList());
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

        User user = userStorage.getUserById(id);
        User otherUser = userStorage.getUserById(otherId);

        return user.getFriendsId().stream()
                .filter(userId -> otherUser.getFriendsId().contains(userId))
                .map(userStorage::getUserById)
                .collect(Collectors.toList());
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

    private void validateNotSameUser(Long userId, Long friendId) {
        checkConditions(friendId);
        checkConditions(userId);

        if (userId.equals(friendId)) {
            log.warn("Пользователь не может находится в своем же списке друзей");
            throw new ValidationException("Пользователь не может находится в своем же списке друзей");
        }
    }
}