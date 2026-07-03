package com.vance.gesturemusicstudio.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Controller
public class PresenceController {

    // In-memory set — order preserved, thread-safe
    private final Set<String> onlineUsers = Collections.synchronizedSet(new LinkedHashSet<>());

    @MessageMapping("/presence/join")
    @SendTo("/topic/presence")
    public Map<String, Object> join(@Payload Map<String, String> payload) {
        String username = payload.getOrDefault("username", "?");
        onlineUsers.add(username);
        return Map.of(
                "type", "join",
                "username", username,
                "onlineUsers", new ArrayList<>(onlineUsers),
                "timestamp", Instant.now().toEpochMilli()
        );
    }

    @MessageMapping("/presence/leave")
    @SendTo("/topic/presence")
    public Map<String, Object> leave(@Payload Map<String, String> payload) {
        String username = payload.getOrDefault("username", "?");
        onlineUsers.remove(username);
        return Map.of(
                "type", "leave",
                "username", username,
                "onlineUsers", new ArrayList<>(onlineUsers),
                "timestamp", Instant.now().toEpochMilli()
        );
    }
}
