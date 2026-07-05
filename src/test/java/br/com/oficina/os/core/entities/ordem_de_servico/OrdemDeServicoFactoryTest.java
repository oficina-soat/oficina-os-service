package br.com.oficina.os.core.entities.ordem_de_servico;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrdemDeServicoFactoryTest {
    private static final UUID CLIENTE_ID = UUID.fromString("d290f1ee-6c54-4b01-90e6-d701748f0851");
    private static final UUID VEICULO_ID = UUID.fromString("7b1f1a8d-7f4a-4f25-8e74-27d50210a61e");

    @Test
    void deveCriarNovaOrdemComEstadoRecebida() {
        var ordem = OrdemDeServicoFactory.criarNovo(CLIENTE_ID, VEICULO_ID);

        assertNotNull(ordem.id());
        assertEquals(CLIENTE_ID, ordem.clienteId());
        assertEquals(VEICULO_ID, ordem.veiculoId());
        assertEquals(TipoDeEstadoDaOrdemDeServico.RECEBIDA, ordem.estadoDaOrdemDeServico());
        assertNotNull(ordem.dataDoEstado());
    }

    @Test
    void deveReconstituirSimples() {
        var id = UUID.randomUUID();
        var estado = new EstadoDaOrdemDeServico(TipoDeEstadoDaOrdemDeServico.FINALIZADA, Instant.now());

        var ordem = OrdemDeServicoFactory.reconstituiSimples(id, CLIENTE_ID, VEICULO_ID, estado);

        assertEquals(id, ordem.id());
        assertEquals(TipoDeEstadoDaOrdemDeServico.FINALIZADA, ordem.estadoDaOrdemDeServico());
    }

    @Test
    void deveReconstituirCompleto() {
        var id = UUID.randomUUID();
        var estado = new EstadoDaOrdemDeServico(TipoDeEstadoDaOrdemDeServico.EM_EXECUCAO, Instant.now());

        var ordem = OrdemDeServicoFactory.reconstituiCompleto(
                id,
                CLIENTE_ID,
                VEICULO_ID,
                estado,
                List.of(estado),
                List.of(),
                List.of());

        assertEquals(id, ordem.id());
        assertEquals(1, ordem.historicoDeEstados().size());
    }
}
