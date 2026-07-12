package br.com.oficina.os.framework.web;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class RuntimeStartupValidatorTest {

    @Test
    void devePermitirMemoriaApenasEmTesteOuDesenvolvimentoLocal() {
        var values = new HashMap<String, String>();
        values.put("oficina.persistence.kind", "memory");
        values.put("oficina.observability.deployment-environment", "test");

        assertDoesNotThrow(() -> RuntimeStartupValidator.validarConfiguracao(List.of("test"), values));

        values.put("oficina.observability.deployment-environment", "local");
        assertDoesNotThrow(() -> RuntimeStartupValidator.validarConfiguracao(List.of("dev"), values));
        assertFalse(RuntimeStartupValidator.runtimeProtegido(List.of("test"), "test"));
    }

    @Test
    void deveProtegerProfileProdOuAmbienteLab() {
        assertTrue(RuntimeStartupValidator.runtimeProtegido(List.of("common", "prod"), "local"));
        assertTrue(RuntimeStartupValidator.runtimeProtegido(List.of("lab"), "local"));
        assertTrue(RuntimeStartupValidator.runtimeProtegido(List.of("test"), " LAB "));
        assertDoesNotThrow(() -> RuntimeStartupValidator.validarConfiguracao(List.of("prod"), validProtectedValues()));

        var labValues = validProtectedValues();
        labValues.put("oficina.observability.deployment-environment", "lab");
        assertDoesNotThrow(() -> RuntimeStartupValidator.validarConfiguracao(List.of("custom"), labValues));
    }

    @Test
    void deveRejeitarFallbacksNoRuntimeProtegido() {
        assertViolation("oficina.persistence.kind deve ser postgresql em prod/lab", Map.of(
                "oficina.persistence.kind", "memory"));
        assertViolation("oficina.messaging.enabled deve ser true em prod/lab", Map.of(
                "oficina.messaging.enabled", "false"));
        assertViolation("oficina.messaging.endpoint-override não é permitido em prod/lab", Map.of(
                "oficina.messaging.endpoint-override", "http://localhost:4566"));
        assertViolation("mp.jwt.verify.audiences deve ser oficina-os-service", Map.of(
                "mp.jwt.verify.audiences", "oficina-app"));
    }

    @Test
    void deveExigirTodasAsConfiguracoesDoRuntimeProtegido() {
        for (var property : List.of(
                "quarkus.datasource.username",
                "quarkus.datasource.password",
                "quarkus.datasource.jdbc.url",
                "quarkus.datasource.reactive.url",
                "AWS_REGION",
                "mp.jwt.verify.issuer",
                "mp.jwt.verify.audiences",
                "mp.jwt.verify.publickey.location")) {
            assertViolation(property + " é obrigatória em prod/lab", Map.of(property, " "));
        }
        assertViolation("mp.jwt.verify.issuer é obrigatória em prod/lab", Map.of(
                "mp.jwt.verify.issuer", "OFICINA_AUTH_ISSUER_PLACEHOLDER"));
    }

    @Test
    void deveRejeitarKindDesconhecidoECredenciaisAwsParciaisEmQualquerProfile() {
        var unknownKind = new HashMap<String, String>();
        unknownKind.put("oficina.persistence.kind", "arquivo");
        unknownKind.put("oficina.observability.deployment-environment", "local");
        var kindFailure = assertThrows(
                IllegalStateException.class,
                () -> RuntimeStartupValidator.validarConfiguracao(List.of("dev"), unknownKind));
        assertTrue(kindFailure.getMessage().contains("oficina.persistence.kind deve ser postgresql ou memory"));

        var accessKeyOnly = validProtectedValues();
        accessKeyOnly.put("oficina.messaging.aws-access-key-id", "access-key");
        var accessKeyFailure = assertThrows(
                IllegalStateException.class,
                () -> RuntimeStartupValidator.validarConfiguracao(List.of("prod"), accessKeyOnly));
        assertTrue(accessKeyFailure.getMessage().contains("credenciais AWS explícitas estão incompletas"));

        var secretKeyOnly = validProtectedValues();
        secretKeyOnly.put("oficina.messaging.aws-secret-access-key", "secret-key");
        var secretKeyFailure = assertThrows(
                IllegalStateException.class,
                () -> RuntimeStartupValidator.validarConfiguracao(List.of("prod"), secretKeyOnly));
        assertTrue(secretKeyFailure.getMessage().contains("credenciais AWS explícitas estão incompletas"));

        var sessionTokenOnly = validProtectedValues();
        sessionTokenOnly.put("oficina.messaging.aws-session-token", "session-token");
        var sessionTokenFailure = assertThrows(
                IllegalStateException.class,
                () -> RuntimeStartupValidator.validarConfiguracao(List.of("prod"), sessionTokenOnly));
        assertTrue(sessionTokenFailure.getMessage().contains("credenciais AWS explícitas estão incompletas"));

        var temporaryCredentials = validProtectedValues();
        temporaryCredentials.put("oficina.messaging.aws-access-key-id", "access-key");
        temporaryCredentials.put("oficina.messaging.aws-secret-access-key", "secret-key");
        temporaryCredentials.put("oficina.messaging.aws-session-token", "session-token");
        assertDoesNotThrow(() -> RuntimeStartupValidator.validarConfiguracao(List.of("prod"), temporaryCredentials));
    }

    @Test
    void deveValidarDisponibilidadeDoPostgres() {
        assertDoesNotThrow(() -> RuntimeStartupValidator.validarPostgres(dataSource(connection(true), null)));

        var invalidConnection = assertThrows(
                IllegalStateException.class,
                () -> RuntimeStartupValidator.validarPostgres(dataSource(connection(false), null)));
        assertTrue(invalidConnection.getMessage().contains("não respondeu"));

        var unavailable = assertThrows(
                IllegalStateException.class,
                () -> RuntimeStartupValidator.validarPostgres(dataSource(null, new SQLException("indisponível"))));
        assertTrue(unavailable.getMessage().contains("não está acessível"));
    }

    private static void assertViolation(String expectedMessage, Map<String, String> overrides) {
        var values = validProtectedValues();
        values.putAll(overrides);
        var failure = assertThrows(
                IllegalStateException.class,
                () -> RuntimeStartupValidator.validarConfiguracao(List.of("prod"), values));
        assertTrue(failure.getMessage().contains(expectedMessage), failure.getMessage());
    }

    private static HashMap<String, String> validProtectedValues() {
        return new HashMap<>(Map.ofEntries(
                Map.entry("oficina.observability.deployment-environment", "prod"),
                Map.entry("oficina.persistence.kind", "postgresql"),
                Map.entry("oficina.messaging.endpoint-override", ""),
                Map.entry("quarkus.datasource.username", "oficina_os_user"),
                Map.entry("quarkus.datasource.password", "secret"),
                Map.entry("quarkus.datasource.jdbc.url", "jdbc:postgresql://rds:5432/oficina_os"),
                Map.entry("quarkus.datasource.reactive.url", "postgresql://rds:5432/oficina_os"),
                Map.entry("AWS_REGION", "us-east-1"),
                Map.entry("mp.jwt.verify.issuer", "https://auth.example.com"),
                Map.entry("mp.jwt.verify.audiences", "oficina-os-service"),
                Map.entry("mp.jwt.verify.publickey.location", "https://auth.example.com/.well-known/jwks.json"),
                Map.entry("oficina.messaging.enabled", "true"),
                Map.entry("oficina.messaging.publisher.enabled", "true"),
                Map.entry("oficina.messaging.consumer.enabled", "true"),
                Map.entry("oficina.messaging.worker.enabled", "true")));
    }

    private static DataSource dataSource(Connection connection, SQLException failure) {
        return (DataSource) Proxy.newProxyInstance(
                RuntimeStartupValidatorTest.class.getClassLoader(),
                new Class<?>[]{DataSource.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getConnection")) {
                        if (failure != null) {
                            throw failure;
                        }
                        return connection;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static Connection connection(boolean valid) {
        return (Connection) Proxy.newProxyInstance(
                RuntimeStartupValidatorTest.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isValid" -> valid;
                    case "close" -> null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
