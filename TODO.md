# TODO do oficina-os-service

## Próximas Tarefas

- [ ] Copiar e adaptar domínio de Pessoa e Usuário do `oficina-app`.
- [ ] Copiar e adaptar domínio de Cliente e Veículo.
- [ ] Copiar e adaptar domínio de Ordem de Serviço.
- [ ] Alinhar controllers, presenters, DTOs e validações às rotas da [OpenAPI do oficina-os-service](../oficina-platform/contracts/openapi/oficina-os-service.yaml).
- [ ] Criar migrations e seed limpo para o database `oficina_os`.
- [ ] Implementar histórico de estados conforme [Contrato de Estados da Ordem de Serviço](../oficina-platform/contracts/Contrato%20de%20Estados%20da%20Ordem%20de%20Serviço.md).
- [ ] Implementar Outbox para eventos produzidos pelo serviço.
- [ ] Implementar publicação dos eventos de OS e Saga.
- [ ] Implementar consumo dos eventos de Billing e Execution.
- [ ] Implementar orquestração da Saga conforme [Contrato de Saga do oficina-os-service](../oficina-platform/contracts/saga/oficina-os-saga-v1.md).
- [ ] Criar testes unitários, de integração e de contrato para APIs, persistência, eventos, idempotência e Saga.

## Eventos Produzidos

- `ordemDeServicoCriada`
- `pecaIncluidaNaOrdemDeServico`
- `servicoIncluidoNaOrdemDeServico`
- `ordemDeServicoFinalizada`
- `ordemDeServicoEntregue`
- `sagaCompensada`
- `sagaFinalizadaComSucesso`

## Eventos Consumidos

- `diagnosticoIniciado`
- `diagnosticoFinalizado`
- `orcamentoGerado`
- `orcamentoAprovado`
- `orcamentoRecusado`
- `execucaoIniciada`
- `execucaoFinalizada`
- `pagamentoSolicitado`
- `pagamentoConfirmado`
- `pagamentoRecusado`
