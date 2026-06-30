package com.vance.gesturemusicstudio.dao.impl;

import com.vance.gesturemusicstudio.dao.FavoriteLayoutDao;
import com.vance.gesturemusicstudio.model.FavoriteLayout;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FavoriteLayoutDaoImpl implements FavoriteLayoutDao {

    private final FavoriteLayoutJpaRepository favoriteLayoutJpaRepository;

    @Override
    public List<FavoriteLayout> findByUserId(Long userId) {
        return favoriteLayoutJpaRepository.findByUserId(userId);
    }

    @Override
    public Optional<FavoriteLayout> findByIdAndUserId(Long id, Long userId) {
        return favoriteLayoutJpaRepository.findByIdAndUserId(id, userId);
    }

    @Override
    public FavoriteLayout save(FavoriteLayout favoriteLayout) {
        return favoriteLayoutJpaRepository.save(favoriteLayout);
    }

    @Override
    public void delete(FavoriteLayout favoriteLayout) {
        favoriteLayoutJpaRepository.delete(favoriteLayout);
    }
}

interface FavoriteLayoutJpaRepository extends JpaRepository<FavoriteLayout, Long> {

    List<FavoriteLayout> findByUserId(Long userId);

    Optional<FavoriteLayout> findByIdAndUserId(Long id, Long userId);
}
