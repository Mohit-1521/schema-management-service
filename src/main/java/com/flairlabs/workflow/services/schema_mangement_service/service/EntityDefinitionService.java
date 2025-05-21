package com.flairlabs.workflow.services.schema_mangement_service.service;

import com.flairlabs.workflow.services.schema_mangement_service.dtos.EntityDefinitionDto;
import com.flairlabs.workflow.services.schema_mangement_service.dtos.FieldDefinitionDto;
import com.flairlabs.workflow.services.schema_mangement_service.mapper.EntityMapper;
import com.flairlabs.workflow.services.schema_mangement_service.models.EntityDefinition;
import com.flairlabs.workflow.services.schema_mangement_service.models.FieldDefinition;
import com.flairlabs.workflow.services.schema_mangement_service.repository.EntityDefinitionRepository;
import com.flairlabs.workflow.services.schema_mangement_service.repository.FieldDefinitionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntityDefinitionService {

    private final EntityDefinitionRepository entityRepository;
    private final FieldDefinitionRepository fieldRepository;
    private final SchemaService schemaService;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public List<EntityDefinitionDto> getAllEntities() {
        return mapper.toDtoList(entityRepository.findAll());
    }

    @Transactional(readOnly = true)
    public EntityDefinitionDto getEntityById(Long id) {
        return mapper.toDto(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public EntityDefinitionDto getEntityByName(String name) {
        return mapper.toDto(findEntityByName(name));
    }

    @Transactional
    public EntityDefinitionDto createEntity(EntityDefinitionDto entityDto) {
        // Validate the entity name doesn't already exist
        if (entityRepository.existsByName(entityDto.getName())) {
            throw new IllegalArgumentException("Entity with name '" + entityDto.getName() + "' already exists");
        }

        // Convert to entity
        EntityDefinition entity = mapper.toEntity(entityDto);
        // Save the entity definition first
        entity = entityRepository.save(entity);

        // Create the corresponding database table
        schemaService.createEntityTable(entity);

        return mapper.toDto(entity);
    }

    @Transactional
    public EntityDefinitionDto updateEntity(Long id, EntityDefinitionDto entityDto) {
        EntityDefinition existingEntity = findEntityById(id);

        // If name is changing, make sure it doesn't conflict with another entity
        if (!existingEntity.getName().equals(entityDto.getName()) &&
                entityRepository.existsByName(entityDto.getName())) {
            throw new IllegalArgumentException("Entity with name '" + entityDto.getName() + "' already exists");
        }

        // Handle renamed entity here if needed (database renaming is complex and not implemented in this example)
        if (!existingEntity.getName().equals(entityDto.getName())) {
            log.warn("Entity renaming is not fully supported. Only metadata will be updated, not the actual table name.");
        }

        // Update basic properties
        existingEntity.setName(entityDto.getName());
        existingEntity.setDescription(entityDto.getDescription());

        // Handle field updates
        updateEntityFields(existingEntity, entityDto.getFields());

        // Save changes
        existingEntity = entityRepository.save(existingEntity);

        return mapper.toDto(existingEntity);
    }

    @Transactional
    public void deleteEntity(Long id) {
        EntityDefinition entity = findEntityById(id);

        // Drop the database table first
        schemaService.dropEntityTable(entity);

        // Delete the entity definition
        entityRepository.delete(entity);
    }

    @Transactional
    public EntityDefinitionDto addFieldToEntity(Long entityId, FieldDefinitionDto fieldDto) {
        EntityDefinition entity = findEntityById(entityId);

        // Validate the field name doesn't already exist for this entity
        if (fieldRepository.existsByEntityAndName(entity, fieldDto.getName())) {
            throw new IllegalArgumentException("Field with name '" + fieldDto.getName() +
                    "' already exists for entity '" + entity.getName() + "'");
        }

        // Convert and link the field to the entity
        FieldDefinition field = mapper.toField(fieldDto);
        entity.addField(field);

        // Save the field
        field = fieldRepository.save(field);

        // Add the field to the database table
        schemaService.addFieldToEntity(entity, field);

        return mapper.toDto(entity);
    }

    @Transactional
    public EntityDefinitionDto updateField(Long entityId, Long fieldId, FieldDefinitionDto fieldDto) {
        EntityDefinition entity = findEntityById(entityId);
        FieldDefinition existingField = findFieldByIdAndEntityId(fieldId, entityId);

        // If name is changing, make sure it doesn't conflict with another field in the same entity
        if (!existingField.getName().equals(fieldDto.getName()) &&
                fieldRepository.existsByEntityAndName(entity, fieldDto.getName())) {
            throw new IllegalArgumentException("Field with name '" + fieldDto.getName() +
                    "' already exists for entity '" + entity.getName() + "'");
        }

        // Create a copy of the existing field for schema update
        FieldDefinition oldField = FieldDefinition.builder()
                .id(existingField.getId())
                .name(existingField.getName())
                .fieldType(existingField.getFieldType())
                .required(existingField.getRequired())
                .maxLength(existingField.getMaxLength())
                .defaultValue(existingField.getDefaultValue())
                .entity(existingField.getEntity())
                .build();

        // Update the field
        mapper.updateFieldFromDto(fieldDto, existingField);

        // Save the field
        existingField = fieldRepository.save(existingField);

        // Modify the field in the database table
        schemaService.modifyFieldInEntity(entity, oldField, existingField);

        return mapper.toDto(entity);
    }

    @Transactional
    public EntityDefinitionDto deleteField(Long entityId, Long fieldId) {
        EntityDefinition entity = findEntityById(entityId);
        FieldDefinition field = findFieldByIdAndEntityId(fieldId, entityId);

        // Remove the field from the database table
        schemaService.dropFieldFromEntity(entity, field);

        // Remove the field from the entity and delete it
        entity.removeField(field);
        fieldRepository.delete(field);

        return mapper.toDto(entity);
    }

    // Helper method to find an entity by ID
    private EntityDefinition findEntityById(Long id) {
        return entityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found with id: " + id));
    }

    // Helper method to find an entity by name
    private EntityDefinition findEntityByName(String name) {
        return entityRepository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found with name: " + name));
    }

    // Helper method to find a field by ID and entity ID
    private FieldDefinition findFieldByIdAndEntityId(Long fieldId, Long entityId) {
        FieldDefinition field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new EntityNotFoundException("Field not found with id: " + fieldId));

        if (!field.getEntity().getId().equals(entityId)) {
            throw new IllegalArgumentException("Field with id " + fieldId +
                    " does not belong to entity with id " + entityId);
        }

        return field;
    }

    // Helper method to handle field updates during entity update
    private void updateEntityFields(EntityDefinition entity, List<FieldDefinitionDto> fieldDtos) {
        // Create maps for easier lookup
        Map<Long, FieldDefinition> existingFields = entity.getFields().stream()
                .collect(Collectors.toMap(FieldDefinition::getId, f -> f));

        Map<Long, FieldDefinitionDto> updatedFields = fieldDtos.stream()
                .filter(f -> f.getId() != null)
                .collect(Collectors.toMap(FieldDefinitionDto::getId, f -> f));

        List<FieldDefinitionDto> newFields = fieldDtos.stream()
                .filter(f -> f.getId() == null)
                .collect(Collectors.toList());

        // Find fields to delete (in existing but not in updated)
        Set<Long> fieldsToDelete = new HashSet<>(existingFields.keySet());
        fieldsToDelete.removeAll(updatedFields.keySet());

        // Delete fields that are no longer present
        for (Long fieldId : fieldsToDelete) {
            FieldDefinition fieldToDelete = existingFields.get(fieldId);
            schemaService.dropFieldFromEntity(entity, fieldToDelete);
            entity.removeField(fieldToDelete);
            fieldRepository.delete(fieldToDelete);
        }

        // Update existing fields
        for (Map.Entry<Long, FieldDefinitionDto> entry : updatedFields.entrySet()) {
            FieldDefinition existingField = existingFields.get(entry.getKey());
            FieldDefinitionDto updatedFieldDto = entry.getValue();

            // Create a copy of the existing field for schema update
            FieldDefinition oldField = FieldDefinition.builder()
                    .id(existingField.getId())
                    .name(existingField.getName())
                    .fieldType(existingField.getFieldType())
                    .required(existingField.getRequired())
                    .maxLength(existingField.getMaxLength())
                    .defaultValue(existingField.getDefaultValue())
                    .entity(existingField.getEntity())
                    .build();

            // Update the field
            mapper.updateFieldFromDto(updatedFieldDto, existingField);

            // Modify the field in the database table if needed
            if (!oldField.getName().equals(existingField.getName()) ||
                    !oldField.getFieldType().equals(existingField.getFieldType()) ||
                    !Objects.equals(oldField.getRequired(), existingField.getRequired()) ||
                    !Objects.equals(oldField.getMaxLength(), existingField.getMaxLength())) {
                schemaService.modifyFieldInEntity(entity, oldField, existingField);
            }
        }

        // Add new fields
        for (FieldDefinitionDto newFieldDto : newFields) {
            // Check for duplicate field names
            if (entity.getFields().stream()
                    .anyMatch(f -> f.getName().equals(newFieldDto.getName()))) {
                throw new IllegalArgumentException("Field with name '" + newFieldDto.getName() +
                        "' already exists for entity '" + entity.getName() + "'");
            }

            FieldDefinition newField = mapper.toField(newFieldDto);
            entity.addField(newField);
            fieldRepository.save(newField);
            schemaService.addFieldToEntity(entity, newField);
        }
    }
}

