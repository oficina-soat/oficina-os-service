package br.com.oficina.os.core.entities.cliente;

import java.util.Objects;

public record Cnpj(String valor) implements Documento {
    public Cnpj(String valor) {
        Objects.requireNonNull(valor, "CNPJ não pode ser nulo");

        var somenteNumeros = valor.trim().replaceAll("\\D", "");

        if (!isValid(somenteNumeros)) throw new IllegalArgumentException("CNPJ inválido: " + valor);

        this.valor = somenteNumeros;
    }

    private boolean isValid(String somenteNumeros) {
        if (somenteNumeros == null) return false;
        if (somenteNumeros.length() != 14) return false;
        if (somenteNumeros.chars().distinct().count() == 1) return false;

        int primeiroDigito = calculateDigit(somenteNumeros, 12);
        int segundoDigito = calculateDigit(somenteNumeros, 13);

        return primeiroDigito == Character.getNumericValue(somenteNumeros.charAt(12))
                && segundoDigito == Character.getNumericValue(somenteNumeros.charAt(13));

    }

    private static int calculateDigit(String cnpj, int quantidadePosicoes) {
        int soma = 0;
        int peso = quantidadePosicoes - 7;

        for (int i = 0; i < quantidadePosicoes; i++) {
            int num = Character.getNumericValue(cnpj.charAt(i));
            soma += num * peso--;
            if (peso < 2) {
                peso = 9;
            }
        }

        int resto = soma % 11;
        return (resto < 2) ? 0 : 11 - resto;
    }
}
