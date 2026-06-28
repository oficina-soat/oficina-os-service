package br.com.oficina.os.core.entities.pessoa;

public enum TipoPessoa {
    FISICA,
    JURIDICA;

    public static TipoPessoa fromDocumento(String documento) {
        if (documento == null || documento.isBlank()) {
            throw new IllegalArgumentException("Documento é obrigatório");
        }

        return switch (documento.trim().replaceAll("\\D", "").length()) {
            case 11 -> FISICA;
            case 14 -> JURIDICA;
            default -> throw new IllegalArgumentException("Documento inválido: " + documento);
        };
    }
}
