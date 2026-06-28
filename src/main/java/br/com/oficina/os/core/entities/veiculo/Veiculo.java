package br.com.oficina.os.core.entities.veiculo;

public final class Veiculo {
    private final long id;
    private PlacaDeVeiculo placa;
    private MarcaDeVeiculo marca;
    private ModeloDeVeiculo modelo;
    private int ano;

    public Veiculo(
            long id,
            PlacaDeVeiculo placa,
            MarcaDeVeiculo marca,
            ModeloDeVeiculo modelo,
            int ano) {
        this.id = id;
        this.placa = placa;
        this.marca = marca;
        this.modelo = modelo;
        this.ano = ano;
    }

    public void corrigeInformacoes(Veiculo dadosCorrigidos) {
        this.placa = dadosCorrigidos.placa();
        this.marca = dadosCorrigidos.marca();
        this.modelo = dadosCorrigidos.modelo();
        this.ano = dadosCorrigidos.ano();
    }

    public long id() {
        return id;
    }

    public PlacaDeVeiculo placa() {
        return placa;
    }

    public MarcaDeVeiculo marca() {
        return marca;
    }

    public ModeloDeVeiculo modelo() {
        return modelo;
    }

    public int ano() {
        return ano;
    }
}
