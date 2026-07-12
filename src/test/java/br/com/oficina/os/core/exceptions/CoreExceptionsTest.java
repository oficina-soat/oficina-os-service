package br.com.oficina.os.core.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class CoreExceptionsTest {

    @Test
    void deveConstruirExcecoesDeNaoEncontrado() {
        assertNotNull(new VeiculoNaoEncontradoException("ABC1D23").getMessage());
        assertNotNull(new VeiculoNaoEncontradoException(10L).getMessage());
        assertNotNull(new PessoaNaoEncontradaException(20L).getMessage());
        assertNotNull(new PessoaNaoEncontradaException("84191404067").getMessage());
        assertNotNull(new ClienteNaoEncontradoException("84191404067").getMessage());
        assertNotNull(new ClienteNaoEncontradoException(30L).getMessage());
        assertNotNull(new UsuarioNaoEncontradoException(40L).getMessage());
        assertEquals(
                OrdemDeServicoNaoEncontradaException.MENSAGEM_PADRAO,
                new OrdemDeServicoNaoEncontradaException().getMessage());
    }

    @Test
    void deveConstruirExcecoesDeRegraDeDominio() {
        assertEquals("Item invalido", new ItemDaOrdemDeServicoInvalidoException("Item invalido").getMessage());
        assertEquals("Estado invalido", new EstadoDaOrdemDeServicoInvalidoException("Estado invalido").getMessage());
        assertEquals("Magic link expirado", new MagicLinkInvalidoException("Magic link expirado").getMessage());
    }
}
