package br.com.oficina.os.core.entities.usuario;

import java.util.Arrays;

public enum TipoDePapel {
    ADMINISTRATIVO(TipoDePapelValues.ADMINISTRATIVO),
    RECEPCIONISTA(TipoDePapelValues.RECEPCIONISTA),
    MECANICO(TipoDePapelValues.MECANICO);

    private final String valor;

    TipoDePapel(String valor) {
         this.valor = valor;
    }

    public String valor() {
        return valor;
    }

    public static TipoDePapel fromValor(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Papel é obrigatório");
        }

        var normalizado = valor.trim().toLowerCase();
        return Arrays.stream(values())
                .filter(tipoDePapel -> tipoDePapel.valor.equals(normalizado))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Papel inválido: " + valor));
    }
}
