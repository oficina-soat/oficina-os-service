package br.com.oficina.os.core.entities.usuario;

import br.com.oficina.os.core.entities.pessoa.Pessoa;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class Usuario {
    private final UUID id;
    private final Pessoa pessoa;
    private UsuarioStatus status;
    private final Set<TipoDePapel> papeis;
    private final OffsetDateTime criadoEm;
    private OffsetDateTime atualizadoEm;

    public Usuario(UUID id,
                   Pessoa pessoa,
                   UsuarioStatus status,
                   Set<TipoDePapel> papeis) {
        this(id, pessoa, status, papeis, agora());
    }

    private Usuario(UUID id,
                    Pessoa pessoa,
                    UsuarioStatus status,
                    Set<TipoDePapel> papeis,
                    OffsetDateTime agora) {
        this(id, pessoa, status, papeis, agora, agora);
    }

    public Usuario(UUID id,
                   Pessoa pessoa,
                   UsuarioStatus status,
                   Set<TipoDePapel> papeis,
                   OffsetDateTime criadoEm,
                   OffsetDateTime atualizadoEm) {
        this.id = Objects.requireNonNull(id, "Identificador do usuário é obrigatório");
        this.pessoa = Objects.requireNonNull(pessoa, "Pessoa é obrigatória");
        this.status = Objects.requireNonNull(status, "Status é obrigatório");
        this.papeis = papeisObrigatorios(papeis);
        this.criadoEm = Objects.requireNonNull(criadoEm, "Data de criação é obrigatória");
        this.atualizadoEm = Objects.requireNonNull(atualizadoEm, "Data de atualização é obrigatória");
    }

    public Usuario atualizado(Pessoa novaPessoa, UsuarioStatus novoStatus, Set<TipoDePapel> novosPapeis) {
        return new Usuario(
                id,
                novaPessoa,
                novoStatus,
                novosPapeis,
                criadoEm,
                agora());
    }

    public void inativar() {
        if (status != UsuarioStatus.INATIVO) {
            status = UsuarioStatus.INATIVO;
            atualizadoEm = agora();
        }
    }

    public UUID id() {
        return id;
    }

    public Pessoa pessoa() {
        return pessoa;
    }

    public UsuarioStatus status() {
        return status;
    }

    public Set<TipoDePapel> papeis() {
        return papeis;
    }

    public OffsetDateTime criadoEm() {
        return criadoEm;
    }

    public OffsetDateTime atualizadoEm() {
        return atualizadoEm;
    }

    private static Set<TipoDePapel> papeisObrigatorios(Set<TipoDePapel> papeis) {
        if (papeis == null || papeis.isEmpty()) {
            throw new IllegalArgumentException("Ao menos um papel é obrigatório");
        }
        return Set.copyOf(new LinkedHashSet<>(papeis));
    }

    private static OffsetDateTime agora() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
