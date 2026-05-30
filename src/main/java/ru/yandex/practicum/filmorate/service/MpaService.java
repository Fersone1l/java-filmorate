package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.MpaStorage;

import java.util.Collection;

@Service
@RequiredArgsConstructor
@Slf4j
public class MpaService {
    private final MpaStorage mpaStorage;

    public Collection<Mpa> getAllMpa() {
       return mpaStorage.getAllMpa();
    }

    public Mpa getMpaById(Long id) {
        return mpaStorage.getMpaById(id);
    }
}
