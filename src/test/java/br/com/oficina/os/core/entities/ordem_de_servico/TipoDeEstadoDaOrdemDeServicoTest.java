package br.com.oficina.os.core.entities.ordem_de_servico;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TipoDeEstadoDaOrdemDeServicoTest {

    @Test
    void deveConterTodosOsEstadosDoFluxo() {
        assertEquals(6, TipoDeEstadoDaOrdemDeServico.values().length);
        assertEquals(TipoDeEstadoDaOrdemDeServico.RECEBIDA, TipoDeEstadoDaOrdemDeServico.valueOf("RECEBIDA"));
        assertEquals(TipoDeEstadoDaOrdemDeServico.ENTREGUE, TipoDeEstadoDaOrdemDeServico.valueOf("ENTREGUE"));
    }
}
