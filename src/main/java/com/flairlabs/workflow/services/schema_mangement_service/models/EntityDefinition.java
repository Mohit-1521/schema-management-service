package com.flairlabs.workflow.services.schema_mangement_service.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "entity_definition")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @OneToMany(mappedBy = "entity", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FieldDefinition> fields = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper method to add a field
    public void addField(FieldDefinition field) {
        fields.add(field);
        field.setEntity(this);
    }

    // Helper method to remove a field
    public void removeField(FieldDefinition field) {
        fields.remove(field);
        field.setEntity(null);
    }
}

