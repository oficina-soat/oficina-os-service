package br.com.oficina.os.interfaces.presenters.view_model;

import java.time.OffsetDateTime;

public record OperacaoAssincronaViewModel(
        String status,
        OffsetDateTime solicitadoEm) {
}
