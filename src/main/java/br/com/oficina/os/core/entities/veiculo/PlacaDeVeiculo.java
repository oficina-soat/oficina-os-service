package br.com.oficina.os.core.entities.veiculo;

import java.util.Objects;
import java.util.regex.Pattern;

public record PlacaDeVeiculo(String valor) {
    private static final Pattern PLACA_ANTIGA = Pattern.compile("^[A-Z]{3}[0-9]{4}$");
    private static final Pattern PLACA_MERCOSUL = Pattern.compile("^[A-Z]{3}[0-9][A-Z][0-9]{2}$");

    public PlacaDeVeiculo(String valor) {
        Objects.requireNonNull(valor, "Placa não pode ser nula");

        String normalized = normalize(valor);

        if (!isValid(normalized)) {
            throw new IllegalArgumentException("Placa de veículo inválida: " + valor);
        }

        this.valor = normalized;
    }

    public static boolean isValid(String placa) {
        if (placa == null) {
            return false;
        }

        String normalized = normalize(placa);

        return PLACA_ANTIGA.matcher(normalized).matches()
                || PLACA_MERCOSUL.matcher(normalized).matches();
    }

    private static String normalize(String placa) {
        return placa
                .trim()
                .toUpperCase()
                .replace("-", "")
                .replace(" ", "");
    }
}
