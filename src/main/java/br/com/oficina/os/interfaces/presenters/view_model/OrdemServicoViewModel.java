package br.com.oficina.os.interfaces.presenters.view_model;

import br.com.oficina.os.core.entities.ordem_de_servico.TipoDeEstadoDaOrdemDeServico;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrdemServicoViewModel(
        UUID ordemServicoId,
        UUID clienteId,
        UUID veiculoId,
        String descricaoProblema,
        TipoDeEstadoDaOrdemDeServico estado,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm) {
}
