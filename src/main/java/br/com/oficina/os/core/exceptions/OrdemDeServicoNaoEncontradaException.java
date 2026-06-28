package br.com.oficina.os.core.exceptions;

public class OrdemDeServicoNaoEncontradaException extends RuntimeException {

    public static final String MENSAGEM_PADRAO = "Ordem de serviço não encontrada.";

    public OrdemDeServicoNaoEncontradaException() {
        super(MENSAGEM_PADRAO);
    }
}
