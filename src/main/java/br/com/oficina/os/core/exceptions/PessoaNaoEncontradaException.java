package br.com.oficina.os.core.exceptions;

public class PessoaNaoEncontradaException extends RuntimeException {
    public PessoaNaoEncontradaException(long id) {
        super("Pessoa não encontrada: " + id);
    }

    public PessoaNaoEncontradaException(String documento) {
        super("Pessoa não encontrada: " + documento);
    }
}
