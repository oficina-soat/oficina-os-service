package br.com.oficina.os.core.entities.usuario;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UsuarioTest {

    @Test
    void deveCriarUsuarioComPapeisNormalizadosEImutaveis() {
        var usuario = new Usuario(
                10L,
                20L,
                " Joao ",
                "84191404067",
                UsuarioStatus.ATIVO,
                Set.of(TipoDePapel.ADMINISTRATIVO, TipoDePapel.MECANICO));

        assertEquals(10L, usuario.id());
        assertEquals(20L, usuario.pessoaId());
        assertEquals("Joao", usuario.nome());
        assertEquals(UsuarioStatus.ATIVO, usuario.status());
        assertEquals(2, usuario.papeis().size());
        assertThrows(UnsupportedOperationException.class, () -> usuario.papeis().add(TipoDePapel.RECEPCIONISTA));
    }

    @Test
    void deveResolverPapelPorValorCanonico() {
        assertEquals(TipoDePapel.RECEPCIONISTA, TipoDePapel.fromValor(" recepcionista "));
        assertThrows(IllegalArgumentException.class, () -> TipoDePapel.fromValor("gerente"));
    }
}
