package br.com.oficina.os.core.entities.pessoa;

import br.com.oficina.os.core.entities.cliente.Documento;

import java.util.Objects;
import java.util.UUID;

public final class Pessoa {
    private final UUID id;
    private Documento documento;
    private TipoPessoa tipoPessoa;
    private String nome;

    public Pessoa(UUID id, Documento documento, String nome) {
        this(id, documento, TipoPessoa.fromDocumento(documento.valor()), nome);
    }

    public Pessoa(UUID id, Documento documento, TipoPessoa tipoPessoa, String nome) {
        this.id = Objects.requireNonNull(id, "Identificador da pessoa é obrigatório");
        this.documento = Objects.requireNonNull(documento, "Documento é obrigatório");
        this.tipoPessoa = Objects.requireNonNull(tipoPessoa, "Tipo de pessoa é obrigatório");
        this.nome = nomeObrigatorio(nome);
    }

    public void alteraDocumentoPara(Documento documento) {
        this.documento = Objects.requireNonNull(documento, "Documento é obrigatório");
        this.tipoPessoa = TipoPessoa.fromDocumento(documento.valor());
    }

    public void alteraNomePara(String nome) {
        this.nome = nomeObrigatorio(nome);
    }

    public UUID id() {
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

    private static String nomeObrigatorio(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome da pessoa é obrigatório");
        }
        var normalizado = nome.trim();
        if (normalizado.length() > 255) {
            throw new IllegalArgumentException("Nome da pessoa deve ter no máximo 255 caracteres");
        }
        return normalizado;
    }
}
