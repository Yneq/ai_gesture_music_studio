package com.vance.gesturemusicstudio.dao;

import com.vance.gesturemusicstudio.model.User;

import java.util.Optional;

public interface UserDao {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findById(Long id);

    User save(User user);
}
