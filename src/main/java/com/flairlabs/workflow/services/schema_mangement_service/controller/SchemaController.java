package com.flairlabs.workflow.services.schema_mangement_service.controller;

import com.flairlabs.workflow.services.schema_mangement_service.dtos.EntityDefinitionDto;
import com.flairlabs.workflow.services.schema_mangement_service.service.EntityDefinitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/schema")
public class SchemaController {

    @Autowired
    EntityDefinitionService entityDefinitionService;

    @PostMapping()
    public ResponseEntity<String> createSchema(@RequestBody EntityDefinitionDto entity  ) {
        entityDefinitionService.createEntity(entity);
        return ResponseEntity.ok("Schemaa created successfully");
    }

}
