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

- [Matriz de Ownership por Microsserviço](../oficina-platform/docs/architecture/service-ownership.md)
- [Plano de Decomposição do oficina-app](../oficina-platform/docs/architecture/oficina-app-decomposition.md)
- [Contrato de APIs REST](../oficina-platform/contracts/Contrato%20de%20APIs%20REST.md)
- [OpenAPI do oficina-os-service](../oficina-platform/contracts/openapi/oficina-os-service.yaml)
- [Contrato de Estados da Ordem de Serviço](../oficina-platform/contracts/Contrato%20de%20Estados%20da%20Ordem%20de%20Serviço.md)
- [Contrato de Eventos de Domínio](../oficina-platform/contracts/Contrato%20de%20Eventos%20de%20Domínio.md)
- [Contrato de Tópicos de Mensageria](../oficina-platform/contracts/Contrato%20de%20Tópicos%20de%20Mensageria.md)
- [Contrato de Erros REST](../oficina-platform/contracts/error-model.md)
- [Contrato de Idempotência](../oficina-platform/contracts/idempotency.md)
- [Padrão Outbox por Serviço](../oficina-platform/docs/architecture/outbox-pattern.md)
- [Contrato de Saga do oficina-os-service](../oficina-platform/contracts/saga/oficina-os-saga-v1.md)
- [Fluxos da Saga da Ordem de Serviço](../oficina-platform/docs/architecture/saga-flows.md)
- [Padrão de Observabilidade Distribuída](../oficina-platform/docs/observability/observability.md)
- [Proposta de Migrations PostgreSQL Decompostas](../oficina-platform/docs/infrastructure/postgres-migrations-decomposition.md)
- [Padrão de isolamento PostgreSQL no RDS compartilhado](../oficina-platform/docs/infrastructure/rds-postgresql-isolation.md)

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

### Padrão arquitetural alvo

Este serviço deve convergir para o padrão compilado a partir do `oficina-app` no commit `da88d26c`, internalizado em [Template de regras para monolito modular](../oficina-platform/templates/monolito-modular/README.md). O template original é de monólito modular; neste microsserviço, trate `br.com.oficina.os` como o módulo raiz único do serviço.

Valores adaptados para este repositório:

- `APP_NAME`: `oficina-os-service`;
- `BASE_PACKAGE`: `br.com.oficina.os`;
- `BASE_PACKAGE_PATH`: `br/com/oficina/os`;
- `BASE_PACKAGE_REGEX`: `br\.com\.oficina\.os`;
- `MODULES`: `os`, cobrindo Pessoa, Usuário, Cliente, Veículo, Ordem de Serviço, histórico de estados e Saga;
- `JAVA_VERSION`: `25`;
- `FRAMEWORK`: `Quarkus 3.37.0`;
- `BUILD_COMMAND`: `./mvnw test -Ppostgresql`;
- `FULL_VALIDATION_COMMAND`: `./mvnw -B verify -Ppostgresql -DskipITs=false -DfailIfNoTests=false`.

Layout alvo:

- `core/entities`: entidades, value objects, enums e factories de domínio;
- `core/exceptions`: exceções de negócio;
- `core/interfaces/gateway`: contratos de persistência e integrações;
- `core/interfaces/messaging`: contratos de eventos usados pelo domínio;
- `core/interfaces/presenter`: contratos de saída dos use cases, quando houver;
- `core/interfaces/presenter/dto`: DTOs usados pelos presenters, quando houver;
- `core/interfaces/sender`: contratos de envio/eventos/notificações, quando houver;
- `core/usecases/<subdominio>`: casos de uso e serviços de domínio;
- `interfaces/controllers`: controllers puros que recebem requests, montam commands e chamam use cases;
- `interfaces/presenters`: adapters concretos de presenter;
- `interfaces/presenters/view_model`: modelos devolvidos pela borda HTTP;
- `framework/web`: resources HTTP, exception mappers e configuração CDI;
- `framework/db`: adapters de persistência PostgreSQL e mapeamentos técnicos;
- `framework/messaging`: publishers, consumers e adapters de mensageria;
- `framework/service`: clientes externos e adapters de integração, quando houver;
- `framework/security`: JWT, tokens, autorização e criptografia, quando houver.

Regras de camadas:

- o `core/` deve ser livre de framework: não importe `framework/`, `interfaces/`, `jakarta`, `javax`, `io.quarkus`, `io.smallrye`, `org.jboss` ou `org.eclipse.microprofile` em entidades, portas ou casos de uso;
- use cases devem ser classes Java puras, sem CDI, JAX-RS, Quarkus, JDBC, JSON framework ou annotations de runtime;
- use cases devem expor método principal `executar(...)` e receber comandos por `record Command` quando a entrada tiver mais de um campo conceitual;
- controllers em `interfaces/controllers` devem ser classes públicas sem escopo CDI, com dependências via construtor e sem anotações HTTP;
- controllers devem converter requests em commands e delegar para use cases; regra de negócio deve ficar em use case ou entidade;
- presenters em `interfaces/presenters` devem usar sufixo `PresenterAdapter`, implementar contrato do `core` quando houver, guardar estado da request em campo privado e expor `viewModel()` ou `viewModels()`;
- resources HTTP devem ficar em `framework/web`, usar sufixo `Resource`, concentrar `@Path`, verbos HTTP, autorização, sessão/transação e adaptação de tipos da borda;
- adapters de banco/integração devem implementar gateways do `core`, ficar em `framework/db`, `framework/service` ou `framework/messaging`, e converter entre modelo técnico e domínio dentro do adapter;
- classes de composição devem ficar em `framework/web` com sufixo `Configuration`, ser `@ApplicationScoped`, criar use cases explicitamente e produzir presenters stateful como `@RequestScoped`;
- o modo em memória deve ser fixture explícita de teste/local e não deve conter regra arquitetural que pertença ao `core`;
- facades transitórios, como `AtendimentoSeedStore`, devem permanecer pequenos e não devem receber regra de negócio, SQL ou manipulação extensa de estruturas de memória.

Constraints esperadas:

- mantenha um teste estrutural em `src/test/java/br/com/oficina/os/architecture/ArchitectureConstraintsTest.java` ou equivalente;
- use [ArchitectureConstraintsTest.template.java](../oficina-platform/templates/monolito-modular/ArchitectureConstraintsTest.template.java) como base quando o serviço for ajustado ao padrão alvo;
- exceções legadas devem ser nominais, com caminho completo do arquivo, para impedir que o desvio se espalhe.
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

Mudanças que afetem dependências entre camadas devem passar também pelo teste `CleanArchitectureBoundaryTest`, executado no ciclo Maven padrão.

Quando as ferramentas estiverem disponíveis, use também as validações complementares documentadas em [Ferramentas de validação local](../oficina-platform/docs/delivery/validation-tooling.md):

- alterações em GitHub Actions: `actionlint`;
- alterações em `Dockerfile`: `hadolint Dockerfile`;
- alterações em scripts shell: `bash -n`, `shellcheck` e `shfmt -d`;
- mudanças prontas para CI/CD ou release: `./mvnw -B verify -Ppostgresql -DskipITs=false -DfailIfNoTests=false`;
- investigação de falhas remotas de CI/CD: use `gh` autenticado para consultar runs, jobs e logs.

Se uma ferramenta complementar esperada não estiver instalada, registre a limitação na resposta final e execute a melhor validação equivalente disponível.

Para mudanças em contratos de API, eventos, Saga ou estados, valide também os artefatos correspondentes em [../oficina-platform](../oficina-platform/).

## Versionamento

Antes de concluir qualquer alteração relevante, verifique o `project.version` em [pom.xml](pom.xml) e registre a decisão na revisão final.

Trate como mudança publicável qualquer alteração em código Java, `pom.xml`, `Dockerfile`, configuração runtime, resources, migrations, testes que alterem o artefato validado, dependências, observabilidade, segurança, mensageria ou scripts usados pelo build da imagem.

Toda mudança publicável candidata a merge em `main`, publicação de imagem, release ou deploy deve incrementar `project.version` no mesmo commit ou PR. Use SemVer fechado `MAJOR.MINOR.PATCH`, sem sufixo `SNAPSHOT`; prefira patch para correções compatíveis, minor para funcionalidades compatíveis e major apenas quando houver quebra deliberada acompanhada dos contratos ou ADRs necessários no [oficina-platform](../oficina-platform/).

Antes de considerar a mudança pronta, compare `project.version` com a base do PR ou com o commit anterior da `main`; se a versão não aumentou para uma versão ainda não publicada como tag `v<project.version>` e imagem `<project.version>`, ajuste o `pom.xml` antes de concluir. Não reutilize versão já publicada para novo build, release ou rollout.

## Commits

Ao concluir alteração relevante neste repositório, crie commit local em português seguindo Conventional Commits, por exemplo:

```bash
git commit -m "feat: implementa cadastro de clientes"
```
