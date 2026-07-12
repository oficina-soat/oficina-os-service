package br.com.oficina.os.core.usecases.usuario;

import br.com.oficina.os.core.entities.usuario.Usuario;
import br.com.oficina.os.core.interfaces.gateway.UsuarioGateway;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CriarUsuarioUseCase {
    private final UsuarioGateway gateway;

    public CriarUsuarioUseCase(UsuarioGateway gateway) {
        this.gateway = gateway;
    }

    public CompletableFuture<Usuario> executar(Command command) {
        var pessoa = UsuarioCommandSupport.pessoa(UUID.randomUUID(), command.nome(), command.documento());
        var usuario = new Usuario(
                UUID.randomUUID(),
                pessoa,
                UsuarioCommandSupport.statusCriacao(command.status()),
                UsuarioCommandSupport.papeis(command.papeis()));
        return CompletableFuture.completedFuture(gateway.criar(usuario));
    }

    public record Command(String nome, String documento, String status, List<String> papeis) {
    }
}
