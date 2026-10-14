package com.glowora.config;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

import java.sql.SQLException;

/**
 * Custom Spring Boot FailureAnalyzer that intercepts PostgreSQL connection failures at startup
 * and prints clear troubleshooting instructions without falling back to an in-memory database.
 */
public class DatabaseConnectionFailureAnalyzer extends AbstractFailureAnalyzer<SQLException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, SQLException cause) {
        String description = String.format(
                "Glowora Backend could not connect to the PostgreSQL database (glowora_db).%n" +
                "Underlying SQL Error: %s (SQLState: %s)",
                cause.getMessage(), cause.getSQLState()
        );

        String action =
                "Please verify the following PostgreSQL setup steps:\n" +
                "  1. Ensure PostgreSQL server is installed and running on localhost:5432.\n" +
                "  2. Ensure the database 'glowora_db' exists:\n" +
                "       psql -U postgres -h localhost -p 5432 -c \"CREATE DATABASE glowora_db;\"\n" +
                "  3. Verify the credentials in backend/src/main/resources/application.properties or set DB_PASSWORD:\n" +
                "       Username: postgres\n" +
                "       Password: ${DB_PASSWORD:MEENU}\n" +
                "  4. Restart the Spring Boot application with: mvn spring-boot:run";

        return new FailureAnalysis(description, action, cause);
    }
}
