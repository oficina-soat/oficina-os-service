package br.com.oficina.os.core.interfaces.gateway;

import br.com.oficina.os.core.entities.ordem_de_servico.EstadoSaga;
import br.com.oficina.os.core.entities.ordem_de_servico.TipoDeEstadoDaOrdemDeServico;
import br.com.oficina.os.core.interfaces.messaging.DomainEventEnvelope;
import br.com.oficina.os.core.interfaces.messaging.OutboxEventRecord;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AtendimentoGateway {
    UUID SEED_CLIENTE_ID = UUID.fromString("d290f1ee-6c54-4b01-90e6-d701748f0851");
    UUID SEED_VEICULO_ID = UUID.fromString("7b1f1a8d-7f4a-4f25-8e74-27d50210a61e");
    UUID SEED_ORDEM_SERVICO_ID = UUID.fromString("f05dd17b-daae-4658-af7c-363dd6e6fdfb");

    ClienteRecord criarCliente(String nome, String documento, String telefone, String email);

    default List<ClienteRecord> listarClientes() {
        return listarClientes(new ClienteSearchCriteria(null, null, null));
    }

    List<ClienteRecord> listarClientes(ClienteSearchCriteria criteria);

    ClienteRecord buscarCliente(UUID clienteId);

    ClienteRecord atualizarCliente(UUID clienteId, String nome, String documento, String telefone, String email);

    VeiculoRecord criarVeiculo(UUID clienteId, String placa, String marca, String modelo, int ano);

    List<VeiculoRecord> listarVeiculosDoCliente(UUID clienteId);

    VeiculoRecord buscarVeiculo(UUID veiculoId);

    VeiculoRecord atualizarVeiculo(UUID veiculoId, String placa, String marca, String modelo, int ano);

    OrdemServicoRecord criarOrdemServico(UUID clienteId, UUID veiculoId, String descricaoProblema);

    List<OrdemServicoRecord> listarOrdensServico(TipoDeEstadoDaOrdemDeServico estado);

    OrdemServicoRecord buscarOrdemServico(UUID ordemServicoId);

    List<HistoricoRecord> historico(UUID ordemServicoId);

    OrdemServicoRecord alterarEstado(UUID ordemServicoId, TipoDeEstadoDaOrdemDeServico novoEstado, String motivo);

    OperacaoAssincronaRecord cancelar(UUID ordemServicoId, String motivo);

    SagaRecord buscarSaga(UUID ordemServicoId);

    List<SagaHistoricoRecord> historicoSaga(UUID ordemServicoId);

    List<OutboxEventRecord> listarOutbox();

    List<OutboxEventRecord> publicarEventosPendentes();

    List<OutboxEventRecord> listarEventosPendentesParaPublicacao(int limit);

    OutboxEventRecord marcarEventoPublicado(UUID eventId);

    OutboxEventRecord marcarFalhaPublicacao(UUID eventId, String lastError, OffsetDateTime nextAttemptAt, boolean failed);

    SagaRecord consumirEvento(DomainEventEnvelope event);

    record ClienteRecord(
            UUID clienteId,
            String nome,
            String documento,
            String telefone,
            String email,
            OffsetDateTime criadoEm,
            OffsetDateTime atualizadoEm) {
    }

    record ClienteSearchCriteria(String nome, String documento, String email) {
        public ClienteSearchCriteria {
            nome = normalizar(nome);
            documento = normalizar(documento);
            email = normalizar(email);
        }

        private static String normalizar(String value) {
            return value == null || value.isBlank() ? null : value.trim();
        }
    }

    record VeiculoRecord(
            UUID veiculoId,
            UUID clienteId,
            String placa,
            String marca,
            String modelo,
            int ano,
            OffsetDateTime criadoEm,
            OffsetDateTime atualizadoEm) {
    }

    record OrdemServicoRecord(
            UUID ordemServicoId,
            UUID clienteId,
            UUID veiculoId,
            String descricaoProblema,
            TipoDeEstadoDaOrdemDeServico estado,
            OffsetDateTime criadoEm,
            OffsetDateTime atualizadoEm) {
    }

    record HistoricoRecord(
            TipoDeEstadoDaOrdemDeServico estado,
            OffsetDateTime dataDoEstado,
            String motivo) {
    }

    record OperacaoAssincronaRecord(
            String status,
            OffsetDateTime solicitadoEm) {
    }

    record SagaRecord(
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

    record SagaHistoricoRecord(
            UUID sagaId,
            EstadoSaga estadoAnterior,
            EstadoSaga estadoAtual,
            TipoDeEstadoDaOrdemDeServico estadoOrdemServico,
            String etapa,
            String motivo,
            OffsetDateTime ocorridoEm) {
    }
}
