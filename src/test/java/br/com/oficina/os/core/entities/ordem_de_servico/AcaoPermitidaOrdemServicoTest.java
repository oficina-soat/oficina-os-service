package br.com.oficina.os.core.entities.ordem_de_servico;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class AcaoPermitidaOrdemServicoTest {

    @Test
    void deveDerivarAsAcoesPermitidasDoEstado() {
        assertEquals(
                List.of(AcaoPermitidaOrdemServico.CANCELAR),
                AcaoPermitidaOrdemServico.porEstado(TipoDeEstadoDaOrdemDeServico.RECEBIDA, EstadoSaga.INICIADA));
        assertEquals(
                List.of(
                        AcaoPermitidaOrdemServico.INCLUIR_SERVICO,
                        AcaoPermitidaOrdemServico.INCLUIR_PECA,
                        AcaoPermitidaOrdemServico.CANCELAR),
                AcaoPermitidaOrdemServico.porEstado(TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO, EstadoSaga.EM_DIAGNOSTICO));
        assertEquals(
                List.of(AcaoPermitidaOrdemServico.CANCELAR),
                AcaoPermitidaOrdemServico.porEstado(TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO, EstadoSaga.AGUARDANDO_APROVACAO));
        assertEquals(
                List.of(AcaoPermitidaOrdemServico.CANCELAR),
                AcaoPermitidaOrdemServico.porEstado(TipoDeEstadoDaOrdemDeServico.EM_EXECUCAO, EstadoSaga.EM_EXECUCAO));
        assertEquals(
                List.of(),
                AcaoPermitidaOrdemServico.porEstado(TipoDeEstadoDaOrdemDeServico.FINALIZADA, EstadoSaga.AGUARDANDO_PAGAMENTO));
        assertEquals(
                List.of(AcaoPermitidaOrdemServico.ENTREGAR),
                AcaoPermitidaOrdemServico.porEstado(TipoDeEstadoDaOrdemDeServico.FINALIZADA, EstadoSaga.AGUARDANDO_ENTREGA));
        assertEquals(
                List.of(),
                AcaoPermitidaOrdemServico.porEstado(TipoDeEstadoDaOrdemDeServico.ENTREGUE, EstadoSaga.FINALIZADA_COM_SUCESSO));
    }
}
