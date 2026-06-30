package com.vance.gesturemusicstudio.dao;

import com.vance.gesturemusicstudio.model.GestureHistory;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface GestureHistoryDao {

    List<GestureHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<GestureHistory> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    GestureHistory save(GestureHistory gestureHistory);
}
