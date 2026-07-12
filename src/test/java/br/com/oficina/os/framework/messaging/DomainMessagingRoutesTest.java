package br.com.oficina.os.framework.messaging;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DomainMessagingRoutesTest {

    @Test
    void deveReconhecerTopicosProduzidosDeUsuariosOperacionais() {
        assertTrue(DomainMessagingRoutes.isProduced("usuarioAdicionado", "oficina.os.usuario-adicionado"));
        assertTrue(DomainMessagingRoutes.isProduced("usuarioAtualizado", "oficina.os.usuario-atualizado"));
        assertTrue(DomainMessagingRoutes.isProduced("usuarioExcluido", "oficina.os.usuario-excluido"));
    }
}
