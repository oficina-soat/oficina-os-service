package br.com.oficina.os.core.interfaces.gateway;

import br.com.oficina.os.core.entities.usuario.Usuario;
import java.util.List;
import java.util.UUID;

public interface UsuarioGateway {
    Usuario criar(Usuario usuario);

    List<Usuario> listar();

    Usuario buscar(UUID usuarioId);

    Usuario atualizar(Usuario usuario);

    void inativar(UUID usuarioId);
}
