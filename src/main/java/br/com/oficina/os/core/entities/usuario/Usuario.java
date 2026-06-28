package br.com.oficina.os.core.entities.usuario;



import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class Usuario {
    private final long id;
    private long pessoaId;
    private String nome;
    private String documento;
    private UsuarioStatus status;
    private Set<TipoDePapel> papeis;

    public Usuario(long id,
                   long pessoaId,
                   String nome,
                   String documento,
                   UsuarioStatus status,
                   Set<TipoDePapel> papeis) {
        this.id = id;
        this.pessoaId = pessoaId;
        this.nome = nome == null ? null : nome.trim();
        this.documento = documento;
        this.status = Objects.requireNonNull(status, "Status é obrigatório");
        this.papeis = normalizarPapeis(papeis);
    }

    public Usuario(long id,
                   String nome,
                   String documento,
                   UsuarioStatus status,
                   Set<TipoDePapel> papeis) {
        this(id, 0, nome, documento, status, papeis);
    }

    public void alteraPessoaPara(long pessoaId) {
        this.pessoaId = pessoaId;
    }

    public void alteraNomePara(String nome) {
        this.nome = nome == null ? null : nome.trim();
    }

    public void alteraDocumentoPara(String documento) {
        this.documento = Objects.requireNonNull(documento, "Documento é obrigatório");
    }

    public void alteraStatusPara(UsuarioStatus status) {
        this.status = Objects.requireNonNull(status, "Status é obrigatório");
    }

    public void alteraPapeisPara(Set<TipoDePapel> papeis) {
        this.papeis = normalizarPapeis(papeis);
    }

    public long id() {
        return id;
    }

    public long pessoaId() {
        return pessoaId;
    }

    public String nome() {
        return nome;
    }

    public String documento() {
        return documento;
    }

    public UsuarioStatus status() {
        return status;
    }

    public Set<TipoDePapel> papeis() {
        return papeis;
    }

    private static Set<TipoDePapel> normalizarPapeis(Set<TipoDePapel> papeis) {
        if (papeis == null) {
            return Set.of();
        }

        return Set.copyOf(new LinkedHashSet<>(papeis));
    }
}
