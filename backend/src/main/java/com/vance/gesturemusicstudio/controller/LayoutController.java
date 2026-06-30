package com.vance.gesturemusicstudio.controller;

import com.vance.gesturemusicstudio.dto.layout.LayoutRequest;
import com.vance.gesturemusicstudio.dto.layout.LayoutResponse;
import com.vance.gesturemusicstudio.service.LayoutService;
import com.vance.gesturemusicstudio.util.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/layouts")
@RequiredArgsConstructor
public class LayoutController {

    private final LayoutService layoutService;

    @GetMapping
    public List<LayoutResponse> listLayouts(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return layoutService.listLayouts(userDetails.getUser());
    }

    @GetMapping("/{id}")
    public LayoutResponse getLayout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id
    ) {
        return layoutService.getLayout(userDetails.getUser(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LayoutResponse createLayout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody LayoutRequest request
    ) {
        return layoutService.createLayout(userDetails.getUser(), request);
    }

    @PutMapping("/{id}")
    public LayoutResponse updateLayout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody LayoutRequest request
    ) {
        return layoutService.updateLayout(userDetails.getUser(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLayout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id
    ) {
        layoutService.deleteLayout(userDetails.getUser(), id);
    }
}
