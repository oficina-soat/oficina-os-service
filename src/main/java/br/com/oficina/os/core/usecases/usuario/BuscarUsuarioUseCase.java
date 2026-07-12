package br.com.oficina.os.core.usecases.usuario;

import br.com.oficina.os.core.entities.usuario.Usuario;
import br.com.oficina.os.core.interfaces.gateway.UsuarioGateway;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BuscarUsuarioUseCase {
    private final UsuarioGateway gateway;

    public BuscarUsuarioUseCase(UsuarioGateway gateway) {
        this.gateway = gateway;
    }

    public CompletableFuture<Usuario> executar(UUID usuarioId) {
        return CompletableFuture.completedFuture(gateway.buscar(usuarioId));
    }
}
