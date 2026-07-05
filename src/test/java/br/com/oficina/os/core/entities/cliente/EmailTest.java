package br.com.oficina.os.core.entities.cliente;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailTest {

    @Test
    void deveNormalizarEmailValido() {
        var email = new Email(" Cliente@Teste.COM ");

        assertEquals("cliente@teste.com", email.valor());
    }

    @Test
    void deveValidarEmailPeloMetodoEstatico() {
        assertTrue(Email.isValid("cliente@oficina.com"));
    }

    @Test
    void deveFalharQuandoEmailForInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new Email("cliente-invalido"));
    }
}
