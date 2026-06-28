package br.com.oficina.os.framework.db;

import br.com.oficina.os.core.entities.cliente.DocumentoFactory;
import br.com.oficina.os.core.entities.cliente.Email;
import br.com.oficina.os.core.entities.ordem_de_servico.TipoDeEstadoDaOrdemDeServico;
import br.com.oficina.os.core.entities.veiculo.MarcaDeVeiculo;
import br.com.oficina.os.core.entities.veiculo.ModeloDeVeiculo;
import br.com.oficina.os.core.entities.veiculo.PlacaDeVeiculo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
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
        return atualizado;
    }

    public synchronized OperacaoAssincronaRecord cancelar(UUID ordemServicoId, String motivo) {
        buscarOrdemServico(ordemServicoId);
        return new OperacaoAssincronaRecord("ACEITO", OffsetDateTime.now(ZoneOffset.UTC));
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
}
