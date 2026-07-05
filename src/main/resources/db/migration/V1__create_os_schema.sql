CREATE TABLE dominio_tipo_pessoa (
    codigo varchar(20) PRIMARY KEY,
    descricao varchar(255) NOT NULL
);

CREATE TABLE dominio_status_usuario (
    codigo varchar(20) PRIMARY KEY,
    descricao varchar(255) NOT NULL
);

CREATE TABLE dominio_estado_ordem_servico (
    codigo varchar(40) PRIMARY KEY,
    descricao varchar(255) NOT NULL,
    estado_final boolean NOT NULL DEFAULT false
);

CREATE TABLE pessoa (
    id uuid PRIMARY KEY,
    documento varchar(20) NOT NULL,
    tipo_pessoa varchar(20) NOT NULL,
    nome varchar(255) NOT NULL,
    criado_em timestamptz NOT NULL,
    atualizado_em timestamptz NOT NULL,
    CONSTRAINT uk_pessoa_documento UNIQUE (documento),
    CONSTRAINT fk_pessoa_tipo
        FOREIGN KEY (tipo_pessoa)
        REFERENCES dominio_tipo_pessoa (codigo)
);

CREATE TABLE papel (
    codigo varchar(40) PRIMARY KEY,
    descricao varchar(255) NOT NULL
);

CREATE TABLE usuario (
    id uuid PRIMARY KEY,
    pessoa_id uuid NOT NULL,
    password_hash varchar(255) NOT NULL,
    status varchar(20) NOT NULL,
    criado_em timestamptz NOT NULL,
    atualizado_em timestamptz NOT NULL,
    CONSTRAINT uk_usuario_pessoa UNIQUE (pessoa_id),
    CONSTRAINT fk_usuario_pessoa
        FOREIGN KEY (pessoa_id)
        REFERENCES pessoa (id),
    CONSTRAINT fk_usuario_status
        FOREIGN KEY (status)
        REFERENCES dominio_status_usuario (codigo)
);

CREATE TABLE usuario_papel (
    usuario_id uuid NOT NULL,
    papel_codigo varchar(40) NOT NULL,
    PRIMARY KEY (usuario_id, papel_codigo),
    CONSTRAINT fk_usuario_papel_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario (id),
    CONSTRAINT fk_usuario_papel_papel
        FOREIGN KEY (papel_codigo)
        REFERENCES papel (codigo)
);

CREATE TABLE cliente (
    id uuid PRIMARY KEY,
    pessoa_id uuid NOT NULL,
    email varchar(255),
    telefone varchar(30),
    criado_em timestamptz NOT NULL,
    atualizado_em timestamptz NOT NULL,
    CONSTRAINT uk_cliente_pessoa UNIQUE (pessoa_id),
    CONSTRAINT fk_cliente_pessoa
        FOREIGN KEY (pessoa_id)
        REFERENCES pessoa (id)
);

CREATE TABLE veiculo (
    id uuid PRIMARY KEY,
    cliente_id uuid NOT NULL,
    placa varchar(10) NOT NULL,
    marca varchar(100) NOT NULL,
    modelo varchar(100) NOT NULL,
    ano integer NOT NULL,
    criado_em timestamptz NOT NULL,
    atualizado_em timestamptz NOT NULL,
    CONSTRAINT uk_veiculo_placa UNIQUE (placa),
    CONSTRAINT fk_veiculo_cliente
        FOREIGN KEY (cliente_id)
        REFERENCES cliente (id),
    CONSTRAINT ck_veiculo_ano_minimo CHECK (ano >= 1900)
);

CREATE TABLE ordem_de_servico (
    id uuid PRIMARY KEY,
    cliente_id uuid NOT NULL,
    veiculo_id uuid NOT NULL,
    descricao_problema varchar(1000) NOT NULL,
    estado_atual varchar(40) NOT NULL,
    criado_em timestamptz NOT NULL,
    atualizado_em timestamptz NOT NULL,
    CONSTRAINT fk_os_cliente
        FOREIGN KEY (cliente_id)
        REFERENCES cliente (id),
    CONSTRAINT fk_os_veiculo
        FOREIGN KEY (veiculo_id)
        REFERENCES veiculo (id),
    CONSTRAINT fk_os_estado_atual
        FOREIGN KEY (estado_atual)
        REFERENCES dominio_estado_ordem_servico (codigo)
);

CREATE TABLE estado_ordem_servico (
    id uuid PRIMARY KEY,
    ordem_de_servico_id uuid NOT NULL,
    tipo_estado varchar(40) NOT NULL,
    data_estado timestamptz NOT NULL,
    motivo varchar(500),
    CONSTRAINT fk_estado_os
        FOREIGN KEY (ordem_de_servico_id)
        REFERENCES ordem_de_servico (id),
    CONSTRAINT fk_estado_tipo
        FOREIGN KEY (tipo_estado)
        REFERENCES dominio_estado_ordem_servico (codigo)
);

CREATE TABLE os_item_peca (
    id uuid PRIMARY KEY,
    ordem_de_servico_id uuid NOT NULL,
    peca_id uuid NOT NULL,
    peca_nome varchar(255) NOT NULL,
    quantidade numeric(15, 3) NOT NULL,
    valor_unitario numeric(14, 2) NOT NULL,
    valor_total numeric(14, 2) NOT NULL,
    CONSTRAINT fk_os_item_peca_os
        FOREIGN KEY (ordem_de_servico_id)
        REFERENCES ordem_de_servico (id),
    CONSTRAINT ck_os_item_peca_quantidade_positiva CHECK (quantidade > 0),
    CONSTRAINT ck_os_item_peca_valor_unitario_nao_negativo CHECK (valor_unitario >= 0),
    CONSTRAINT ck_os_item_peca_valor_total_nao_negativo CHECK (valor_total >= 0)
);

CREATE TABLE os_item_servico (
    id uuid PRIMARY KEY,
    ordem_de_servico_id uuid NOT NULL,
    servico_id uuid NOT NULL,
    servico_nome varchar(255) NOT NULL,
    quantidade numeric(15, 3) NOT NULL,
    valor_unitario numeric(14, 2) NOT NULL,
    valor_total numeric(14, 2) NOT NULL,
    CONSTRAINT fk_os_item_servico_os
        FOREIGN KEY (ordem_de_servico_id)
        REFERENCES ordem_de_servico (id),
    CONSTRAINT ck_os_item_servico_quantidade_positiva CHECK (quantidade > 0),
    CONSTRAINT ck_os_item_servico_valor_unitario_nao_negativo CHECK (valor_unitario >= 0),
    CONSTRAINT ck_os_item_servico_valor_total_nao_negativo CHECK (valor_total >= 0)
);

CREATE TABLE outbox_event (
    id uuid PRIMARY KEY,
    aggregate_id varchar(100) NOT NULL,
    event_type varchar(100) NOT NULL,
    event_version integer NOT NULL,
    topic varchar(200) NOT NULL,
    producer varchar(100) NOT NULL,
    payload jsonb NOT NULL,
    status varchar(30) NOT NULL DEFAULT 'PENDING',
    correlation_id varchar(100),
    occurred_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL,
    published_at timestamptz,
    attempts integer NOT NULL DEFAULT 0,
    next_attempt_at timestamptz,
    last_error text,
    CONSTRAINT ck_outbox_event_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED'))
);

CREATE INDEX ix_pessoa_tipo ON pessoa (tipo_pessoa);
CREATE INDEX ix_usuario_status ON usuario (status);
CREATE INDEX ix_cliente_pessoa ON cliente (pessoa_id);
CREATE INDEX ix_veiculo_cliente ON veiculo (cliente_id);
CREATE INDEX ix_os_cliente ON ordem_de_servico (cliente_id);
CREATE INDEX ix_os_veiculo ON ordem_de_servico (veiculo_id);
CREATE INDEX ix_os_estado ON ordem_de_servico (estado_atual);
CREATE INDEX ix_estado_os ON estado_ordem_servico (ordem_de_servico_id, data_estado);
CREATE INDEX ix_os_item_peca_os ON os_item_peca (ordem_de_servico_id);
CREATE INDEX ix_os_item_peca_catalogo ON os_item_peca (peca_id);
CREATE INDEX ix_os_item_servico_os ON os_item_servico (ordem_de_servico_id);
CREATE INDEX ix_os_item_servico_catalogo ON os_item_servico (servico_id);
CREATE INDEX ix_outbox_event_status_next_attempt ON outbox_event (status, next_attempt_at, created_at);
CREATE INDEX ix_outbox_event_aggregate ON outbox_event (aggregate_id, occurred_at);
