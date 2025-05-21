package com.flairlabs.workflow.services.schema_mangement_service.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "dynamic_changelog")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicChangelog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "change_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ChangeType changeType;

    @Column(name = "entity_name", nullable = false)
    private String entityName;

    @Column(name = "field_name")
    private String fieldName;

    @Column(name = "field_type")
    private String fieldType;

    @Column(name = "applied_at")
    @CreationTimestamp
    private LocalDateTime appliedAt;

    @Column(name = "applied_by")
    private String appliedBy;

    @Column(name = "change_details")
    private String changeDetails;

    public enum ChangeType {
        CREATE_ENTITY,
        DROP_ENTITY,
        ADD_FIELD,
        MODIFY_FIELD,
        DROP_FIELD
    }
}

