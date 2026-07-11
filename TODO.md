# TODO do oficina-os-service

## Próximas Tarefas

- [x] Copiar e adaptar domínio de Pessoa e Usuário do `oficina-app`.
- [x] Copiar e adaptar domínio de Cliente e Veículo.
- [x] Copiar e adaptar domínio de Ordem de Serviço.
- [x] Alinhar controllers, presenters, DTOs e validações às rotas da [OpenAPI do oficina-os-service](../oficina-platform/contracts/openapi/oficina-os-service.yaml).
- [x] Criar migrations e seed limpo para o database `oficina_os`.
- [x] Implementar histórico de estados conforme [Contrato de Estados da Ordem de Serviço](../oficina-platform/contracts/Contrato%20de%20Estados%20da%20Ordem%20de%20Serviço.md).
- [x] Implementar Outbox para eventos produzidos pelo serviço.
- [x] Implementar publicação dos eventos de OS e Saga.
- [x] Implementar consumo dos eventos de Billing e Execution.
- [x] Implementar orquestração da Saga conforme [Contrato de Saga do oficina-os-service](../oficina-platform/contracts/saga/oficina-os-saga-v1.md).
- [x] Substituir o store de processo por persistência PostgreSQL runtime para Cliente, Veículo, Ordem de Serviço, histórico de estados, Saga, Inbox e Outbox, mantendo memória apenas como fixture de teste.
- [x] Reorganizar a aplicação em portas e casos de uso de Clean Architecture, isolando `core/` de CDI/JAX-RS/framework e mantendo `AtendimentoSeedStore` apenas como facade de seleção de adapter.
- [x] Criar testes unitários e de integração mínimos para APIs, persistência, eventos, idempotência e Saga.
- [x] Criar cenários BDD automatizados do fluxo feliz e de falha compensada da Saga em [saga_ordem_servico.feature](src/test/resources/features/saga_ordem_servico.feature).
- [x] Configurar cobertura mínima de 80% com JaCoCo e evidência no README/CI, conforme [Padrão BDD, Cobertura e Qualidade](../oficina-platform/docs/delivery/bdd-testing.md).
- [x] Validar contratos OpenAPI, schemas JSON de eventos, erro padronizado, idempotência e Saga.
- [x] Copiar e adaptar workflows de CI/CD, garantindo build, testes, Quality Gate, publicação de imagem e deploy automatizado condicionado por variáveis de ambiente.

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
