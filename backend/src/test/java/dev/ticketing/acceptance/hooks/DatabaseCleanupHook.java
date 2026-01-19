package dev.ticketing.acceptance.hooks;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;

import java.util.List;
import java.util.stream.Collectors;

import io.cucumber.java.Before;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
public class DatabaseCleanupHook {

    @PersistenceContext
    private EntityManager entityManager;

    private List<String> tableNames;

    @Before
    @Transactional
    public void cleanupDatabase() {
        if (tableNames == null) {
            initTableNames();
        }

        entityManager.flush();
        // PostgreSQL can use TRUNCATE ... CASCADE to handle foreign keys
        for (String tableName : tableNames) {
            entityManager.createNativeQuery("TRUNCATE TABLE " + tableName + " CASCADE").executeUpdate();
        }

        log.info("[BDD] Database cleaned up successfully (Dynamic Table Discovery)");
    }

    private void initTableNames() {
        tableNames = entityManager.getMetamodel().getEntities().stream()
                .filter(e -> e.getJavaType().getAnnotation(Entity.class) != null)
                .map(this::getTableName)
                .collect(Collectors.toList());
        log.info("[BDD] Initialized table names for cleanup: {}", tableNames);
    }

    private String getTableName(EntityType<?> entityType) {
        Table tableAnnotation = entityType.getJavaType().getAnnotation(Table.class);
        if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
            return tableAnnotation.name();
        }
        return camelToSnake(entityType.getName());
    }

    private String camelToSnake(String str) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0)
                    result.append('_');
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
