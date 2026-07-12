package br.com.oficina.os.core.exceptions;

public class UsuarioConflitanteException extends UsuarioException {
    public UsuarioConflitanteException(String message) {
        super(message, Kind.CONFLITO);
    }
}
