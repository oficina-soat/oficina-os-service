# oficina-os-service

Microsserviço responsável por Pessoa, Usuário, Cliente, Veículo, Ordem de Serviço, histórico de estados e orquestração da Saga da plataforma de oficina.

Este repositório segue a governança definida em [../oficina-platform](../oficina-platform/). Para tarefas automatizadas, leia também [AGENTS.md](AGENTS.md) e [TODO.md](TODO.md).

## Responsabilidades

- manter cadastros operacionais de Pessoa, Usuário, Cliente e Veículo;
- abrir, consultar, atualizar estado, cancelar, finalizar e entregar Ordens de Serviço;
- manter snapshots de itens de peça e serviço vinculados à OS;
- registrar histórico de estados da OS;
- orquestrar a Saga da Ordem de Serviço;
- produzir e consumir eventos de domínio ligados à OS e à Saga.

O serviço não é dono de catálogo técnico, estoque, orçamento ou pagamento.

## Stack

- Java 25
- Quarkus 3.37.0
- PostgreSQL no database `oficina_os`
- Flyway para migrations
- JWT, OpenAPI, Health, métricas Prometheus, logs JSON e OpenTelemetry

## Execução local

```bash
./mvnw test -Ppostgresql
./mvnw package -Ppostgresql
```

## Docker

```bash
docker build --build-arg MAVEN_PROFILE=postgresql -t oficina-os-service:local .
docker run --rm -p 8080:8080 oficina-os-service:local
```

## Endpoint técnico

- `GET /api/v1/status`: expõe identidade do serviço, ambiente e status técnico básico.

Health checks do Quarkus ficam em `/q/health`, `/q/health/live` e `/q/health/ready`.

## Contratos

- [Contrato de APIs REST](../oficina-platform/contracts/Contrato%20de%20APIs%20REST.md)
- [OpenAPI do oficina-os-service](../oficina-platform/contracts/openapi/oficina-os-service.yaml)
- [Contrato de Eventos de Domínio](../oficina-platform/contracts/Contrato%20de%20Eventos%20de%20Domínio.md)
- [Contrato de Estados da Ordem de Serviço](../oficina-platform/contracts/Contrato%20de%20Estados%20da%20Ordem%20de%20Serviço.md)
- [Contrato de Erros REST](../oficina-platform/contracts/error-model.md)
- [Contrato de Idempotência](../oficina-platform/contracts/idempotency.md)
- [Contrato de Saga do oficina-os-service](../oficina-platform/contracts/saga/oficina-os-saga-v1.md)

## Variáveis principais

- `DB_USERNAME`
- `DB_PASSWORD`
- `JDBC_DATABASE_URL`
- `REACTIVE_DATABASE_URL`
- `OFICINA_AUTH_ISSUER`
- `OFICINA_AUTH_AUDIENCE`
- `MP_JWT_VERIFY_PUBLICKEY_LOCATION`
- `OTEL_EXPORTER_OTLP_ENDPOINT`
- `DEPLOYMENT_ENVIRONMENT`

## Estrutura

```text
src/main/java/br/com/oficina/os/
  core/
  interfaces/
  framework/
src/main/resources/
  db/migration/
```

## Próximo Trabalho

O backlog local está em [TODO.md](TODO.md). O próximo incremento esperado é migrar Pessoa, Usuário, Cliente, Veículo e Ordem de Serviço a partir de [../oficina-app](../oficina-app/), mantendo alinhamento com o [Plano de Decomposição do oficina-app](../oficina-platform/docs/oficina-app-decomposition.md).
