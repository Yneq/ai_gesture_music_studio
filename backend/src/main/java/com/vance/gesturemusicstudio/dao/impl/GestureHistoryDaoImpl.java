package com.vance.gesturemusicstudio.dao.impl;

import com.vance.gesturemusicstudio.dao.GestureHistoryDao;
import com.vance.gesturemusicstudio.model.GestureHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GestureHistoryDaoImpl implements GestureHistoryDao {

    private final GestureHistoryJpaRepository repo;

    @Override
    public List<GestureHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    public Optional<GestureHistory> findFirstByUserIdOrderByCreatedAtDesc(Long userId) {
        return repo.findFirstByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public long countByUserId(Long userId) {
        return repo.countByUserId(userId);
    }

    @Override
    public List<Object[]> findTopGesture(Long userId, Pageable pageable) {
        return repo.findTopGestureByUserId(userId, pageable);
    }

    @Override
    public GestureHistory save(GestureHistory gestureHistory) {
        return repo.save(gestureHistory);
    }
}

interface GestureHistoryJpaRepository extends JpaRepository<GestureHistory, Long> {

    List<GestureHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<GestureHistory> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COUNT(g) FROM GestureHistory g WHERE g.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT g.gesture, COUNT(g) AS cnt FROM GestureHistory g WHERE g.user.id = :userId GROUP BY g.gesture ORDER BY cnt DESC")
    List<Object[]> findTopGestureByUserId(@Param("userId") Long userId, Pageable pageable);
}
