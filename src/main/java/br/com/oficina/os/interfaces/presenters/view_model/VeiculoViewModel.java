package br.com.oficina.os.interfaces.presenters.view_model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VeiculoViewModel(
        UUID veiculoId,
        UUID clienteId,
        String placa,
        String marca,
        String modelo,
        int ano,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm) {
}
