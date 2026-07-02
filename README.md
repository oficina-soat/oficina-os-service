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

## Saga orquestrada

A plataforma usa **Saga orquestrada** pelo `oficina-os-service`, conforme a [ADR-009 - Estratégia de Saga Pattern](../oficina-platform/adr/ADR-009%20-%20Estratégia%20de%20Saga%20Pattern.md), os [Fluxos da Saga da Ordem de Serviço](../oficina-platform/docs/saga-flows.md) e o [Contrato de Saga do oficina-os-service](../oficina-platform/contracts/saga/oficina-os-saga-v1.md).

O `oficina-os-service` é o orquestrador porque mantém o ciclo de vida global da Ordem de Serviço, registra histórico de estados e centraliza a decisão de avançar, aguardar, compensar ou bloquear o fluxo distribuído. Essa escolha deixa a sequência de negócio explícita, facilita observabilidade e evita que regras de compensação fiquem dispersas entre `oficina-billing-service` e `oficina-execution-service`.

Os serviços participantes preservam seus próprios bancos e regras de domínio. Este serviço coordena comandos idempotentes e consome eventos de Billing e Execution para atualizar a Saga, publicar `sagaFinalizadaComSucesso` ou executar compensações que resultem em `sagaCompensada`.

## Stack

- Java 25
- Quarkus 3.37.0
- PostgreSQL no database `oficina_os`
- Flyway para migrations
- JWT, OpenAPI, Health, métricas Prometheus, logs JSON e OpenTelemetry

## Setup local

Pré-requisitos:

- Java 25;
- Docker, para build de imagem e dependências locais;
- acesso ao repositório `../oficina-platform`, usado pelos testes de contrato;
- acesso opcional ao repositório `../oficina-infra`, usado para subir dependências compartilhadas da suíte.

Dependências locais compartilhadas podem ser iniciadas pelo `oficina-infra`:

```bash
cd ../oficina-infra
docker compose -f compose.local.yml up -d postgres dynamodb localstack
scripts/local/bootstrap-local.sh
```

Volte para este repositório antes de executar o serviço:

```bash
cd ../oficina-os-service
```

## Execução local

```bash
./mvnw quarkus:dev -Ppostgresql
./mvnw test -Ppostgresql
./mvnw -B verify -Ppostgresql -DskipITs=false -DfailIfNoTests=false
./mvnw -B package -Ppostgresql
```

O comando `verify` executa testes unitários, integração, contrato, BDD e verificação de cobertura JaCoCo.

## Testes e BDD

O cenário BDD do fluxo feliz da Saga está em [src/test/resources/features/saga_ordem_servico.feature](src/test/resources/features/saga_ordem_servico.feature), com steps em [src/test/java/br/com/oficina/os/bdd/SagaOrdemServicoSteps.java](src/test/java/br/com/oficina/os/bdd/SagaOrdemServicoSteps.java). Ele valida a travessia da OS por eventos consumidos de `oficina-execution-service` e `oficina-billing-service`, encerrando a Saga com `sagaFinalizadaComSucesso`.

O runner Cucumber participa do ciclo Maven padrão. Assim, o comando usado pelo [Template GitHub Actions para Microsserviços](../oficina-platform/templates/github-actions/README.md) executa o BDD junto com os demais testes:

```bash
./mvnw -B verify -P"${MAVEN_PROFILE}" -DskipITs=false -DfailIfNoTests=false
```

Evidência local de execução compatível com CI em 2026-07-01:

```text
./mvnw -B verify -Ppostgresql -DskipITs=false -DfailIfNoTests=false
1 scenarios (1 passed)
8 steps (8 passed)
Tests run: 79, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Cobertura

O JaCoCo é executado no `verify`, gera relatório em `target/jacoco-report/` e falha o build quando a cobertura de instruções do bundle fica abaixo de 80%. O [Template GitHub Actions para Microsserviços](../oficina-platform/templates/github-actions/README.md) publica esse diretório como artifact `jacoco-report-oficina-os-service`.

Evidência local de cobertura em 2026-07-01:

```text
instruction=92.24% branch=69.73% line=90.94% complexity=72.01%
```

## CI/CD

Os workflows ficam em [.github/workflows/service-ci.yml](.github/workflows/service-ci.yml) e [.github/workflows/open-pr-to-main.yml](.github/workflows/open-pr-to-main.yml), derivados do [Template GitHub Actions para Microsserviços](../oficina-platform/templates/github-actions/README.md).

Pull requests e pushes na `main` executam o check `service-ci-validate` com `./mvnw -B verify -Ppostgresql -DskipITs=false -DfailIfNoTests=false`, validam a cobertura mínima de 80%, executam o Quality Gate SonarCloud e publicam o artifact `jacoco-report-oficina-os-service`.

A publicação de imagem e o deploy Kubernetes são condicionais:

- `ENABLE_IMAGE_PUBLISH=true` habilita consulta ao ECR, build/push da imagem Docker e release com metadados da imagem;
- `ENABLE_K8S_DEPLOY=true` habilita atualização do Deployment no EKS;
- em `workflow_dispatch`, os inputs `publish_image` e `deploy` permitem acionar esses estágios manualmente.

Enquanto os manifests executáveis não estiverem materializados no `oficina-infra`, mantenha `ENABLE_K8S_DEPLOY=false` e use o job de validação como checagem obrigatória de branch.

## Validação de contratos

O teste [PlatformContractsTest](src/test/java/br/com/oficina/os/contracts/PlatformContractsTest.java) valida o serviço contra os contratos canônicos em `../oficina-platform/contracts`: OpenAPI, schemas JSON de eventos, [Contrato de Erros REST](../oficina-platform/contracts/error-model.md), [Contrato de Idempotência](../oficina-platform/contracts/idempotency.md) e [Contrato de Saga do oficina-os-service](../oficina-platform/contracts/saga/oficina-os-saga-v1.md).

## Docker

```bash
docker build --build-arg MAVEN_PROFILE=postgresql -t oficina-os-service:local .
docker run --rm -p 8080:8080 oficina-os-service:local
```

## Kubernetes

A estratégia de entrega dos manifests está definida em [Estratégia de entrega dos manifestos Kubernetes](../oficina-platform/docs/kubernetes-manifest-strategy.md).

Este repositório mantém o Dockerfile do serviço e não mantém cópia executável dos manifests Kubernetes para evitar divergência. A referência normativa do serviço fica em [Template Kubernetes do oficina-os-service](../oficina-platform/templates/kubernetes/base/oficina-os-service/), e o destino canônico de deploy é `../oficina-infra/k8s/base/microservices/oficina-os-service/`.

O deploy automatizado só deve ser habilitado com `ENABLE_K8S_DEPLOY=true` depois que o Deployment `oficina-os-service` estiver materializado no `oficina-infra` e renderizado pelo overlay `../oficina-infra/k8s/overlays/lab/`.

## Endpoint técnico

- `GET /api/v1/status`: expõe identidade do serviço, ambiente e status técnico básico.

Health checks do Quarkus ficam em `/q/health`, `/q/health/live` e `/q/health/ready`.

## Swagger/OpenAPI

O contrato canônico do serviço é a [OpenAPI do oficina-os-service](../oficina-platform/contracts/openapi/oficina-os-service.yaml), mantida no repositório de plataforma.

Com o serviço em execução local na porta `8080`, a documentação gerada pelo Quarkus fica disponível em:

- Swagger UI: `http://localhost:8080/q/swagger-ui/`;
- OpenAPI YAML: `http://localhost:8080/q/openapi`;
- OpenAPI JSON: `http://localhost:8080/q/openapi?format=json`.

O teste [PlatformContractsTest](src/test/java/br/com/oficina/os/contracts/PlatformContractsTest.java) valida que a OpenAPI gerada em runtime mantém os caminhos e métodos definidos no contrato canônico.

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

Em ambiente local, valores de desenvolvimento ficam em `src/main/resources/application.properties`. Em Kubernetes, variáveis de banco vêm do secret `oficina-os-service-database-env`, e variáveis não sensíveis vêm do ConfigMap definido pelo manifest canônico no `oficina-infra`.

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

O backlog local está em [TODO.md](TODO.md). Os próximos incrementos esperados no Épico B2 são configurar a proteção da branch `main` e manter a documentação local atualizada conforme novos manifests, variáveis e evidências forem materializados.
