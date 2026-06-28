package br.com.oficina.os.core.entities.ordem_de_servico;

import br.com.oficina.os.core.exceptions.EstadoDaOrdemDeServicoInvalidoException;
import br.com.oficina.os.core.exceptions.ItemDaOrdemDeServicoInvalidoException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrdemDeServicoTest {

    @Test
    void deveExecutarFluxoCompletoDeEstados() {
        var ordem = OrdemDeServicoFactory.criarNovo(1L, 2L);

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
        var ordem = OrdemDeServicoFactory.criarNovo(1L, 2L);

        assertThrows(EstadoDaOrdemDeServicoInvalidoException.class, ordem::finalizarDiagnostico);
        assertThrows(EstadoDaOrdemDeServicoInvalidoException.class, ordem::iniciarExecucao);
    }

    @Test
    void deveValidarQuantidadeEValorAoAdicionarItens() {
        var ordem = OrdemDeServicoFactory.criarNovo(1L, 2L);
        ordem.iniciarDiagnostico();

        assertThrows(ItemDaOrdemDeServicoInvalidoException.class,
                () -> ordem.adicionaPeca(1L, "Filtro", BigDecimal.ZERO, BigDecimal.ONE));
        assertThrows(ItemDaOrdemDeServicoInvalidoException.class,
                () -> ordem.adicionaServico(1L, "Mao de obra", BigDecimal.ONE, new BigDecimal("-1")));
    }

    @Test
    void deveFalharEmTodasAsTransicoesInvalidas() {
        var ordem = OrdemDeServicoFactory.criarNovo(1L, 2L);
        assertThrows(EstadoDaOrdemDeServicoInvalidoException.class, ordem::entregar);
        ordem.iniciarDiagnostico();
        assertThrows(EstadoDaOrdemDeServicoInvalidoException.class, ordem::entregar);
        ordem.finalizarDiagnostico();
        assertThrows(EstadoDaOrdemDeServicoInvalidoException.class, ordem::finalizar);
    }

    @Test
    void listasDevemSerImutaveisParaConsumoExterno() {
        var ordem = OrdemDeServicoFactory.criarNovo(1L, 2L);
        ordem.iniciarDiagnostico();
        ordem.adicionaPeca(1L, "Filtro", BigDecimal.ONE, BigDecimal.ONE);

        assertThrows(UnsupportedOperationException.class,
                () -> ordem.pecas().add(new ItemPeca(2L, "X", BigDecimal.ONE, BigDecimal.ONE)));
        assertThrows(UnsupportedOperationException.class,
                () -> ordem.historicoDeEstados().add(new EstadoDaOrdemDeServico(
                        TipoDeEstadoDaOrdemDeServico.RECEBIDA,
                        ordem.dataDoEstado())));
    }
}
