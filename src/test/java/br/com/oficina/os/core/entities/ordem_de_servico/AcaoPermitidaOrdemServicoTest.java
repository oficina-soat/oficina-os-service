package br.com.oficina.os.core.entities.ordem_de_servico;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class AcaoPermitidaOrdemServicoTest {

    @Test
    void deveDerivarAsAcoesPermitidasDoEstado() {
        assertEquals(
                List.of(AcaoPermitidaOrdemServico.INICIAR_DIAGNOSTICO, AcaoPermitidaOrdemServico.CANCELAR),
                AcaoPermitidaOrdemServico.porEstado(TipoDeEstadoDaOrdemDeServico.RECEBIDA));
        assertEquals(
                List.of(
                        AcaoPermitidaOrdemServico.INCLUIR_SERVICO,
                        AcaoPermitidaOrdemServico.INCLUIR_PECA,
                        AcaoPermitidaOrdemServico.CONCLUIR_DIAGNOSTICO,
                        AcaoPermitidaOrdemServico.CANCELAR),
                AcaoPermitidaOrdemServico.porEstado(TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO));
        assertEquals(
                List.of(
                        AcaoPermitidaOrdemServico.INICIAR_EXECUCAO,
                        AcaoPermitidaOrdemServico.RETOMAR_DIAGNOSTICO,
                        AcaoPermitidaOrdemServico.CANCELAR),
                AcaoPermitidaOrdemServico.porEstado(TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO));
        assertEquals(
                List.of(AcaoPermitidaOrdemServico.FINALIZAR, AcaoPermitidaOrdemServico.CANCELAR),
                AcaoPermitidaOrdemServico.porEstado(TipoDeEstadoDaOrdemDeServico.EM_EXECUCAO));
        assertEquals(
                List.of(AcaoPermitidaOrdemServico.ENTREGAR),
                AcaoPermitidaOrdemServico.porEstado(TipoDeEstadoDaOrdemDeServico.FINALIZADA));
        assertEquals(
                List.of(),
                AcaoPermitidaOrdemServico.porEstado(TipoDeEstadoDaOrdemDeServico.ENTREGUE));
    }
}
