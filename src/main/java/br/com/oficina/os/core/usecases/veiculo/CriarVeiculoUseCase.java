package br.com.oficina.os.core.usecases.veiculo;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.VeiculoRecord;
import java.util.UUID;

public class CriarVeiculoUseCase {
    private final AtendimentoGateway gateway;

    public CriarVeiculoUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public VeiculoRecord executar(UUID clienteId, String placa, String marca, String modelo, int ano) {
        return gateway.criarVeiculo(clienteId, placa, marca, modelo, ano);
    }
}
