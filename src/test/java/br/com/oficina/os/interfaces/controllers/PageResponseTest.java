package br.com.oficina.os.interfaces.controllers;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageResponseTest {

    @Test
    void deveNormalizarPaginacaoECalcularTotais() {
        var page = PageResponse.of(List.of("a", "b", "c"), -1, 2);

        assertEquals(List.of("a", "b"), page.items());
        assertEquals(0, page.page());
        assertEquals(2, page.size());
        assertEquals(3, page.totalItems());
        assertEquals(2, page.totalPages());
    }

    @Test
    void deveRetornarPaginaVaziaQuandoOffsetPassaDoTotal() {
        var page = PageResponse.of(List.of("a", "b", "c"), 3, 2);

        assertTrue(page.items().isEmpty());
        assertEquals(3, page.page());
        assertEquals(2, page.size());
        assertEquals(3, page.totalItems());
        assertEquals(2, page.totalPages());
    }
}
