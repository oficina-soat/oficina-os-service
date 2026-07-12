package br.com.oficina.os.core.entities.cliente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClienteTest {

    @Test
    void deveCriarClienteEAlterarDocumento() {
        var clienteId = UUID.randomUUID();
        var cliente = new Cliente(clienteId, new Cpf("84191404067"), new Email("cliente@oficina.com"));

        assertEquals(clienteId, cliente.id());
        assertEquals("84191404067", cliente.documento().valor());
        assertEquals("cliente@oficina.com", cliente.email().valor());

        cliente.alteraDocumentoPara(new Cnpj("04252011000110"));
        cliente.alteraEmailPara(new Email("outro@oficina.com"));

        assertEquals("04252011000110", cliente.documento().valor());
        assertEquals("outro@oficina.com", cliente.email().valor());
    }

    @Test
    void deveCriarClienteComPessoaNomeENormalizarAlteracoes() {
        var clienteId = UUID.randomUUID();
        var cliente = new Cliente(
                clienteId,
                20L,
                new Cpf("84191404067"),
                "  Maria Souza  ",
                new Email("maria@oficina.com"));

        assertEquals(clienteId, cliente.id());
        assertEquals(20L, cliente.pessoaId());
        assertEquals("Maria Souza", cliente.nome());

        cliente.alteraPessoaPara(30L);
        cliente.alteraNomePara("  Maria Atualizada  ");
        cliente.alteraDocumentoPara(new Cnpj("04252011000110"));
        cliente.alteraEmailPara(null);

        assertEquals(30L, cliente.pessoaId());
        assertEquals("Maria Atualizada", cliente.nome());
        assertEquals("04252011000110", cliente.documento().valor());
        assertNull(cliente.email());

        cliente.alteraNomePara(null);
        assertNull(cliente.nome());
    }
}
