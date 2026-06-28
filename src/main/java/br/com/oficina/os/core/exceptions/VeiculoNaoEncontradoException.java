package br.com.oficina.os.core.exceptions;

public class VeiculoNaoEncontradoException extends RuntimeException {

    public VeiculoNaoEncontradoException(String placa) {
        super("Veículo não encontrado para a placa informada: " + placa);
    }

    public VeiculoNaoEncontradoException(long id) {
        super("Veículo não encontrado: " + id);
    }
}
