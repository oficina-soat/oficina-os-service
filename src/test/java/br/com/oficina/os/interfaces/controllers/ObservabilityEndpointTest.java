package br.com.oficina.os.interfaces.controllers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import br.com.oficina.os.framework.db.AtendimentoSeedStore;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ObservabilityEndpointTest {
    @Inject
    AtendimentoSeedStore store;

    @Test
    void shouldExposeLiveHealthEndpoint() {
        given()
                .when()
                .get("/q/health/live")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    void shouldExposeReadyHealthEndpoint() {
        given()
                .when()
                .get("/q/health/ready")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    void shouldExposePrometheusMetricsEndpoint() {
        store.criarOrdemServico(
                AtendimentoSeedStore.SEED_CLIENTE_ID,
                AtendimentoSeedStore.SEED_VEICULO_ID,
                "Validar metrica Prometheus da Saga");

        given()
                .when()
                .get("/q/metrics")
                .then()
                .statusCode(200)
                .body(not(emptyOrNullString()))
                .body(containsString("# HELP"))
                .body(containsString("saga_instances_started_count"))
                .body(containsString("saga_step_duration"));
    }
}
