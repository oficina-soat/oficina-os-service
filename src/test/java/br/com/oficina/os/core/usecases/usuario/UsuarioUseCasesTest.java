package br.com.oficina.os.core.usecases.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.oficina.os.core.entities.usuario.TipoDePapel;
import br.com.oficina.os.core.entities.usuario.Usuario;
import br.com.oficina.os.core.entities.usuario.UsuarioStatus;
import br.com.oficina.os.core.exceptions.UsuarioNaoEncontradoException;
import br.com.oficina.os.core.interfaces.gateway.UsuarioGateway;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UsuarioUseCasesTest {

    @Test
    void deveExecutarFluxoCompletoDosCasosDeUso() {
        var gateway = new FakeUsuarioGateway();
        var criado = new CriarUsuarioUseCase(gateway).executar(new CriarUsuarioUseCase.Command(
                " Ana Operadora ",
                "84191404067",
                null,
                List.of("mecanico"))).join();

        assertEquals("Ana Operadora", criado.pessoa().nome());
        assertEquals("84191404067", criado.pessoa().documento().valor());
        assertEquals(UsuarioStatus.ATIVO, criado.status());
        assertEquals(java.util.Set.of(TipoDePapel.MECANICO), criado.papeis());
        assertEquals(criado.id(), new BuscarUsuarioUseCase(gateway).executar(criado.id()).join().id());
        assertEquals(1, new ListarUsuariosUseCase(gateway).executar().join().size());

        var atualizado = new AtualizarUsuarioUseCase(gateway).executar(new AtualizarUsuarioUseCase.Command(
                criado.id(),
                "Ana Administradora",
                "36655462007",
                "bloqueado",
                List.of("administrativo", "recepcionista"))).join();

        assertEquals(criado.pessoa().id(), atualizado.pessoa().id());
        assertEquals("36655462007", atualizado.pessoa().documento().valor());
        assertEquals(UsuarioStatus.BLOQUEADO, atualizado.status());

        new InativarUsuarioUseCase(gateway).executar(criado.id()).join();
        assertEquals(UsuarioStatus.INATIVO, gateway.buscar(criado.id()).status());
    }

    @Test
    void deveRejeitarEntradasInvalidas() {
        var useCase = new CriarUsuarioUseCase(new FakeUsuarioGateway());

        assertThrows(IllegalArgumentException.class, () -> useCase.executar(new CriarUsuarioUseCase.Command(
                " ", "84191404067", "ATIVO", List.of("mecanico"))));
        assertThrows(IllegalArgumentException.class, () -> useCase.executar(new CriarUsuarioUseCase.Command(
                "Ana", "123", "ATIVO", List.of("mecanico"))));
        assertThrows(IllegalArgumentException.class, () -> useCase.executar(new CriarUsuarioUseCase.Command(
                "Ana", "84191404067", "SUSPENSO", List.of("mecanico"))));
        assertThrows(IllegalArgumentException.class, () -> useCase.executar(new CriarUsuarioUseCase.Command(
                "Ana", "84191404067", "ATIVO", List.of())));
        assertThrows(IllegalArgumentException.class, () -> useCase.executar(new CriarUsuarioUseCase.Command(
                "Ana", "84191404067", "ATIVO", List.of("mecanico", "mecanico"))));
        assertThrows(IllegalArgumentException.class, () -> useCase.executar(new CriarUsuarioUseCase.Command(
                "Ana", "84191404067", "ATIVO", List.of("financeiro"))));
    }

    private static final class FakeUsuarioGateway implements UsuarioGateway {
        private final LinkedHashMap<UUID, Usuario> usuarios = new LinkedHashMap<>();

        @Override
        public Usuario criar(Usuario usuario) {
            usuarios.put(usuario.id(), usuario);
            return usuario;
        }

        @Override
        public List<Usuario> listar() {
            return List.copyOf(usuarios.values());
        }

        @Override
        public Usuario buscar(UUID usuarioId) {
            var usuario = usuarios.get(usuarioId);
            if (usuario == null) {
                throw new UsuarioNaoEncontradoException(usuarioId);
            }
            return usuario;
        }

        @Override
        public Usuario atualizar(Usuario usuario) {
            usuarios.put(usuario.id(), usuario);
            return usuario;
        }

        @Override
        public void inativar(UUID usuarioId) {
            buscar(usuarioId).inativar();
        }
    }
}
