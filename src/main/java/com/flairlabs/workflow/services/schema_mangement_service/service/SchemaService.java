package com.flairlabs.workflow.services.schema_mangement_service.service;

import com.flairlabs.workflow.services.schema_mangement_service.models.DynamicChangelog;
import com.flairlabs.workflow.services.schema_mangement_service.models.EntityDefinition;
import com.flairlabs.workflow.services.schema_mangement_service.models.FieldDefinition;
import com.flairlabs.workflow.services.schema_mangement_service.repository.DynamicChangelogRepository;
import jakarta.annotation.PostConstruct;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.change.AddColumnConfig;
import liquibase.change.ColumnConfig;
import liquibase.change.ConstraintsConfig;
import liquibase.change.core.*;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.serializer.core.xml.XMLChangeLogSerializer;
import liquibase.statement.DatabaseFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchemaService {

    private final DataSource dataSource;
    private final DynamicChangelogRepository changelogRepository;

    private Database database;

    @PostConstruct
    public void init() {
        try (Connection connection = dataSource.getConnection()) {
            database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        } catch (SQLException | LiquibaseException e) {
            log.error("Failed to initialize Liquibase database connection", e);
            throw new RuntimeException("Failed to initialize schema service", e);
        }
    }

    @Transactional
    public void createEntityTable(EntityDefinition entity) {
        try (Connection connection = dataSource.getConnection()) {
            // System.out.println("data"+database);
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            // Create dynamic changelog
            DatabaseChangeLog changeLog = new DatabaseChangeLog("in-memory-changelog-" + entity.getName());
            changeLog.setChangeLogParameters(new ChangeLogParameters(database));

            // Create changeset
            CreateTableChange createTableChange = new CreateTableChange();
            createTableChange.setTableName(entity.getName().toLowerCase());

            // Add primary key column
            ColumnConfig idColumn = new ColumnConfig();
            idColumn.setName("id");
            idColumn.setType("BIGINT");
            idColumn.setAutoIncrement(true);
            ConstraintsConfig idConstraints = new ConstraintsConfig();
            idConstraints.setPrimaryKey(true);
            idConstraints.setNullable(false);
            idColumn.setConstraints(idConstraints);
            createTableChange.addColumn(idColumn);

            // Add audit columns
            ColumnConfig createdAtColumn = new ColumnConfig();
            createdAtColumn.setName("created_at");
            createdAtColumn.setType("TIMESTAMP");
            createdAtColumn.setDefaultValueComputed(new DatabaseFunction("CURRENT_TIMESTAMP"));
            createTableChange.addColumn(createdAtColumn);

            ColumnConfig updatedAtColumn = new ColumnConfig();
            updatedAtColumn.setName("updated_at");
            updatedAtColumn.setType("TIMESTAMP");
            updatedAtColumn.setDefaultValueComputed(new DatabaseFunction("CURRENT_TIMESTAMP"));
            createTableChange.addColumn(updatedAtColumn);

            // Add field columns
            for (FieldDefinition field : entity.getFields()) {
                ColumnConfig column = createColumnFromField(field);
                createTableChange.addColumn(column);
            }

            // Create changeset and add to changelog
            String changeSetId = "create-" + entity.getName().toLowerCase() + "-" + System.currentTimeMillis();
            ChangeSet changeSet = new ChangeSet(changeSetId, "system", false, false,
                    "", "", "", changeLog);
            changeSet.addChange(createTableChange);
            changeLog.addChangeSet(changeSet);

            // Execute the change
            Liquibase liquibase = new Liquibase(changeLog, new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts(), new LabelExpression());

            // Log the changes
            DynamicChangelog changelog = DynamicChangelog.builder()
                    .changeType(DynamicChangelog.ChangeType.CREATE_ENTITY)
                    .entityName(entity.getName())
                    .appliedBy("system")
                    .changeDetails("Created entity table: " + entity.getName())
                    .build();
            changelogRepository.save(changelog);

            log.info("Created new entity table: {}", entity.getName());
        } catch (Exception e) {
            log.error("Failed to create entity table: {}", entity.getName(), e);
            throw new RuntimeException("Failed to create entity table", e);
        }
    }

    @Transactional
    public void addFieldToEntity(EntityDefinition entity, FieldDefinition field) {
        try (Connection connection = dataSource.getConnection()) {
            // Create dynamic changelog
            DatabaseChangeLog changeLog = new DatabaseChangeLog();
            changeLog.setChangeLogParameters(new ChangeLogParameters(database));

            // Create changeset
            AddColumnChange addColumnChange = new AddColumnChange();
            addColumnChange.setTableName(entity.getName().toLowerCase());

            AddColumnConfig column = createColumnFromField(field);
            addColumnChange.addColumn(column);

            // Create changeset and add to changelog
            String changeSetId = "add-field-" + entity.getName().toLowerCase() + "-" + field.getName() + "-" + System.currentTimeMillis();
            ChangeSet changeSet = new ChangeSet(changeSetId, "system", false, false,
                    "", "", "", changeLog);
            changeSet.addChange(addColumnChange);
            changeLog.addChangeSet(changeSet);

            // Execute the change
            Liquibase liquibase = new Liquibase(changeLog, new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts(), new LabelExpression());

            // Log the changes
            DynamicChangelog changelog = DynamicChangelog.builder()
                    .changeType(DynamicChangelog.ChangeType.ADD_FIELD)
                    .entityName(entity.getName())
                    .fieldName(field.getName())
                    .fieldType(field.getFieldType().name())
                    .appliedBy("system")
                    .changeDetails("Added field: " + field.getName() + " to entity: " + entity.getName())
                    .build();
            changelogRepository.save(changelog);

            log.info("Added new field: {} to entity: {}", field.getName(), entity.getName());
        } catch (Exception e) {
            log.error("Failed to add field: {} to entity: {}", field.getName(), entity.getName(), e);
            throw new RuntimeException("Failed to add field to entity", e);
        }
    }

    @Transactional
    public void modifyFieldInEntity(EntityDefinition entity, FieldDefinition oldField, FieldDefinition newField) {
        try (Connection connection = dataSource.getConnection()) {
            // Create dynamic changelog
            DatabaseChangeLog changeLog = new DatabaseChangeLog();
            changeLog.setChangeLogParameters(new ChangeLogParameters(database));

            // If the field name has changed, we need to rename the column
            if (!oldField.getName().equals(newField.getName())) {
                RenameColumnChange renameColumnChange = new RenameColumnChange();
                renameColumnChange.setTableName(entity.getName().toLowerCase());
                renameColumnChange.setOldColumnName(oldField.getName().toLowerCase());
                renameColumnChange.setNewColumnName(newField.getName().toLowerCase());
                renameColumnChange.setColumnDataType(mapFieldTypeToSqlType(newField.getFieldType(), newField.getMaxLength()));

                String renameChangeSetId = "rename-field-" + entity.getName().toLowerCase() + "-" + oldField.getName() +
                        "-to-" + newField.getName() + "-" + System.currentTimeMillis();
                ChangeSet renameChangeSet = new ChangeSet(renameChangeSetId, "system", false, false,
                        "", "", "", changeLog);
                renameChangeSet.addChange(renameColumnChange);
                changeLog.addChangeSet(renameChangeSet);
            }

            // If the type or constraints have changed, we need to modify the column
            if (!oldField.getFieldType().equals(newField.getFieldType()) ||
                    !oldField.getRequired().equals(newField.getRequired()) ||
                    !oldField.getMaxLength().equals(newField.getMaxLength())) {

                ModifyDataTypeChange modifyDataTypeChange = new ModifyDataTypeChange();
                modifyDataTypeChange.setTableName(entity.getName().toLowerCase());
                modifyDataTypeChange.setColumnName(newField.getName().toLowerCase());
                modifyDataTypeChange.setNewDataType(mapFieldTypeToSqlType(newField.getFieldType(), newField.getMaxLength()));

                String modifyChangeSetId = "modify-field-" + entity.getName().toLowerCase() + "-" + newField.getName() +
                        "-" + System.currentTimeMillis();
                ChangeSet modifyChangeSet = new ChangeSet(modifyChangeSetId, "system", false, false,
                        "", "", "", changeLog);
                modifyChangeSet.addChange(modifyDataTypeChange);
                changeLog.addChangeSet(modifyChangeSet);

                // If nullability has changed
                if (!oldField.getRequired().equals(newField.getRequired())) {
                    if (Boolean.TRUE.equals(newField.getRequired())) {
                        // Make non-nullable
                        AddNotNullConstraintChange notNullChange = new AddNotNullConstraintChange();
                        notNullChange.setTableName(entity.getName().toLowerCase());
                        notNullChange.setColumnName(newField.getName().toLowerCase());
                        notNullChange.setColumnDataType(mapFieldTypeToSqlType(newField.getFieldType(), newField.getMaxLength()));
                        modifyChangeSet.addChange(notNullChange);
                    } else {
                        // Make nullable
                        DropNotNullConstraintChange dropNotNullChange = new DropNotNullConstraintChange();
                        dropNotNullChange.setTableName(entity.getName().toLowerCase());
                        dropNotNullChange.setColumnName(newField.getName().toLowerCase());
                        dropNotNullChange.setColumnDataType(mapFieldTypeToSqlType(newField.getFieldType(), newField.getMaxLength()));
                        modifyChangeSet.addChange(dropNotNullChange);
                    }
                }
            }

            // Execute the changes
            Liquibase liquibase = new Liquibase(changeLog, new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts(), new LabelExpression());

            // Log the changes
            DynamicChangelog changelog = DynamicChangelog.builder()
                    .changeType(DynamicChangelog.ChangeType.MODIFY_FIELD)
                    .entityName(entity.getName())
                    .fieldName(newField.getName())
                    .fieldType(newField.getFieldType().name())
                    .appliedBy("system")
                    .changeDetails("Modified field: " + oldField.getName() + " to " + newField.getName() + " in entity: " + entity.getName())
                    .build();
            changelogRepository.save(changelog);

            log.info("Modified field: {} to {} in entity: {}", oldField.getName(), newField.getName(), entity.getName());
        } catch (Exception e) {
            log.error("Failed to modify field: {} in entity: {}", newField.getName(), entity.getName(), e);
            throw new RuntimeException("Failed to modify field in entity", e);
        }
    }

    @Transactional
    public void dropFieldFromEntity(EntityDefinition entity, FieldDefinition field) {
        try (Connection connection = dataSource.getConnection()) {
            // Create dynamic changelog
            DatabaseChangeLog changeLog = new DatabaseChangeLog();
            changeLog.setChangeLogParameters(new ChangeLogParameters(database));

            // Create changeset
            DropColumnChange dropColumnChange = new DropColumnChange();
            dropColumnChange.setTableName(entity.getName().toLowerCase());
            dropColumnChange.setColumnName(field.getName().toLowerCase());

            // Create changeset and add to changelog
            String changeSetId = "drop-field-" + entity.getName().toLowerCase() + "-" + field.getName() + "-" + System.currentTimeMillis();
            ChangeSet changeSet = new ChangeSet(changeSetId, "system", false, false,
                    "", "", "", changeLog);
            changeSet.addChange(dropColumnChange);
            changeLog.addChangeSet(changeSet);

            // Execute the change
            Liquibase liquibase = new Liquibase(changeLog, new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts(), new LabelExpression());

            // Log the changes
            DynamicChangelog changelog = DynamicChangelog.builder()
                    .changeType(DynamicChangelog.ChangeType.DROP_FIELD)
                    .entityName(entity.getName())
                    .fieldName(field.getName())
                    .fieldType(field.getFieldType().name())
                    .appliedBy("system")
                    .changeDetails("Dropped field: " + field.getName() + " from entity: " + entity.getName())
                    .build();
            changelogRepository.save(changelog);

            log.info("Dropped field: {} from entity: {}", field.getName(), entity.getName());
        } catch (Exception e) {
            log.error("Failed to drop field: {} from entity: {}", field.getName(), entity.getName(), e);
            throw new RuntimeException("Failed to drop field from entity", e);
        }
    }

    @Transactional
    public void dropEntityTable(EntityDefinition entity) {
        try (Connection connection = dataSource.getConnection()) {
            // Create dynamic changelog
            DatabaseChangeLog changeLog = new DatabaseChangeLog();
            changeLog.setChangeLogParameters(new ChangeLogParameters(database));

            // Create changeset
            DropTableChange dropTableChange = new DropTableChange();
            dropTableChange.setTableName(entity.getName().toLowerCase());

            // Create changeset and add to changelog
            String changeSetId = "drop-" + entity.getName().toLowerCase() + "-" + System.currentTimeMillis();
            ChangeSet changeSet = new ChangeSet(changeSetId, "system", false, false,
                    "", "", "", changeLog);
            changeSet.addChange(dropTableChange);
            changeLog.addChangeSet(changeSet);

            // Execute the change
            Liquibase liquibase = new Liquibase(changeLog, new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts(), new LabelExpression());

            // Log the changes
            DynamicChangelog changelog = DynamicChangelog.builder()
                    .changeType(DynamicChangelog.ChangeType.DROP_ENTITY)
                    .entityName(entity.getName())
                    .appliedBy("system")
                    .changeDetails("Dropped entity table: " + entity.getName())
                    .build();
            changelogRepository.save(changelog);

            log.info("Dropped entity table: {}", entity.getName());
        } catch (Exception e) {
            log.error("Failed to drop entity table: {}", entity.getName(), e);
            throw new RuntimeException("Failed to drop entity table", e);
        }
    }

    // Helper method to convert a field definition to a column configuration
    private AddColumnConfig createColumnFromField(FieldDefinition field) {
        AddColumnConfig column = new AddColumnConfig();
        column.setName(field.getName().toLowerCase());
        column.setType(mapFieldTypeToSqlType(field.getFieldType(), field.getMaxLength()));

        if (Boolean.TRUE.equals(field.getRequired())) {
            ConstraintsConfig constraints = new ConstraintsConfig();
            constraints.setNullable(false);
            column.setConstraints(constraints);
        }

        if (field.getDefaultValue() != null && !field.getDefaultValue().isEmpty()) {
            column.setDefaultValue(field.getDefaultValue());
        }

        return column;
    }

    // Helper method to map field types to SQL types
    private String mapFieldTypeToSqlType(FieldDefinition.FieldType fieldType, Integer maxLength) {
        switch (fieldType) {
            case TEXT:
                if (maxLength != null && maxLength > 0) {
                    return "VARCHAR(" + Math.min(maxLength, 4000) + ")";
                }
                return "VARCHAR(255)";
            case NUMBER:
                return "INTEGER";
            case DECIMAL:
                return "DECIMAL(19, 2)";
            case BOOLEAN:
                return "BOOLEAN";
            case DATE:
                return "DATE";
            case DATETIME:
                return "TIMESTAMP";
            case EMAIL:
            case URL:
            case PHONE:
                if (maxLength != null && maxLength > 0) {
                    return "VARCHAR(" + Math.min(maxLength, 255) + ")";
                }
                return "VARCHAR(100)";
            default:
                return "VARCHAR(255)";
        }
    }

    // Helper method to serialize changelog to XML
    private String serializeChangeLog(DatabaseChangeLog changeLog) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XMLChangeLogSerializer serializer = new XMLChangeLogSerializer();
        try {
            serializer.write(changeLog.getChangeSets(), outputStream);
            return outputStream.toString(StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            log.error("Failed to serialize changelog", e);
            return "Failed to serialize changelog: " + e.getMessage();
        }
    }
}
