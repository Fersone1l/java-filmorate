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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserStorage userStorage;

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
        User friend = userStorage.getUserById(friendId);

        if (user.getFriendsId().contains(friendId)) {
            log.warn("Пользователи уже являются друзьями");
            throw new ValidationException("Пользователи уже являются друзьями");
        }

        user.getFriendsId().add(friendId);
        friend.getFriendsId().add(userId);

        log.info("Пользователи {} и {} теперь друзья", user.getLogin(), user.getLogin());

        return user;
    }

    public User removeFriend(Long userId, Long friendId) {
        validateNotSameUser(userId, friendId);

        User user = userStorage.getUserById(userId);
        User friend = userStorage.getUserById(friendId);

        user.getFriendsId().remove(friendId);
        friend.getFriendsId().remove(userId);

        log.info("Пользователь {} и {} больше не друзья", user.getLogin(), friend.getLogin());

        return user;
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
