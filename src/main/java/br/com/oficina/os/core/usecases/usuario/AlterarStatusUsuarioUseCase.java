package br.com.oficina.os.core.usecases.usuario;

import br.com.oficina.os.core.entities.usuario.Usuario;
import br.com.oficina.os.core.entities.usuario.UsuarioStatus;
import br.com.oficina.os.core.exceptions.UsuarioConflitanteException;
import br.com.oficina.os.core.interfaces.gateway.UsuarioGateway;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AlterarStatusUsuarioUseCase {
    private final UsuarioGateway gateway;

    public AlterarStatusUsuarioUseCase(UsuarioGateway gateway) {
        this.gateway = gateway;
    }

    public CompletableFuture<Usuario> executar(Command command) {
        var atual = gateway.buscar(command.usuarioId());
        return switch (command.acao()) {
            case BLOQUEAR -> bloquear(atual);
            case REATIVAR -> reativar(atual);
        };
    }

    private CompletableFuture<Usuario> bloquear(Usuario atual) {
        if (atual.status() == UsuarioStatus.BLOQUEADO) {
            return CompletableFuture.completedFuture(atual);
        }
        if (atual.status() != UsuarioStatus.ATIVO) {
            throw new UsuarioConflitanteException("Somente usuário ativo pode ser bloqueado.");
        }
        return CompletableFuture.completedFuture(atualizarStatus(atual, UsuarioStatus.BLOQUEADO));
    }

    private CompletableFuture<Usuario> reativar(Usuario atual) {
        if (atual.status() == UsuarioStatus.ATIVO) {
            return CompletableFuture.completedFuture(atual);
        }
        return CompletableFuture.completedFuture(atualizarStatus(atual, UsuarioStatus.ATIVO));
    }

    private Usuario atualizarStatus(Usuario atual, UsuarioStatus status) {
        return gateway.atualizar(atual.atualizado(atual.pessoa(), status, atual.papeis()));
    }

    public record Command(UUID usuarioId, Acao acao) {
    }

    public enum Acao {
        BLOQUEAR,
        REATIVAR
    }
}
