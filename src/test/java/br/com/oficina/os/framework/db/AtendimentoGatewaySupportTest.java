package br.com.oficina.os.framework.db;

import static br.com.oficina.os.framework.db.AtendimentoGatewaySupport.PAYLOAD_EXECUCAO_ID;
import static br.com.oficina.os.framework.db.AtendimentoGatewaySupport.PAYLOAD_MOTIVO;
import static br.com.oficina.os.framework.db.AtendimentoGatewaySupport.normalizar;
import static br.com.oficina.os.framework.db.AtendimentoGatewaySupport.stringFromPayload;
import static br.com.oficina.os.framework.db.AtendimentoGatewaySupport.uuidFromPayload;
import static br.com.oficina.os.framework.db.AtendimentoGatewaySupport.validarCliente;
import static br.com.oficina.os.framework.db.AtendimentoGatewaySupport.validarTransicao;
import static br.com.oficina.os.framework.db.AtendimentoGatewaySupport.validarVeiculo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.oficina.os.core.entities.ordem_de_servico.TipoDeEstadoDaOrdemDeServico;
import jakarta.ws.rs.WebApplicationException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AtendimentoGatewaySupportTest {
    @Test
    void deveExtrairUuidDoPayloadComFallback() {
        var fallback = UUID.randomUUID();
        var fromString = UUID.randomUUID();
        var fromUuid = UUID.randomUUID();

        assertEquals(fallback, uuidFromPayload(Map.of(), PAYLOAD_EXECUCAO_ID, fallback));
        assertEquals(fromString, uuidFromPayload(Map.of(PAYLOAD_EXECUCAO_ID, fromString.toString()), PAYLOAD_EXECUCAO_ID, fallback));
        assertEquals(fromUuid, uuidFromPayload(Map.of(PAYLOAD_EXECUCAO_ID, fromUuid), PAYLOAD_EXECUCAO_ID, fallback));
    }

    @Test
    void deveExtrairTextoOpcionalDoPayload() {
        assertNull(stringFromPayload(Map.of(), PAYLOAD_MOTIVO));
        assertEquals("123", stringFromPayload(Map.of(PAYLOAD_MOTIVO, 123), PAYLOAD_MOTIVO));
    }

    @Test
    void deveValidarCliente() {
        assertDoesNotThrow(() -> validarCliente("Ana", "84191404067", "ana@example.com"));
        assertDoesNotThrow(() -> validarCliente("Ana", "84191404067", " "));
        assertThrows(IllegalArgumentException.class, () -> validarCliente(null, "84191404067", "ana@example.com"));
        assertThrows(IllegalArgumentException.class, () -> validarCliente(" ", "84191404067", "ana@example.com"));
        assertThrows(IllegalArgumentException.class, () -> validarCliente("Ana", "84191404067", "email-invalido"));
    }

    @Test
    void deveValidarVeiculo() {
        assertDoesNotThrow(() -> validarVeiculo("ABC1D23", "Volkswagen", "Gol", 2020));
        assertThrows(IllegalArgumentException.class, () -> validarVeiculo("ABC1D23", "Volkswagen", "Gol", 1899));
        assertThrows(IllegalArgumentException.class, () -> validarVeiculo("placa", "Volkswagen", "Gol", 2020));
    }

    @Test
    void deveValidarTransicoesDeEstado() {
        assertDoesNotThrow(() -> validarTransicao(TipoDeEstadoDaOrdemDeServico.RECEBIDA, TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO));
        assertDoesNotThrow(() -> validarTransicao(TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO, TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO));
        assertThrows(WebApplicationException.class,
                () -> validarTransicao(TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO, TipoDeEstadoDaOrdemDeServico.EM_EXECUCAO));
        assertDoesNotThrow(() -> AtendimentoGatewaySupport.validarTransicaoPorEvento(
                TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO,
                TipoDeEstadoDaOrdemDeServico.EM_EXECUCAO));
        assertDoesNotThrow(() -> validarTransicao(TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO, TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO));
        assertDoesNotThrow(() -> validarTransicao(TipoDeEstadoDaOrdemDeServico.EM_EXECUCAO, TipoDeEstadoDaOrdemDeServico.FINALIZADA));
        assertDoesNotThrow(() -> validarTransicao(TipoDeEstadoDaOrdemDeServico.FINALIZADA, TipoDeEstadoDaOrdemDeServico.ENTREGUE));

        assertThrows(WebApplicationException.class, () -> validarTransicao(TipoDeEstadoDaOrdemDeServico.RECEBIDA, TipoDeEstadoDaOrdemDeServico.ENTREGUE));
        assertThrows(WebApplicationException.class, () -> validarTransicao(TipoDeEstadoDaOrdemDeServico.ENTREGUE, TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO));
    }

    @Test
    void deveNormalizarValoresOpcionais() {
        assertNull(normalizar(null));
        assertNull(normalizar(" "));
        assertEquals("valor", normalizar(" valor "));
    }
}
