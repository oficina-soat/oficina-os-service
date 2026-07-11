package br.com.oficina.os.core.usecases.cliente;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.ClienteRecord;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AtualizarClienteUseCase {
    private final AtendimentoGateway gateway;

    public AtualizarClienteUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public CompletableFuture<ClienteRecord> executar(Command command) {
        return CompletableFuture.completedFuture(gateway.atualizarCliente(
                command.clienteId(),
                command.nome(),
                command.documento(),
                command.telefone(),
                command.email()));
    }

    public record Command(
            UUID clienteId,
            String nome,
            String documento,
            String telefone,
            String email) {
    }
}
