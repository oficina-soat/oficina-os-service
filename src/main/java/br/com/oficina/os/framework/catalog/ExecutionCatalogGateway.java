package br.com.oficina.os.framework.catalog;

import br.com.oficina.os.core.interfaces.gateway.CatalogoGateway;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class ExecutionCatalogGateway implements CatalogoGateway {
    private final ExecutionCatalogClient client;

    @Inject
    public ExecutionCatalogGateway(@RestClient ExecutionCatalogClient client) {
        this.client = client;
    }

    @Override
    public CatalogoItem buscarServico(UUID servicoId, String correlationId) {
        try {
            var item = client.buscarServico(servicoId, correlationId);
            return new CatalogoItem(item.servicoId(), item.nome(), item.valorBase(), item.ativo());
        } catch (ProcessingException exception) {
            throw new ServiceUnavailableException(Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("Catalogo de servicos indisponivel.").build(), exception);
        }
    }

    @Override
    public CatalogoItem buscarPeca(UUID pecaId, String correlationId) {
        try {
            var item = client.buscarPeca(pecaId, correlationId);
            return new CatalogoItem(item.pecaId(), item.nome(), item.valorUnitario(), item.ativo());
        } catch (ProcessingException exception) {
            throw new ServiceUnavailableException(Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("Catalogo de pecas indisponivel.").build(), exception);
        }
    }
}
