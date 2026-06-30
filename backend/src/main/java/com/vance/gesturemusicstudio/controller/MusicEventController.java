package com.vance.gesturemusicstudio.controller;

import com.vance.gesturemusicstudio.dto.music.MusicEventRequest;
import com.vance.gesturemusicstudio.dto.music.MusicEventResponse;
import com.vance.gesturemusicstudio.service.MusicEventService;
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
@RequestMapping("/api/music-events")
@RequiredArgsConstructor
public class MusicEventController {

    private final MusicEventService musicEventService;

    @PostMapping
    public MusicEventResponse recordEvent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MusicEventRequest request
    ) {
        return musicEventService.recordEvent(userDetails.getUser(), request);
    }

    @GetMapping("/recent")
    public List<MusicEventResponse> getRecentEvents(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return musicEventService.getRecentEvents(userDetails.getUser(), limit);
    }
}
