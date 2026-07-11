package br.com.oficina.os.core.usecases.veiculo;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.VeiculoRecord;
import java.util.List;
import java.util.UUID;

public class ListarVeiculosDoClienteUseCase {
    private final AtendimentoGateway gateway;

    public ListarVeiculosDoClienteUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public List<VeiculoRecord> executar(UUID clienteId) {
        return gateway.listarVeiculosDoCliente(clienteId);
    }
}
