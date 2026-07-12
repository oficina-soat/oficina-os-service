package br.com.oficina.os.framework.db;

import br.com.oficina.os.core.interfaces.messaging.OutboxEventRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.SQLException;

final class PostgresPersistenceSupport {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String SQL_INSERT_OUTBOX = """
            INSERT INTO outbox_event (
                id, aggregate_id, event_type, event_version, topic, producer, payload, status,
                correlation_id, occurred_at, created_at, published_at, attempts, next_attempt_at, last_error
            ) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, NULL, ?)
            """;

    private PostgresPersistenceSupport() {
    }

    static void insertOutbox(Connection connection, OutboxEventRecord event) throws SQLException {
        try (var statement = connection.prepareStatement(SQL_INSERT_OUTBOX)) {
            statement.setObject(1, event.eventId());
            statement.setString(2, event.aggregateId().toString());
            statement.setString(3, event.eventType());
            statement.setInt(4, event.eventVersion());
            statement.setString(5, event.topic());
            statement.setString(6, event.producer());
            statement.setString(7, toJson(event));
            statement.setString(8, event.status());
            statement.setString(9, event.correlationId());
            statement.setObject(10, event.occurredAt());
            statement.setObject(11, event.createdAt());
            statement.setObject(12, event.publishedAt());
            statement.setInt(13, event.attempts());
            statement.setString(14, event.lastError());
            statement.executeUpdate();
        }
    }

    static <T> T executeTransaction(Connection connection, SqlOperation<T> operation) throws SQLException {
        var previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            var result = operation.execute(connection);
            connection.commit();
            return result;
        } catch (SQLException | RuntimeException exception) {
            rollback(connection);
            throw exception;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private static String toJson(OutboxEventRecord event) throws SQLException {
        try {
            return JSON.writeValueAsString(event.payload());
        } catch (JsonProcessingException exception) {
            throw new SQLException("Payload do evento de outbox é inválido.", exception);
        }
    }

    private static void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException _) {
            // A falha original é mais útil para o chamador.
        }
    }

    @FunctionalInterface
    interface SqlOperation<T> {
        T execute(Connection connection) throws SQLException;
    }
}
