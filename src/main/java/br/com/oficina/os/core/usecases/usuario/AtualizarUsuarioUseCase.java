package br.com.oficina.os.core.usecases.usuario;

import br.com.oficina.os.core.entities.usuario.Usuario;
import br.com.oficina.os.core.interfaces.gateway.UsuarioGateway;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AtualizarUsuarioUseCase {
    private final UsuarioGateway gateway;

    public AtualizarUsuarioUseCase(UsuarioGateway gateway) {
        this.gateway = gateway;
    }

    public CompletableFuture<Usuario> executar(Command command) {
        var atual = gateway.buscar(command.usuarioId());
        var pessoa = UsuarioCommandSupport.pessoa(atual.pessoa().id(), command.nome(), command.documento());
        var atualizado = atual.atualizado(
                pessoa,
                UsuarioCommandSupport.statusAtualizacao(command.status()),
                UsuarioCommandSupport.papeis(command.papeis()));
        return CompletableFuture.completedFuture(gateway.atualizar(atualizado));
    }

    public record Command(
            UUID usuarioId,
            String nome,
            String documento,
            String status,
            List<String> papeis) {
    }
}
