package br.com.oficina.os.core.entities.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.oficina.os.core.entities.cliente.Cpf;
import br.com.oficina.os.core.entities.pessoa.Pessoa;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UsuarioTest {

    @Test
    void deveManterPessoaEPapeisDoUsuario() {
        var usuarioId = UUID.randomUUID();
        var pessoaId = UUID.randomUUID();
        var pessoa = new Pessoa(pessoaId, new Cpf("84191404067"), "  Ana Silva  ");
        var usuario = new Usuario(
                usuarioId,
                pessoa,
                UsuarioStatus.ATIVO,
                new LinkedHashSet<>(List.of(TipoDePapel.MECANICO, TipoDePapel.MECANICO)));

        assertEquals(usuarioId, usuario.id());
        assertEquals(pessoaId, usuario.pessoa().id());
        assertEquals("Ana Silva", usuario.pessoa().nome());
        assertEquals("84191404067", usuario.pessoa().documento().valor());
        assertEquals(UsuarioStatus.ATIVO, usuario.status());
        assertEquals(Set.of(TipoDePapel.MECANICO), usuario.papeis());
    }

    @Test
    void deveAtualizarEInativarUsuarioPreservandoCriacao() {
        var criadoEm = OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        var pessoa = new Pessoa(UUID.randomUUID(), new Cpf("84191404067"), "Ana");
        var usuario = new Usuario(
                UUID.randomUUID(),
                pessoa,
                UsuarioStatus.ATIVO,
                Set.of(TipoDePapel.MECANICO),
                criadoEm,
                criadoEm);
        var novaPessoa = new Pessoa(pessoa.id(), new Cpf("52998224725"), "Bruno");

        var atualizado = usuario.atualizado(
                novaPessoa,
                UsuarioStatus.BLOQUEADO,
                Set.of(TipoDePapel.ADMINISTRATIVO, TipoDePapel.RECEPCIONISTA));
        atualizado.inativar();
        var atualizadoEm = atualizado.atualizadoEm();
        atualizado.inativar();

        assertEquals(criadoEm, atualizado.criadoEm());
        assertEquals("Bruno", atualizado.pessoa().nome());
        assertEquals(UsuarioStatus.INATIVO, atualizado.status());
        assertEquals(Set.of(TipoDePapel.ADMINISTRATIVO, TipoDePapel.RECEPCIONISTA), atualizado.papeis());
        assertEquals(atualizadoEm, atualizado.atualizadoEm());

        assertThrows(NullPointerException.class, () ->
                new Usuario(null, pessoa, UsuarioStatus.ATIVO, Set.of(TipoDePapel.MECANICO)));
        assertThrows(NullPointerException.class, () ->
                new Usuario(UUID.randomUUID(), null, UsuarioStatus.ATIVO, Set.of(TipoDePapel.MECANICO)));
        assertThrows(NullPointerException.class, () ->
                new Usuario(UUID.randomUUID(), pessoa, null, Set.of(TipoDePapel.MECANICO)));
        assertThrows(IllegalArgumentException.class, () ->
                new Usuario(UUID.randomUUID(), pessoa, UsuarioStatus.ATIVO, Set.of()));
    }

    @Test
    void deveConverterPapeisEStatus() {
        assertEquals("36655462007", new CpfOperacional("36655462007").valor());
        assertEquals(TipoDePapel.MECANICO, TipoDePapel.fromValor("  mecanico  "));
        assertEquals(TipoDePapel.RECEPCIONISTA, TipoDePapel.fromValor("recepcionista"));
        assertEquals(UsuarioStatus.ATIVO, UsuarioStatus.from(" ativo "));
        assertEquals(UsuarioStatus.INATIVO, UsuarioStatus.from("INATIVO"));
        assertEquals(UsuarioStatus.BLOQUEADO, UsuarioStatus.from("bloqueado"));

        assertThrows(IllegalArgumentException.class, () -> TipoDePapel.fromValor(null));
        assertThrows(IllegalArgumentException.class, () -> TipoDePapel.fromValor("  "));
        assertThrows(IllegalArgumentException.class, () -> TipoDePapel.fromValor("financeiro"));
        assertThrows(NullPointerException.class, () -> new CpfOperacional(null));
        assertThrows(IllegalArgumentException.class, () -> new CpfOperacional("123"));
        assertThrows(IllegalArgumentException.class, () -> new CpfOperacional("366.554.620-07"));
        assertThrows(IllegalArgumentException.class, () -> new CpfOperacional("cpf36655462007"));
        assertThrows(IllegalArgumentException.class, () -> UsuarioStatus.from(null));
        assertThrows(IllegalArgumentException.class, () -> UsuarioStatus.from("  "));
        assertThrows(IllegalArgumentException.class, () -> UsuarioStatus.from("suspenso"));
    }
}
