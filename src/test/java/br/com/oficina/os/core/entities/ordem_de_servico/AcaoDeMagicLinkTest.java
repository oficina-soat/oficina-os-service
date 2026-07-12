package br.com.oficina.os.core.entities.ordem_de_servico;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class AcaoDeMagicLinkTest {

    @Test
    void deveExporAcoesContratadas() {
        assertArrayEquals(
                new AcaoDeMagicLink[] {
                        AcaoDeMagicLink.APROVAR,
                        AcaoDeMagicLink.RECUSAR,
                        AcaoDeMagicLink.ACOMPANHAR
                },
                AcaoDeMagicLink.values());
    }
}
