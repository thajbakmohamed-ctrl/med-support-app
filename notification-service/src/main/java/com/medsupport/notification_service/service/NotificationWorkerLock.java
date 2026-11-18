
package com.medsupport.notification_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;

@Component
public class NotificationWorkerLock {

    private final DriverManagerDataSource lockDataSource;
    private final int queryTimeoutSeconds;

    public NotificationWorkerLock(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${app.notification.worker-lock.connect-timeout-seconds:5}")
            int connectTimeoutSeconds,
            @Value("${app.notification.worker-lock.socket-timeout-seconds:10}")
            int socketTimeoutSeconds,
            @Value("${app.notification.worker-lock.query-timeout-seconds:5}")
            int queryTimeoutSeconds
    ) {

        if (!url.startsWith("jdbc:postgresql:")) {
            throw new IllegalArgumentException("Notification worker coordination requires PostgreSQL");
        }

        if (connectTimeoutSeconds <= 0
                || socketTimeoutSeconds <= 0
                || queryTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("Worker lock timeouts must be positive");
        }

        Properties properties = new Properties();

        properties.setProperty("connectTimeout",
                Integer.toString(connectTimeoutSeconds));

        properties.setProperty("socketTimeout",
                Integer.toString(socketTimeoutSeconds));

        properties.setProperty("tcpKeepAlive", "true");


        this.lockDataSource = new DriverManagerDataSource();
        this.lockDataSource.setUrl(url);
        this.lockDataSource.setUsername(username);
        this.lockDataSource.setPassword(password);
        this.lockDataSource.setConnectionProperties(properties);

        this.queryTimeoutSeconds = queryTimeoutSeconds;
    }

    public <T> Optional<T> tryWithLock(Long notificationId, Supplier<T> action) {

        if (notificationId == null || notificationId <= 0) {
            throw new IllegalArgumentException("Valid notification ID is required");
        }

        Objects.requireNonNull(action, "Lock action is required");

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Worker coordination must start outside a transaction");
        }


        long lockKey = -notificationId;

        try (Connection connection = lockDataSource.getConnection()) {

            if (!connection.getAutoCommit()) {throw new IllegalStateException(
                        "Worker lock connection must use auto-commit");
            }

            if (!tryAcquire(connection, lockKey)) {
                return Optional.empty();
            }


            T result = Objects.requireNonNull(action.get(),
                    "Worker lock action must return a non-null result");

            return Optional.of(result);

        } catch (SQLException ex) {
            throw new IllegalStateException("PostgreSQL worker coordination failed for notification "
                    + notificationId, ex);
        }
    }

    private boolean tryAcquire(Connection connection, long lockKey
    ) throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT pg_try_advisory_lock(?)")) {

            statement.setLong(1, lockKey);
            statement.setQueryTimeout(queryTimeoutSeconds);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    throw new SQLException("PostgreSQL returned no advisory-lock result");
                }

                return resultSet.getBoolean(1);
            }
        }
    }
}
