package br.com.oficina.os.framework.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.oficina.os.core.entities.pessoa.Pessoa;
import br.com.oficina.os.core.entities.usuario.CpfOperacional;
import br.com.oficina.os.core.entities.usuario.TipoDePapel;
import br.com.oficina.os.core.entities.usuario.Usuario;
import br.com.oficina.os.core.entities.usuario.UsuarioStatus;
import br.com.oficina.os.core.exceptions.UsuarioConflitanteException;
import br.com.oficina.os.core.exceptions.UsuarioNaoEncontradoException;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemoryUsuarioGatewayTest {

    @Test
    void deveCriarListarAtualizarEInativar() {
        var gateway = new InMemoryUsuarioGateway();
        var criado = gateway.criar(usuario("52998224725", "Operador", UsuarioStatus.ATIVO));

        assertEquals(criado.id(), gateway.buscar(criado.id()).id());
        assertTrue(gateway.listar().stream().anyMatch(candidate -> candidate.id().equals(criado.id())));

        var atualizado = gateway.atualizar(criado.atualizado(
                new Pessoa(criado.pessoa().id(), new CpfOperacional("11144477735"), "Operador Atualizado"),
                UsuarioStatus.BLOQUEADO,
                Set.of(TipoDePapel.ADMINISTRATIVO)));
        assertEquals(UsuarioStatus.BLOQUEADO, atualizado.status());

        gateway.inativar(criado.id());
        gateway.inativar(criado.id());
        assertEquals(UsuarioStatus.INATIVO, gateway.buscar(criado.id()).status());
    }

    @Test
    void deveRejeitarDuplicidadeEIdsInexistentes() {
        var gateway = new InMemoryUsuarioGateway();

        var administradorDuplicado = usuario("84191404067", "Administrador Duplicado", UsuarioStatus.ATIVO);
        assertThrows(UsuarioConflitanteException.class, () -> gateway.criar(administradorDuplicado));
        var usuarioComIdDuplicado = new Usuario(
                UsuarioStore.SEED_ADMIN_ID,
                new Pessoa(UUID.randomUUID(), new CpfOperacional("52998224725"), "ID Duplicado"),
                UsuarioStatus.ATIVO,
                Set.of(TipoDePapel.MECANICO));
        assertThrows(UsuarioConflitanteException.class, () -> gateway.criar(usuarioComIdDuplicado));
        var usuarioInexistenteId = UUID.randomUUID();
        assertThrows(UsuarioNaoEncontradoException.class, () -> gateway.buscar(usuarioInexistenteId));
        var usuarioInexistente = usuario("52998224725", "Inexistente", UsuarioStatus.ATIVO);
        assertThrows(UsuarioNaoEncontradoException.class, () -> gateway.atualizar(usuarioInexistente));

        var criado = gateway.criar(usuario("52998224725", "Primeiro", UsuarioStatus.ATIVO));
        var usuarioConflitante = criado.atualizado(
                new Pessoa(criado.pessoa().id(), new CpfOperacional("36655462007"), "Conflitante"),
                UsuarioStatus.ATIVO,
                Set.of(TipoDePapel.MECANICO));
        assertThrows(UsuarioConflitanteException.class, () -> gateway.atualizar(usuarioConflitante));
    }

    private static Usuario usuario(String documento, String nome, UsuarioStatus status) {
        return new Usuario(
                UUID.randomUUID(),
                new Pessoa(UUID.randomUUID(), new CpfOperacional(documento), nome),
                status,
                Set.of(TipoDePapel.MECANICO));
    }
}
