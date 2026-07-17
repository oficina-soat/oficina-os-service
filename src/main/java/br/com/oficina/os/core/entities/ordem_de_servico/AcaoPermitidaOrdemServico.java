package br.com.oficina.os.core.entities.ordem_de_servico;

import java.util.List;

public enum AcaoPermitidaOrdemServico {
    ENTREGAR,
    INCLUIR_SERVICO,
    INCLUIR_PECA,
    CANCELAR;

    public static List<AcaoPermitidaOrdemServico> porEstado(
            TipoDeEstadoDaOrdemDeServico estado,
            EstadoSaga estadoSaga) {
        return switch (estado) {
            case RECEBIDA, AGUARDANDO_APROVACAO, EM_EXECUCAO -> List.of(CANCELAR);
            case EM_DIAGNOSTICO -> List.of(INCLUIR_SERVICO, INCLUIR_PECA, CANCELAR);
            case FINALIZADA -> estadoSaga == EstadoSaga.AGUARDANDO_ENTREGA ? List.of(ENTREGAR) : List.of();
            case ENTREGUE -> List.of();
        };
    }
}
