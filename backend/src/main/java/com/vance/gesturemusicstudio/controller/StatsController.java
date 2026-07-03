package com.vance.gesturemusicstudio.controller;

import com.vance.gesturemusicstudio.dao.GestureHistoryDao;
import com.vance.gesturemusicstudio.dao.MusicEventDao;
import com.vance.gesturemusicstudio.dto.stats.StatsResponse;
import com.vance.gesturemusicstudio.util.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final MusicEventDao musicEventDao;
    private final GestureHistoryDao gestureHistoryDao;

    @GetMapping("/me")
    public StatsResponse getMyStats(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        var top1 = PageRequest.of(0, 1);

        long totalNotes    = musicEventDao.countByUserId(userId);
        long todayNotes    = musicEventDao.countByUserIdAndCreatedAtAfter(userId, LocalDate.now().atStartOfDay());
        long totalGestures = gestureHistoryDao.countByUserId(userId);

        List<Object[]> instruments = musicEventDao.findTopInstrument(userId, top1);
        String topInstrument      = instruments.isEmpty() ? null : (String) instruments.get(0)[0];
        long   topInstrumentCount = instruments.isEmpty() ? 0    : (long)   instruments.get(0)[1];

        List<Object[]> notes = musicEventDao.findTopNote(userId, top1);
        String topNote      = notes.isEmpty() ? null : (String) notes.get(0)[0];
        long   topNoteCount = notes.isEmpty() ? 0    : (long)   notes.get(0)[1];

        List<Object[]> gestures = gestureHistoryDao.findTopGesture(userId, top1);
        String topGesture      = gestures.isEmpty() ? null : (String) gestures.get(0)[0];
        long   topGestureCount = gestures.isEmpty() ? 0    : (long)   gestures.get(0)[1];

        return StatsResponse.builder()
                .totalNotes(totalNotes)
                .todayNotes(todayNotes)
                .totalGestures(totalGestures)
                .topInstrument(topInstrument)
                .topInstrumentCount(topInstrumentCount)
                .topNote(topNote)
                .topNoteCount(topNoteCount)
                .topGesture(topGesture)
                .topGestureCount(topGestureCount)
                .build();
    }
}
