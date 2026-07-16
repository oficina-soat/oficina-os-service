package br.com.oficina.os.core.usecases.usuario;

import br.com.oficina.os.core.entities.usuario.Usuario;
import br.com.oficina.os.core.interfaces.gateway.UsuarioGateway;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class ListarUsuariosUseCase {
    private final UsuarioGateway gateway;

    public ListarUsuariosUseCase(UsuarioGateway gateway) {
        this.gateway = gateway;
    }

    public CompletableFuture<List<Usuario>> executar() {
        return executar(new Command(null, null, null, null));
    }

    public CompletableFuture<List<Usuario>> executar(Command command) {
        var nome = normalizar(command.nome());
        var documento = normalizar(command.documento());
        var status = normalizar(command.status());
        var papel = normalizar(command.papel());
        return CompletableFuture.completedFuture(gateway.listar().stream()
                .filter(usuario -> nome == null || usuario.pessoa().nome().toLowerCase(Locale.ROOT).contains(nome))
                .filter(usuario -> documento == null || usuario.pessoa().documento().valor().equals(documento))
                .filter(usuario -> status == null || usuario.status().name().equalsIgnoreCase(status))
                .filter(usuario -> papel == null || usuario.papeis().stream().anyMatch(item -> item.valor().equalsIgnoreCase(papel)))
                .toList());
    }

    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toLowerCase(Locale.ROOT);
    }

    public record Command(String nome, String documento, String status, String papel) {
    }
}
