package br.com.oficina.os.framework.web;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.oficina.os.core.usecases.dashboard.ConsultarDashboardOperacionalUseCase;
import br.com.oficina.os.core.usecases.dashboard.ConsultarDashboardOperacionalUseCase.DashboardOrdens;
import br.com.oficina.os.core.usecases.dashboard.ConsultarDashboardOperacionalUseCase.DashboardUsuarios;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class DashboardResourceTest {
    @Test
    void delegaOsDoisSnapshotsAoCasoDeUso() {
        var useCase = mock(ConsultarDashboardOperacionalUseCase.class);
        var ordens = mock(DashboardOrdens.class);
        var usuarios = mock(DashboardUsuarios.class);
        when(useCase.executar()).thenReturn(CompletableFuture.completedFuture(ordens));
        when(useCase.executarUsuarios()).thenReturn(CompletableFuture.completedFuture(usuarios));
        var resource = new DashboardResource(useCase);

        assertSame(ordens, resource.consultarOrdens().await().indefinitely());
        assertSame(usuarios, resource.consultarUsuarios().await().indefinitely());
    }
}
