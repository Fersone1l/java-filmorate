package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserService userService;

    public static final LocalDate CINEMAS_BIRTHDAY = LocalDate.of(1895, 12, 28);


    public Collection<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film create(Film film) {
        validateReleaseDate(film);
        return filmStorage.create(film);
    }

    public Film update(Film newFilm) {
        checkConditions(newFilm);
        validateReleaseDate(newFilm);
        return filmStorage.update(newFilm);
    }

    public Film addLike(Long filmId, Long userId) {
        userExists(userId);

        Film film = filmStorage.getFilmById(filmId);
        Set<Long> likes = film.getLikedUserIds();

        if (likes.contains(userId)) {
            log.warn("Пользователь с id = {} уже поставил лайк на этот фильм", userId);
            throw new ValidationException("Пользователь с id = " + userId + " уже поставил лайк на этот фильм");
        }

        log.info("Пользователь с id = {} поставил лайк на этот фильм", userId);
        likes.add(userId);

        return film;
    }

    public Film removeLike(Long filmId, Long userId) {
        userExists(userId);

        Film film = filmStorage.getFilmById(filmId);
        Set<Long> likes = film.getLikedUserIds();

        if (!likes.contains(userId)) {
            log.warn("Пользователь с id = {} не ставил лайк на этот фильм", userId);
            throw new ValidationException("Пользователь с id = " + userId + " не ставил лайк на этот фильм");
        }

        log.info("Пользователь с id = {} убрал лайк с этого фильма", userId);
        likes.remove(userId);

        return film;
    }

    public List<Film> getTopFilms(int count) {
        return findAll().stream()
                .sorted((f1, f2) -> Integer.compare(
                        f2.getLikedUserIds().size(),
                        f1.getLikedUserIds().size()
                ))
                .limit(count)
                .collect(Collectors.toList());
    }

    private void checkConditions(Film film) {
        if (film.getId() == null) {
            log.warn("Id не был указан");
            throw new ValidationException("Id должен быть указан");
        } else if (!filmStorage.containsFilm(film)) {
            log.warn("Фильм с id = {} не найден", film.getId());
            throw new NotFoundException("Фильм с id = " + film.getId() + " не найден");
        }
    }

    private void validateReleaseDate(Film film) {
        if (film.getReleaseDate().isBefore(CINEMAS_BIRTHDAY)) {
            log.warn("Передана некорректная дата релиза: {}", film.getReleaseDate());
            throw new ValidationException("дата релиза — не раньше 28 декабря 1895 года");
        }
    }

    private void userExists(Long id) {
        if (!userService.userExists(id)) {
            log.warn("Пользователь с id = {} не найден", id);
            throw new NotFoundException("Пользователь с id = " + id + " не найден");
        }
    }
}
