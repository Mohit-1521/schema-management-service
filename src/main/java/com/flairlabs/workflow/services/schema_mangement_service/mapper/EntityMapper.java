package com.flairlabs.workflow.services.schema_mangement_service.mapper;

import com.flairlabs.workflow.services.schema_mangement_service.dtos.EntityDefinitionDto;
import com.flairlabs.workflow.services.schema_mangement_service.dtos.FieldDefinitionDto;
import com.flairlabs.workflow.services.schema_mangement_service.models.EntityDefinition;
import com.flairlabs.workflow.services.schema_mangement_service.models.FieldDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EntityMapper {

    public EntityDefinitionDto toDto(EntityDefinition entity) {
        if (entity == null) {
            return null;
        }

        return EntityDefinitionDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .fields(entity.getFields().stream()
                        .map(this::toFieldDto)
                        .collect(Collectors.toList()))
                .build();
    }

    public List<EntityDefinitionDto> toDtoList(List<EntityDefinition> entities) {
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public EntityDefinition toEntity(EntityDefinitionDto dto) {
        if (dto == null) {
            return null;
        }

        EntityDefinition entity = EntityDefinition.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .build();

        // Process fields separately to properly establish relationships
        if (dto.getFields() != null) {
            dto.getFields().forEach(fieldDto -> {
                FieldDefinition field = toField(fieldDto);
                entity.addField(field);
            });
        }

        return entity;
    }

    public FieldDefinitionDto toFieldDto(FieldDefinition field) {
        if (field == null) {
            return null;
        }

        return FieldDefinitionDto.builder()
                .id(field.getId())
                .name(field.getName())
                .fieldType(field.getFieldType())
                .required(field.getRequired())
                .maxLength(field.getMaxLength())
                .defaultValue(field.getDefaultValue())
                .build();
    }

    public FieldDefinition toField(FieldDefinitionDto dto) {
        if (dto == null) {
            return null;
        }

        return FieldDefinition.builder()
                .id(dto.getId())
                .name(dto.getName())
                .fieldType(dto.getFieldType())
                .required(dto.getRequired())
                .maxLength(dto.getMaxLength())
                .defaultValue(dto.getDefaultValue())
                .build();
    }

    public void updateEntityFromDto(EntityDefinitionDto dto, EntityDefinition entity) {
        // Update basic properties
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());

        // Fields are handled separately in the service
    }

    public void updateFieldFromDto(FieldDefinitionDto dto, FieldDefinition field) {
        field.setName(dto.getName());
        field.setFieldType(dto.getFieldType());
        field.setRequired(dto.getRequired());
        field.setMaxLength(dto.getMaxLength());
        field.setDefaultValue(dto.getDefaultValue());
    }
}


