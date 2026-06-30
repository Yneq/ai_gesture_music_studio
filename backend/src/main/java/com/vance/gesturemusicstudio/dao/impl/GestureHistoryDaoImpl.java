package com.vance.gesturemusicstudio.dao.impl;

import com.vance.gesturemusicstudio.dao.GestureHistoryDao;
import com.vance.gesturemusicstudio.model.GestureHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GestureHistoryDaoImpl implements GestureHistoryDao {

    private final GestureHistoryJpaRepository gestureHistoryJpaRepository;

    @Override
    public List<GestureHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable) {
        return gestureHistoryJpaRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    public Optional<GestureHistory> findFirstByUserIdOrderByCreatedAtDesc(Long userId) {
        return gestureHistoryJpaRepository.findFirstByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public GestureHistory save(GestureHistory gestureHistory) {
        return gestureHistoryJpaRepository.save(gestureHistory);
    }
}

interface GestureHistoryJpaRepository extends JpaRepository<GestureHistory, Long> {

    List<GestureHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<GestureHistory> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
}
