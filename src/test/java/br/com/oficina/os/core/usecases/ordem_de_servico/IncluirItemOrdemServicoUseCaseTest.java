package br.com.oficina.os.core.usecases.ordem_de_servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.oficina.os.core.entities.ordem_de_servico.TipoDeEstadoDaOrdemDeServico;
import br.com.oficina.os.core.entities.ordem_de_servico.EstadoSaga;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.ItemPecaRecord;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.ItemServicoRecord;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.OrdemServicoRecord;
import br.com.oficina.os.core.interfaces.gateway.CatalogoGateway;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class IncluirItemOrdemServicoUseCaseTest {
    private static final UUID ORDEM_ID = UUID.randomUUID();
    private static final UUID ITEM_ID = UUID.randomUUID();

    @Test
    void deveResolverEIncluirSnapshotDoServicoAtivo() {
        var included = new AtomicReference<ItemServicoRecord>();
        var useCase = new IncluirServicoOrdemServicoUseCase(
                atendimento(included, null),
                catalogo(true));

        var result = useCase.executar(new IncluirServicoOrdemServicoUseCase.Command(
                ORDEM_ID, ITEM_ID, new BigDecimal("1.5"), "corr-service")).join();

        assertEquals(ITEM_ID, included.get().servicoId());
        assertEquals("Item catalogado", included.get().nome());
        assertEquals(new BigDecimal("42.50"), included.get().valorUnitario());
        assertEquals(ORDEM_ID, result.ordemServicoId());
    }

    @Test
    void deveResolverEIncluirSnapshotDaPecaAtiva() {
        var included = new AtomicReference<ItemPecaRecord>();
        var useCase = new IncluirPecaOrdemServicoUseCase(
                atendimento(null, included),
                catalogo(true));

        useCase.executar(new IncluirPecaOrdemServicoUseCase.Command(
                ORDEM_ID, ITEM_ID, new BigDecimal("2"), "corr-part")).join();

        assertEquals(ITEM_ID, included.get().pecaId());
        assertEquals(new BigDecimal("2"), included.get().quantidade());
    }

    @Test
    void deveRejeitarItensInativos() {
        var atendimento = atendimento(null, null);
        assertThrows(IllegalArgumentException.class, () -> new IncluirServicoOrdemServicoUseCase(
                atendimento, catalogo(false)).executar(new IncluirServicoOrdemServicoUseCase.Command(
                        ORDEM_ID, ITEM_ID, BigDecimal.ONE, "corr")));
        assertThrows(IllegalArgumentException.class, () -> new IncluirPecaOrdemServicoUseCase(
                atendimento, catalogo(false)).executar(new IncluirPecaOrdemServicoUseCase.Command(
                        ORDEM_ID, ITEM_ID, BigDecimal.ONE, "corr")));
    }

    @Test
    void deveRejeitarIdentificadorAusenteEQuantidadeNaoPositiva() {
        var atendimento = atendimento(null, null);
        var catalogo = catalogo(true);
        var servico = new IncluirServicoOrdemServicoUseCase(atendimento, catalogo);
        var peca = new IncluirPecaOrdemServicoUseCase(atendimento, catalogo);

        assertThrows(IllegalArgumentException.class, () -> servico.executar(
                new IncluirServicoOrdemServicoUseCase.Command(ORDEM_ID, null, BigDecimal.ONE, "corr")));
        assertThrows(IllegalArgumentException.class, () -> servico.executar(
                new IncluirServicoOrdemServicoUseCase.Command(ORDEM_ID, ITEM_ID, BigDecimal.ZERO, "corr")));
        assertThrows(IllegalArgumentException.class, () -> peca.executar(
                new IncluirPecaOrdemServicoUseCase.Command(ORDEM_ID, null, BigDecimal.ONE, "corr")));
        assertThrows(IllegalArgumentException.class, () -> peca.executar(
                new IncluirPecaOrdemServicoUseCase.Command(ORDEM_ID, ITEM_ID, new BigDecimal("-1"), "corr")));
    }

    private CatalogoGateway catalogo(boolean ativo) {
        return new CatalogoGateway() {
            @Override
            public CatalogoItem buscarServico(UUID servicoId, String correlationId) {
                return item(servicoId, ativo);
            }

            @Override
            public CatalogoItem buscarPeca(UUID pecaId, String correlationId) {
                return item(pecaId, ativo);
            }

            private CatalogoItem item(UUID id, boolean itemAtivo) {
                return new CatalogoItem(id, "Item catalogado", new BigDecimal("42.50"), itemAtivo);
            }
        };
    }

    private AtendimentoGateway atendimento(
            AtomicReference<ItemServicoRecord> servico,
            AtomicReference<ItemPecaRecord> peca) {
        return (AtendimentoGateway) Proxy.newProxyInstance(
                AtendimentoGateway.class.getClassLoader(),
                new Class<?>[] { AtendimentoGateway.class },
                (_, method, args) -> switch (method.getName()) {
                    case "incluirServico" -> {
                        if (servico != null) {
                            servico.set((ItemServicoRecord) args[1]);
                        }
                        yield ordem();
                    }
                    case "incluirPeca" -> {
                        if (peca != null) {
                            peca.set((ItemPecaRecord) args[1]);
                        }
                        yield ordem();
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private OrdemServicoRecord ordem() {
        var now = OffsetDateTime.now();
        return new OrdemServicoRecord(
                ORDEM_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Problema",
                TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO,
                now,
                now,
                List.of(),
                List.of(),
                EstadoSaga.EM_DIAGNOSTICO);
    }
}
