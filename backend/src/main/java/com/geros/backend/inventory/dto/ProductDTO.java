package com.geros.backend.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ProductDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "El código es obligatorio")
        @Size(max = 50)
        private String code;

        @NotBlank(message = "La descripción es obligatoria")
        private String description;

        @NotNull(message = "La unidad de medida es obligatoria")
        private Long unitOfMeasureId;

        private Boolean requiresSerial = false;
        private Boolean isActive = true;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String code;
        private String description;
        private Long unitOfMeasureId;
        private String unitOfMeasureName;
        private Boolean requiresSerial;
        private Boolean isActive;
    }
}
