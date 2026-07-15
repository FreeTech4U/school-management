package com.schoolsaas.integration;

import com.schoolsaas.common.enums.SchoolStatus;
import com.schoolsaas.platform.entity.School;
import com.schoolsaas.platform.repository.SchoolRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class MultiTenancyIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("school_saas_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration/public");
        registry.add("app.jwt.secret", () -> "test-secret-key-for-integration-tests-only");
    }

    @Autowired
    private SchoolRepository schoolRepository;

    @Test
    void testPublicSchemaPersistence() {
        School school = School.builder()
                .name("Integration Test School")
                .slug("test-school")
                .schemaName("school_test")
                .email("test@school.com")
                .status(SchoolStatus.ACTIVE)
                .build();
        
        School saved = schoolRepository.save(school);
        assertNotNull(saved.getId());
        
        School fetched = schoolRepository.findById(saved.getId()).orElseThrow();
        assertEquals("test-school", fetched.getSlug());
    }
}
