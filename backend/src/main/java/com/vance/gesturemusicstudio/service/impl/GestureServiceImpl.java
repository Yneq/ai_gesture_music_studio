package com.vance.gesturemusicstudio.service.impl;

import com.vance.gesturemusicstudio.dao.GestureHistoryDao;
import com.vance.gesturemusicstudio.dto.gesture.GestureRequest;
import com.vance.gesturemusicstudio.dto.gesture.GestureResponse;
import com.vance.gesturemusicstudio.model.GestureHistory;
import com.vance.gesturemusicstudio.model.User;
import com.vance.gesturemusicstudio.exception.ApiException;
import com.vance.gesturemusicstudio.service.GestureService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GestureServiceImpl implements GestureService {

    private static final Set<String> VALID_GESTURES = Set.of(
            "OPEN_HAND", "FIST", "THUMB_UP", "OK", "PEACE", "POINT"
    );

    private final GestureHistoryDao gestureHistoryDao;

    @Value("${app.gesture.debounce-cooldown-ms}")
    private long debounceCooldownMs;

    @Override
    @Transactional
    public GestureResponse recordGesture(User user, GestureRequest request) {
        String gesture = request.getGesture().toUpperCase();

        if (!VALID_GESTURES.contains(gesture)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "不支援的手勢類型: " + gesture);
        }

        if (request.getConfidence() < 0.85) {
            return GestureResponse.debounced("confidence_below_threshold");
        }

        var lastGesture = gestureHistoryDao.findFirstByUserIdOrderByCreatedAtDesc(user.getId());
        if (lastGesture.isPresent()) {
            long elapsedMs = Duration.between(
                    lastGesture.get().getCreatedAt(),
                    LocalDateTime.now()
            ).toMillis();

            if (elapsedMs < debounceCooldownMs) {
                return GestureResponse.debounced("debounce_cooldown");
            }
        }

        GestureHistory history = GestureHistory.builder()
                .user(user)
                .gesture(gesture)
                .confidence(request.getConfidence())
                .build();

        return GestureResponse.from(gestureHistoryDao.save(history));
    }

    @Override
    public List<GestureResponse> getRecentGestures(User user, int limit) {
        return gestureHistoryDao
                .findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, limit))
                .stream()
                .map(GestureResponse::from)
                .toList();
    }
}
