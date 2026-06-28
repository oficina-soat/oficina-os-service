package br.com.oficina.os.framework.db;

import br.com.oficina.os.core.entities.cliente.DocumentoFactory;
import br.com.oficina.os.core.entities.cliente.Email;
import br.com.oficina.os.core.entities.ordem_de_servico.EstadoSaga;
import br.com.oficina.os.core.entities.ordem_de_servico.TipoDeEstadoDaOrdemDeServico;
import br.com.oficina.os.core.entities.veiculo.MarcaDeVeiculo;
import br.com.oficina.os.core.entities.veiculo.ModeloDeVeiculo;
import br.com.oficina.os.core.entities.veiculo.PlacaDeVeiculo;
import br.com.oficina.os.framework.messaging.DomainEventEnvelope;
import br.com.oficina.os.framework.messaging.OutboxEventRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class AtendimentoSeedStore {
    public static final UUID SEED_CLIENTE_ID = UUID.fromString("d290f1ee-6c54-4b01-90e6-d701748f0851");
    public static final UUID SEED_VEICULO_ID = UUID.fromString("7b1f1a8d-7f4a-4f25-8e74-27d50210a61e");
    public static final UUID SEED_ORDEM_SERVICO_ID = UUID.fromString("f3d87547-3f5f-4f3a-9e37-b8df70c31696");

    private final LinkedHashMap<UUID, ClienteRecord> clientes = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, VeiculoRecord> veiculos = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, OrdemServicoRecord> ordensServico = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, List<HistoricoRecord>> historicos = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, SagaRecord> sagasByOrdemServico = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, List<SagaHistoricoRecord>> sagaHistoricos = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, OutboxEventRecord> outboxEvents = new LinkedHashMap<>();
    private final Set<UUID> consumedEventIds = new LinkedHashSet<>();

    public AtendimentoSeedStore() {
        var seedTime = OffsetDateTime.of(2026, 6, 23, 15, 30, 0, 0, ZoneOffset.UTC);
        clientes.put(SEED_CLIENTE_ID, new ClienteRecord(
                SEED_CLIENTE_ID,
                "Maria Souza",
                "12345678901",
                "+5511999999999",
                "maria@example.com",
                seedTime,
                seedTime));
        veiculos.put(SEED_VEICULO_ID, new VeiculoRecord(
                SEED_VEICULO_ID,
                SEED_CLIENTE_ID,
                "ABC1D23",
                "Volkswagen",
                "Gol",
                2020,
                seedTime,
                seedTime));
        ordensServico.put(SEED_ORDEM_SERVICO_ID, new OrdemServicoRecord(
                SEED_ORDEM_SERVICO_ID,
                SEED_CLIENTE_ID,
                SEED_VEICULO_ID,
                "Veiculo nao liga",
                TipoDeEstadoDaOrdemDeServico.RECEBIDA,
                seedTime,
                seedTime));
        historicos.put(SEED_ORDEM_SERVICO_ID, new ArrayList<>(List.of(new HistoricoRecord(
                TipoDeEstadoDaOrdemDeServico.RECEBIDA,
                seedTime,
                "Ordem de servico recebida"))));
        criarSagaInicial(SEED_ORDEM_SERVICO_ID, seedTime, "seed");
    }

    public synchronized ClienteRecord criarCliente(String nome, String documento, String telefone, String email) {
        validarCliente(nome, documento, email);
        var agora = OffsetDateTime.now(ZoneOffset.UTC);
        var cliente = new ClienteRecord(UUID.randomUUID(), nome.trim(), documento.trim(), normalizar(telefone), normalizar(email), agora, agora);
        clientes.put(cliente.clienteId(), cliente);
        return cliente;
    }

    public synchronized List<ClienteRecord> listarClientes() {
        return clientes.values().stream()
                .sorted(Comparator.comparing(ClienteRecord::criadoEm))
                .toList();
    }

    public synchronized ClienteRecord buscarCliente(UUID clienteId) {
        var cliente = clientes.get(clienteId);
        if (cliente == null) {
            throw new NotFoundException("Cliente nao encontrado: " + clienteId);
        }
        return cliente;
    }

    public synchronized ClienteRecord atualizarCliente(UUID clienteId, String nome, String documento, String telefone, String email) {
        var atual = buscarCliente(clienteId);
        validarCliente(nome, documento, email);
        var atualizado = new ClienteRecord(
                atual.clienteId(),
                nome.trim(),
                documento.trim(),
                normalizar(telefone),
                normalizar(email),
                atual.criadoEm(),
                OffsetDateTime.now(ZoneOffset.UTC));
        clientes.put(clienteId, atualizado);
        return atualizado;
    }

    public synchronized VeiculoRecord criarVeiculo(UUID clienteId, String placa, String marca, String modelo, int ano) {
        buscarCliente(clienteId);
        validarVeiculo(placa, marca, modelo, ano);
        var agora = OffsetDateTime.now(ZoneOffset.UTC);
        var veiculo = new VeiculoRecord(UUID.randomUUID(), clienteId, placa.trim().toUpperCase(), marca.trim(), modelo.trim(), ano, agora, agora);
        veiculos.put(veiculo.veiculoId(), veiculo);
        return veiculo;
    }

    public synchronized List<VeiculoRecord> listarVeiculosDoCliente(UUID clienteId) {
        buscarCliente(clienteId);
        return veiculos.values().stream()
                .filter(veiculo -> veiculo.clienteId().equals(clienteId))
                .sorted(Comparator.comparing(VeiculoRecord::criadoEm))
                .toList();
    }

    public synchronized VeiculoRecord buscarVeiculo(UUID veiculoId) {
        var veiculo = veiculos.get(veiculoId);
        if (veiculo == null) {
            throw new NotFoundException("Veiculo nao encontrado: " + veiculoId);
        }
        return veiculo;
    }

    public synchronized VeiculoRecord atualizarVeiculo(UUID veiculoId, String placa, String marca, String modelo, int ano) {
        var atual = buscarVeiculo(veiculoId);
        validarVeiculo(placa, marca, modelo, ano);
        var atualizado = new VeiculoRecord(
                atual.veiculoId(),
                atual.clienteId(),
                placa.trim().toUpperCase(),
                marca.trim(),
                modelo.trim(),
                ano,
                atual.criadoEm(),
                OffsetDateTime.now(ZoneOffset.UTC));
        veiculos.put(veiculoId, atualizado);
        return atualizado;
    }

    public synchronized OrdemServicoRecord criarOrdemServico(UUID clienteId, UUID veiculoId, String descricaoProblema) {
        buscarCliente(clienteId);
        var veiculo = buscarVeiculo(veiculoId);
        if (!veiculo.clienteId().equals(clienteId)) {
            throw new WebApplicationException("Veiculo nao pertence ao cliente informado.", Response.Status.CONFLICT);
        }
        if (descricaoProblema == null || descricaoProblema.isBlank()) {
            throw new IllegalArgumentException("Descricao do problema e obrigatoria.");
        }
        var agora = OffsetDateTime.now(ZoneOffset.UTC);
        var ordem = new OrdemServicoRecord(
                UUID.randomUUID(),
                clienteId,
                veiculoId,
                descricaoProblema.trim(),
                TipoDeEstadoDaOrdemDeServico.RECEBIDA,
                agora,
                agora);
        ordensServico.put(ordem.ordemServicoId(), ordem);
        historicos.put(ordem.ordemServicoId(), new ArrayList<>(List.of(new HistoricoRecord(
                ordem.estado(),
                agora,
                "Ordem de servico recebida"))));
        criarSagaInicial(ordem.ordemServicoId(), agora, null);
        enfileirarEvento(
                "ordemDeServicoCriada",
                "oficina.os.ordem-de-servico-criada",
                ordem.ordemServicoId(),
                Map.of(
                        "ordemServicoId", ordem.ordemServicoId().toString(),
                        "clienteId", clienteId.toString(),
                        "veiculoId", veiculoId.toString(),
                        "estadoAtual", ordem.estado().name(),
                        "criadoEm", agora.toString(),
                        "descricaoProblema", ordem.descricaoProblema()),
                null,
                agora);
        return ordem;
    }

    public synchronized List<OrdemServicoRecord> listarOrdensServico(TipoDeEstadoDaOrdemDeServico estado) {
        return ordensServico.values().stream()
                .filter(ordem -> estado == null || ordem.estado() == estado)
                .sorted(Comparator.comparing(OrdemServicoRecord::criadoEm))
                .toList();
    }

    public synchronized OrdemServicoRecord buscarOrdemServico(UUID ordemServicoId) {
        var ordem = ordensServico.get(ordemServicoId);
        if (ordem == null) {
            throw new NotFoundException("Ordem de servico nao encontrada: " + ordemServicoId);
        }
        return ordem;
    }

    public synchronized List<HistoricoRecord> historico(UUID ordemServicoId) {
        buscarOrdemServico(ordemServicoId);
        return List.copyOf(historicos.getOrDefault(ordemServicoId, List.of()));
    }

    public synchronized OrdemServicoRecord alterarEstado(UUID ordemServicoId, TipoDeEstadoDaOrdemDeServico novoEstado, String motivo) {
        var atual = buscarOrdemServico(ordemServicoId);
        validarTransicao(atual.estado(), novoEstado);
        var atualizado = new OrdemServicoRecord(
                atual.ordemServicoId(),
                atual.clienteId(),
                atual.veiculoId(),
                atual.descricaoProblema(),
                novoEstado,
                atual.criadoEm(),
                OffsetDateTime.now(ZoneOffset.UTC));
        ordensServico.put(ordemServicoId, atualizado);
        historicos.computeIfAbsent(ordemServicoId, _ -> new ArrayList<>())
                .add(new HistoricoRecord(novoEstado, atualizado.atualizadoEm(), normalizar(motivo)));
        if (novoEstado == TipoDeEstadoDaOrdemDeServico.ENTREGUE) {
            finalizarSagaComEntrega(atualizado, normalizar(motivo));
        }
        return atualizado;
    }

    public synchronized OperacaoAssincronaRecord cancelar(UUID ordemServicoId, String motivo) {
        buscarOrdemServico(ordemServicoId);
        compensarSaga(ordemServicoId, normalizar(motivo));
        return new OperacaoAssincronaRecord("ACEITO", OffsetDateTime.now(ZoneOffset.UTC));
    }

    public synchronized SagaRecord buscarSaga(UUID ordemServicoId) {
        buscarOrdemServico(ordemServicoId);
        return sagasByOrdemServico.get(ordemServicoId);
    }

    public synchronized List<SagaHistoricoRecord> historicoSaga(UUID ordemServicoId) {
        var saga = buscarSaga(ordemServicoId);
        return saga == null ? List.of() : List.copyOf(sagaHistoricos.getOrDefault(saga.sagaId(), List.of()));
    }

    public synchronized List<OutboxEventRecord> listarOutbox() {
        return List.copyOf(outboxEvents.values());
    }

    public synchronized List<OutboxEventRecord> publicarEventosPendentes() {
        var agora = OffsetDateTime.now(ZoneOffset.UTC);
        var publicados = new ArrayList<OutboxEventRecord>();
        for (var event : new ArrayList<>(outboxEvents.values())) {
            if (!"PENDING".equals(event.status())) {
                continue;
            }
            var publicado = new OutboxEventRecord(
                    event.eventId(),
                    event.aggregateId(),
                    event.eventType(),
                    event.eventVersion(),
                    event.topic(),
                    event.producer(),
                    event.payload(),
                    "PUBLISHED",
                    event.correlationId(),
                    event.occurredAt(),
                    event.createdAt(),
                    agora,
                    event.attempts() + 1,
                    null);
            outboxEvents.put(publicado.eventId(), publicado);
            publicados.add(publicado);
        }
        return publicados;
    }

    public synchronized SagaRecord consumirEvento(DomainEventEnvelope event) {
        if (consumedEventIds.contains(event.eventId())) {
            return sagasByOrdemServico.get(event.aggregateId());
        }
        var ordemServicoId = uuidFromPayload(event.payload(), "ordemServicoId", event.aggregateId());
        var saga = buscarSaga(ordemServicoId);
        if (saga == null) {
            throw new NotFoundException("Saga da ordem de servico nao encontrada: " + ordemServicoId);
        }
        consumedEventIds.add(event.eventId());

        return switch (event.eventType()) {
            case "diagnosticoIniciado" -> processarDiagnosticoIniciado(saga, event);
            case "diagnosticoFinalizado" -> processarDiagnosticoFinalizado(saga, event);
            case "orcamentoGerado" -> transicionarSaga(
                    saga,
                    EstadoSaga.AGUARDANDO_APROVACAO,
                    buscarOrdemServico(ordemServicoId).estado(),
                    "orcamentoGerado",
                    null,
                    saga.execucaoId(),
                    uuidFromPayload(event.payload(), "orcamentoId", saga.orcamentoId()),
                    saga.pagamentoId(),
                    event);
            case "orcamentoAprovado" -> transicionarSaga(
                    saga,
                    EstadoSaga.EM_EXECUCAO,
                    buscarOrdemServico(ordemServicoId).estado(),
                    "orcamentoAprovado",
                    null,
                    saga.execucaoId(),
                    uuidFromPayload(event.payload(), "orcamentoId", saga.orcamentoId()),
                    saga.pagamentoId(),
                    event);
            case "orcamentoRecusado" -> processarOrcamentoRecusado(saga, event);
            case "execucaoIniciada" -> processarExecucaoIniciada(saga, event);
            case "execucaoFinalizada" -> processarExecucaoFinalizada(saga, event);
            case "pagamentoSolicitado" -> transicionarSaga(
                    saga,
                    EstadoSaga.AGUARDANDO_PAGAMENTO,
                    buscarOrdemServico(ordemServicoId).estado(),
                    "pagamentoSolicitado",
                    null,
                    saga.execucaoId(),
                    uuidFromPayload(event.payload(), "orcamentoId", saga.orcamentoId()),
                    uuidFromPayload(event.payload(), "pagamentoId", saga.pagamentoId()),
                    event);
            case "pagamentoConfirmado" -> transicionarSaga(
                    saga,
                    EstadoSaga.AGUARDANDO_ENTREGA,
                    buscarOrdemServico(ordemServicoId).estado(),
                    "pagamentoConfirmado",
                    null,
                    saga.execucaoId(),
                    saga.orcamentoId(),
                    uuidFromPayload(event.payload(), "pagamentoId", saga.pagamentoId()),
                    event);
            case "pagamentoRecusado" -> transicionarSaga(
                    saga,
                    EstadoSaga.AGUARDANDO_PAGAMENTO,
                    buscarOrdemServico(ordemServicoId).estado(),
                    "pagamentoRecusado",
                    stringFromPayload(event.payload(), "motivo"),
                    saga.execucaoId(),
                    saga.orcamentoId(),
                    uuidFromPayload(event.payload(), "pagamentoId", saga.pagamentoId()),
                    event);
            default -> saga;
        };
    }

    private void compensarSaga(UUID ordemServicoId, String motivo) {
        var saga = sagasByOrdemServico.get(ordemServicoId);
        if (saga == null || saga.estado() == EstadoSaga.COMPENSADA || saga.estado() == EstadoSaga.FINALIZADA_COM_SUCESSO) {
            return;
        }
        var agora = OffsetDateTime.now(ZoneOffset.UTC);
        var emCompensacao = transicionarSaga(
                saga,
                EstadoSaga.EM_COMPENSACAO,
                buscarOrdemServico(ordemServicoId).estado(),
                "cancelamentoSolicitado",
                motivo,
                saga.execucaoId(),
                saga.orcamentoId(),
                saga.pagamentoId(),
                agora,
                saga.correlationId());
        transicionarSaga(
                emCompensacao,
                EstadoSaga.COMPENSADA,
                buscarOrdemServico(ordemServicoId).estado(),
                "sagaCompensada",
                motivo,
                saga.execucaoId(),
                saga.orcamentoId(),
                saga.pagamentoId(),
                agora,
                saga.correlationId());
        enfileirarEvento(
                "sagaCompensada",
                "oficina.saga.saga-compensada",
                ordemServicoId,
                Map.of(
                        "sagaId", saga.sagaId().toString(),
                        "ordemServicoId", ordemServicoId.toString(),
                        "motivo", motivo == null ? "Cancelamento solicitado" : motivo,
                        "compensadaEm", agora.toString()),
                saga.correlationId(),
                agora);
    }

    private SagaRecord processarDiagnosticoIniciado(SagaRecord saga, DomainEventEnvelope event) {
        var ordem = buscarOrdemServico(saga.ordemServicoId());
        if (ordem.estado() == TipoDeEstadoDaOrdemDeServico.RECEBIDA) {
            alterarEstado(ordem.ordemServicoId(), TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO, "Diagnostico iniciado");
        }
        return transicionarSaga(
                saga,
                EstadoSaga.EM_DIAGNOSTICO,
                TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO,
                "diagnosticoIniciado",
                null,
                uuidFromPayload(event.payload(), "execucaoId", saga.execucaoId()),
                saga.orcamentoId(),
                saga.pagamentoId(),
                event);
    }

    private SagaRecord processarDiagnosticoFinalizado(SagaRecord saga, DomainEventEnvelope event) {
        var ordem = buscarOrdemServico(saga.ordemServicoId());
        if (ordem.estado() == TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO) {
            alterarEstado(ordem.ordemServicoId(), TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO, "Diagnostico finalizado");
        }
        return transicionarSaga(
                saga,
                EstadoSaga.AGUARDANDO_ORCAMENTO,
                TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO,
                "diagnosticoFinalizado",
                null,
                uuidFromPayload(event.payload(), "execucaoId", saga.execucaoId()),
                saga.orcamentoId(),
                saga.pagamentoId(),
                event);
    }

    private SagaRecord processarOrcamentoRecusado(SagaRecord saga, DomainEventEnvelope event) {
        var ordem = buscarOrdemServico(saga.ordemServicoId());
        if (ordem.estado() == TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO) {
            alterarEstado(ordem.ordemServicoId(), TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO, "Orcamento recusado");
        }
        return transicionarSaga(
                saga,
                EstadoSaga.EM_DIAGNOSTICO,
                TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO,
                "orcamentoRecusado",
                stringFromPayload(event.payload(), "motivo"),
                saga.execucaoId(),
                uuidFromPayload(event.payload(), "orcamentoId", saga.orcamentoId()),
                saga.pagamentoId(),
                event);
    }

    private SagaRecord processarExecucaoIniciada(SagaRecord saga, DomainEventEnvelope event) {
        var ordem = buscarOrdemServico(saga.ordemServicoId());
        if (ordem.estado() == TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO) {
            alterarEstado(ordem.ordemServicoId(), TipoDeEstadoDaOrdemDeServico.EM_EXECUCAO, "Execucao iniciada");
        }
        return transicionarSaga(
                saga,
                EstadoSaga.EM_EXECUCAO,
                TipoDeEstadoDaOrdemDeServico.EM_EXECUCAO,
                "execucaoIniciada",
                null,
                uuidFromPayload(event.payload(), "execucaoId", saga.execucaoId()),
                saga.orcamentoId(),
                saga.pagamentoId(),
                event);
    }

    private SagaRecord processarExecucaoFinalizada(SagaRecord saga, DomainEventEnvelope event) {
        var ordem = buscarOrdemServico(saga.ordemServicoId());
        if (ordem.estado() == TipoDeEstadoDaOrdemDeServico.EM_EXECUCAO) {
            ordem = alterarEstado(ordem.ordemServicoId(), TipoDeEstadoDaOrdemDeServico.FINALIZADA, "Execucao finalizada");
        }
        enfileirarEvento(
                "ordemDeServicoFinalizada",
                "oficina.os.ordem-de-servico-finalizada",
                ordem.ordemServicoId(),
                Map.of(
                        "ordemServicoId", ordem.ordemServicoId().toString(),
                        "estadoAnterior", TipoDeEstadoDaOrdemDeServico.EM_EXECUCAO.name(),
                        "estadoAtual", TipoDeEstadoDaOrdemDeServico.FINALIZADA.name(),
                        "finalizadaEm", OffsetDateTime.now(ZoneOffset.UTC).toString()),
                event.eventId().toString(),
                OffsetDateTime.now(ZoneOffset.UTC));
        return transicionarSaga(
                saga,
                EstadoSaga.AGUARDANDO_PAGAMENTO,
                TipoDeEstadoDaOrdemDeServico.FINALIZADA,
                "execucaoFinalizada",
                null,
                uuidFromPayload(event.payload(), "execucaoId", saga.execucaoId()),
                saga.orcamentoId(),
                saga.pagamentoId(),
                event);
    }

    private void finalizarSagaComEntrega(OrdemServicoRecord ordem, String motivo) {
        var saga = sagasByOrdemServico.get(ordem.ordemServicoId());
        if (saga == null || saga.estado() == EstadoSaga.FINALIZADA_COM_SUCESSO) {
            return;
        }
        var agora = OffsetDateTime.now(ZoneOffset.UTC);
        enfileirarEvento(
                "ordemDeServicoEntregue",
                "oficina.os.ordem-de-servico-entregue",
                ordem.ordemServicoId(),
                Map.of(
                        "ordemServicoId", ordem.ordemServicoId().toString(),
                        "estadoAnterior", TipoDeEstadoDaOrdemDeServico.FINALIZADA.name(),
                        "estadoAtual", TipoDeEstadoDaOrdemDeServico.ENTREGUE.name(),
                        "entregueEm", agora.toString()),
                saga.correlationId(),
                agora);
        enfileirarEvento(
                "sagaFinalizadaComSucesso",
                "oficina.saga.saga-finalizada-com-sucesso",
                ordem.ordemServicoId(),
                Map.of(
                        "sagaId", saga.sagaId().toString(),
                        "ordemServicoId", ordem.ordemServicoId().toString(),
                        "finalizadaEm", agora.toString()),
                saga.correlationId(),
                agora);
        transicionarSaga(saga, EstadoSaga.FINALIZADA_COM_SUCESSO, TipoDeEstadoDaOrdemDeServico.ENTREGUE, "entregaFinalizada", motivo,
                saga.execucaoId(), saga.orcamentoId(), saga.pagamentoId(), agora, saga.correlationId());
    }

    private SagaRecord criarSagaInicial(UUID ordemServicoId, OffsetDateTime agora, String correlationId) {
        var saga = new SagaRecord(
                UUID.randomUUID(),
                ordemServicoId,
                EstadoSaga.INICIADA,
                TipoDeEstadoDaOrdemDeServico.RECEBIDA,
                "ordemDeServicoCriada",
                null,
                null,
                null,
                correlationId,
                agora,
                agora,
                null);
        sagasByOrdemServico.put(ordemServicoId, saga);
        sagaHistoricos.put(saga.sagaId(), new ArrayList<>(List.of(new SagaHistoricoRecord(
                saga.sagaId(),
                null,
                EstadoSaga.INICIADA,
                TipoDeEstadoDaOrdemDeServico.RECEBIDA,
                "ordemDeServicoCriada",
                null,
                agora))));
        return saga;
    }

    private SagaRecord transicionarSaga(
            SagaRecord saga,
            EstadoSaga novoEstado,
            TipoDeEstadoDaOrdemDeServico estadoOrdemServico,
            String etapa,
            String motivo,
            UUID execucaoId,
            UUID orcamentoId,
            UUID pagamentoId,
            DomainEventEnvelope event) {
        return transicionarSaga(saga, novoEstado, estadoOrdemServico, etapa, motivo, execucaoId, orcamentoId, pagamentoId,
                event.occurredAt(), event.eventId().toString());
    }

    private SagaRecord transicionarSaga(
            SagaRecord saga,
            EstadoSaga novoEstado,
            TipoDeEstadoDaOrdemDeServico estadoOrdemServico,
            String etapa,
            String motivo,
            UUID execucaoId,
            UUID orcamentoId,
            UUID pagamentoId,
            OffsetDateTime ocorridoEm,
            String correlationId) {
        if (saga.estado() == novoEstado && etapa.equals(saga.ultimaEtapa())) {
            return saga;
        }
        var atualizada = new SagaRecord(
                saga.sagaId(),
                saga.ordemServicoId(),
                novoEstado,
                estadoOrdemServico,
                etapa,
                execucaoId,
                orcamentoId,
                pagamentoId,
                correlationId,
                saga.criadoEm(),
                ocorridoEm,
                motivo);
        sagasByOrdemServico.put(saga.ordemServicoId(), atualizada);
        sagaHistoricos.computeIfAbsent(saga.sagaId(), _ -> new ArrayList<>())
                .add(new SagaHistoricoRecord(saga.sagaId(), saga.estado(), novoEstado, estadoOrdemServico, etapa, motivo, ocorridoEm));
        return atualizada;
    }

    private void enfileirarEvento(
            String eventType,
            String topic,
            UUID aggregateId,
            Map<String, Object> payload,
            String correlationId,
            OffsetDateTime occurredAt) {
        var event = new OutboxEventRecord(
                UUID.randomUUID(),
                aggregateId,
                eventType,
                1,
                topic,
                "oficina-os-service",
                payload,
                "PENDING",
                correlationId,
                occurredAt,
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                0,
                null);
        outboxEvents.put(event.eventId(), event);
    }

    private static UUID uuidFromPayload(Map<String, Object> payload, String fieldName, UUID fallback) {
        var value = payload.get(fieldName);
        if (value == null) {
            return fallback;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(value.toString());
    }

    private static String stringFromPayload(Map<String, Object> payload, String fieldName) {
        var value = payload.get(fieldName);
        return value == null ? null : value.toString();
    }

    private static void validarCliente(String nome, String documento, String email) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do cliente e obrigatorio.");
        }
        DocumentoFactory.from(documento);
        if (email != null && !email.isBlank()) {
            new Email(email);
        }
    }

    private static void validarVeiculo(String placa, String marca, String modelo, int ano) {
        new PlacaDeVeiculo(placa);
        new MarcaDeVeiculo(marca);
        new ModeloDeVeiculo(modelo);
        if (ano < 1900) {
            throw new IllegalArgumentException("Ano do veiculo deve ser maior ou igual a 1900.");
        }
    }

    private static void validarTransicao(TipoDeEstadoDaOrdemDeServico atual, TipoDeEstadoDaOrdemDeServico novo) {
        boolean valida = switch (atual) {
            case RECEBIDA -> novo == TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO;
            case EM_DIAGNOSTICO -> novo == TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO;
            case AGUARDANDO_APROVACAO -> novo == TipoDeEstadoDaOrdemDeServico.EM_EXECUCAO
                    || novo == TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO;
            case EM_EXECUCAO -> novo == TipoDeEstadoDaOrdemDeServico.FINALIZADA;
            case FINALIZADA -> novo == TipoDeEstadoDaOrdemDeServico.ENTREGUE;
            case ENTREGUE -> false;
        };
        if (!valida) {
            throw new WebApplicationException("Transicao de estado invalida: " + atual + " -> " + novo, Response.Status.CONFLICT);
        }
    }

    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    public record ClienteRecord(
            UUID clienteId,
            String nome,
            String documento,
            String telefone,
            String email,
            OffsetDateTime criadoEm,
            OffsetDateTime atualizadoEm) {
    }

    public record VeiculoRecord(
            UUID veiculoId,
            UUID clienteId,
            String placa,
            String marca,
            String modelo,
            int ano,
            OffsetDateTime criadoEm,
            OffsetDateTime atualizadoEm) {
    }

    public record OrdemServicoRecord(
            UUID ordemServicoId,
            UUID clienteId,
            UUID veiculoId,
            String descricaoProblema,
            TipoDeEstadoDaOrdemDeServico estado,
            OffsetDateTime criadoEm,
            OffsetDateTime atualizadoEm) {
    }

    public record HistoricoRecord(
            TipoDeEstadoDaOrdemDeServico estado,
            OffsetDateTime dataDoEstado,
            String motivo) {
    }

    public record OperacaoAssincronaRecord(
            String status,
            OffsetDateTime solicitadoEm) {
    }

    public record SagaRecord(
            UUID sagaId,
            UUID ordemServicoId,
            EstadoSaga estado,
            TipoDeEstadoDaOrdemDeServico estadoOrdemServico,
            String ultimaEtapa,
            UUID execucaoId,
            UUID orcamentoId,
            UUID pagamentoId,
            String correlationId,
            OffsetDateTime criadoEm,
            OffsetDateTime atualizadoEm,
            String ultimoErro) {
    }

    public record SagaHistoricoRecord(
            UUID sagaId,
            EstadoSaga estadoAnterior,
            EstadoSaga estadoAtual,
            TipoDeEstadoDaOrdemDeServico estadoOrdemServico,
            String etapa,
            String motivo,
            OffsetDateTime ocorridoEm) {
    }
}
