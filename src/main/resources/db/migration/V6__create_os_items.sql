CREATE TABLE ordem_servico_servico (
    ordem_servico_id UUID NOT NULL REFERENCES ordem_de_servico(id),
    servico_id UUID NOT NULL,
    nome VARCHAR(200) NOT NULL,
    quantidade NUMERIC(12, 3) NOT NULL CHECK (quantidade > 0),
    valor_unitario NUMERIC(14, 2) NOT NULL CHECK (valor_unitario >= 0),
    criado_em TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (ordem_servico_id, servico_id)
);

CREATE TABLE ordem_servico_peca (
    ordem_servico_id UUID NOT NULL REFERENCES ordem_de_servico(id),
    peca_id UUID NOT NULL,
    nome VARCHAR(200) NOT NULL,
    quantidade NUMERIC(12, 3) NOT NULL CHECK (quantidade > 0),
    valor_unitario NUMERIC(14, 2) NOT NULL CHECK (valor_unitario >= 0),
    criado_em TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (ordem_servico_id, peca_id)
);
