package com.flairlabs.workflow.services.schema_mangement_service.repository;

import com.flairlabs.workflow.services.schema_mangement_service.models.EntityDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EntityDefinitionRepository extends JpaRepository<EntityDefinition, Long> {

    boolean existsByName(String name);

    Optional<EntityDefinition> findByName(String name);
}
