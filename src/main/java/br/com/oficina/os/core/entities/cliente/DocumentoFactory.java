package br.com.oficina.os.core.entities.cliente;

public class DocumentoFactory {

    private DocumentoFactory() {
    }

    public static Documento from(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Documento vazio");
        }

        var somenteNumeros = input.trim().replaceAll("\\D", "");
        return switch (somenteNumeros.length()) {
            case 11 -> new Cpf(somenteNumeros);
            case 14 -> new Cnpj(somenteNumeros);
            default -> throw new IllegalArgumentException("Documento inválido");
        };
    }
}
