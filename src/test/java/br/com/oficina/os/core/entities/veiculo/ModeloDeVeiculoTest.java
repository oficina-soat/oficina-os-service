package br.com.oficina.os.core.entities.veiculo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModeloDeVeiculoTest {

    @Test
    void deveReterValorDoModelo() {
        var modelo = new ModeloDeVeiculo("Civic");

        assertEquals("Civic", modelo.valor());
    }
}
