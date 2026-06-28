INSERT INTO dominio_tipo_pessoa (codigo, descricao) VALUES
    ('FISICA', 'Pessoa fisica'),
    ('JURIDICA', 'Pessoa juridica');

INSERT INTO dominio_status_usuario (codigo, descricao) VALUES
    ('ATIVO', 'Ativo'),
    ('INATIVO', 'Inativo'),
    ('BLOQUEADO', 'Bloqueado');

INSERT INTO dominio_estado_ordem_servico (codigo, descricao, estado_final) VALUES
    ('RECEBIDA', 'Recebida', false),
    ('EM_DIAGNOSTICO', 'Em diagnostico', false),
    ('AGUARDANDO_APROVACAO', 'Aguardando aprovacao', false),
    ('EM_EXECUCAO', 'Em execucao', false),
    ('FINALIZADA', 'Finalizada', false),
    ('ENTREGUE', 'Entregue', true);

INSERT INTO papel (codigo, descricao) VALUES
    ('administrativo', 'Administrativo'),
    ('mecanico', 'Mecanico'),
    ('recepcionista', 'Recepcionista');

INSERT INTO pessoa (id, documento, tipo_pessoa, nome, criado_em, atualizado_em) VALUES
    ('10000000-0000-4000-8000-000000000001', '84191404067', 'FISICA', 'Administrador Laboratorio', '2026-01-17 10:00:00+00', '2026-01-17 10:00:00+00'),
    ('10000000-0000-4000-8000-000000000002', '36655462007', 'FISICA', 'Mecanico Laboratorio', '2026-01-17 10:00:00+00', '2026-01-17 10:00:00+00'),
    ('10000000-0000-4000-8000-000000000003', '17245011010', 'FISICA', 'Recepcionista Laboratorio', '2026-01-17 10:00:00+00', '2026-01-17 10:00:00+00'),
    ('10000000-0000-4000-8000-000000000004', '50132372037', 'FISICA', 'Cliente Laboratorio 1', '2026-01-17 10:00:00+00', '2026-01-17 10:00:00+00'),
    ('10000000-0000-4000-8000-000000000005', '68996860077', 'FISICA', 'Cliente Laboratorio 2', '2026-01-17 10:00:00+00', '2026-01-17 10:00:00+00');

INSERT INTO usuario (id, pessoa_id, password_hash, status, criado_em, atualizado_em) VALUES
    ('20000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', '$2a$10$NCIhuJtwVsBnhWcN/DaXGOmrQFI0hxnKYLQhan4BFpJJG2d2WU6Fm', 'ATIVO', '2026-01-17 10:00:00+00', '2026-01-17 10:00:00+00'),
    ('20000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000002', '$2a$10$QFTTAjNFeLpbkthpoFvsPOAoPaC/pVDODjQbXn/LUATgz6hPAhZ6a', 'ATIVO', '2026-01-17 10:00:00+00', '2026-01-17 10:00:00+00'),
    ('20000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000003', '$2a$10$AuD9uXJZnsb9TAP.Mj0mmuYqvL3/nL.nuTsNsGh.mEl1Ay49zB0Ty', 'ATIVO', '2026-01-17 10:00:00+00', '2026-01-17 10:00:00+00');

INSERT INTO usuario_papel (usuario_id, papel_codigo) VALUES
    ('20000000-0000-4000-8000-000000000001', 'administrativo'),
    ('20000000-0000-4000-8000-000000000001', 'mecanico'),
    ('20000000-0000-4000-8000-000000000001', 'recepcionista'),
    ('20000000-0000-4000-8000-000000000002', 'mecanico'),
    ('20000000-0000-4000-8000-000000000003', 'recepcionista');

INSERT INTO cliente (id, pessoa_id, email, telefone, criado_em, atualizado_em) VALUES
    ('d290f1ee-6c54-4b01-90e6-d701748f0851', '10000000-0000-4000-8000-000000000004', 'cliente1@oficina.com', '+5511999999999', '2026-01-17 10:00:00+00', '2026-01-17 10:00:00+00'),
    ('30000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000005', 'cliente2@oficina.com', '+5511888888888', '2026-01-17 10:00:00+00', '2026-01-17 10:00:00+00');

INSERT INTO veiculo (id, cliente_id, placa, marca, modelo, ano, criado_em, atualizado_em) VALUES
    ('7b1f1a8d-7f4a-4f25-8e74-27d50210a61e', 'd290f1ee-6c54-4b01-90e6-d701748f0851', 'ABC1D23', 'Volkswagen', 'Gol', 2020, '2026-01-17 10:00:00+00', '2026-01-17 10:00:00+00'),
    ('40000000-0000-4000-8000-000000000002', '30000000-0000-4000-8000-000000000002', 'BRA2E24', 'Toyota', 'Corolla', 2024, '2026-01-17 10:00:00+00', '2026-01-17 10:00:00+00');

INSERT INTO ordem_de_servico (id, cliente_id, veiculo_id, descricao_problema, estado_atual, criado_em, atualizado_em) VALUES
    ('2b2276e8-fa72-4f4c-a3b0-2c5b1bf427ef', 'd290f1ee-6c54-4b01-90e6-d701748f0851', '7b1f1a8d-7f4a-4f25-8e74-27d50210a61e', 'Veiculo com falha intermitente no painel', 'EM_DIAGNOSTICO', '2026-01-17 10:00:00+00', '2026-01-17 10:10:00+00'),
    ('f05dd17b-daae-4658-af7c-363dd6e6fdfb', 'd290f1ee-6c54-4b01-90e6-d701748f0851', '7b1f1a8d-7f4a-4f25-8e74-27d50210a61e', 'Veiculo nao liga', 'RECEBIDA', '2026-01-17 11:00:00+00', '2026-01-17 11:00:00+00'),
    ('5b2276e8-fa72-4f4c-a3b0-2c5b1bf427ef', 'd290f1ee-6c54-4b01-90e6-d701748f0851', '7b1f1a8d-7f4a-4f25-8e74-27d50210a61e', 'Revisao preventiva com itens de orcamento', 'AGUARDANDO_APROVACAO', '2026-01-17 12:00:00+00', '2026-01-17 12:30:00+00'),
    ('6b2276e8-fa72-4f4c-a3b0-2c5b1bf427ef', '30000000-0000-4000-8000-000000000002', '40000000-0000-4000-8000-000000000002', 'Troca de oleo aprovada', 'EM_EXECUCAO', '2026-01-17 13:00:00+00', '2026-01-17 13:45:00+00'),
    ('7b2276e8-fa72-4f4c-a3b0-2c5b1bf427ef', '30000000-0000-4000-8000-000000000002', '40000000-0000-4000-8000-000000000002', 'Servico finalizado aguardando entrega', 'FINALIZADA', '2026-01-17 14:00:00+00', '2026-01-17 15:00:00+00');

INSERT INTO estado_ordem_servico (id, ordem_de_servico_id, tipo_estado, data_estado, motivo) VALUES
    ('50000000-0000-4000-8000-000000000001', '2b2276e8-fa72-4f4c-a3b0-2c5b1bf427ef', 'RECEBIDA', '2026-01-17 10:00:00+00', 'Ordem de servico recebida'),
    ('50000000-0000-4000-8000-000000000002', '2b2276e8-fa72-4f4c-a3b0-2c5b1bf427ef', 'EM_DIAGNOSTICO', '2026-01-17 10:10:00+00', 'Diagnostico iniciado'),
    ('50000000-0000-4000-8000-000000000003', 'f05dd17b-daae-4658-af7c-363dd6e6fdfb', 'RECEBIDA', '2026-01-17 11:00:00+00', 'Ordem de servico recebida'),
    ('50000000-0000-4000-8000-000000000004', '5b2276e8-fa72-4f4c-a3b0-2c5b1bf427ef', 'RECEBIDA', '2026-01-17 12:00:00+00', 'Ordem de servico recebida'),
    ('50000000-0000-4000-8000-000000000005', '5b2276e8-fa72-4f4c-a3b0-2c5b1bf427ef', 'EM_DIAGNOSTICO', '2026-01-17 12:10:00+00', 'Diagnostico iniciado'),
    ('50000000-0000-4000-8000-000000000006', '5b2276e8-fa72-4f4c-a3b0-2c5b1bf427ef', 'AGUARDANDO_APROVACAO', '2026-01-17 12:30:00+00', 'Diagnostico finalizado'),
    ('50000000-0000-4000-8000-000000000007', '6b2276e8-fa72-4f4c-a3b0-2c5b1bf427ef', 'RECEBIDA', '2026-01-17 13:00:00+00', 'Ordem de servico recebida'),
    ('50000000-0000-4000-8000-000000000008', '6b2276e8-fa72-4f4c-a3b0-2c5b1bf427ef', 'EM_DIAGNOSTICO', '2026-01-17 13:10:00+00', 'Diagnostico iniciado'),
    ('50000000-0000-4000-8000-000000000009', '6b2276e8-fa72-4f4c-a3b0-2c5b1bf427ef', 'AGUARDANDO_APROVACAO', '2026-01-17 13:25:00+00', 'Diagnostico finalizado'),
    ('50000000-0000-4000-8000-000000000010', '6b2276e8-fa72-4f4c-a3b0-2c5b1bf427ef', 'EM_EXECUCAO', '2026-01-17 13:45:00+00', 'Orcamento aprovado'),
    ('50000000-0000-4000-8000-000000000011', '7b2276e8-fa72-4f4c-a3b0-2c5b1bf427ef', 'RECEBIDA', '2026-01-17 14:00:00+00', 'Ordem de servico recebida'),
    ('50000000-0000-4000-8000-000000000012', '7b2276e8-fa72-4f4c-a3b0-2c5b1bf427ef', 'EM_DIAGNOSTICO', '2026-01-17 14:10:00+00', 'Diagnostico iniciado'),
    ('50000000-0000-4000-8000-000000000013', '7b2276e8-fa72-4f4c-a3b0-2c5b1bf427ef', 'AGUARDANDO_APROVACAO', '2026-01-17 14:25:00+00', 'Diagnostico finalizado'),
    ('50000000-0000-4000-8000-000000000014', '7b2276e8-fa72-4f4c-a3b0-2c5b1bf427ef', 'EM_EXECUCAO', '2026-01-17 14:40:00+00', 'Orcamento aprovado'),
    ('50000000-0000-4000-8000-000000000015', '7b2276e8-fa72-4f4c-a3b0-2c5b1bf427ef', 'FINALIZADA', '2026-01-17 15:00:00+00', 'Execucao finalizada');

INSERT INTO os_item_peca (id, ordem_de_servico_id, peca_id, peca_nome, quantidade, valor_unitario, valor_total) VALUES
    ('60000000-0000-4000-8000-000000000001', '5b2276e8-fa72-4f4c-a3b0-2c5b1bf427ef', '70000000-0000-4000-8000-000000000001', 'Volante', 2.000, 50.00, 100.00),
    ('60000000-0000-4000-8000-000000000002', '6b2276e8-fa72-4f4c-a3b0-2c5b1bf427ef', '70000000-0000-4000-8000-000000000002', 'Filtro de oleo', 1.000, 40.00, 40.00);

INSERT INTO os_item_servico (id, ordem_de_servico_id, servico_id, servico_nome, quantidade, valor_unitario, valor_total) VALUES
    ('80000000-0000-4000-8000-000000000001', '5b2276e8-fa72-4f4c-a3b0-2c5b1bf427ef', '90000000-0000-4000-8000-000000000001', 'Diagnostico eletrico', 1.000, 120.00, 120.00),
    ('80000000-0000-4000-8000-000000000002', '6b2276e8-fa72-4f4c-a3b0-2c5b1bf427ef', '90000000-0000-4000-8000-000000000002', 'Troca de oleo', 1.000, 150.00, 150.00);
