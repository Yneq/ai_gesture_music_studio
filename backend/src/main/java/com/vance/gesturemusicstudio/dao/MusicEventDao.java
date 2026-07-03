package com.vance.gesturemusicstudio.dao;

import com.vance.gesturemusicstudio.model.MusicEvent;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface MusicEventDao {

    List<MusicEvent> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserId(Long userId);

    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);

    List<Object[]> findTopInstrument(Long userId, Pageable pageable);

    List<Object[]> findTopNote(Long userId, Pageable pageable);

    MusicEvent save(MusicEvent musicEvent);
}
