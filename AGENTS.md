# AGENTS.md

## Contexto

Este repositório implementa o microsserviço `oficina-os-service`.

O repositório normativo da plataforma é [../oficina-platform](../oficina-platform/). Antes de alterar contratos, rotas, eventos, estados, banco, mensageria, autenticação, observabilidade ou decisões arquiteturais, consulte os artefatos relacionados no `oficina-platform`.

## Ownership do Serviço

O `oficina-os-service` é dono de:

- Pessoa;
- Usuário;
- Cliente;
- Veículo;
- Ordem de Serviço;
- itens da Ordem de Serviço como snapshot;
- histórico de estados;
- estado global da Ordem de Serviço;
- orquestração da Saga.

Banco canônico:

```text
Amazon RDS for PostgreSQL
database: oficina_os
usuario: oficina_os_user
```

## Referências Normativas

- [Matriz de Ownership por Microsserviço](../oficina-platform/docs/service-ownership.md)
- [Plano de Decomposição do oficina-app](../oficina-platform/docs/oficina-app-decomposition.md)
- [Contrato de APIs REST](../oficina-platform/contracts/Contrato%20de%20APIs%20REST.md)
- [OpenAPI do oficina-os-service](../oficina-platform/contracts/openapi/oficina-os-service.yaml)
- [Contrato de Estados da Ordem de Serviço](../oficina-platform/contracts/Contrato%20de%20Estados%20da%20Ordem%20de%20Serviço.md)
- [Contrato de Eventos de Domínio](../oficina-platform/contracts/Contrato%20de%20Eventos%20de%20Domínio.md)
- [Contrato de Tópicos de Mensageria](../oficina-platform/contracts/Contrato%20de%20Tópicos%20de%20Mensageria.md)
- [Contrato de Erros REST](../oficina-platform/contracts/error-model.md)
- [Contrato de Idempotência](../oficina-platform/contracts/idempotency.md)
- [Padrão Outbox por Serviço](../oficina-platform/docs/outbox-pattern.md)
- [Contrato de Saga do oficina-os-service](../oficina-platform/contracts/saga/oficina-os-saga-v1.md)
- [Fluxos da Saga da Ordem de Serviço](../oficina-platform/docs/saga-flows.md)
- [Padrão de Observabilidade Distribuída](../oficina-platform/docs/observability.md)
- [Proposta de Migrations PostgreSQL Decompostas](../oficina-platform/docs/postgres-migrations-decomposition.md)
- [Padrão de isolamento PostgreSQL no RDS compartilhado](../oficina-platform/docs/rds-postgresql-isolation.md)

## Regras de Implementação

- Preserve a estrutura Clean Architecture do repositório:

```text
src/main/java/br/com/oficina/os/
  core/
    entities/
    exceptions/
    interfaces/
    usecases/
  interfaces/
    controllers/
    presenters/
  framework/
    db/
    messaging/
    web/
```

- Não crie biblioteca Java compartilhada entre microsserviços.
- Não acesse o database `oficina_billing` nem tabelas DynamoDB do `oficina-execution-service`.
- Não implemente catálogo técnico, estoque, orçamento ou pagamento neste serviço.
- Publique eventos somente após persistência local bem-sucedida, usando Outbox.
- Operações mutáveis devem exigir `Idempotency-Key`.
- Propague `X-Correlation-Id` em HTTP, eventos, logs e traces.
- Respostas de erro REST devem seguir o contrato de erro da plataforma.
- A autenticação deve usar JWT conforme issuer, audience e JWKS documentados na plataforma.
- Se precisar alterar rota, evento, payload, estado ou ownership, atualize primeiro ou em conjunto os contratos no `oficina-platform`.

## Fontes de Migração

Use [../oficina-app](../oficina-app/) apenas como referência e origem de cópia controlada. Não adapte o `oficina-app` diretamente neste fluxo.

Componentes esperados de origem:

- `br.com.oficina.atendimento`;
- `br.com.oficina.common.core.entities.Pessoa`;
- `br.com.oficina.common.core.entities.Usuario`;
- persistência e recursos web relacionados a Pessoa, Usuário, Cliente, Veículo e Ordem de Serviço.

## Validação

Antes de encerrar alterações relevantes, execute validação proporcional ao impacto:

```bash
./mvnw test -Ppostgresql
```

Quando as ferramentas estiverem disponíveis, use também as validações complementares documentadas em [Ferramentas de validação local](../oficina-platform/docs/validation-tooling.md):

- alterações em GitHub Actions: `actionlint`;
- alterações em `Dockerfile`: `hadolint Dockerfile`;
- alterações em scripts shell: `bash -n`, `shellcheck` e `shfmt -d`;
- mudanças prontas para CI/CD ou release: `./mvnw -B verify -Ppostgresql -DskipITs=false -DfailIfNoTests=false`;
- investigação de falhas remotas de CI/CD: use `gh` autenticado para consultar runs, jobs e logs.

Se uma ferramenta complementar esperada não estiver instalada, registre a limitação na resposta final e execute a melhor validação equivalente disponível.

Para mudanças em contratos de API, eventos, Saga ou estados, valide também os artefatos correspondentes em [../oficina-platform](../oficina-platform/).

## Versionamento

Antes de concluir qualquer alteração relevante, verifique o `project.version` em [pom.xml](pom.xml). Não deixe versões `*-SNAPSHOT` em mudanças prontas para merge, publicação de imagem, release ou deploy; feche a versão no mesmo escopo da alteração ou incremente para uma nova versão fechada quando a mudança exigir novo artefato publicável.

## Commits

Ao concluir alteração relevante neste repositório, crie commit local em português seguindo Conventional Commits, por exemplo:

```bash
git commit -m "feat: implementa cadastro de clientes"
```
