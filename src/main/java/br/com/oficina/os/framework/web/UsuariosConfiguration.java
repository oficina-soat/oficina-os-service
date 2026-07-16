package br.com.oficina.os.framework.web;

import br.com.oficina.os.core.interfaces.gateway.UsuarioGateway;
import br.com.oficina.os.core.usecases.usuario.AtualizarUsuarioUseCase;
import br.com.oficina.os.core.usecases.usuario.BuscarUsuarioUseCase;
import br.com.oficina.os.core.usecases.usuario.CriarUsuarioUseCase;
import br.com.oficina.os.core.usecases.usuario.InativarUsuarioUseCase;
import br.com.oficina.os.core.usecases.usuario.ListarUsuariosUseCase;
import br.com.oficina.os.core.usecases.usuario.AlterarStatusUsuarioUseCase;
import br.com.oficina.os.interfaces.controllers.UsuariosController;
import br.com.oficina.os.interfaces.presenters.UsuarioPresenterAdapter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class UsuariosConfiguration {
    @Produces
    CriarUsuarioUseCase criarUsuarioUseCase(UsuarioGateway gateway) {
        return new CriarUsuarioUseCase(gateway);
    }

    @Produces
    ListarUsuariosUseCase listarUsuariosUseCase(UsuarioGateway gateway) {
        return new ListarUsuariosUseCase(gateway);
    }

    @Produces
    BuscarUsuarioUseCase buscarUsuarioUseCase(UsuarioGateway gateway) {
        return new BuscarUsuarioUseCase(gateway);
    }

    @Produces
    AtualizarUsuarioUseCase atualizarUsuarioUseCase(UsuarioGateway gateway) {
        return new AtualizarUsuarioUseCase(gateway);
    }

    @Produces
    InativarUsuarioUseCase inativarUsuarioUseCase(UsuarioGateway gateway) {
        return new InativarUsuarioUseCase(gateway);
    }

    @Produces
    AlterarStatusUsuarioUseCase alterarStatusUsuarioUseCase(UsuarioGateway gateway) {
        return new AlterarStatusUsuarioUseCase(gateway);
    }

    @Produces
    UsuariosController usuariosController(
            CriarUsuarioUseCase criarUsuario,
            ListarUsuariosUseCase listarUsuarios,
            BuscarUsuarioUseCase buscarUsuario,
            AtualizarUsuarioUseCase atualizarUsuario,
            InativarUsuarioUseCase inativarUsuario,
            AlterarStatusUsuarioUseCase alterarStatusUsuario,
            UsuarioPresenterAdapter presenter) {
        return new UsuariosController(
                criarUsuario,
                listarUsuarios,
                buscarUsuario,
                atualizarUsuario,
                inativarUsuario,
                alterarStatusUsuario,
                presenter);
    }

    @Produces
    @RequestScoped
    UsuarioPresenterAdapter usuarioPresenter() {
        return new UsuarioPresenterAdapter();
    }
}
