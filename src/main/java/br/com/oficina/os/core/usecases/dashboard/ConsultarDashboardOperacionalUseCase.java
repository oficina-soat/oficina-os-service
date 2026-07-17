package br.com.oficina.os.core.usecases.dashboard;

import br.com.oficina.os.core.entities.ordem_de_servico.AcaoPermitidaOrdemServico;
import br.com.oficina.os.core.entities.ordem_de_servico.TipoDeEstadoDaOrdemDeServico;
import br.com.oficina.os.core.entities.usuario.AcaoPermitidaUsuario;
import br.com.oficina.os.core.entities.usuario.UsuarioStatus;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.UsuarioGateway;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ConsultarDashboardOperacionalUseCase {
    private static final int LIMITE_ATENCOES = 5;
    private static final int REFRESH_SECONDS = 30;
    private final AtendimentoGateway atendimentoGateway;
    private final UsuarioGateway usuarioGateway;
    private final Clock clock;

    public ConsultarDashboardOperacionalUseCase(
            AtendimentoGateway atendimentoGateway,
            UsuarioGateway usuarioGateway) {
        this(atendimentoGateway, usuarioGateway, Clock.systemUTC());
    }

    ConsultarDashboardOperacionalUseCase(
            AtendimentoGateway atendimentoGateway,
            UsuarioGateway usuarioGateway,
            Clock clock) {
        this.atendimentoGateway = atendimentoGateway;
        this.usuarioGateway = usuarioGateway;
        this.clock = clock;
    }

    public CompletableFuture<DashboardOrdens> executar() {
        var ordens = atendimentoGateway.listarOrdensServico(null);
        var contagens = Arrays.stream(TipoDeEstadoDaOrdemDeServico.values())
                .map(estado -> new ContagemOrdem(estado, ordens.stream().filter(o -> o.estado() == estado).count()))
                .toList();
        var atencoes = ordens.stream()
                .filter(ordem -> ordem.estado() == TipoDeEstadoDaOrdemDeServico.RECEBIDA
                        || ordem.estado() == TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO
                        || ordem.estado() == TipoDeEstadoDaOrdemDeServico.FINALIZADA)
                .map(ordem -> new OrdemAtencao(
                        ordem.ordemServicoId(),
                        ordem.estado(),
                        ordem.descricaoProblema(),
                        entrouNoEstadoEm(ordem.ordemServicoId(), ordem.estado(), ordem.atualizadoEm()),
                        AcaoPermitidaOrdemServico.porEstado(ordem.estado())))
                .sorted(Comparator.comparing(OrdemAtencao::entrouNoEstadoEm))
                .limit(LIMITE_ATENCOES)
                .toList();
        var now = OffsetDateTime.now(clock);
        return CompletableFuture.completedFuture(
                new DashboardOrdens(now, now, REFRESH_SECONDS, contagens, atencoes));
    }

    public CompletableFuture<DashboardUsuarios> executarUsuarios() {
        var usuarios = usuarioGateway.listar();
        var contagens = Arrays.stream(UsuarioStatus.values())
                .map(status -> new ContagemUsuario(status, usuarios.stream().filter(u -> u.status() == status).count()))
                .toList();
        var atencoes = usuarios.stream()
                .filter(usuario -> usuario.status() == UsuarioStatus.BLOQUEADO)
                .sorted(Comparator.comparing(usuario -> usuario.atualizadoEm()))
                .limit(LIMITE_ATENCOES)
                .map(usuario -> new UsuarioAtencao(
                        usuario.id(),
                        usuario.pessoa().nome(),
                        usuario.status(),
                        usuario.atualizadoEm(),
                        AcaoPermitidaUsuario.para(usuario.status())))
                .toList();
        var now = OffsetDateTime.now(clock);
        return CompletableFuture.completedFuture(
                new DashboardUsuarios(now, now, REFRESH_SECONDS, contagens, atencoes));
    }

    private OffsetDateTime entrouNoEstadoEm(
            UUID ordemServicoId,
            TipoDeEstadoDaOrdemDeServico estado,
            OffsetDateTime fallback) {
        return atendimentoGateway.historico(ordemServicoId).stream()
                .filter(item -> item.estado() == estado)
                .map(AtendimentoGateway.HistoricoRecord::dataDoEstado)
                .max(Comparator.naturalOrder())
                .orElse(fallback);
    }

    public record DashboardOrdens(
            OffsetDateTime generatedAt,
            OffsetDateTime dataAsOf,
            int refreshAfterSeconds,
            List<ContagemOrdem> contagensPorEstado,
            List<OrdemAtencao> atencoes) {
    }

    public record ContagemOrdem(TipoDeEstadoDaOrdemDeServico estado, long quantidade) {
    }

    public record OrdemAtencao(
            UUID ordemServicoId,
            TipoDeEstadoDaOrdemDeServico estado,
            String descricaoProblema,
            OffsetDateTime entrouNoEstadoEm,
            List<AcaoPermitidaOrdemServico> acoesPermitidas) {
    }

    public record DashboardUsuarios(
            OffsetDateTime generatedAt,
            OffsetDateTime dataAsOf,
            int refreshAfterSeconds,
            List<ContagemUsuario> contagensPorStatus,
            List<UsuarioAtencao> atencoes) {
    }

    public record ContagemUsuario(UsuarioStatus status, long quantidade) {
    }

    public record UsuarioAtencao(
            UUID usuarioId,
            String nome,
            UsuarioStatus status,
            OffsetDateTime atualizadoEm,
            List<AcaoPermitidaUsuario> acoesPermitidas) {
    }
}
