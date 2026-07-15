package br.com.oficina.os.core.entities.ordem_de_servico;

import java.util.List;

public enum AcaoPermitidaOrdemServico {
    INICIAR_DIAGNOSTICO,
    CONCLUIR_DIAGNOSTICO,
    RETOMAR_DIAGNOSTICO,
    INICIAR_EXECUCAO,
    FINALIZAR,
    ENTREGAR,
    CANCELAR;

    public static List<AcaoPermitidaOrdemServico> porEstado(TipoDeEstadoDaOrdemDeServico estado) {
        return switch (estado) {
            case RECEBIDA -> List.of(INICIAR_DIAGNOSTICO, CANCELAR);
            case EM_DIAGNOSTICO -> List.of(CONCLUIR_DIAGNOSTICO, CANCELAR);
            case AGUARDANDO_APROVACAO -> List.of(INICIAR_EXECUCAO, RETOMAR_DIAGNOSTICO, CANCELAR);
            case EM_EXECUCAO -> List.of(FINALIZAR, CANCELAR);
            case FINALIZADA -> List.of(ENTREGAR);
            case ENTREGUE -> List.of();
        };
    }
}
