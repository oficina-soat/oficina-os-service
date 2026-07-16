package br.com.oficina.os.interfaces.presenters.view_model;

import br.com.oficina.os.core.entities.ordem_de_servico.TipoDeEstadoDaOrdemDeServico;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;
import java.math.BigDecimal;
import br.com.oficina.os.core.entities.ordem_de_servico.AcaoPermitidaOrdemServico;

public record OrdemServicoViewModel(
        UUID ordemServicoId,
        UUID clienteId,
        UUID veiculoId,
        String descricaoProblema,
        TipoDeEstadoDaOrdemDeServico estado,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm,
        List<ItemServicoViewModel> servicos,
        List<ItemPecaViewModel> pecas,
        List<AcaoPermitidaOrdemServico> acoesPermitidas) {
    public record ItemServicoViewModel(UUID servicoId, String nome, BigDecimal quantidade,
            BigDecimal valorUnitario, BigDecimal valorTotal) {
    }

    public record ItemPecaViewModel(UUID pecaId, String nome, BigDecimal quantidade,
            BigDecimal valorUnitario, BigDecimal valorTotal) {
    }
}
