package br.com.oficina.os.contracts;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.oficina.os.core.entities.ordem_de_servico.EstadoSaga;
import br.com.oficina.os.core.interfaces.messaging.DomainEventEnvelope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PlatformContractsTest {
    private static final Path CONTRACTS_DIR = Path.of("..", "oficina-platform", "contracts");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern OPENAPI_PATH = Pattern.compile("^  (/[^:]+):\\s*$");
    private static final Pattern OPENAPI_METHOD = Pattern.compile("^    (get|post|put|patch|delete):\\s*$");
    private static final Set<String> ENVELOPE_FIELDS = Set.of(
            "eventId", "eventType", "eventVersion", "occurredAt", "producer", "aggregateId", "payload");

    @Test
    void deveExporOperacoesDoOpenApiCanonico() throws IOException {
        var expectedOperations = canonicalOpenApiOperations("openapi/oficina-os-service.yaml");
        var runtimeOperations = runtimeOpenApiOperations();

        expectedOperations.forEach((path, methods) -> {
            assertTrue(runtimeOperations.containsKey(path), () -> "Rota ausente no OpenAPI gerado: " + path);
            assertTrue(runtimeOperations.get(path).containsAll(methods),
                    () -> "Metodos ausentes no OpenAPI gerado para " + path + ": " + methods);
        });
    }

    @Test
    void deveAplicarContratosDeErroEIdempotencia() {
        given()
                .header("X-Correlation-Id", "contract-correlation-os")
                .when()
                .post("/api/v1/status")
                .then()
                .statusCode(400)
                .header("X-Correlation-Id", equalTo("contract-correlation-os"))
                .body("timestamp", notNullValue())
                .body("status", equalTo(400))
                .body("error", equalTo("Bad Request"))
                .body("code", equalTo("IDEMPOTENCY_KEY_REQUIRED"))
                .body("path", equalTo("/api/v1/status"))
                .body("correlationId", equalTo("contract-correlation-os"))
                .body("service", equalTo("oficina-os-service"))
                .body("details.size()", equalTo(0));
    }

    @Test
    void deveValidarEnvelopeLocalContraSchemaComumDeEventos() throws IOException {
        var commonSchema = readJsonContract("events/schemas/common.schema.json");
        var required = fieldNames(commonSchema.at("/$defs/eventEnvelope/required"));
        var recordFields = Arrays.stream(DomainEventEnvelope.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        assertEquals(ENVELOPE_FIELDS, required);
        assertTrue(recordFields.containsAll(ENVELOPE_FIELDS));
        assertTrue(recordFields.contains("correlationId"));
    }

    @Test
    void deveValidarEventosProduzidosEConsumidosContraSchemasJson() {
        producedEvents().forEach((eventType, topic) ->
                assertEventSchema(eventType, "oficina-os-service", topic));

        consumedEvents().forEach((eventType, producer) ->
                assertEventSchema(eventType, producer, null));
    }

    @Test
    void deveValidarContratoDaSagaOrquestrada() throws IOException {
        var sagaContract = Files.readString(contract("saga/oficina-os-saga-v1.md"));

        for (EstadoSaga estado : EstadoSaga.values()) {
            assertTrue(sagaContract.contains(estado.name()), () -> "Estado de Saga ausente do contrato: " + estado);
        }
        sagaProducedEvents().forEach(eventType ->
                assertTrue(sagaContract.contains(eventType), () -> "Evento produzido ausente do contrato da Saga: " + eventType));
        consumedEvents().keySet().forEach(eventType ->
                assertTrue(sagaContract.contains(eventType), () -> "Evento consumido ausente do contrato da Saga: " + eventType));
    }

    private static Map<String, String> producedEvents() {
        return Map.of(
                "ordemDeServicoCriada", "oficina.os.ordem-de-servico-criada",
                "ordemDeServicoFinalizada", "oficina.os.ordem-de-servico-finalizada",
                "ordemDeServicoEntregue", "oficina.os.ordem-de-servico-entregue",
                "usuarioAdicionado", "oficina.os.usuario-adicionado",
                "usuarioAtualizado", "oficina.os.usuario-atualizado",
                "usuarioExcluido", "oficina.os.usuario-excluido",
                "sagaCompensada", "oficina.saga.saga-compensada",
                "sagaFinalizadaComSucesso", "oficina.saga.saga-finalizada-com-sucesso");
    }

    private static Set<String> sagaProducedEvents() {
        return Set.of(
                "ordemDeServicoCriada",
                "ordemDeServicoFinalizada",
                "ordemDeServicoEntregue",
                "sagaCompensada",
                "sagaFinalizadaComSucesso");
    }

    private static Map<String, String> consumedEvents() {
        return Map.ofEntries(
                Map.entry("diagnosticoIniciado", "oficina-execution-service"),
                Map.entry("diagnosticoFinalizado", "oficina-execution-service"),
                Map.entry("orcamentoGerado", "oficina-billing-service"),
                Map.entry("orcamentoAprovado", "oficina-billing-service"),
                Map.entry("orcamentoRecusado", "oficina-billing-service"),
                Map.entry("execucaoIniciada", "oficina-execution-service"),
                Map.entry("execucaoFinalizada", "oficina-execution-service"),
                Map.entry("pagamentoSolicitado", "oficina-billing-service"),
                Map.entry("pagamentoConfirmado", "oficina-billing-service"),
                Map.entry("pagamentoRecusado", "oficina-billing-service"));
    }

    private static void assertEventSchema(String eventType, String producer, String topic) {
        try {
            var schema = readJsonContract("events/schemas/" + eventType + ".schema.json");

            assertEquals(eventType, schema.path("title").asText());
            assertEquals(eventType, schema.at("/properties/eventType/const").asText());
            assertEquals(1, schema.at("/properties/eventVersion/const").asInt());
            assertEquals(producer, schema.at("/properties/producer/const").asText());
            if (topic != null) {
                assertEquals(topic, schema.path("x-topic").asText());
            }
        } catch (IOException exception) {
            throw new AssertionError("Falha ao ler schema do evento " + eventType, exception);
        }
    }

    private static Map<String, Set<String>> runtimeOpenApiOperations() throws IOException {
        var body = given()
                .accept(ContentType.JSON)
                .queryParam("format", "json")
                .when()
                .get("/q/openapi")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        var operations = new LinkedHashMap<String, Set<String>>();
        var paths = MAPPER.readTree(body).path("paths");
        paths.properties().forEach(entry -> {
            var methods = new LinkedHashSet<String>();
            entry.getValue().properties().forEach(method -> methods.add(method.getKey()));
            operations.put(normalizeApiPath(entry.getKey()), methods);
        });
        return operations;
    }

    private static Map<String, Set<String>> canonicalOpenApiOperations(String relativePath) throws IOException {
        var operations = new LinkedHashMap<String, Set<String>>();
        String currentPath = null;

        for (String line : Files.readAllLines(contract(relativePath))) {
            var pathMatcher = OPENAPI_PATH.matcher(line);
            if (pathMatcher.matches()) {
                currentPath = pathMatcher.group(1);
                operations.putIfAbsent(currentPath, new LinkedHashSet<>());
                continue;
            }

            var methodMatcher = OPENAPI_METHOD.matcher(line);
            if (currentPath != null && methodMatcher.matches()) {
                operations.get(currentPath).add(methodMatcher.group(1));
            }
        }

        return operations;
    }

    private static Set<String> fieldNames(JsonNode arrayNode) {
        var values = new LinkedHashSet<String>();
        arrayNode.forEach(node -> values.add(node.asText()));
        return values;
    }

    private static JsonNode readJsonContract(String relativePath) throws IOException {
        return MAPPER.readTree(contract(relativePath).toFile());
    }

    private static Path contract(String relativePath) {
        var path = CONTRACTS_DIR.resolve(relativePath).normalize();
        assertTrue(Files.isRegularFile(path), () -> "Contrato nao encontrado: " + path.toAbsolutePath());
        return path;
    }

    private static String normalizeApiPath(String path) {
        return path.startsWith("/api/v1") ? path.substring("/api/v1".length()) : path;
    }
}
