package br.com.oficina.os.core.entities.ordem_de_servico;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EstadoDaOrdemDeServicoTest {

    @Test
    void deveReterEstadoEData() {
        var data = Instant.now();
        var estado = new EstadoDaOrdemDeServico(TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO, data);

        assertEquals(TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO, estado.estado());
        assertEquals(data, estado.dataDoEstado());
    }
}
