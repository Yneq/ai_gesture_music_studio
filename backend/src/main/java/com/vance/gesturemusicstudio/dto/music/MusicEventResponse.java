package com.vance.gesturemusicstudio.dto.music;

import com.vance.gesturemusicstudio.model.MusicEvent;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MusicEventResponse {

    private Long id;
    private String username;
    private String note;
    private String instrument;
    private Integer volume;
    private LocalDateTime createdAt;

    public static MusicEventResponse from(MusicEvent event) {
        return MusicEventResponse.builder()
                .id(event.getId())
                .username(event.getUser().getUsername())
                .note(event.getNote())
                .instrument(event.getInstrument())
                .volume(event.getVolume())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
