package com.vance.gesturemusicstudio.service.impl;

import com.vance.gesturemusicstudio.dao.MusicEventDao;
import com.vance.gesturemusicstudio.dto.music.MusicEventRequest;
import com.vance.gesturemusicstudio.dto.music.MusicEventResponse;
import com.vance.gesturemusicstudio.model.MusicEvent;
import com.vance.gesturemusicstudio.model.User;
import com.vance.gesturemusicstudio.exception.ApiException;
import com.vance.gesturemusicstudio.service.MusicEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MusicEventServiceImpl implements MusicEventService {

    private static final Set<String> VALID_INSTRUMENTS = Set.of(
            "piano", "guitar", "synth", "drum"
    );

    private final MusicEventDao musicEventDao;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public MusicEventResponse recordEvent(User user, MusicEventRequest request) {
        String instrument = request.getInstrument().toLowerCase();

        if (!VALID_INSTRUMENTS.contains(instrument)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "不支援的樂器: " + instrument);
        }

        MusicEvent event = MusicEvent.builder()
                .user(user)
                .note(request.getNote().toUpperCase())
                .instrument(instrument)
                .volume(request.getVolume())
                .build();

        MusicEventResponse response = MusicEventResponse.from(musicEventDao.save(event));
        messagingTemplate.convertAndSend("/topic/notes", response);
        return response;
    }

    @Override
    public List<MusicEventResponse> getRecentEvents(User user, int limit) {
        return musicEventDao
                .findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, limit))
                .stream()
                .map(MusicEventResponse::from)
                .toList();
    }
}
