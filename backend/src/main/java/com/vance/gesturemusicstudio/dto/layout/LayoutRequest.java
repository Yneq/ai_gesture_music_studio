package com.vance.gesturemusicstudio.dto.layout;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LayoutRequest {

    @NotBlank
    private String layoutJson;
}
