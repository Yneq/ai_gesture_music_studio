package com.vance.gesturemusicstudio.dto.music;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MusicEventRequest {

    @NotBlank
    @Size(max = 10)
    private String note;

    @NotBlank
    @Size(max = 20)
    private String instrument;

    @NotNull
    @Min(0)
    @Max(127)
    private Integer volume;
}
