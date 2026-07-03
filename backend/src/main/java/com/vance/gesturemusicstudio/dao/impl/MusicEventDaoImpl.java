package com.vance.gesturemusicstudio.dao.impl;

import com.vance.gesturemusicstudio.dao.MusicEventDao;
import com.vance.gesturemusicstudio.model.MusicEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MusicEventDaoImpl implements MusicEventDao {

    private final MusicEventJpaRepository repo;

    @Override
    public List<MusicEvent> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    public long countByUserId(Long userId) {
        return repo.countByUserId(userId);
    }

    @Override
    public long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after) {
        return repo.countByUserIdAndCreatedAtAfter(userId, after);
    }

    @Override
    public List<Object[]> findTopInstrument(Long userId, Pageable pageable) {
        return repo.findTopInstrumentByUserId(userId, pageable);
    }

    @Override
    public List<Object[]> findTopNote(Long userId, Pageable pageable) {
        return repo.findTopNoteByUserId(userId, pageable);
    }

    @Override
    public MusicEvent save(MusicEvent musicEvent) {
        return repo.save(musicEvent);
    }
}

interface MusicEventJpaRepository extends JpaRepository<MusicEvent, Long> {

    List<MusicEvent> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT COUNT(e) FROM MusicEvent e WHERE e.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(e) FROM MusicEvent e WHERE e.user.id = :userId AND e.createdAt >= :after")
    long countByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("after") LocalDateTime after);

    @Query("SELECT e.instrument, COUNT(e) AS cnt FROM MusicEvent e WHERE e.user.id = :userId GROUP BY e.instrument ORDER BY cnt DESC")
    List<Object[]> findTopInstrumentByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT e.note, COUNT(e) AS cnt FROM MusicEvent e WHERE e.user.id = :userId GROUP BY e.note ORDER BY cnt DESC")
    List<Object[]> findTopNoteByUserId(@Param("userId") Long userId, Pageable pageable);
}
