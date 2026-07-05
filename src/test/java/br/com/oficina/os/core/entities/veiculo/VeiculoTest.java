package br.com.oficina.os.core.entities.veiculo;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VeiculoTest {

    @Test
    void deveCriarVeiculoECorrigirInformacoes() {
        var veiculoId = UUID.randomUUID();
        var original = new Veiculo(
                veiculoId,
                new PlacaDeVeiculo("ABC1234"),
                new MarcaDeVeiculo("Fiat"),
                new ModeloDeVeiculo("Uno"),
                2001);

        var corrigido = new Veiculo(
                veiculoId,
                new PlacaDeVeiculo("BRA1D23"),
                new MarcaDeVeiculo("Toyota"),
                new ModeloDeVeiculo("Corolla"),
                2024);

        original.corrigeInformacoes(corrigido);

        assertEquals(veiculoId, original.id());
        assertEquals("BRA1D23", original.placa().valor());
        assertEquals("Toyota", original.marca().valor());
        assertEquals("Corolla", original.modelo().valor());
        assertEquals(2024, original.ano());
    }
}
