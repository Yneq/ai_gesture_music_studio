package com.vance.gesturemusicstudio.controller;

import com.vance.gesturemusicstudio.dto.gesture.GestureRequest;
import com.vance.gesturemusicstudio.dto.gesture.GestureResponse;
import com.vance.gesturemusicstudio.service.GestureService;
import com.vance.gesturemusicstudio.util.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GestureController {

    private final GestureService gestureService;

    /** Python AI 服務送離散指令手勢的端點 */
    @PostMapping("/gesture")
    public GestureResponse recordGesture(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody GestureRequest request
    ) {
        return gestureService.recordGesture(userDetails.getUser(), request);
    }

    @GetMapping("/gestures/recent")
    public List<GestureResponse> getRecentGestures(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return gestureService.getRecentGestures(userDetails.getUser(), limit);
    }
}
