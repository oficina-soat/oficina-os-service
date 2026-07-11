package br.com.oficina.os.interfaces.presenters.view_model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClienteViewModel(
        UUID clienteId,
        String nome,
        String documento,
        String telefone,
        String email,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm) {
}
