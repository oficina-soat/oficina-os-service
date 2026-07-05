package br.com.oficina.os.core.entities.pessoa;

import br.com.oficina.os.core.entities.cliente.Documento;

import java.util.Objects;

public final class Pessoa {
    private final long id;
    private Documento documento;
    private TipoPessoa tipoPessoa;
    private String nome;

    public Pessoa(long id, Documento documento, String nome) {
        this(id, documento, TipoPessoa.fromDocumento(documento.valor()), nome);
    }

    public Pessoa(long id, Documento documento, TipoPessoa tipoPessoa, String nome) {
        this.id = id;
        this.documento = Objects.requireNonNull(documento, "Documento é obrigatório");
        this.tipoPessoa = Objects.requireNonNull(tipoPessoa, "Tipo de pessoa é obrigatório");
        this.nome = nome == null ? null : nome.trim();
    }

    public void alteraDocumentoPara(Documento documento) {
        this.documento = Objects.requireNonNull(documento, "Documento é obrigatório");
        this.tipoPessoa = TipoPessoa.fromDocumento(documento.valor());
    }

    public void alteraNomePara(String nome) {
        this.nome = nome == null ? null : nome.trim();
    }

    public long id() {
        return id;
    }

    public Documento documento() {
        return documento;
    }

    public TipoPessoa tipoPessoa() {
        return tipoPessoa;
    }

    public String nome() {
        return nome;
    }
}
