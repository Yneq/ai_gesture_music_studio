package com.vance.gesturemusicstudio.dao.impl;

import com.vance.gesturemusicstudio.dao.MusicEventDao;
import com.vance.gesturemusicstudio.model.MusicEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MusicEventDaoImpl implements MusicEventDao {

    private final MusicEventJpaRepository musicEventJpaRepository;

    @Override
    public List<MusicEvent> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable) {
        return musicEventJpaRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    public MusicEvent save(MusicEvent musicEvent) {
        return musicEventJpaRepository.save(musicEvent);
    }
}

interface MusicEventJpaRepository extends JpaRepository<MusicEvent, Long> {

    List<MusicEvent> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
