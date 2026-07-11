package com.schoolsaas.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.sql.init.mode=never",
        "spring.jpa.properties.hibernate.multiTenancy=NONE",
        "spring.jpa.properties.hibernate.multi_tenant_connection_provider=org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl",
        "spring.jpa.properties.hibernate.tenant_identifier_resolver=org.hibernate.context.internal.DefaultIdentifierResolver"
})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class OpenApiGeneratorTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void generateOpenApiJson() throws Exception {
        String json = mockMvc.perform(get("/api-docs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                        .getContentAsString();

        Path path = Paths.get("target/openapi-spec.json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, json);
        System.out.println("OpenAPI JSON generated at: " + path.toAbsolutePath());
    }
}
