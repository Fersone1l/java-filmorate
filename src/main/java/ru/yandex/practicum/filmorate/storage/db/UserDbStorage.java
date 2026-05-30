package ru.yandex.practicum.filmorate.storage.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;


import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Qualifier("UserDbStorage")
@Component
@RequiredArgsConstructor
@Slf4j
public class UserDbStorage implements UserStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Collection<User> findAll() {
        String sql = "SELECT * FROM users";

        List<User> users = jdbcTemplate.query(sql, this::mapRowToUser);

        for (User user : users) {
            user.setFriendsId(loadFriends(user.getId()));
        }

        return users;
    }

    @Override
    public User create(User user) {

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.info("Поле name отсутствует или пустое, поэтому name установлено значение из login: {}", user.getLogin());
        }

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
            User user = jdbcTemplate.queryForObject(sql, this::mapRowToUser, id);

            user.setFriendsId(loadFriends(user.getId()));
            return user;

        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Пользователь с id = " + id + " не найден");
        }
    }

    private User mapRowToUser(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();

        user.setId(rs.getLong("id"));
        user.setEmail(rs.getString("email"));
        user.setLogin(rs.getString("login"));
        user.setName(rs.getString("name"));
        user.setBirthday(rs.getDate("birthday").toLocalDate());

        return user;
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

}