package com.vance.gesturemusicstudio.service;

import com.vance.gesturemusicstudio.dto.gesture.GestureRequest;
import com.vance.gesturemusicstudio.dto.gesture.GestureResponse;
import com.vance.gesturemusicstudio.model.User;

import java.util.List;

public interface GestureService {

    GestureResponse recordGesture(User user, GestureRequest request);

    List<GestureResponse> getRecentGestures(User user, int limit);
}
