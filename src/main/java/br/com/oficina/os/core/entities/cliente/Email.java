package br.com.oficina.os.core.entities.cliente;

import java.util.Objects;
import java.util.regex.Pattern;

public record Email(String valor) {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    public Email(String valor) {
        Objects.requireNonNull(valor, "E-mail não pode ser nulo");

        var normalizado = valor.trim().toLowerCase();

        if (!isValid(normalizado)) {
            throw new IllegalArgumentException("E-mail inválido: " + valor);
        }

        this.valor = normalizado;
    }

    public static boolean isValid(String valor) {
        if (valor == null) {
            return false;
        }

        var normalizado = valor.trim();
        if (normalizado.isEmpty()) {
            return false;
        }

        return EMAIL_PATTERN.matcher(normalizado).matches();
    }
}
