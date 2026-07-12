package br.com.oficina.os.core.usecases.usuario;

import br.com.oficina.os.core.entities.pessoa.Pessoa;
import br.com.oficina.os.core.entities.usuario.CpfOperacional;
import br.com.oficina.os.core.entities.usuario.TipoDePapel;
import br.com.oficina.os.core.entities.usuario.UsuarioStatus;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class UsuarioCommandSupport {
    private UsuarioCommandSupport() {
    }

    static Pessoa pessoa(UUID pessoaId, String nome, String documento) {
        return new Pessoa(pessoaId, new CpfOperacional(documento), nome);
    }

    static UsuarioStatus statusCriacao(String status) {
        return status == null || status.isBlank() ? UsuarioStatus.ATIVO : UsuarioStatus.from(status);
    }

    static UsuarioStatus statusAtualizacao(String status) {
        return UsuarioStatus.from(status);
    }

    static Set<TipoDePapel> papeis(List<String> papeis) {
        if (papeis == null || papeis.isEmpty()) {
            throw new IllegalArgumentException("Ao menos um papel é obrigatório");
        }
        var papeisNormalizados = papeis.stream()
                .map(TipoDePapel::fromValor)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (papeisNormalizados.size() != papeis.size()) {
            throw new IllegalArgumentException("Papéis não podem ser duplicados");
        }
        return papeisNormalizados;
    }
}
