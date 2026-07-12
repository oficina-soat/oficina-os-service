package br.com.oficina.os.core.entities.usuario;

import br.com.oficina.os.core.entities.cliente.Documento;
import java.util.Objects;

public record CpfOperacional(String valor) implements Documento {
    public CpfOperacional(String valor) {
        Objects.requireNonNull(valor, "CPF operacional não pode ser nulo");
        var normalizado = valor.trim();
        if (!normalizado.matches("\\d{11}")) {
            throw new IllegalArgumentException("CPF operacional deve conter 11 dígitos");
        }
        this.valor = normalizado;
    }
}
