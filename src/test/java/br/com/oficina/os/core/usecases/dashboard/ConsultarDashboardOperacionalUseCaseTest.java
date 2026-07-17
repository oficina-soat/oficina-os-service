package br.com.oficina.os.core.usecases.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.oficina.os.core.entities.ordem_de_servico.TipoDeEstadoDaOrdemDeServico;
import br.com.oficina.os.core.entities.pessoa.Pessoa;
import br.com.oficina.os.core.entities.usuario.Usuario;
import br.com.oficina.os.core.entities.usuario.UsuarioStatus;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.UsuarioGateway;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarDashboardOperacionalUseCaseTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-17T18:30:00Z");

    @Test
    void agregaEstadosESelecionaAtencoesNaOrdemDoEstado() {
        var atendimento = mock(AtendimentoGateway.class);
        var usuarios = mock(UsuarioGateway.class);
        var ordemId = UUID.randomUUID();
        var ordem = new AtendimentoGateway.OrdemServicoRecord(
                ordemId, UUID.randomUUID(), UUID.randomUUID(), "Motor falhando",
                TipoDeEstadoDaOrdemDeServico.RECEBIDA, NOW.minusDays(1), NOW.minusHours(1), List.of(), List.of());
        when(atendimento.listarOrdensServico(null)).thenReturn(List.of(ordem));
        when(atendimento.historico(ordemId)).thenReturn(List.of(new AtendimentoGateway.HistoricoRecord(
                TipoDeEstadoDaOrdemDeServico.RECEBIDA, NOW.minusHours(2), null)));
        when(usuarios.listar()).thenReturn(List.of());

        var result = useCase(atendimento, usuarios).executar().join();

        assertEquals(NOW, result.generatedAt());
        assertEquals(1, result.atencoes().size());
        assertEquals(NOW.minusHours(2), result.atencoes().getFirst().entrouNoEstadoEm());
        assertEquals(1, result.contagensPorEstado().getFirst().quantidade());
    }

    @Test
    void retornaSomenteUsuariosBloqueadosComoAtencao() {
        var atendimento = mock(AtendimentoGateway.class);
        var usuarios = mock(UsuarioGateway.class);
        var bloqueado = mock(Usuario.class);
        var pessoa = mock(Pessoa.class);
        when(bloqueado.status()).thenReturn(UsuarioStatus.BLOQUEADO);
        when(bloqueado.id()).thenReturn(UUID.randomUUID());
        when(bloqueado.pessoa()).thenReturn(pessoa);
        when(pessoa.nome()).thenReturn("Pessoa bloqueada");
        when(bloqueado.atualizadoEm()).thenReturn(NOW.minusHours(1));
        when(usuarios.listar()).thenReturn(List.of(bloqueado));

        var result = useCase(atendimento, usuarios).executarUsuarios().join();

        assertEquals(1, result.atencoes().size());
        assertEquals("Pessoa bloqueada", result.atencoes().getFirst().nome());
        assertEquals(1, result.contagensPorStatus().stream()
                .filter(item -> item.status() == UsuarioStatus.BLOQUEADO).findFirst().orElseThrow().quantidade());
    }

    private ConsultarDashboardOperacionalUseCase useCase(
            AtendimentoGateway atendimento,
            UsuarioGateway usuarios) {
        return new ConsultarDashboardOperacionalUseCase(
                atendimento, usuarios, Clock.fixed(Instant.from(NOW), ZoneOffset.UTC));
    }
}
