package com.flairlabs.workflow.services.schema_mangement_service.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import org.springframework.lang.NonNull;

@AllArgsConstructor
@NoArgsConstructor
@MappedSuperclass
@Getter
@Setter
public class TenantBaseModel {

     @JsonProperty("tenant_id")
     @Column(name="tenant_id")
     @NonNull
     @TenantId // discriminator annotation
     private String tenantId;
}
