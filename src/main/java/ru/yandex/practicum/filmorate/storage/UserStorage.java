package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.List;

public interface UserStorage {

    Collection<User> findAll();

    User create(User user);

    User update(User newUser);

    Boolean containsUserId(Long id);

    User getUserById(Long id);

    User addFriend(Long userId, Long friendId);

    User removeFriend(Long userId, Long friendId);

    List<User> getCommonFriends(Long id, Long otherId);

    List<User> getFriends(Long id);
}