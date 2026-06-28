package br.com.oficina.os.core.entities.ordem_de_servico;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrdemDeServicoFactoryTest {

    @Test
    void deveCriarNovaOrdemComEstadoRecebida() {
        var ordem = OrdemDeServicoFactory.criarNovo(1L, 2L);

        assertNotNull(ordem.id());
        assertEquals(1L, ordem.clienteId());
        assertEquals(2L, ordem.veiculoId());
        assertEquals(TipoDeEstadoDaOrdemDeServico.RECEBIDA, ordem.estadoDaOrdemDeServico());
        assertNotNull(ordem.dataDoEstado());
    }

    @Test
    void deveReconstituirSimples() {
        var id = UUID.randomUUID();
        var estado = new EstadoDaOrdemDeServico(TipoDeEstadoDaOrdemDeServico.FINALIZADA, Instant.now());

        var ordem = OrdemDeServicoFactory.reconstituiSimples(id, 9L, 8L, estado);

        assertEquals(id, ordem.id());
        assertEquals(TipoDeEstadoDaOrdemDeServico.FINALIZADA, ordem.estadoDaOrdemDeServico());
    }

    @Test
    void deveReconstituirCompleto() {
        var id = UUID.randomUUID();
        var estado = new EstadoDaOrdemDeServico(TipoDeEstadoDaOrdemDeServico.EM_EXECUCAO, Instant.now());

        var ordem = OrdemDeServicoFactory.reconstituiCompleto(
                id,
                3L,
                4L,
                estado,
                List.of(estado),
                List.of(),
                List.of());

        assertEquals(id, ordem.id());
        assertEquals(1, ordem.historicoDeEstados().size());
    }
}
