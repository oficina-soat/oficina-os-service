package br.com.oficina.os.core.entities.veiculo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VeiculoTest {

    @Test
    void deveCriarVeiculoECorrigirInformacoes() {
        var original = new Veiculo(
                1L,
                new PlacaDeVeiculo("ABC1234"),
                new MarcaDeVeiculo("Fiat"),
                new ModeloDeVeiculo("Uno"),
                2001);

        var corrigido = new Veiculo(
                2L,
                new PlacaDeVeiculo("BRA1D23"),
                new MarcaDeVeiculo("Toyota"),
                new ModeloDeVeiculo("Corolla"),
                2024);

        original.corrigeInformacoes(corrigido);

        assertEquals(1L, original.id());
        assertEquals("BRA1D23", original.placa().valor());
        assertEquals("Toyota", original.marca().valor());
        assertEquals("Corolla", original.modelo().valor());
        assertEquals(2024, original.ano());
    }
}
