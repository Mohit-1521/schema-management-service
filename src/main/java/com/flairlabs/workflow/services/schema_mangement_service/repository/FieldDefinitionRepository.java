package com.flairlabs.workflow.services.schema_mangement_service.repository;

import com.flairlabs.workflow.services.schema_mangement_service.models.EntityDefinition;
import com.flairlabs.workflow.services.schema_mangement_service.models.FieldDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FieldDefinitionRepository extends JpaRepository<FieldDefinition, Long> {

    List<FieldDefinition> findByEntityId(Long entityId);

    Optional<FieldDefinition> findByEntityIdAndName(Long entityId, String name);

    boolean existsByEntityAndName(EntityDefinition entity, String name);
}
