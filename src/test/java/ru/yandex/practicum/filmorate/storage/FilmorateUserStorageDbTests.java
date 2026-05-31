package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.db.UserDbStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import(UserDbStorage.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FilmorateUserStorageDbTests {

    private final UserDbStorage userStorage;
    private final JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Очищаем таблицы перед каждым тестом
        jdbcTemplate.execute("DELETE FROM friendships");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    void testCreateUser() {
        User user = new User();
        user.setEmail("test@test.ru");
        user.setLogin("testuser");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User created = userStorage.create(user);

        assertThat(created.getId()).isPositive();
        assertThat(created.getEmail()).isEqualTo("test@test.ru");
        assertThat(created.getLogin()).isEqualTo("testuser");
        assertThat(created.getName()).isEqualTo("Test User");
        assertThat(created.getBirthday()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(created.getFriendsId()).isEmpty();
    }

    @Test
    void testFindUserById() {
        User user = new User();
        user.setEmail("test@test.ru");
        user.setLogin("testuser");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User created = userStorage.create(user);

        Optional<User> found = Optional.ofNullable(userStorage.getUserById(created.getId()));

        assertThat(found)
                .isPresent()
                .hasValueSatisfying(u ->
                        assertThat(u.getId()).isEqualTo(created.getId())
                );
    }

    @Test
    void testGetUserByIdNotFound() {
        assertThatThrownBy(() -> userStorage.getUserById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Пользователь с id = 999 не найден");
    }

    @Test
    void testUpdateUser() {
        User user = new User();
        user.setEmail("old@test.ru");
        user.setLogin("olduser");
        user.setName("Old User");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User created = userStorage.create(user);

        created.setEmail("new@test.ru");
        created.setLogin("newuser");
        created.setName("New User");
        created.setBirthday(LocalDate.of(1991, 1, 1));

        User updated = userStorage.update(created);

        assertThat(updated.getEmail()).isEqualTo("new@test.ru");
        assertThat(updated.getLogin()).isEqualTo("newuser");
        assertThat(updated.getName()).isEqualTo("New User");
        assertThat(updated.getBirthday()).isEqualTo(LocalDate.of(1991, 1, 1));
    }

    @Test
    void testUpdateNonExistentUserShouldThrowException() {
        User user = new User();
        user.setId(999L);
        user.setEmail("test@test.ru");
        user.setLogin("testuser");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        assertThatThrownBy(() -> userStorage.update(user))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Пользователь с id = 999 не найден");
    }

    @Test
    void testFindAllUsers() {
        User user1 = new User();
        user1.setEmail("user1@test.ru");
        user1.setLogin("user1");
        user1.setName("User 1");
        user1.setBirthday(LocalDate.of(1990, 1, 1));

        User user2 = new User();
        user2.setEmail("user2@test.ru");
        user2.setLogin("user2");
        user2.setName("User 2");
        user2.setBirthday(LocalDate.of(1991, 1, 1));

        userStorage.create(user1);
        userStorage.create(user2);

        Collection<User> users = userStorage.findAll();

        assertThat(users).hasSize(2);
        assertThat(users).extracting(User::getLogin).containsExactlyInAnyOrder("user1", "user2");
    }

    @Test
    void testFindAllUsersWhenEmpty() {
        Collection<User> users = userStorage.findAll();

        assertThat(users).isEmpty();
    }

    @Test
    void testContainsUserId() {
        User user = new User();
        user.setEmail("test@test.ru");
        user.setLogin("testuser");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User created = userStorage.create(user);

        assertTrue(userStorage.containsUserId(created.getId()));
        assertFalse(userStorage.containsUserId(999L));
    }

    @Test
    void testAddFriend() {
        User user1 = new User();
        user1.setEmail("user1@test.ru");
        user1.setLogin("user1");
        user1.setName("User 1");
        user1.setBirthday(LocalDate.of(1990, 1, 1));

        User user2 = new User();
        user2.setEmail("user2@test.ru");
        user2.setLogin("user2");
        user2.setName("User 2");
        user2.setBirthday(LocalDate.of(1991, 1, 1));

        User created1 = userStorage.create(user1);
        User created2 = userStorage.create(user2);

        User updated = userStorage.addFriend(created1.getId(), created2.getId());

        assertThat(updated.getFriendsId()).contains(created2.getId());
        assertThat(updated.getFriendsId()).hasSize(1);
    }

    @Test
    void testAddMultipleFriends() {
        User user1 = new User();
        user1.setEmail("user1@test.ru");
        user1.setLogin("user1");
        user1.setName("User 1");
        user1.setBirthday(LocalDate.of(1990, 1, 1));

        User user2 = new User();
        user2.setEmail("user2@test.ru");
        user2.setLogin("user2");
        user2.setName("User 2");
        user2.setBirthday(LocalDate.of(1991, 1, 1));

        User user3 = new User();
        user3.setEmail("user3@test.ru");
        user3.setLogin("user3");
        user3.setName("User 3");
        user3.setBirthday(LocalDate.of(1992, 1, 1));

        User created1 = userStorage.create(user1);
        User created2 = userStorage.create(user2);
        User created3 = userStorage.create(user3);

        userStorage.addFriend(created1.getId(), created2.getId());
        userStorage.addFriend(created1.getId(), created3.getId());

        User updated = userStorage.getUserById(created1.getId());

        assertThat(updated.getFriendsId()).hasSize(2);
        assertThat(updated.getFriendsId()).contains(created2.getId(), created3.getId());
    }

    @Test
    void testAddFriendSameUserShouldNotAddDuplicate() {
        User user1 = new User();
        user1.setEmail("user1@test.ru");
        user1.setLogin("user1");
        user1.setName("User 1");
        user1.setBirthday(LocalDate.of(1990, 1, 1));

        User user2 = new User();
        user2.setEmail("user2@test.ru");
        user2.setLogin("user2");
        user2.setName("User 2");
        user2.setBirthday(LocalDate.of(1991, 1, 1));

        User created1 = userStorage.create(user1);
        User created2 = userStorage.create(user2);

        userStorage.addFriend(created1.getId(), created2.getId());

        // Повторное добавление должно выбросить исключение из-за уникального ограничения
        assertThatThrownBy(() -> userStorage.addFriend(created1.getId(), created2.getId()))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);

        // Проверяем, что друг все еще один
        User updated = userStorage.getUserById(created1.getId());
        assertThat(updated.getFriendsId()).hasSize(1);
        assertThat(updated.getFriendsId()).contains(created2.getId());
    }

    @Test
    void testRemoveFriend() {
        User user1 = new User();
        user1.setEmail("user1@test.ru");
        user1.setLogin("user1");
        user1.setName("User 1");
        user1.setBirthday(LocalDate.of(1990, 1, 1));

        User user2 = new User();
        user2.setEmail("user2@test.ru");
        user2.setLogin("user2");
        user2.setName("User 2");
        user2.setBirthday(LocalDate.of(1991, 1, 1));

        User created1 = userStorage.create(user1);
        User created2 = userStorage.create(user2);

        userStorage.addFriend(created1.getId(), created2.getId());

        User beforeRemove = userStorage.getUserById(created1.getId());
        assertThat(beforeRemove.getFriendsId()).contains(created2.getId());

        User afterRemove = userStorage.removeFriend(created1.getId(), created2.getId());

        assertThat(afterRemove.getFriendsId()).doesNotContain(created2.getId());
        assertThat(afterRemove.getFriendsId()).isEmpty();
    }

    @Test
    void testRemoveNonExistentFriend() {
        User user1 = new User();
        user1.setEmail("user1@test.ru");
        user1.setLogin("user1");
        user1.setName("User 1");
        user1.setBirthday(LocalDate.of(1990, 1, 1));

        User created1 = userStorage.create(user1);

        // Удаление несуществующего друга не должно вызывать ошибку
        User afterRemove = userStorage.removeFriend(created1.getId(), 999L);

        assertThat(afterRemove.getFriendsId()).isEmpty();
    }

    @Test
    void testGetUserWithFriends() {
        User user1 = new User();
        user1.setEmail("user1@test.ru");
        user1.setLogin("user1");
        user1.setName("User 1");
        user1.setBirthday(LocalDate.of(1990, 1, 1));

        User user2 = new User();
        user2.setEmail("user2@test.ru");
        user2.setLogin("user2");
        user2.setName("User 2");
        user2.setBirthday(LocalDate.of(1991, 1, 1));

        User created1 = userStorage.create(user1);
        User created2 = userStorage.create(user2);

        userStorage.addFriend(created1.getId(), created2.getId());

        User found = userStorage.getUserById(created1.getId());

        assertThat(found.getFriendsId()).hasSize(1);
        assertThat(found.getFriendsId()).contains(created2.getId());
    }

    @Test
    void testFriendshipIsBidirectional() {
        User user1 = new User();
        user1.setEmail("user1@test.ru");
        user1.setLogin("user1");
        user1.setName("User 1");
        user1.setBirthday(LocalDate.of(1990, 1, 1));

        User user2 = new User();
        user2.setEmail("user2@test.ru");
        user2.setLogin("user2");
        user2.setName("User 2");
        user2.setBirthday(LocalDate.of(1991, 1, 1));

        User created1 = userStorage.create(user1);
        User created2 = userStorage.create(user2);

        userStorage.addFriend(created1.getId(), created2.getId());

        User user1WithFriends = userStorage.getUserById(created1.getId());
        User user2WithFriends = userStorage.getUserById(created2.getId());

        assertThat(user1WithFriends.getFriendsId()).contains(created2.getId());
        assertThat(user2WithFriends.getFriendsId()).doesNotContain(created1.getId()); // Дружба не двунаправленная
    }
}