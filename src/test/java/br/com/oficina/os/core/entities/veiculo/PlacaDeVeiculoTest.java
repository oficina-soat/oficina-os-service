package br.com.oficina.os.core.entities.veiculo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlacaDeVeiculoTest {

    @Test
    @DisplayName("Deve validar placas antigas (ABC1234) com e sem hífen")
    void shouldValidateOldPattern() {
        assertTrue(PlacaDeVeiculo.isValid("ABC-1234"));
        assertTrue(PlacaDeVeiculo.isValid("ABC1234"));
        assertTrue(PlacaDeVeiculo.isValid("abc-1234")); // lowercase deve ser aceito via normalização
        assertTrue(PlacaDeVeiculo.isValid("abc1234"));
    }

    @Test
    @DisplayName("Deve validar placas Mercosul (ABC1D23) com variações de case e espaços")
    void shouldValidateMercosulPattern() {
        assertTrue(PlacaDeVeiculo.isValid("ABC1D23"));
        assertTrue(PlacaDeVeiculo.isValid("abc1d23"));
        assertTrue(PlacaDeVeiculo.isValid("  aBc1D23  "));
        assertTrue(PlacaDeVeiculo.isValid("ABC 1D23")); // remove espaços
    }

    @Test
    @DisplayName("Deve rejeitar formatos inválidos")
    void shouldRejectInvalidFormats() {
        assertFalse(PlacaDeVeiculo.isValid(null));
        assertFalse(PlacaDeVeiculo.isValid(""));
        assertFalse(PlacaDeVeiculo.isValid("   "));

        // curtas/longas demais
        assertFalse(PlacaDeVeiculo.isValid("AB-1234"));
        assertFalse(PlacaDeVeiculo.isValid("ABCD-1234"));
        assertFalse(PlacaDeVeiculo.isValid("ABC-123"));
        assertFalse(PlacaDeVeiculo.isValid("ABC-12345"));

        // letras/números em posições erradas
        assertFalse(PlacaDeVeiculo.isValid("AB1-2345"));
        assertFalse(PlacaDeVeiculo.isValid("ABC12D3"));
        assertFalse(PlacaDeVeiculo.isValid("ABC1D2A"));
        assertFalse(PlacaDeVeiculo.isValid("A1C1D23"));

        // caracteres especiais indevidos
        assertFalse(PlacaDeVeiculo.isValid("ABC_1234"));
        assertFalse(PlacaDeVeiculo.isValid("ABC@1234"));
        assertFalse(PlacaDeVeiculo.isValid("ABC-12#4"));
    }

    @Test
    @DisplayName("of() deve normalizar (uppercase e sem hífen/espaços) e criar VO quando válido")
    void ofShouldNormalizeAndCreate() {
        PlacaDeVeiculo p1 = new PlacaDeVeiculo("abc-1234");
        assertEquals("ABC1234", p1.valor());

        PlacaDeVeiculo p2 = new PlacaDeVeiculo("  aBc1d23  ");
        assertEquals("ABC1D23", p2.valor());
    }

    @Test
    @DisplayName("of() deve lançar exceção quando inválido")
    void ofShouldThrowWhenInvalid() {
        assertThrows(NullPointerException.class, () -> new PlacaDeVeiculo(null));
        assertThrows(IllegalArgumentException.class, () -> new PlacaDeVeiculo(""));
        assertThrows(IllegalArgumentException.class, () -> new PlacaDeVeiculo("AAA-12A4"));
        assertThrows(IllegalArgumentException.class, () -> new PlacaDeVeiculo("ABCD1234"));
        assertThrows(IllegalArgumentException.class, () -> new PlacaDeVeiculo("ABC12D3"));
    }

    @Test
    @DisplayName("equals/hashCode devem funcionar para o mesmo valor normalizado")
    void equalsAndHashCodeShouldWork() {
        PlacaDeVeiculo a = new PlacaDeVeiculo("abc-1234");
        PlacaDeVeiculo b = new PlacaDeVeiculo("ABC1234");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
