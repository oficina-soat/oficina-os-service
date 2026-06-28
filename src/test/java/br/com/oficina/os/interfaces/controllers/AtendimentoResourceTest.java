package br.com.oficina.os.interfaces.controllers;

import br.com.oficina.os.framework.db.AtendimentoSeedStore;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
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
    void deveAtualizarClienteEConsultarPaginado() {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "nome": "Maria Souza Atualizada",
                          "documento": "84191404067",
                          "telefone": "+5511777777777",
                          "email": "maria.atualizada@example.com"
                        }
                        """)
                .when()
                .put("/api/v1/clientes/{clienteId}", AtendimentoSeedStore.SEED_CLIENTE_ID)
                .then()
                .statusCode(200)
                .body("clienteId", equalTo(AtendimentoSeedStore.SEED_CLIENTE_ID.toString()))
                .body("nome", equalTo("Maria Souza Atualizada"))
                .body("telefone", equalTo("+5511777777777"))
                .body("email", equalTo("maria.atualizada@example.com"));

        given()
                .queryParam("page", 0)
                .queryParam("size", 1)
                .when()
                .get("/api/v1/clientes")
                .then()
                .statusCode(200)
                .body("items.size()", equalTo(1))
                .body("page", equalTo(0))
                .body("size", equalTo(1))
                .body("totalItems", greaterThanOrEqualTo(1))
                .body("totalPages", greaterThanOrEqualTo(1));
    }

    @Test
    void deveCriarConsultarEAtualizarVeiculoDoCliente() {
        var veiculoId = given()
                .header("X-Idempotency-Key", "veiculo-create-001")
                .contentType("application/json")
                .body("""
                        {
                          "placa": "def2g34",
                          "marca": "Honda",
                          "modelo": "Fit",
                          "ano": 2021
                        }
                        """)
                .when()
                .post("/api/v1/clientes/{clienteId}/veiculos", AtendimentoSeedStore.SEED_CLIENTE_ID)
                .then()
                .statusCode(201)
                .header("Location", containsString("/api/v1/veiculos/"))
                .body("veiculoId", notNullValue())
                .body("clienteId", equalTo(AtendimentoSeedStore.SEED_CLIENTE_ID.toString()))
                .body("placa", equalTo("DEF2G34"))
                .extract()
                .path("veiculoId")
                .toString();

        given()
                .when()
                .get("/api/v1/veiculos/{veiculoId}", veiculoId)
                .then()
                .statusCode(200)
                .body("veiculoId", equalTo(veiculoId))
                .body("marca", equalTo("Honda"));

        given()
                .contentType("application/json")
                .body("""
                        {
                          "placa": "ghi5j67",
                          "marca": "Toyota",
                          "modelo": "Corolla",
                          "ano": 2024
                        }
                        """)
                .when()
                .put("/api/v1/veiculos/{veiculoId}", veiculoId)
                .then()
                .statusCode(200)
                .body("veiculoId", equalTo(veiculoId))
                .body("placa", equalTo("GHI5J67"))
                .body("marca", equalTo("Toyota"))
                .body("modelo", equalTo("Corolla"))
                .body("ano", equalTo(2024));
    }

    @Test
    void deveRejeitarClienteInvalidoComErroPadronizado() {
        given()
                .header("X-Idempotency-Key", "cliente-invalid-001")
                .contentType("application/json")
                .body("""
                        {
                          "nome": "   ",
                          "documento": "123",
                          "email": "email-invalido"
                        }
                        """)
                .when()
                .post("/api/v1/clientes")
                .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("errorCode", equalTo("VALIDATION_ERROR"))
                .body("detail", equalTo("Nome do cliente e obrigatorio."));
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
    void deveListarOrdensPorEstadoERejeitarTransicaoInvalida() {
        given()
                .queryParam("estado", "RECEBIDA")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/api/v1/ordens-servico")
                .then()
                .statusCode(200)
                .body("items.size()", greaterThanOrEqualTo(1))
                .body("items[0].estado", equalTo("RECEBIDA"));

        given()
                .header("X-Idempotency-Key", "os-invalid-state-001")
                .contentType("application/json")
                .body("""
                        {
                          "estado": "FINALIZADA",
                          "motivo": "Pular fluxo"
                        }
                        """)
                .when()
                .patch("/api/v1/ordens-servico/{ordemServicoId}/estado", AtendimentoSeedStore.SEED_ORDEM_SERVICO_ID)
                .then()
                .statusCode(409)
                .body("status", equalTo(409))
                .body("errorCode", equalTo("HTTP_409"))
                .body("detail", containsString("Transicao de estado invalida"));
    }

    @Test
    void deveRetornarNotFoundParaRecursosInexistentes() {
        given()
                .when()
                .get("/api/v1/veiculos/{veiculoId}", UUID.fromString("aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa"))
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("detail", containsString("Veiculo nao encontrado"));
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
