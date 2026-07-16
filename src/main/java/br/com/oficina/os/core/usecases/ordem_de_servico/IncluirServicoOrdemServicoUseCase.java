package br.com.oficina.os.core.usecases.ordem_de_servico;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.CatalogoGateway;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class IncluirServicoOrdemServicoUseCase {
    private final AtendimentoGateway atendimento;
    private final CatalogoGateway catalogo;

    public IncluirServicoOrdemServicoUseCase(AtendimentoGateway atendimento, CatalogoGateway catalogo) {
        this.atendimento = atendimento;
        this.catalogo = catalogo;
    }

    public CompletableFuture<AtendimentoGateway.OrdemServicoRecord> executar(Command command) {
        validar(command.servicoId(), command.quantidade());
        var item = catalogo.buscarServico(command.servicoId(), command.correlationId());
        if (!item.ativo()) {
            throw new IllegalArgumentException("Servico inativo nao pode ser incluido na ordem de servico.");
        }
        return CompletableFuture.completedFuture(atendimento.incluirServico(
                command.ordemServicoId(),
                new AtendimentoGateway.ItemServicoRecord(item.id(), item.nome(), command.quantidade(), item.valorUnitario()),
                command.correlationId()));
    }

    private static void validar(UUID id, BigDecimal quantidade) {
        if (id == null || quantidade == null || quantidade.signum() <= 0) {
            throw new IllegalArgumentException("Servico e quantidade positiva sao obrigatorios.");
        }
    }

    public record Command(UUID ordemServicoId, UUID servicoId, BigDecimal quantidade, String correlationId) {
    }
}
