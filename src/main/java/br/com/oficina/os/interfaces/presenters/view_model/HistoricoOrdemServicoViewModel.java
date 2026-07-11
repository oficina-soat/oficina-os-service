package br.com.oficina.os.interfaces.presenters.view_model;

import br.com.oficina.os.core.entities.ordem_de_servico.TipoDeEstadoDaOrdemDeServico;
import java.time.OffsetDateTime;

public record HistoricoOrdemServicoViewModel(
        TipoDeEstadoDaOrdemDeServico estado,
        OffsetDateTime dataDoEstado,
        String motivo) {
}
