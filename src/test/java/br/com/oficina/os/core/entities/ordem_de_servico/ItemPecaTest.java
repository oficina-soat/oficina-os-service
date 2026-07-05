package br.com.oficina.os.core.entities.ordem_de_servico;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemPecaTest {

    @Test
    void deveCalcularValorTotal() {
        var item = new ItemPeca(1L, "Filtro", new BigDecimal("2.5"), new BigDecimal("10.00"));

        assertEquals(new BigDecimal("25.000"), item.valorTotal());
    }
}
