package com.flairlabs.workflow.services.schema_mangement_service.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityDefinitionDto {

    private Long id;

    @NotBlank(message = "Entity name is required")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "Entity name must start with a letter and contain only letters, numbers, and underscores")
    @Size(min = 2, max = 50, message = "Entity name must be between 2 and 50 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @Valid
    @Builder.Default
    private List<FieldDefinitionDto> fields = new ArrayList<>();
}


