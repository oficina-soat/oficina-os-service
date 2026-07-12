package br.com.oficina.os.core.entities.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UsuarioTest {

    @Test
    void deveNormalizarNomeEPapeis() {
        var usuario = new Usuario(
                10L,
                20L,
                "  Ana Silva  ",
                "84191404067",
                UsuarioStatus.ATIVO,
                new LinkedHashSet<>(List.of(TipoDePapel.MECANICO, TipoDePapel.MECANICO)));

        assertEquals(10L, usuario.id());
        assertEquals(20L, usuario.pessoaId());
        assertEquals("Ana Silva", usuario.nome());
        assertEquals("84191404067", usuario.documento());
        assertEquals(UsuarioStatus.ATIVO, usuario.status());
        assertEquals(Set.of(TipoDePapel.MECANICO), usuario.papeis());
    }

    @Test
    void devePermitirAlteracoesENormalizarNulos() {
        var usuario = new Usuario(11L, null, "doc", UsuarioStatus.INATIVO, null);

        assertEquals(0L, usuario.pessoaId());
        assertNull(usuario.nome());
        assertEquals(Set.of(), usuario.papeis());

        usuario.alteraPessoaPara(30L);
        usuario.alteraNomePara("  Bruno  ");
        usuario.alteraDocumentoPara("novo-doc");
        usuario.alteraStatusPara(UsuarioStatus.ATIVO);
        usuario.alteraPapeisPara(Set.of(TipoDePapel.ADMINISTRATIVO, TipoDePapel.RECEPCIONISTA));

        assertEquals(30L, usuario.pessoaId());
        assertEquals("Bruno", usuario.nome());
        assertEquals("novo-doc", usuario.documento());
        assertEquals(UsuarioStatus.ATIVO, usuario.status());
        assertEquals(Set.of(TipoDePapel.ADMINISTRATIVO, TipoDePapel.RECEPCIONISTA), usuario.papeis());

        assertThrows(NullPointerException.class, () -> usuario.alteraDocumentoPara(null));
        assertThrows(NullPointerException.class, () -> usuario.alteraStatusPara(null));
        assertThrows(NullPointerException.class, () ->
                new Usuario(12L, "Carlos", "doc", null, Set.of()));
    }

    @Test
    void deveConverterPapeisEStatus() {
        assertEquals(TipoDePapel.MECANICO, TipoDePapel.fromValor("  mecanico  "));
        assertEquals(TipoDePapel.RECEPCIONISTA, TipoDePapel.fromValor("recepcionista"));
        assertEquals(UsuarioStatus.ATIVO, UsuarioStatus.from(" ativo "));
        assertEquals(UsuarioStatus.INATIVO, UsuarioStatus.from("INATIVO"));

        assertThrows(IllegalArgumentException.class, () -> TipoDePapel.fromValor(null));
        assertThrows(IllegalArgumentException.class, () -> TipoDePapel.fromValor("  "));
        assertThrows(IllegalArgumentException.class, () -> TipoDePapel.fromValor("financeiro"));
        assertThrows(IllegalArgumentException.class, () -> UsuarioStatus.from(null));
        assertThrows(IllegalArgumentException.class, () -> UsuarioStatus.from("  "));
        assertThrows(IllegalArgumentException.class, () -> UsuarioStatus.from("bloqueado"));
    }
}
