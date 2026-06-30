package com.vance.gesturemusicstudio.dto.layout;

import com.vance.gesturemusicstudio.model.FavoriteLayout;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LayoutResponse {

    private Long id;
    private String layoutJson;

    public static LayoutResponse from(FavoriteLayout layout) {
        return LayoutResponse.builder()
                .id(layout.getId())
                .layoutJson(layout.getLayoutJson())
                .build();
    }
}
