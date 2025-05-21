package com.flairlabs.workflow.services.schema_mangement_service.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "field_definition", uniqueConstraints = {
        @UniqueConstraint(name = "uk_field_entity_name", columnNames = {"entity_id", "name"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", nullable = false)
    @JsonIgnore
    private EntityDefinition entity;

    @Column(nullable = false)
    private String name;

    @Column(name = "field_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private FieldType fieldType;

    private Boolean required;

    @Column(name = "max_length")
    private Integer maxLength;

    @Column(name = "default_value")
    private String defaultValue;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum FieldType {
        TEXT,
        NUMBER,
        BOOLEAN,
        DATE,
        DATETIME,
        EMAIL,
        PHONE,
        URL,
        DECIMAL
    }
}

