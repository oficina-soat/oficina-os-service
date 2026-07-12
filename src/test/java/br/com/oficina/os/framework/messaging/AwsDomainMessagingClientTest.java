package br.com.oficina.os.framework.messaging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class AwsDomainMessagingClientTest {

    @Test
    void deveRejeitarCredenciaisEstaticasParciais() {
        var accessKeyOnly = assertThrows(IllegalStateException.class, () -> new AwsDomainMessagingClient(
                DomainMessagingRoutes.SERVICE_NAME,
                "us-east-1",
                Optional.of("http://localhost:4566"),
                Optional.of("000000000000"),
                Optional.of("access-key"),
                Optional.empty(),
                Optional.empty()));
        assertTrue(accessKeyOnly.getMessage().contains("incompletas"));

        var secretKeyOnly = assertThrows(IllegalStateException.class, () -> new AwsDomainMessagingClient(
                DomainMessagingRoutes.SERVICE_NAME,
                "us-east-1",
                Optional.of("http://localhost:4566"),
                Optional.of("000000000000"),
                Optional.empty(),
                Optional.of("secret-key"),
                Optional.empty()));
        assertTrue(secretKeyOnly.getMessage().contains("incompletas"));
    }

    @Test
    void deveAceitarParPermanenteETrioDeCredenciaisTemporarias() {
        assertDoesNotThrow(() -> criarClient(Optional.empty()).close());
        assertDoesNotThrow(() -> criarClient(Optional.of("session-token")).close());
    }

    @Test
    void deveRejeitarSessionTokenSemAccessESecretKeys() {
        var failure = assertThrows(IllegalStateException.class, () -> new AwsDomainMessagingClient(
                DomainMessagingRoutes.SERVICE_NAME,
                "us-east-1",
                Optional.of("http://localhost:4566"),
                Optional.of("000000000000"),
                Optional.empty(),
                Optional.empty(),
                Optional.of("session-token")));
        assertTrue(failure.getMessage().contains("incompletas"));
    }

    private static AwsDomainMessagingClient criarClient(Optional<String> sessionToken) {
        return new AwsDomainMessagingClient(
                DomainMessagingRoutes.SERVICE_NAME,
                "us-east-1",
                Optional.of("http://localhost:4566"),
                Optional.of("000000000000"),
                Optional.of("access-key"),
                Optional.of("secret-key"),
                sessionToken);
    }
}
