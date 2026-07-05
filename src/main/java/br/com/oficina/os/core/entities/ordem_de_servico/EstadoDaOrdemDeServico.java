package br.com.oficina.os.core.entities.ordem_de_servico;

import java.time.Instant;

public record EstadoDaOrdemDeServico(
        TipoDeEstadoDaOrdemDeServico estado,
        Instant dataDoEstado) {
}
