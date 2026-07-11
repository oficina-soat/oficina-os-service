package br.com.oficina.os.core.entities.ordem_de_servico;

import br.com.oficina.os.core.exceptions.EstadoDaOrdemDeServicoInvalidoException;
import br.com.oficina.os.core.exceptions.ItemDaOrdemDeServicoInvalidoException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrdemDeServicoTest {
    private static final UUID CLIENTE_ID = UUID.fromString("d290f1ee-6c54-4b01-90e6-d701748f0851");
    private static final UUID VEICULO_ID = UUID.fromString("7b1f1a8d-7f4a-4f25-8e74-27d50210a61e");

    @Test
    void deveExecutarFluxoCompletoDeEstados() {
        var ordem = OrdemDeServicoFactory.criarNovo(CLIENTE_ID, VEICULO_ID);

        ordem.iniciarDiagnostico();
        ordem.adicionaPeca(1L, "Filtro", new BigDecimal("1"), new BigDecimal("99.99"));
        ordem.adicionaServico(2L, "Troca", new BigDecimal("2"), new BigDecimal("50"));
        ordem.finalizarDiagnostico();
        ordem.iniciarExecucao();
        ordem.finalizar();
        ordem.entregar();

        assertEquals(TipoDeEstadoDaOrdemDeServico.ENTREGUE, ordem.estadoDaOrdemDeServico());
        assertEquals(1, ordem.pecas().size());
        assertEquals(1, ordem.servicos().size());
        assertEquals(5, ordem.historicoDeEstados().size());
    }

    @Test
    void deveFalharEmTransicaoInvalida() {
        var ordem = OrdemDeServicoFactory.criarNovo(CLIENTE_ID, VEICULO_ID);

        assertThrows(EstadoDaOrdemDeServicoInvalidoException.class, ordem::finalizarDiagnostico);
        assertThrows(EstadoDaOrdemDeServicoInvalidoException.class, ordem::iniciarExecucao);
    }

    @Test
    void deveValidarQuantidadeEValorAoAdicionarItens() {
        var ordem = OrdemDeServicoFactory.criarNovo(CLIENTE_ID, VEICULO_ID);
        ordem.iniciarDiagnostico();
        var valorUnitarioInvalido = new BigDecimal("-1");

        assertThrows(ItemDaOrdemDeServicoInvalidoException.class,
                () -> ordem.adicionaPeca(1L, "Filtro", BigDecimal.ZERO, BigDecimal.ONE));
        assertThrows(ItemDaOrdemDeServicoInvalidoException.class,
                () -> ordem.adicionaServico(1L, "Mao de obra", BigDecimal.ONE, valorUnitarioInvalido));
    }

    @Test
    void deveFalharEmTodasAsTransicoesInvalidas() {
        var ordem = OrdemDeServicoFactory.criarNovo(CLIENTE_ID, VEICULO_ID);
        assertThrows(EstadoDaOrdemDeServicoInvalidoException.class, ordem::entregar);
        ordem.iniciarDiagnostico();
        assertThrows(EstadoDaOrdemDeServicoInvalidoException.class, ordem::entregar);
        ordem.finalizarDiagnostico();
        assertThrows(EstadoDaOrdemDeServicoInvalidoException.class, ordem::finalizar);
    }

    @Test
    void listasDevemSerImutaveisParaConsumoExterno() {
        var ordem = OrdemDeServicoFactory.criarNovo(CLIENTE_ID, VEICULO_ID);
        ordem.iniciarDiagnostico();
        ordem.adicionaPeca(1L, "Filtro", BigDecimal.ONE, BigDecimal.ONE);
        var pecas = ordem.pecas();
        var historico = ordem.historicoDeEstados();
        var itemPeca = new ItemPeca(2L, "X", BigDecimal.ONE, BigDecimal.ONE);
        var estado = new EstadoDaOrdemDeServico(
                TipoDeEstadoDaOrdemDeServico.RECEBIDA,
                ordem.dataDoEstado());

        assertThrows(UnsupportedOperationException.class, () -> pecas.add(itemPeca));
        assertThrows(UnsupportedOperationException.class, () -> historico.add(estado));
    }
}
