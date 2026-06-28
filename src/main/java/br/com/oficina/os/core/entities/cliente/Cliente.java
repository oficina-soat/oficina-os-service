package br.com.oficina.os.core.entities.cliente;

public final class Cliente {
    private final long id;
    private long pessoaId;
    private Documento documento;
    private String nome;
    private Email email;

    public Cliente(
            long id,
            long pessoaId,
            Documento documento,
            String nome,
            Email email) {
        this.id = id;
        this.pessoaId = pessoaId;
        this.documento = documento;
        this.nome = nome == null ? null : nome.trim();
        this.email = email;
    }

    public Cliente(long id, Documento documento, Email email) {
        this(id, 0, documento, null, email);
    }

    public void alteraPessoaPara(long pessoaId) {
        this.pessoaId = pessoaId;
    }

    public void alteraDocumentoPara(Documento documento) {
        this.documento = documento;
    }

    public void alteraNomePara(String nome) {
        this.nome = nome == null ? null : nome.trim();
    }

    public void alteraEmailPara(Email email) {
        this.email = email;
    }

    public long id() {
        return id;
    }

    public long pessoaId() {
        return pessoaId;
    }

    public Documento documento() {
        return documento;
    }

    public String nome() {
        return nome;
    }

    public Email email() {
        return email;
    }
}
