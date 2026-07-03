package com.vance.gesturemusicstudio.dto.stats;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StatsResponse {
    private long totalNotes;
    private long todayNotes;
    private long totalGestures;
    private String topInstrument;
    private long topInstrumentCount;
    private String topNote;
    private long topNoteCount;
    private String topGesture;
    private long topGestureCount;
}
