package br.com.oficina.os.core.entities.ordem_de_servico;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class OrdemDeServicoFactory {
    public static OrdemDeServico criarNovo(
            UUID clienteId,
            UUID veiculoId) {
        return new OrdemDeServico(
                UUID.randomUUID(),
                clienteId,
                veiculoId,
                new EstadoDaOrdemDeServico(
                        TipoDeEstadoDaOrdemDeServico.RECEBIDA,
                        Instant.now()));
    }

    public static OrdemDeServico reconstituiCompleto(
            UUID id,
            UUID clienteId,
            UUID veiculoId,
            EstadoDaOrdemDeServico estadoAtual,
            List<EstadoDaOrdemDeServico> historicoDeEstados,
            List<ItemPeca> pecas,
            List<ItemServico> servicos) {
        return new OrdemDeServico(
                id,
                clienteId,
                veiculoId,
                estadoAtual,
                historicoDeEstados,
                pecas,
                servicos);
    }

    public static OrdemDeServico reconstituiSimples(
            UUID id,
            UUID clienteId,
            UUID veiculoId,
            EstadoDaOrdemDeServico estadoAtual) {
        return new OrdemDeServico(
                id,
                clienteId,
                veiculoId,
                estadoAtual);
    }
}
