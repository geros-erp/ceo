package com.geros.backend.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitOfMeasureDTO {
    private Long id;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 100)
    private String description;

    @NotBlank(message = "La abreviatura es obligatoria")
    @Size(max = 10)
    private String abbreviation;

    private Boolean isActive;

    private Boolean allowsDecimal;
}
