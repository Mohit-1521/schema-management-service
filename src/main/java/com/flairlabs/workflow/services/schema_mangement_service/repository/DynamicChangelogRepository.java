package com.flairlabs.workflow.services.schema_mangement_service.repository;

import com.flairlabs.workflow.services.schema_mangement_service.models.DynamicChangelog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DynamicChangelogRepository extends JpaRepository<DynamicChangelog, Long> {

    List<DynamicChangelog> findByEntityNameOrderByAppliedAtDesc(String entityName);

    List<DynamicChangelog> findByEntityNameAndFieldNameOrderByAppliedAtDesc(String entityName, String fieldName);
}
