package com.vance.gesturemusicstudio.dao;

import com.vance.gesturemusicstudio.model.MusicEvent;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MusicEventDao {

    List<MusicEvent> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    MusicEvent save(MusicEvent musicEvent);
}
