package br.com.oficina.os.core.entities.cliente;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CnpjTest {

    @Test
    @DisplayName("Deve validar CNPJs válidos (com e sem máscara)")
    void shouldValidateValidCnpjs() {
        // CNPJs reais e bem conhecidos em exemplos de validação
        assertDoesNotThrow(() -> new Cnpj("04.252.011/0001-10")); // Google Brasil
        assertDoesNotThrow(() -> new Cnpj("40.688.134/0001-61")); // Receita Federal (exemplo comum)
        assertDoesNotThrow(() -> new Cnpj("04252011000110"));
        assertDoesNotThrow(() -> new Cnpj("40688134000161"));
    }

    @Test
    @DisplayName("Deve rejeitar CNPJs com tamanho inválido")
    void shouldRejectInvalidLength() {
        assertThrows(NullPointerException.class, () -> new Cnpj(null));
        assertThrows(IllegalArgumentException.class, () -> new Cnpj(""));
        assertThrows(IllegalArgumentException.class, () -> new Cnpj("123"));
        assertThrows(IllegalArgumentException.class, () -> new Cnpj("1234567890123"));  // 13
        assertThrows(IllegalArgumentException.class, () -> new Cnpj("123456789012345")); // 15
    }

    @Test
    @DisplayName("Deve rejeitar sequências repetidas (000..., 111..., etc.)")
    void shouldRejectRepeatedSequences() {
        assertThrows(IllegalArgumentException.class, () -> new Cnpj("00000000000000"));
        assertThrows(IllegalArgumentException.class, () -> new Cnpj("11111111111111"));
        assertThrows(IllegalArgumentException.class, () -> new Cnpj("22222222222222"));
        assertThrows(IllegalArgumentException.class, () -> new Cnpj("99999999999999"));
    }

    @Test
    @DisplayName("Deve rejeitar CNPJ com dígitos verificadores incorretos")
    void shouldRejectWrongCheckDigits() {
        // pega um válido e altera o último dígito
        assertThrows(IllegalArgumentException.class, () -> new Cnpj("04.252.011/0001-11"));
        assertThrows(IllegalArgumentException.class, () -> new Cnpj("04252011000111"));
    }

    @Test
    @DisplayName("of() deve retornar VO normalizado (apenas dígitos) quando válido")
    void ofShouldNormalizeDigits() {
        Cnpj cnpj = new Cnpj("04.252.011/0001-10");
        assertEquals("04252011000110", cnpj.valor());
        assertEquals("Cnpj[valor=04252011000110]", cnpj.toString());
    }

    @Test
    @DisplayName("of() deve lançar exceção quando inválido")
    void ofShouldThrowWhenInvalid() {
        assertThrows(NullPointerException.class, () -> new Cnpj(null));
        assertThrows(IllegalArgumentException.class, () -> new Cnpj("00.000.000/0000-00"));
        assertThrows(IllegalArgumentException.class, () -> new Cnpj("123"));
        assertThrows(IllegalArgumentException.class, () -> new Cnpj("04.252.011/0001-11"));
    }

    @Test
    @DisplayName("equals/hashCode devem funcionar para o mesmo CNPJ normalizado")
    void equalsAndHashCodeShouldWork() {
        Cnpj a = new Cnpj("04.252.011/0001-10");
        Cnpj b = new Cnpj("04252011000110");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
