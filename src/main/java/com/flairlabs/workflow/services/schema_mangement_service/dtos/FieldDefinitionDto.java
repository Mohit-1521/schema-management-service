package com.flairlabs.workflow.services.schema_mangement_service.dtos;

import com.flairlabs.workflow.services.schema_mangement_service.models.FieldDefinition;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldDefinitionDto {

    private Long id;

    @NotBlank(message = "Field name is required")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "Field name must start with a letter and contain only letters, numbers, and underscores")
    @Size(min = 2, max = 50, message = "Field name must be between 2 and 50 characters")
    private String name;

    @NotNull(message = "Field type is required")
    private FieldDefinition.FieldType fieldType;

    @Builder.Default
    private Boolean required = false;

    @Min(value = 1, message = "Maximum length must be at least 1")
    @Max(value = 4000, message = "Maximum length cannot exceed 4000")
    private Integer maxLength;

    private String defaultValue;
}

