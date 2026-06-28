package br.com.oficina.os.interfaces.controllers;

import br.com.oficina.os.framework.db.AtendimentoSeedStore;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class AtendimentoResourceTest {

    @Test
    void deveConsultarClienteEVeiculoDoSeed() {
        given()
                .when()
                .get("/api/v1/clientes/{clienteId}", AtendimentoSeedStore.SEED_CLIENTE_ID)
                .then()
                .statusCode(200)
                .body("clienteId", equalTo(AtendimentoSeedStore.SEED_CLIENTE_ID.toString()))
                .body("nome", equalTo("Maria Souza"))
                .body("documento", equalTo("12345678901"));

        given()
                .when()
                .get("/api/v1/clientes/{clienteId}/veiculos", AtendimentoSeedStore.SEED_CLIENTE_ID)
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("[0].veiculoId", equalTo(AtendimentoSeedStore.SEED_VEICULO_ID.toString()))
                .body("[0].clienteId", equalTo(AtendimentoSeedStore.SEED_CLIENTE_ID.toString()));
    }

    @Test
    void deveCriarClienteComHeaderCanonicoDeIdempotencia() {
        given()
                .header("X-Idempotency-Key", "cliente-create-001")
                .contentType("application/json")
                .body("""
                        {
                          "nome": "Joao Silva",
                          "documento": "84191404067",
                          "telefone": "+5511888888888",
                          "email": "joao@example.com"
                        }
                        """)
                .when()
                .post("/api/v1/clientes")
                .then()
                .statusCode(201)
                .header("Location", notNullValue())
                .body("clienteId", notNullValue())
                .body("nome", equalTo("Joao Silva"))
                .body("documento", equalTo("84191404067"));
    }

    @Test
    void deveAbrirOrdemServicoAlterarEstadoEConsultarHistorico() {
        var ordemServicoId = given()
                .header("X-Idempotency-Key", "os-create-001")
                .contentType("application/json")
                .body("""
                        {
                          "clienteId": "%s",
                          "veiculoId": "%s",
                          "descricaoProblema": "Veiculo nao liga"
                        }
                        """.formatted(AtendimentoSeedStore.SEED_CLIENTE_ID, AtendimentoSeedStore.SEED_VEICULO_ID))
                .when()
                .post("/api/v1/ordens-servico")
                .then()
                .statusCode(201)
                .body("ordemServicoId", notNullValue())
                .body("estado", equalTo("RECEBIDA"))
                .extract()
                .path("ordemServicoId")
                .toString();

        given()
                .header("X-Idempotency-Key", "os-state-001")
                .contentType("application/json")
                .body("""
                        {
                          "estado": "EM_DIAGNOSTICO",
                          "motivo": "Diagnostico iniciado"
                        }
                        """)
                .when()
                .patch("/api/v1/ordens-servico/{ordemServicoId}/estado", ordemServicoId)
                .then()
                .statusCode(200)
                .body("estado", equalTo("EM_DIAGNOSTICO"));

        given()
                .when()
                .get("/api/v1/ordens-servico/{ordemServicoId}/historico", ordemServicoId)
                .then()
                .statusCode(200)
                .body("size()", equalTo(2))
                .body("[1].estado", equalTo("EM_DIAGNOSTICO"))
                .body("[1].motivo", equalTo("Diagnostico iniciado"));
    }

    @Test
    void deveAceitarCancelamentoAssincrono() {
        given()
                .header("X-Idempotency-Key", "os-cancel-001")
                .contentType("application/json")
                .body("""
                        {
                          "motivo": "Solicitado pelo cliente"
                        }
                        """)
                .when()
                .post("/api/v1/ordens-servico/{ordemServicoId}/cancelamento", AtendimentoSeedStore.SEED_ORDEM_SERVICO_ID)
                .then()
                .statusCode(202)
                .body("status", equalTo("ACEITO"))
                .body("solicitadoEm", notNullValue());
    }
}
