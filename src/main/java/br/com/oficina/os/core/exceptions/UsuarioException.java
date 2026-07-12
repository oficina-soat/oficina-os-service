package br.com.oficina.os.core.exceptions;

public abstract class UsuarioException extends RuntimeException {
    private final Kind kind;

    protected UsuarioException(String message, Kind kind) {
        super(message);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public enum Kind {
        NAO_ENCONTRADO,
        CONFLITO
    }
}
