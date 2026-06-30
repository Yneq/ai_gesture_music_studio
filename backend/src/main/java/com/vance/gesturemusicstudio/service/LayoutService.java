package com.vance.gesturemusicstudio.service;

import com.vance.gesturemusicstudio.dto.layout.LayoutRequest;
import com.vance.gesturemusicstudio.dto.layout.LayoutResponse;
import com.vance.gesturemusicstudio.model.User;

import java.util.List;

public interface LayoutService {

    List<LayoutResponse> listLayouts(User user);

    LayoutResponse getLayout(User user, Long id);

    LayoutResponse createLayout(User user, LayoutRequest request);

    LayoutResponse updateLayout(User user, Long id, LayoutRequest request);

    void deleteLayout(User user, Long id);
}
