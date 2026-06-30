package com.vance.gesturemusicstudio.dto.gesture;

import com.vance.gesturemusicstudio.model.GestureHistory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GestureResponse {

    private Long id;
    private String gesture;
    private Double confidence;
    private LocalDateTime createdAt;
    private boolean saved;
    private String reason;

    public static GestureResponse from(GestureHistory history) {
        return GestureResponse.builder()
                .id(history.getId())
                .gesture(history.getGesture())
                .confidence(history.getConfidence())
                .createdAt(history.getCreatedAt())
                .saved(true)
                .build();
    }

    public static GestureResponse debounced(String reason) {
        return GestureResponse.builder()
                .saved(false)
                .reason(reason)
                .build();
    }
}
