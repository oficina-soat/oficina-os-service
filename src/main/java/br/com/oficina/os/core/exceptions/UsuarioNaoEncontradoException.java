package br.com.oficina.os.core.exceptions;

import java.util.UUID;

public class UsuarioNaoEncontradoException extends UsuarioException {
    public UsuarioNaoEncontradoException(long id) {
        super("Usuário não encontrado: " + id, Kind.NAO_ENCONTRADO);
    }

    public UsuarioNaoEncontradoException(UUID id) {
        super("Usuário não encontrado: " + id, Kind.NAO_ENCONTRADO);
    }
}
