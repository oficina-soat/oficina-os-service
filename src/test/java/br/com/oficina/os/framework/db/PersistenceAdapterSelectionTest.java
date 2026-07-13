package br.com.oficina.os.framework.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.oficina.os.framework.idempotency.PersistentIdempotencyStore;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PersistenceAdapterSelectionTest {

    @Test
    void deveSelecionarStoresEmMemoriaSomenteQuandoExplicitamenteConfigurados() {
        var atendimento = new AtendimentoSeedStore("memory", null);
        assertEquals("Maria Souza", atendimento.buscarCliente(AtendimentoSeedStore.SEED_CLIENTE_ID).nome());

        var usuarios = new UsuarioStore(null, "memory");
        assertEquals(
                "Administrador Laboratorio",
                usuarios.buscar(UsuarioStore.SEED_ADMIN_ID).pessoa().nome());

        var idempotency = new PersistentIdempotencyStore("memory", null);
        var idempotencyRecord = idempotency.createProcessing(
                "scope",
                "key",
                "hash",
                "correlation-id",
                "request-id",
                OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        assertEquals("key", idempotencyRecord.key());
    }

    @Test
    void deveRejeitarKindDesconhecidoSemFallbackParaPostgresOuMemoria() {
        var atendimentoFailure = assertThrows(
                IllegalArgumentException.class,
                () -> new AtendimentoSeedStore("arquivo", null));
        assertEquals(
                "oficina.persistence.kind deve ser postgresql ou memory: arquivo",
                atendimentoFailure.getMessage());

        var usuarioFailure = assertThrows(
                IllegalArgumentException.class,
                () -> new UsuarioStore(null, "arquivo"));
        assertEquals(
                "oficina.persistence.kind deve ser postgresql ou memory: arquivo",
                usuarioFailure.getMessage());

        var idempotencyFailure = assertThrows(
                IllegalArgumentException.class,
                () -> new PersistentIdempotencyStore("arquivo", null));
        assertEquals(
                "oficina.persistence.kind deve ser postgresql ou memory: arquivo",
                idempotencyFailure.getMessage());
    }
}
