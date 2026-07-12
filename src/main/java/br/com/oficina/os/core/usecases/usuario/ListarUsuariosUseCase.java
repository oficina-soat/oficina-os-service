package br.com.oficina.os.core.usecases.usuario;

import br.com.oficina.os.core.entities.usuario.Usuario;
import br.com.oficina.os.core.interfaces.gateway.UsuarioGateway;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ListarUsuariosUseCase {
    private final UsuarioGateway gateway;

    public ListarUsuariosUseCase(UsuarioGateway gateway) {
        this.gateway = gateway;
    }

    public CompletableFuture<List<Usuario>> executar() {
        return CompletableFuture.completedFuture(gateway.listar());
    }
}
