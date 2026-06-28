package br.com.oficina.os.core.entities.ordem_de_servico;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemServicoTest {

    @Test
    void deveCalcularValorTotal() {
        var item = new ItemServico(1L, "Alinhamento", new BigDecimal("1"), new BigDecimal("150.90"));

        assertEquals(new BigDecimal("150.90"), item.valorTotal());
    }
}
