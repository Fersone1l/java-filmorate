package ru.yandex.practicum.filmorate.storage.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.mapper.UserMapper;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;


import java.sql.Date;
import java.sql.PreparedStatement;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserDbStorage implements UserStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Collection<User> findAll() {
        String sql = "SELECT * FROM users";

        List<User> users = jdbcTemplate.query(sql, UserMapper::mapRowToUser);

        for (User user : users) {
            user.setFriendsId(loadFriends(user.getId()));
        }

        return users;
    }

    @Override
    public User create(User user) {
        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, new String[]{"id"});

            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());
            ps.setDate(4, Date.valueOf(user.getBirthday()));
            return ps;
        }, keyHolder);

        long generatedId = keyHolder.getKey().longValue();
        user.setId(generatedId);

        log.info("Пользователь '{}' успешно создан с id = {}", user.getLogin(), generatedId);

        return getUserById(generatedId);
    }

    @Override
    public User update(User newUser) {
        String sql = """
                UPDATE users
                SET email = ?,
                login = ?,
                name = ?,
                birthday = ?
                WHERE id = ?
                """;
        int rowsUpdated = jdbcTemplate.update(sql,
                newUser.getEmail(),
                newUser.getLogin(),
                newUser.getName(),
                Date.valueOf(newUser.getBirthday()),
                newUser.getId());

        if (rowsUpdated == 0) {
            throw new NotFoundException("Пользователь с id = " + newUser.getId() + " не найден");
        }

        log.info("Пользователь с id = {} успешно обновлён", newUser.getId());

        return getUserById(newUser.getId());
    }

    @Override
    public User addFriend(Long userId, Long friendId) {
        String sql = "INSERT INTO friendships (user_id, friend_id, status) VALUES (?, ?, 'CONFIRMED')";
        jdbcTemplate.update(sql, userId, friendId);
        log.info("Пользователь {} добавил в друзья {}", userId, friendId);
        return getUserById(userId);
    }

    @Override
    public User removeFriend(Long userId, Long friendId) {
        String sql = "DELETE FROM friendships WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(sql, userId, friendId);
        log.info("Пользователь {} удалил из друзей {}", userId, friendId);
        return getUserById(userId);
    }

    @Override
    public Boolean containsUserId(Long id) {
        String sql = "SELECT COUNT(*) FROM users WHERE id = ?";

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count > 0;
    }

    @Override
    public User getUserById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";

        try {
            User user = jdbcTemplate.queryForObject(sql, UserMapper::mapRowToUser, id);

            user.setFriendsId(loadFriends(user.getId()));
            return user;

        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Пользователь с id = " + id + " не найден");
        }
    }

    private Set<Long> loadFriends(Long userId) {
        String sql = """
        SELECT friend_id
        FROM friendships
        WHERE user_id = ?
        ORDER BY friend_id
        """;

        List<Long> friends = jdbcTemplate.queryForList(sql, Long.class, userId);
        return new HashSet<>(friends);
    }

    @Override
    public List<User> getCommonFriends(Long id, Long otherId) {
        String sql = """
                SELECT u.*
                FROM users u
                INNER JOIN friendships f1 ON u.id = f1.friend_id AND f1.user_id = ?
                INNER JOIN friendships f2 ON u.id = f2.friend_id AND f2.user_id = ?
                """;

        return jdbcTemplate.query(sql, UserMapper::mapRowToUser, id, otherId);
    }

    @Override
    public List<User> getFriends(Long id) {
        String sql = """
                SELECT u.*
                FROM users u
                INNER JOIN friendships f ON u.id = f.friend_id AND f.user_id = ?
                """;

        return jdbcTemplate.query(sql, UserMapper::mapRowToUser, id);
    }

}