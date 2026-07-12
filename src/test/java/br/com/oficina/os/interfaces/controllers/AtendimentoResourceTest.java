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
import static org.hamcrest.Matchers.nullValue;

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
                .body("documento", equalTo("12345678901"))
                .body("telefone", equalTo("+5511999999999"))
                .body("email", equalTo("maria@example.com"))
                .body("criadoEm", notNullValue())
                .body("atualizadoEm", notNullValue());

        given()
                .when()
                .get("/api/v1/clientes/{clienteId}/veiculos", AtendimentoSeedStore.SEED_CLIENTE_ID)
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("[0].veiculoId", equalTo(AtendimentoSeedStore.SEED_VEICULO_ID.toString()))
                .body("[0].clienteId", equalTo(AtendimentoSeedStore.SEED_CLIENTE_ID.toString()))
                .body("[0].placa", equalTo("ABC1D23"))
                .body("[0].marca", equalTo("Volkswagen"))
                .body("[0].modelo", equalTo("Gol"))
                .body("[0].ano", equalTo(2020))
                .body("[0].criadoEm", notNullValue())
                .body("[0].atualizadoEm", notNullValue());
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
                .body("documento", equalTo("84191404067"))
                .body("telefone", equalTo("+5511888888888"))
                .body("email", equalTo("joao@example.com"))
                .body("criadoEm", notNullValue())
                .body("atualizadoEm", notNullValue());
    }

    @Test
    void deveRepetirRespostaIdempotenteERejeitarPayloadDivergente() {
        var idempotencyKey = "cliente-replay-" + UUID.randomUUID();
        var requestBody = """
                {
                  "nome": "Cliente Idempotente",
                  "documento": "84191404067",
                  "telefone": "+5511666666666",
                  "email": "idempotente@example.com"
                }
                """;

        var clienteId = given()
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType("application/json")
                .body(requestBody)
                .when()
                .post("/api/v1/clientes")
                .then()
                .statusCode(201)
                .body("clienteId", notNullValue())
                .extract()
                .path("clienteId")
                .toString();

        given()
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType("application/json")
                .body(requestBody)
                .when()
                .post("/api/v1/clientes")
                .then()
                .statusCode(201)
                .body("clienteId", equalTo(clienteId))
                .body("nome", equalTo("Cliente Idempotente"));

        given()
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType("application/json")
                .body("""
                        {
                          "nome": "Cliente Idempotente Divergente",
                          "documento": "84191404067",
                          "telefone": "+5511666666666",
                          "email": "idempotente@example.com"
                        }
                        """)
                .when()
                .post("/api/v1/clientes")
                .then()
                .statusCode(409)
                .body("code", equalTo("IDEMPOTENCY_CONFLICT"))
                .body("message", equalTo("Chave de idempotencia reutilizada com payload divergente."));
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
                .body("items[0].clienteId", notNullValue())
                .body("items[0].nome", notNullValue())
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
                .body("marca", equalTo("Honda"))
                .body("modelo", equalTo("Fit"))
                .body("ano", equalTo(2021))
                .body("criadoEm", notNullValue())
                .body("atualizadoEm", notNullValue())
                .extract()
                .path("veiculoId")
                .toString();

        given()
                .when()
                .get("/api/v1/veiculos/{veiculoId}", veiculoId)
                .then()
                .statusCode(200)
                .body("veiculoId", equalTo(veiculoId))
                .body("clienteId", equalTo(AtendimentoSeedStore.SEED_CLIENTE_ID.toString()))
                .body("placa", equalTo("DEF2G34"))
                .body("marca", equalTo("Honda"))
                .body("modelo", equalTo("Fit"))
                .body("ano", equalTo(2021));

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
                .body("timestamp", notNullValue())
                .body("status", equalTo(400))
                .body("error", equalTo("Bad Request"))
                .body("code", equalTo("VALIDATION_ERROR"))
                .body("message", equalTo("Nome do cliente e obrigatorio."))
                .body("path", equalTo("/api/v1/clientes"))
                .body("correlationId", notNullValue())
                .body("service", equalTo("oficina-os-service"))
                .body("details.size()", equalTo(0));
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
                .body("clienteId", equalTo(AtendimentoSeedStore.SEED_CLIENTE_ID.toString()))
                .body("veiculoId", equalTo(AtendimentoSeedStore.SEED_VEICULO_ID.toString()))
                .body("descricaoProblema", equalTo("Veiculo nao liga"))
                .body("estado", equalTo("RECEBIDA"))
                .body("criadoEm", notNullValue())
                .body("atualizadoEm", notNullValue())
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
                .body("ordemServicoId", equalTo(ordemServicoId))
                .body("estado", equalTo("EM_DIAGNOSTICO"));

        given()
                .when()
                .get("/api/v1/ordens-servico/{ordemServicoId}/historico", ordemServicoId)
                .then()
                .statusCode(200)
                .body("size()", equalTo(2))
                .body("[0].estado", equalTo("RECEBIDA"))
                .body("[0].dataDoEstado", notNullValue())
                .body("[1].estado", equalTo("EM_DIAGNOSTICO"))
                .body("[1].dataDoEstado", notNullValue())
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
                .body("error", equalTo("Conflict"))
                .body("code", equalTo("INVALID_STATE_TRANSITION"))
                .body("message", containsString("Transicao de estado invalida"))
                .body("path", equalTo("/api/v1/ordens-servico/" + AtendimentoSeedStore.SEED_ORDEM_SERVICO_ID + "/estado"))
                .body("correlationId", notNullValue())
                .body("service", equalTo("oficina-os-service"))
                .body("details.size()", equalTo(0));
    }

    @Test
    void deveRetornarNotFoundParaRecursosInexistentes() {
        given()
                .when()
                .get("/api/v1/veiculos/{veiculoId}", UUID.fromString("aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa"))
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("code", equalTo("RESOURCE_NOT_FOUND"))
                .body("message", containsString("Veiculo nao encontrado"))
                .body("path", equalTo("/api/v1/veiculos/aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa"))
                .body("correlationId", notNullValue())
                .body("service", equalTo("oficina-os-service"))
                .body("details.size()", equalTo(0));
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

    @Test
    void deveRetornarNotFoundContratadoAoConsultarClienteInexistente() {
        given()
                .when()
                .get("/api/v1/clientes/{clienteId}", UUID.fromString("bbbbbbbb-bbbb-4bbb-bbbb-bbbbbbbbbbbb"))
                .then()
                .statusCode(404)
                .body("timestamp", notNullValue())
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("code", equalTo("RESOURCE_NOT_FOUND"))
                .body("message", containsString("Cliente nao encontrado"))
                .body("path", equalTo("/api/v1/clientes/bbbbbbbb-bbbb-4bbb-bbbb-bbbbbbbbbbbb"))
                .body("correlationId", notNullValue())
                .body("requestId", nullValue())
                .body("traceId", nullValue())
                .body("spanId", nullValue())
                .body("service", equalTo("oficina-os-service"))
                .body("logReference", notNullValue())
                .body("details.size()", equalTo(0));
    }

    @Test
    void deveRetornarNotFoundContratadoAoAtualizarClienteInexistente() {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "nome": "Cliente Inexistente",
                          "documento": "84191404067",
                          "telefone": "+5511666666666",
                          "email": "cliente.inexistente@example.com"
                        }
                        """)
                .when()
                .put("/api/v1/clientes/{clienteId}", UUID.fromString("cccccccc-cccc-4ccc-cccc-cccccccccccc"))
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("code", equalTo("RESOURCE_NOT_FOUND"))
                .body("message", containsString("Cliente nao encontrado"))
                .body("path", equalTo("/api/v1/clientes/cccccccc-cccc-4ccc-cccc-cccccccccccc"));
    }

    @Test
    void deveRetornarErroContratadoAoCriarVeiculoParaClienteInexistente() {
        given()
                .header("X-Idempotency-Key", "veiculo-cliente-inexistente-001")
                .contentType("application/json")
                .body("""
                        {
                          "placa": "jkl8m90",
                          "marca": "Ford",
                          "modelo": "Ka",
                          "ano": 2020
                        }
                        """)
                .when()
                .post("/api/v1/clientes/{clienteId}/veiculos", UUID.fromString("dddddddd-dddd-4ddd-dddd-dddddddddddd"))
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("code", equalTo("RESOURCE_NOT_FOUND"))
                .body("message", containsString("Cliente nao encontrado"))
                .body("path", equalTo("/api/v1/clientes/dddddddd-dddd-4ddd-dddd-dddddddddddd/veiculos"));
    }

    @Test
    void deveRetornarErroContratadoAoListarVeiculosDeClienteInexistente() {
        given()
                .when()
                .get("/api/v1/clientes/{clienteId}/veiculos", UUID.fromString("eeeeeeee-eeee-4eee-eeee-eeeeeeeeeeee"))
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("code", equalTo("RESOURCE_NOT_FOUND"))
                .body("message", containsString("Cliente nao encontrado"))
                .body("path", equalTo("/api/v1/clientes/eeeeeeee-eeee-4eee-eeee-eeeeeeeeeeee/veiculos"));
    }

    @Test
    void deveRetornarErroContratadoAoAtualizarVeiculoInvalido() {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "placa": "ghi5j67",
                          "marca": "Toyota",
                          "modelo": "Corolla",
                          "ano": 1899
                        }
                        """)
                .when()
                .put("/api/v1/veiculos/{veiculoId}", AtendimentoSeedStore.SEED_VEICULO_ID)
                .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("error", equalTo("Bad Request"))
                .body("code", equalTo("VALIDATION_ERROR"))
                .body("message", equalTo("Ano do veiculo deve ser maior ou igual a 1900."))
                .body("path", equalTo("/api/v1/veiculos/" + AtendimentoSeedStore.SEED_VEICULO_ID));
    }

    @Test
    void deveConsultarOrdemServicoERecusarOrdemInexistenteComErroContratado() {
        given()
                .when()
                .get("/api/v1/ordens-servico/{ordemServicoId}", AtendimentoSeedStore.SEED_ORDEM_SERVICO_ID)
                .then()
                .statusCode(200)
                .body("ordemServicoId", equalTo(AtendimentoSeedStore.SEED_ORDEM_SERVICO_ID.toString()))
                .body("clienteId", equalTo(AtendimentoSeedStore.SEED_CLIENTE_ID.toString()))
                .body("veiculoId", equalTo(AtendimentoSeedStore.SEED_VEICULO_ID.toString()))
                .body("descricaoProblema", equalTo("Veiculo nao liga"))
                .body("estado", notNullValue());

        given()
                .when()
                .get("/api/v1/ordens-servico/{ordemServicoId}", UUID.fromString("ffffffff-ffff-4fff-ffff-ffffffffffff"))
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("code", equalTo("RESOURCE_NOT_FOUND"))
                .body("message", containsString("Ordem de servico nao encontrada"))
                .body("path", equalTo("/api/v1/ordens-servico/ffffffff-ffff-4fff-ffff-ffffffffffff"));
    }

    @Test
    void deveRetornarErroContratadoAoConsultarHistoricoDeOrdemInexistente() {
        given()
                .when()
                .get("/api/v1/ordens-servico/{ordemServicoId}/historico", UUID.fromString("99999999-9999-4999-9999-999999999999"))
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("code", equalTo("RESOURCE_NOT_FOUND"))
                .body("message", containsString("Ordem de servico nao encontrada"))
                .body("path", equalTo("/api/v1/ordens-servico/99999999-9999-4999-9999-999999999999/historico"));
    }

    @Test
    void deveRetornarErroContratadoAoAbrirOrdemServicoSemDescricao() {
        given()
                .header("X-Idempotency-Key", "os-descricao-invalida-001")
                .contentType("application/json")
                .body("""
                        {
                          "clienteId": "%s",
                          "veiculoId": "%s",
                          "descricaoProblema": " "
                        }
                        """.formatted(AtendimentoSeedStore.SEED_CLIENTE_ID, AtendimentoSeedStore.SEED_VEICULO_ID))
                .when()
                .post("/api/v1/ordens-servico")
                .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("error", equalTo("Bad Request"))
                .body("code", equalTo("VALIDATION_ERROR"))
                .body("message", equalTo("Descricao do problema e obrigatoria."))
                .body("path", equalTo("/api/v1/ordens-servico"));
    }

    @Test
    void deveRetornarErroContratadoAoAlterarEstadoDeOrdemInexistente() {
        given()
                .header("X-Idempotency-Key", "os-state-inexistente-001")
                .contentType("application/json")
                .body("""
                        {
                          "estado": "EM_DIAGNOSTICO",
                          "motivo": "Diagnostico iniciado"
                        }
                        """)
                .when()
                .patch("/api/v1/ordens-servico/{ordemServicoId}/estado", UUID.fromString("88888888-8888-4888-8888-888888888888"))
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("code", equalTo("RESOURCE_NOT_FOUND"))
                .body("message", containsString("Ordem de servico nao encontrada"))
                .body("path", equalTo("/api/v1/ordens-servico/88888888-8888-4888-8888-888888888888/estado"));
    }

    @Test
    void deveRetornarErroContratadoAoCancelarOrdemInexistente() {
        given()
                .header("X-Idempotency-Key", "os-cancel-inexistente-001")
                .contentType("application/json")
                .body("""
                        {
                          "motivo": "Solicitado pelo cliente"
                        }
                        """)
                .when()
                .post("/api/v1/ordens-servico/{ordemServicoId}/cancelamento", UUID.fromString("77777777-7777-4777-7777-777777777777"))
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("code", equalTo("RESOURCE_NOT_FOUND"))
                .body("message", containsString("Ordem de servico nao encontrada"))
                .body("path", equalTo("/api/v1/ordens-servico/77777777-7777-4777-7777-777777777777/cancelamento"));
    }

    @Test
    void deveRetornarErroContratadoQuandoIdempotencyKeyObrigatoriaEstaAusente() {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "nome": "Sem Header",
                          "documento": "84191404067",
                          "telefone": "+5511555555555",
                          "email": "sem.header@example.com"
                        }
                        """)
                .when()
                .post("/api/v1/clientes")
                .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("error", equalTo("Bad Request"))
                .body("code", equalTo("IDEMPOTENCY_KEY_REQUIRED"))
                .body("message", containsString("Header X-Idempotency-Key obrigatorio"))
                .body("path", equalTo("/api/v1/clientes"));
    }
}
