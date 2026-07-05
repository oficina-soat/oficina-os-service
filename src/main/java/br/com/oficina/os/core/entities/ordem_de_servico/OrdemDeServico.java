package br.com.oficina.os.core.entities.ordem_de_servico;

import br.com.oficina.os.core.exceptions.EstadoDaOrdemDeServicoInvalidoException;
import br.com.oficina.os.core.exceptions.ItemDaOrdemDeServicoInvalidoException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrdemDeServico {
    private final UUID id;
    private final UUID clienteId;
    private final UUID veiculoId;

    private EstadoDaOrdemDeServico estadoAtual;
    private final List<EstadoDaOrdemDeServico> historicoDeEstados = new ArrayList<>();

    private final List<ItemPeca> pecas = new ArrayList<>();
    private final List<ItemServico> servicos = new ArrayList<>();

    OrdemDeServico(
            UUID id,
            UUID clienteId,
            UUID veiculoId,
            EstadoDaOrdemDeServico estadoAtual) {
        this.id = id;
        this.clienteId = clienteId;
        this.veiculoId = veiculoId;
        this.estadoAtual = estadoAtual;
    }

    OrdemDeServico(
            UUID id,
            UUID clienteId,
            UUID veiculoId,
            EstadoDaOrdemDeServico estadoAtual,
            List<EstadoDaOrdemDeServico> historicoDeEstados,
            List<ItemPeca> pecas,
            List<ItemServico> servicos) {
        this.id = id;
        this.clienteId = clienteId;
        this.veiculoId = veiculoId;
        this.estadoAtual = estadoAtual;
        this.historicoDeEstados.addAll(historicoDeEstados);
        this.pecas.addAll(pecas);
        this.servicos.addAll(servicos);
    }

    public void iniciarDiagnostico() {
        exigeRecebida();
        alterarEstado(TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO);
    }

    public void adicionaPeca(long pecaId, String nome, BigDecimal quantidade, BigDecimal valorUnitario) {
        exigeEmDiagnostico();
        validarQtdValor(quantidade, valorUnitario);
        pecas.add(new ItemPeca(pecaId, nome, quantidade, valorUnitario));
    }

    public void adicionaServico(long servicoId, String nome, BigDecimal quantidade, BigDecimal valorUnitario) {
        exigeEmDiagnostico();
        validarQtdValor(quantidade, valorUnitario);
        servicos.add(new ItemServico(servicoId, nome, quantidade, valorUnitario));
    }

    public void finalizarDiagnostico() {
        exigeEmDiagnostico();
        alterarEstado(TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO);
    }

    public void iniciarExecucao() {
        exigeAguardandoAprovacao();
        alterarEstado(TipoDeEstadoDaOrdemDeServico.EM_EXECUCAO);
    }

    public void recusarOrcamento() {
        exigeAguardandoAprovacao();
        alterarEstado(TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO);
    }

    public void finalizar() {
        exigeEmExecucao();
        alterarEstado(TipoDeEstadoDaOrdemDeServico.FINALIZADA);
    }

    public void entregar() {
        exigeFinalizada();
        alterarEstado(TipoDeEstadoDaOrdemDeServico.ENTREGUE);
    }

    private void alterarEstado(TipoDeEstadoDaOrdemDeServico estado) {
        this.estadoAtual = new EstadoDaOrdemDeServico(estado, Instant.now());
        this.historicoDeEstados.add(this.estadoAtual);
    }

    private void exigeRecebida() {
        if (estadoDaOrdemDeServico() != TipoDeEstadoDaOrdemDeServico.RECEBIDA)
            throw new EstadoDaOrdemDeServicoInvalidoException("OS não está RECEBIDA.");
    }

    private void exigeEmDiagnostico() {
        if (estadoDaOrdemDeServico() != TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO)
            throw new EstadoDaOrdemDeServicoInvalidoException("OS não está EM_DIAGNOSTICO.");
    }

    private void exigeAguardandoAprovacao() {
        if (estadoDaOrdemDeServico() != TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO)
            throw new EstadoDaOrdemDeServicoInvalidoException("OS não está AGUARDANDO_APROVACAO.");
    }

    private void exigeEmExecucao() {
        if (estadoDaOrdemDeServico() != TipoDeEstadoDaOrdemDeServico.EM_EXECUCAO)
            throw new EstadoDaOrdemDeServicoInvalidoException("OS não está EM_EXECUCAO.");
    }

    private void exigeFinalizada() {
        if (estadoDaOrdemDeServico() != TipoDeEstadoDaOrdemDeServico.FINALIZADA)
            throw new EstadoDaOrdemDeServicoInvalidoException("OS não está FINALIZADA.");
    }

    private void validarQtdValor(BigDecimal qtd, BigDecimal unit) {
        if (qtd == null || qtd.signum() <= 0) throw new ItemDaOrdemDeServicoInvalidoException("Quantidade inválida.");
        if (unit == null || unit.signum() < 0) throw new ItemDaOrdemDeServicoInvalidoException("Valor unitário inválido.");
    }

    public UUID id() {
        return id;
    }

    public UUID clienteId() {
        return clienteId;
    }

    public UUID veiculoId() {
        return veiculoId;
    }

    public TipoDeEstadoDaOrdemDeServico estadoDaOrdemDeServico() {
        return estadoAtual.estado();
    }

    public Instant dataDoEstado() {
        return estadoAtual.dataDoEstado();
    }

    public List<EstadoDaOrdemDeServico> historicoDeEstados() {
        return List.copyOf(historicoDeEstados);
    }

    public List<ItemPeca> pecas() {
        return List.copyOf(pecas);
    }

    public List<ItemServico> servicos() {
        return List.copyOf(servicos);
    }
}
