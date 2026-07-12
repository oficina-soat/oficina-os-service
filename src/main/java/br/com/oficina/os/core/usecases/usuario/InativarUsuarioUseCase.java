package br.com.oficina.os.core.usecases.usuario;

import br.com.oficina.os.core.interfaces.gateway.UsuarioGateway;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class InativarUsuarioUseCase {
    private final UsuarioGateway gateway;

    public InativarUsuarioUseCase(UsuarioGateway gateway) {
        this.gateway = gateway;
    }

    public CompletableFuture<Void> executar(UUID usuarioId) {
        gateway.inativar(usuarioId);
        return CompletableFuture.completedFuture(null);
    }
}
