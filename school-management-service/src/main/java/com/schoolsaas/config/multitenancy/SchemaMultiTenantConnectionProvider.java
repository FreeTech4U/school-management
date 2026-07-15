package com.schoolsaas.config.multitenancy;

import com.schoolsaas.common.util.SchemaNameValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaMultiTenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    private final DataSource dataSource;

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        log.info("[DIAG] getConnection() appelé avec tenantIdentifier = {}", tenantIdentifier);
        validateSchemaName(tenantIdentifier);
        final Connection connection = getAnyConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path = " + tenantIdentifier + ", public");
        } catch (SQLException e) {
            log.error("Error setting search_path to {}: {}", tenantIdentifier, e.getMessage());
            connection.close();
            throw e;
        }
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path = public");
        } catch (SQLException e) {
            log.warn("Error resetting search_path to public: {}", e.getMessage());
        }
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return unwrapType != null && unwrapType.isAssignableFrom(getClass());
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        if (isUnwrappableAs(unwrapType)) {
            return unwrapType.cast(this);
        }
        return null;
    }

    private void validateSchemaName(String schemaName) {
        if (!SchemaNameValidator.isValid(schemaName)) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }
    }
}
