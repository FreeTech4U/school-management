package com.schoolsaas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class SchoolSaasApplicationTests {

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
		registry.add("spring.flyway.locations", () -> "classpath:db/migration/public");
		registry.add("app.jwt.secret", () -> "test-secret-key-for-integration-tests-only");
	}

	@Test
	void contextLoads() {
	}

}
