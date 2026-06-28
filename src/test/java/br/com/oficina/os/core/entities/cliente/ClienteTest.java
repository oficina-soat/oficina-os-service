package br.com.oficina.os.core.entities.cliente;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClienteTest {

    @Test
    void deveCriarClienteEAlterarDocumento() {
        var cliente = new Cliente(10L, new Cpf("84191404067"), new Email("cliente@oficina.com"));

        assertEquals(10L, cliente.id());
        assertEquals("84191404067", cliente.documento().valor());
        assertEquals("cliente@oficina.com", cliente.email().valor());

        cliente.alteraDocumentoPara(new Cnpj("04252011000110"));
        cliente.alteraEmailPara(new Email("outro@oficina.com"));

        assertEquals("04252011000110", cliente.documento().valor());
        assertEquals("outro@oficina.com", cliente.email().valor());
    }
}
