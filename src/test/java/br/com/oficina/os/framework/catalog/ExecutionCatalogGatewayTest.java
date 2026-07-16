package br.com.oficina.os.framework.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.ServiceUnavailableException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExecutionCatalogGatewayTest {
    private static final UUID ITEM_ID = UUID.randomUUID();

    @Test
    void deveMapearServicoEPecaDoExecution() {
        var gateway = new ExecutionCatalogGateway(client(false));

        var servico = gateway.buscarServico(ITEM_ID, "corr-1");
        var peca = gateway.buscarPeca(ITEM_ID, "corr-2");

        assertEquals("Revisao", servico.nome());
        assertEquals(new BigDecimal("120.00"), servico.valorUnitario());
        assertTrue(servico.ativo());
        assertEquals("Filtro", peca.nome());
        assertEquals(new BigDecimal("35.00"), peca.valorUnitario());
    }

    @Test
    void deveTraduzirIndisponibilidadeDoExecution() {
        var gateway = new ExecutionCatalogGateway(client(true));

        assertThrows(ServiceUnavailableException.class, () -> gateway.buscarServico(ITEM_ID, "corr"));
        assertThrows(ServiceUnavailableException.class, () -> gateway.buscarPeca(ITEM_ID, "corr"));
    }

    private ExecutionCatalogClient client(boolean indisponivel) {
        return (ExecutionCatalogClient) Proxy.newProxyInstance(
                ExecutionCatalogClient.class.getClassLoader(),
                new Class<?>[] {ExecutionCatalogClient.class},
                (_, method, _) -> {
                    if (indisponivel) {
                        throw new ProcessingException("Execution indisponivel");
                    }
                    return switch (method.getName()) {
                        case "buscarServico" ->
                            new ExecutionCatalogClient.ServicoResponse(
                                    ITEM_ID, "Revisao", new BigDecimal("120.00"), true);
                        case "buscarPeca" -> new ExecutionCatalogClient.PecaResponse(
                                ITEM_ID, "Filtro", new BigDecimal("35.00"), true);
                        default -> throw new UnsupportedOperationException(method.getName());
                    };
                });
    }
}
