package br.com.oficina.os.core.exceptions;

public class ClienteNaoEncontradoException extends RuntimeException {

    public ClienteNaoEncontradoException(String documento) {
        super("Cliente não encontrado para o documento informado: " + documento);
    }

    public ClienteNaoEncontradoException(long id) {
        super("Cliente não encontrado: " + id);
    }
}
