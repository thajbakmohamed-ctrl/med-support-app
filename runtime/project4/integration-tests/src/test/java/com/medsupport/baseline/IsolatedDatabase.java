package com.medsupport.baseline;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;

final class IsolatedDatabase {
    static final Path REPO = Path.of(System.getProperty("baseline.repo")).toAbsolutePath().normalize();
    final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("med_support").withReuse(false);
    DriverManagerDataSource dataSource;
    JdbcTemplate jdbc;

    void start() throws Exception {
        postgres.start();
        // No externally supplied URL, fixed port, Compose network, mount or reuse.
        if (!postgres.isRunning() || postgres.getMappedPort(5432) == 5434)
            throw new IllegalStateException("Disposable PostgreSQL safety check failed");
        dataSource = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        try (Connection connection = dataSource.getConnection()) {
            for (String file : List.of(
                    "booking-service/src/main/resources/db/manual/shared/S001__hospital_and_shared_audit.sql",
                    "booking-service/src/main/resources/db/manual/shared/S002__identity_tables.sql",
                    "booking-service/src/main/resources/db/manual/shared/S003__blood_requests_and_donation_slots.sql",
                    "booking-service/src/main/resources/db/manual/shared/S004__donation_bookings_and_outbox.sql",
                    "notification-service/src/main/resources/db/manual/V001__notification_service_schema.sql")) {
                // PostgreSQL dollar-quoted DO blocks contain semicolons: execute the
                // complete script using the driver, rather than splitting statements.
                try (var statement = connection.createStatement()) {
                    statement.execute(java.nio.file.Files.readString(REPO.resolve(file)));
                }
            }
        }
    }

    void close() { postgres.stop(); }
}
