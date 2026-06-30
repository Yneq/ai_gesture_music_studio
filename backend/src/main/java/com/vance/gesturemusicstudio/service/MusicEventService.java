package com.vance.gesturemusicstudio.service;

import com.vance.gesturemusicstudio.dto.music.MusicEventRequest;
import com.vance.gesturemusicstudio.dto.music.MusicEventResponse;
import com.vance.gesturemusicstudio.model.User;

import java.util.List;

public interface MusicEventService {

    MusicEventResponse recordEvent(User user, MusicEventRequest request);

    List<MusicEventResponse> getRecentEvents(User user, int limit);
}
