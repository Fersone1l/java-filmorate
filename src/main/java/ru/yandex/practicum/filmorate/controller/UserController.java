    package ru.yandex.practicum.filmorate.controller;

    import jakarta.validation.Valid;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;

    import org.springframework.web.bind.annotation.*;
    import ru.yandex.practicum.filmorate.model.User;
    import ru.yandex.practicum.filmorate.service.UserService;

    import java.util.Collection;
    import java.util.List;

    @RestController
    @RequestMapping("/users")
    @Slf4j
    @RequiredArgsConstructor
    public class UserController {
        private final UserService userService;


        @GetMapping
        public Collection<User> findAll() {
            return userService.findAll();
        }

        @PostMapping
        public User create(@Valid @RequestBody User user) {
           return userService.create(user);
        }

        @PutMapping
        public User update(@Valid @RequestBody User newUser) {
            return userService.update(newUser);
        }

        @PutMapping("/{id}/friends/{friendId}")
        public User addFriend(@PathVariable long id, @PathVariable long friendId) {
            return userService.addFriend(id, friendId);
        }

        @DeleteMapping("/{id}/friends/{friendId}")
        public User removeFriend(@PathVariable long id, @PathVariable long friendId) {
            return userService.removeFriend(id, friendId);
        }

        @GetMapping("/{id}/friends")
        public List<User> getFriends(@PathVariable long id) {
            return userService.getFriends(id);
        }

        @GetMapping("/{id}/friends/common/{otherId}")
        public List<User> getFriends(@PathVariable long id, @PathVariable long otherId) {
            return userService.getCommonFriends(id, otherId);
        }
    }
