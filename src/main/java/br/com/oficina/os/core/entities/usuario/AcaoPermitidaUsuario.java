package br.com.oficina.os.core.entities.usuario;

import java.util.List;

public enum AcaoPermitidaUsuario {
    ATUALIZAR_DADOS,
    BLOQUEAR,
    REATIVAR,
    INATIVAR;

    public static List<AcaoPermitidaUsuario> para(UsuarioStatus status) {
        return switch (status) {
            case ATIVO -> List.of(ATUALIZAR_DADOS, BLOQUEAR, INATIVAR);
            case BLOQUEADO -> List.of(ATUALIZAR_DADOS, REATIVAR, INATIVAR);
            case INATIVO -> List.of(REATIVAR);
        };
    }
}
