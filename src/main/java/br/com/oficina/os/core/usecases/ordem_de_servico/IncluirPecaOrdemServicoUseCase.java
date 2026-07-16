package br.com.oficina.os.core.usecases.ordem_de_servico;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.CatalogoGateway;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class IncluirPecaOrdemServicoUseCase {
    private final AtendimentoGateway atendimento;
    private final CatalogoGateway catalogo;

    public IncluirPecaOrdemServicoUseCase(AtendimentoGateway atendimento, CatalogoGateway catalogo) {
        this.atendimento = atendimento;
        this.catalogo = catalogo;
    }

    public CompletableFuture<AtendimentoGateway.OrdemServicoRecord> executar(Command command) {
        validar(command.pecaId(), command.quantidade());
        var item = catalogo.buscarPeca(command.pecaId(), command.correlationId());
        if (!item.ativo()) {
            throw new IllegalArgumentException("Peca inativa nao pode ser incluida na ordem de servico.");
        }
        return CompletableFuture.completedFuture(atendimento.incluirPeca(
                command.ordemServicoId(),
                new AtendimentoGateway.ItemPecaRecord(item.id(), item.nome(), command.quantidade(), item.valorUnitario()),
                command.correlationId()));
    }

    private static void validar(UUID id, BigDecimal quantidade) {
        if (id == null || quantidade == null || quantidade.signum() <= 0) {
            throw new IllegalArgumentException("Peca e quantidade positiva sao obrigatorias.");
        }
    }

    public record Command(UUID ordemServicoId, UUID pecaId, BigDecimal quantidade, String correlationId) {
    }
}
