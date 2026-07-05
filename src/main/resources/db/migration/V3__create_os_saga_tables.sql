CREATE TABLE dominio_estado_saga (
    codigo varchar(40) PRIMARY KEY,
    descricao varchar(255) NOT NULL,
    estado_final boolean NOT NULL DEFAULT false
);

INSERT INTO dominio_estado_saga (codigo, descricao, estado_final) VALUES
    ('INICIADA', 'Saga iniciada', false),
    ('EM_DIAGNOSTICO', 'Saga em diagnostico', false),
    ('AGUARDANDO_ORCAMENTO', 'Saga aguardando orcamento', false),
    ('AGUARDANDO_APROVACAO', 'Saga aguardando aprovacao', false),
    ('EM_EXECUCAO', 'Saga em execucao', false),
    ('AGUARDANDO_PAGAMENTO', 'Saga aguardando pagamento', false),
    ('AGUARDANDO_ENTREGA', 'Saga aguardando entrega', false),
    ('FINALIZADA_COM_SUCESSO', 'Saga finalizada com sucesso', true),
    ('EM_COMPENSACAO', 'Saga em compensacao', false),
    ('COMPENSADA', 'Saga compensada', true),
    ('FALHA_MANUAL', 'Saga aguardando intervencao manual', true);

CREATE TABLE saga_ordem_servico (
    id uuid PRIMARY KEY,
    ordem_de_servico_id uuid NOT NULL,
    estado varchar(40) NOT NULL,
    estado_os varchar(40) NOT NULL,
    ultima_etapa varchar(100) NOT NULL,
    execucao_id uuid,
    orcamento_id uuid,
    pagamento_id uuid,
    correlation_id varchar(100),
    criado_em timestamptz NOT NULL,
    atualizado_em timestamptz NOT NULL,
    ultimo_erro text,
    CONSTRAINT uk_saga_os UNIQUE (ordem_de_servico_id),
    CONSTRAINT fk_saga_os
        FOREIGN KEY (ordem_de_servico_id)
        REFERENCES ordem_de_servico (id),
    CONSTRAINT fk_saga_estado
        FOREIGN KEY (estado)
        REFERENCES dominio_estado_saga (codigo),
    CONSTRAINT fk_saga_estado_os
        FOREIGN KEY (estado_os)
        REFERENCES dominio_estado_ordem_servico (codigo)
);

CREATE TABLE saga_estado_historico (
    id uuid PRIMARY KEY,
    saga_id uuid NOT NULL,
    estado_anterior varchar(40),
    estado_atual varchar(40) NOT NULL,
    estado_os varchar(40) NOT NULL,
    etapa varchar(100) NOT NULL,
    motivo varchar(500),
    ocorrido_em timestamptz NOT NULL,
    CONSTRAINT fk_saga_hist_saga
        FOREIGN KEY (saga_id)
        REFERENCES saga_ordem_servico (id),
    CONSTRAINT fk_saga_hist_estado_anterior
        FOREIGN KEY (estado_anterior)
        REFERENCES dominio_estado_saga (codigo),
    CONSTRAINT fk_saga_hist_estado_atual
        FOREIGN KEY (estado_atual)
        REFERENCES dominio_estado_saga (codigo),
    CONSTRAINT fk_saga_hist_estado_os
        FOREIGN KEY (estado_os)
        REFERENCES dominio_estado_ordem_servico (codigo)
);

CREATE TABLE inbox_event (
    event_id uuid PRIMARY KEY,
    event_type varchar(100) NOT NULL,
    event_version integer NOT NULL,
    producer varchar(100) NOT NULL,
    aggregate_id uuid NOT NULL,
    correlation_id varchar(100),
    payload jsonb NOT NULL,
    occurred_at timestamptz NOT NULL,
    consumed_at timestamptz NOT NULL,
    status varchar(30) NOT NULL DEFAULT 'CONSUMED',
    last_error text,
    CONSTRAINT ck_inbox_event_status CHECK (status IN ('CONSUMED', 'FAILED'))
);

CREATE INDEX ix_saga_os_estado ON saga_ordem_servico (estado, atualizado_em);
CREATE INDEX ix_saga_os_ordem_servico ON saga_ordem_servico (ordem_de_servico_id);
CREATE INDEX ix_saga_hist_saga ON saga_estado_historico (saga_id, ocorrido_em);
CREATE INDEX ix_inbox_event_aggregate ON inbox_event (aggregate_id, occurred_at);
CREATE INDEX ix_inbox_event_type ON inbox_event (event_type, consumed_at);
