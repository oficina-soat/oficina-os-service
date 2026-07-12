package br.com.oficina.os.interfaces.presenters.view_model;

import br.com.oficina.os.core.entities.pessoa.TipoPessoa;
import br.com.oficina.os.core.entities.usuario.UsuarioStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record UsuarioViewModel(
        UUID usuarioId,
        UUID pessoaId,
        String nome,
        String documento,
        TipoPessoa tipoPessoa,
        UsuarioStatus status,
        List<String> papeis,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm) {
}
