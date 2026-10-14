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
                "  1. If connecting to Railway from Local PC, use DATABASE_PUBLIC_URL (from Railway Postgres Variables),\n" +
                "     as postgres.railway.internal is only resolvable inside Railway's internal cloud network.\n" +
                "  2. If running locally with local PostgreSQL:\n" +
                "     - Ensure PostgreSQL service is running on localhost:5432.\n" +
                "     - Ensure the database 'glowora_db' exists: CREATE DATABASE glowora_db;\n" +
                "  3. If running on Railway:\n" +
                "     - Ensure DATABASE_URL is set or referenced (${{Postgres.DATABASE_URL}}).\n" +
                "  4. Restart the Spring Boot application with: mvn spring-boot:run";

        return new FailureAnalysis(description, action, cause);
    }
}
