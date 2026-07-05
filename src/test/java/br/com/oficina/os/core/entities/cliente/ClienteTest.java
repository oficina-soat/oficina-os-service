package br.com.oficina.os.core.entities.cliente;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
