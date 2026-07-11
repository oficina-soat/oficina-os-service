package br.com.oficina.os.core.usecases.veiculo;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.VeiculoRecord;
import java.util.UUID;

public class AtualizarVeiculoUseCase {
    private final AtendimentoGateway gateway;

    public AtualizarVeiculoUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public VeiculoRecord executar(UUID veiculoId, String placa, String marca, String modelo, int ano) {
        return gateway.atualizarVeiculo(veiculoId, placa, marca, modelo, ano);
    }
}
