package br.com.oficina.os.interfaces.controllers;

import br.com.oficina.os.core.usecases.usuario.AtualizarUsuarioUseCase;
import br.com.oficina.os.core.usecases.usuario.BuscarUsuarioUseCase;
import br.com.oficina.os.core.usecases.usuario.CriarUsuarioUseCase;
import br.com.oficina.os.core.usecases.usuario.InativarUsuarioUseCase;
import br.com.oficina.os.core.usecases.usuario.ListarUsuariosUseCase;
import br.com.oficina.os.interfaces.presenters.UsuarioPresenterAdapter;
import br.com.oficina.os.interfaces.presenters.view_model.PageResponse;
import br.com.oficina.os.interfaces.presenters.view_model.UsuarioViewModel;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class UsuariosController {
    private final CriarUsuarioUseCase criarUsuario;
    private final ListarUsuariosUseCase listarUsuarios;
    private final BuscarUsuarioUseCase buscarUsuario;
    private final AtualizarUsuarioUseCase atualizarUsuario;
    private final InativarUsuarioUseCase inativarUsuario;
    private final UsuarioPresenterAdapter presenter;

    public UsuariosController(
            CriarUsuarioUseCase criarUsuario,
            ListarUsuariosUseCase listarUsuarios,
            BuscarUsuarioUseCase buscarUsuario,
            AtualizarUsuarioUseCase atualizarUsuario,
            InativarUsuarioUseCase inativarUsuario,
            UsuarioPresenterAdapter presenter) {
        this.criarUsuario = criarUsuario;
        this.listarUsuarios = listarUsuarios;
        this.buscarUsuario = buscarUsuario;
        this.atualizarUsuario = atualizarUsuario;
        this.inativarUsuario = inativarUsuario;
        this.presenter = presenter;
    }

    public CompletableFuture<UsuarioViewModel> criar(UsuarioCreateRequest request) {
        validarCorpo(request);
        var command = new CriarUsuarioUseCase.Command(
                request.nome(), request.documento(), request.status(), request.papeis());
        return criarUsuario.executar(command).thenApply(presenter::usuario);
    }

    public CompletableFuture<PageResponse<UsuarioViewModel>> listar(Integer page, Integer size) {
        return listarUsuarios.executar()
                .thenApply(usuarios -> usuarios.stream().map(presenter::usuario).toList())
                .thenApply(usuarios -> PageResponse.of(
                        usuarios,
                        page == null ? 0 : page,
                        size == null ? 20 : size));
    }

    public CompletableFuture<UsuarioViewModel> buscar(UUID usuarioId) {
        return buscarUsuario.executar(usuarioId).thenApply(presenter::usuario);
    }

    public CompletableFuture<UsuarioViewModel> atualizar(UUID usuarioId, UsuarioUpdateRequest request) {
        validarCorpo(request);
        var command = new AtualizarUsuarioUseCase.Command(
                usuarioId,
                request.nome(),
                request.documento(),
                request.status(),
                request.papeis());
        return atualizarUsuario.executar(command).thenApply(presenter::usuario);
    }

    public CompletableFuture<Void> inativar(UUID usuarioId) {
        return inativarUsuario.executar(usuarioId);
    }

    private static void validarCorpo(Object request) {
        if (request == null) {
            throw new IllegalArgumentException("Corpo da requisição é obrigatório");
        }
    }

    public record UsuarioCreateRequest(String nome, String documento, String status, List<String> papeis) {
    }

    public record UsuarioUpdateRequest(String nome, String documento, String status, List<String> papeis) {
    }
}
