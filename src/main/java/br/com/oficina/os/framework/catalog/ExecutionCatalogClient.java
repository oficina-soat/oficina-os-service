package br.com.oficina.os.framework.catalog;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import java.math.BigDecimal;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "execution-catalog")
@Path("/api/v1")
public interface ExecutionCatalogClient {
    @GET
    @Path("/servicos/{id}")
    ServicoResponse buscarServico(@PathParam("id") UUID id, @HeaderParam("X-Correlation-Id") String correlationId);

    @GET
    @Path("/pecas/{id}")
    PecaResponse buscarPeca(@PathParam("id") UUID id, @HeaderParam("X-Correlation-Id") String correlationId);

    record ServicoResponse(UUID servicoId, String nome, BigDecimal valorBase, boolean ativo) {
    }

    record PecaResponse(UUID pecaId, String nome, BigDecimal valorUnitario, boolean ativo) {
    }
}
