package com.vance.gesturemusicstudio.dao;

import com.vance.gesturemusicstudio.model.FavoriteLayout;

import java.util.List;
import java.util.Optional;

public interface FavoriteLayoutDao {

    List<FavoriteLayout> findByUserId(Long userId);

    Optional<FavoriteLayout> findByIdAndUserId(Long id, Long userId);

    FavoriteLayout save(FavoriteLayout favoriteLayout);

    void delete(FavoriteLayout favoriteLayout);
}
