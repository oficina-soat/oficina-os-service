package br.com.oficina.os.core.usecases.cliente;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.ClienteRecord;
import java.util.concurrent.CompletableFuture;

public class CriarClienteUseCase {
    private final AtendimentoGateway gateway;

    public CriarClienteUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public CompletableFuture<ClienteRecord> executar(Command command) {
        return CompletableFuture.completedFuture(gateway.criarCliente(
                command.nome(),
                command.documento(),
                command.telefone(),
                command.email()));
    }

    public record Command(
            String nome,
            String documento,
            String telefone,
            String email) {
    }
}
