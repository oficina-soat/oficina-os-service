package br.com.oficina.os.core.entities.ordem_de_servico;

import java.util.List;

public enum AcaoPermitidaOrdemServico {
    INICIAR_DIAGNOSTICO,
    CONCLUIR_DIAGNOSTICO,
    RETOMAR_DIAGNOSTICO,
    FINALIZAR,
    ENTREGAR,
    INCLUIR_SERVICO,
    INCLUIR_PECA,
    CANCELAR;

    public static List<AcaoPermitidaOrdemServico> porEstado(TipoDeEstadoDaOrdemDeServico estado) {
        return switch (estado) {
            case RECEBIDA -> List.of(INICIAR_DIAGNOSTICO, CANCELAR);
            case EM_DIAGNOSTICO -> List.of(INCLUIR_SERVICO, INCLUIR_PECA, CONCLUIR_DIAGNOSTICO, CANCELAR);
            case AGUARDANDO_APROVACAO -> List.of(RETOMAR_DIAGNOSTICO, CANCELAR);
            case EM_EXECUCAO -> List.of(FINALIZAR, CANCELAR);
            case FINALIZADA -> List.of(ENTREGAR);
            case ENTREGUE -> List.of();
        };
    }
}
