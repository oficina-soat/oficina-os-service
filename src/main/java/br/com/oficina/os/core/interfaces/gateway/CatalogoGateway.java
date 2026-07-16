package br.com.oficina.os.core.interfaces.gateway;

import java.math.BigDecimal;
import java.util.UUID;

public interface CatalogoGateway {
    CatalogoItem buscarServico(UUID servicoId, String correlationId);

    CatalogoItem buscarPeca(UUID pecaId, String correlationId);

    record CatalogoItem(UUID id, String nome, BigDecimal valorUnitario, boolean ativo) {
    }
}
