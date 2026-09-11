# Plano — Evoluir `sample-jee8` para um CRUD REST completo de *Books* com JPA + H2 in-memory

Documento de planejamento. Nada aqui foi implementado ainda.

---

## 1. Objetivo

Transformar o skeleton atual (um único recurso JAX-RS `TestResource`) em uma API REST
**CRUD completa** para a entidade **`Book`**, com **persistência JPA** sobre um banco
**H2 em memória**, mantendo tudo que já existe: Java EE 8 (`javax.*`), JAX-RS 2.1, CDI 2.0,
deploy no **WebLogic 14.1.2 / JDK 21**, gate de formatação **Spotless**, **Lombok** e
documentação **OpenAPI/Swagger**.

### Critérios de sucesso

- `POST/GET/GET{id}/PUT/PATCH/DELETE` em `/sample-jee8/api/books` funcionando no WebLogic.
- Dados iniciais carregados por script de seed; persistência real via `EntityManager`.
- Validação de entrada (Bean Validation) e respostas de erro padronizadas (JSON).
- Testes automatizados de repositório e serviço rodando em `mvn test` (H2, sem container).
- `mvn clean install` verde (Spotless + testes) e `autodeploy.ps1` publicando sem config extra de domínio.
- `/api/openapi.json` e `api-docs.html` refletindo os endpoints de `books`.

---

## 2. Estado atual (baseline)

| Item | Situação |
|---|---|
| Runtime | WebLogic 14.1.2, JDK 21, context root `/sample-jee8`, JAX-RS em `/api` |
| Código | `ApplicationConfig` (registra `TestResource` + `OpenApiResource`), `TestResource` (`GET /api/test`) |
| Persistência | **nenhuma** |
| Build | Maven wrapper; `maven-compiler-plugin` (release 21, Lombok em `annotationProcessorPaths`); `spotless-maven-plugin` (`check` em `process-sources`) |
| Libs | `javaee-api:8.0.1` (provided), `lombok` (provided/optional), `swagger-jaxrs2`, `swagger-ui` |
| Descritores | `web.xml` (Servlet 4.0), `beans.xml` (`bean-discovery-mode="annotated"`), `weblogic.xml` (context-root + `prefer-application-packages` p/ jackson/snakeyaml) |

---

## 3. Decisões de arquitetura (com justificativa)

### 3.1 Estratégia de transação: **RESOURCE_LOCAL, EntityManager gerenciado pela aplicação**

Duas opções:

| Abordagem | Prós | Contras |
|---|---|---|
| **A. RESOURCE_LOCAL + EM da aplicação** (escolhida) | Zero configuração no domínio WebLogic; `autodeploy.ps1` continua igual; portável; roda idêntico nos testes | Nós controlamos `begin/commit/rollback` (via interceptor) |
| B. JTA + DataSource no WebLogic | Injeção `@PersistenceContext` container-managed; `@Transactional` nativo | Exige criar DataSource no `base_domain` (console/WLST) e pôr o driver H2 em `wlserver/.../lib`; quebra o "deploy e pronto" |

→ **Opção A.** Um `EntityManagerFactory` `@ApplicationScoped` (produtor CDI), um `EntityManager`
`@RequestScoped` (produtor CDI, fechado no `@Disposes`), e um **interceptor CDI `@Tx`** que
envolve os métodos de escrita do serviço com uma transação de recurso local.
Alternativa mais simples (se não quiser interceptor): abrir/commitar a transação dentro de cada
método do repositório.

### 3.2 Provedor JPA: **EclipseLink do próprio WebLogic — não empacotar**

WebLogic 14.1.2 já traz EclipseLink como provedor padrão. Empacotar outro provedor no WAR
arrisca conflito de classloader. `persistence.xml` declara explicitamente
`org.eclipse.persistence.jpa.PersistenceProvider` para o container e os testes concordarem.

- **Nos testes** (fora do container) adicionamos `org.eclipse.persistence:eclipselink:2.7.15`
  em escopo `test` — a linha **2.7.x** ainda é `javax.persistence` (3.0+ já é `jakarta`).

### 3.3 Banco: **H2 2.3.x in-memory**, driver no WAR (`scope=compile`)

```
jdbc:h2:mem:sampledb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
```

- `DB_CLOSE_DELAY=-1` é **obrigatório**: sem ele o schema é descartado quando a primeira
  conexão fecha.
- Schema criado por *schema generation* padrão do JPA
  (`javax.persistence.schema-generation.database.action=drop-and-create`).
- Seed via `javax.persistence.sql-load-script-source=META-INF/sql/books-seed.sql` (padrão JPA 2.1,
  independente de provedor).
- `eclipselink.weaving=false` — sem weaving dinâmico no WAR; o modelo é simples e não usa lazy.
- Dados **resetam a cada redeploy/restart** — comportamento aceito para um exemplo.

### 3.4 Contrato: **DTOs de entrada/saída**, entidade nunca exposta

`BookRequest` / `BookPatchRequest` (entrada, com Bean Validation) e `BookResponse` (saída).
Mantém o contrato estável e separa validação de API da modelagem de banco.

### 3.5 Mapeamento entidade ⇄ DTO: **manual, num `BookMapper` CDI**

Simples e sem processador extra. MapStruct fica como melhoria opcional (Fase 8) — exigiria
`lombok-mapstruct-binding` e ordenar os processadores.

### 3.6 Lombok + entidade JPA

- `@Getter`/`@Setter` + `@NoArgsConstructor` na entidade.
- **Não** usar `@Data`/`@EqualsAndHashCode` gerado na entidade (armadilha clássica com
  coleções lazy e identidade JPA). `equals`/`hashCode` por chave de negócio (`isbn`) escritos à mão,
  ou deixar identidade padrão.
- Nos DTOs, `@Data`/`@Builder` à vontade.

### 3.7 Ativação do interceptor

Via `@Priority(Interceptor.Priority.APPLICATION)` na classe do interceptor (CDI 1.1+ ativa
globalmente) — evita mexer no `beans.xml`.

---

## 4. Modelo de dados — entidade `Book`

| Campo | Tipo Java | Coluna / regra |
|---|---|---|
| `id` | `Long` | `@Id @GeneratedValue(strategy = IDENTITY)` |
| `title` | `String` | `NOT NULL`, ≤ 200 |
| `author` | `String` | `NOT NULL`, ≤ 120 |
| `isbn` | `String` | **único**, ≤ 20 (ISBN-13 com hífens) |
| `publishedYear` | `Integer` | 1450 … (ano atual + 1) |
| `genre` | `BookGenre` (enum) | `@Enumerated(STRING)`, nullable |
| `pages` | `Integer` | > 0, nullable |
| `createdAt` | `Instant` | `@PrePersist`, imutável |
| `updatedAt` | `Instant` | `@PrePersist` + `@PreUpdate` |
| `version` | `Long` | `@Version` (lock otimista) |

`BookGenre`: `FICTION, NONFICTION, TECHNOLOGY, SCIENCE, HISTORY, BIOGRAPHY, OTHER`.

---

## 5. Contrato REST — `/sample-jee8/api/books`

| Método | Caminho | Corpo | Sucesso | Erros |
|---|---|---|---|---|
| `POST` | `/books` | `BookRequest` | `201 Created` + header `Location` + `BookResponse` | `400` validação · `409` ISBN duplicado |
| `GET` | `/books` | — (query: `page`, `size`, `sort`, `title`, `author`, `genre`) | `200` `PageResponse<BookResponse>` | `400` parâmetros inválidos |
| `GET` | `/books/{id}` | — | `200` `BookResponse` | `404` |
| `PUT` | `/books/{id}` | `BookRequest` (substituição total) | `200` `BookResponse` | `400` · `404` · `409` |
| `PATCH` | `/books/{id}` | `BookPatchRequest` (campos parciais) | `200` `BookResponse` | `400` · `404` · `409` |
| `DELETE` | `/books/{id}` | — | `204 No Content` | `404` |

`PageResponse<T>`: `{ items: T[], page, size, totalElements, totalPages }`.
Paginação default: `page=0`, `size=20`, `size` máx. `100`. `sort` = `campo,asc|desc`.

### `ApiError` (corpo de todo erro)

```json
{
  "timestamp": "2026-09-10T21:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Payload inválido",
  "path": "/sample-jee8/api/books",
  "details": ["title: não pode estar em branco", "isbn: formato inválido"]
}
```

---

## 6. Validação (Bean Validation, `javax.validation`)

- `BookRequest`: `@NotBlank` (`title`, `author`, `isbn`), `@Size` nos textos,
  `@Pattern` no `isbn` (ISBN-10/13), `@Min/@Max` em `publishedYear`, `@Positive` em `pages`.
- `BookResource` usa `@Valid` no parâmetro de corpo → dispara `ConstraintViolationException`.
- `BookPatchRequest`: mesmos limites de tamanho/pattern, **sem** `@NotBlank` (campos opcionais).
- Regra de ano "≤ ano atual + 1" → validador customizado `@ValidPublishedYear` **ou** checagem no serviço.

---

## 7. Tratamento de erros (`ExceptionMapper`)

| Exceção | HTTP | Origem |
|---|---|---|
| `BookNotFoundException` (custom) | `404` | serviço, quando `findById` vazio |
| `DuplicateIsbnException` (custom) | `409` | serviço, antes de `persist`/`merge` |
| `ConstraintViolationException` | `400` | Bean Validation (`@Valid`) — mapper com prioridade acima do default |
| `jakarta`/`javax` `PersistenceException` (unique) | `409` | rede de segurança para corrida de ISBN |
| `Exception` (fallback) | `500` | qualquer outra — corpo sem stacktrace, log completo no servidor |

Todos produzem `ApiError`. Mappers registrados em `ApplicationConfig.getClasses()`.

---

## 8. Estrutura de pacotes / arquivos

```
src/main/java/org/timsoft/api/
  ApplicationConfig.java            (MOD: registrar BookResource + mappers)
  book/
    Book.java                      entidade
    BookGenre.java                 enum
    BookResource.java              JAX-RS  @Path("books")  @RequestScoped
    BookService.java               regras de negócio       @ApplicationScoped, writes com @Tx
    BookRepository.java            acesso a dados (JPQL/Criteria) @ApplicationScoped
    BookMapper.java                entidade <-> DTO         @ApplicationScoped
    dto/
      BookRequest.java
      BookPatchRequest.java
      BookResponse.java
      PageResponse.java
  persistence/
    EntityManagerProducer.java     @Produces EMF (@ApplicationScoped) + EM (@RequestScoped)
    Tx.java                        @InterceptorBinding
    TransactionalInterceptor.java  @Interceptor @Tx @Priority(APPLICATION)
  error/
    ApiError.java
    BookNotFoundException.java
    DuplicateIsbnException.java
    BookNotFoundExceptionMapper.java
    DuplicateIsbnExceptionMapper.java
    ConstraintViolationExceptionMapper.java
    PersistenceExceptionMapper.java
    FallbackExceptionMapper.java

src/main/resources/META-INF/
  persistence.xml
  sql/books-seed.sql

src/test/java/org/timsoft/api/book/
  BookRepositoryTest.java          H2 real, RESOURCE_LOCAL
  BookServiceTest.java             Mockito no repositório
  BookMapperTest.java
src/test/resources/META-INF/
  persistence.xml                  PU de teste (mesmo nome ou "sampledbPU-test")
```

`ApplicationConfig` atualizado:

```java
return Set.of(
    TestResource.class,
    OpenApiResource.class,
    BookResource.class,
    BookNotFoundExceptionMapper.class,
    DuplicateIsbnExceptionMapper.class,
    ConstraintViolationExceptionMapper.class,
    PersistenceExceptionMapper.class,
    FallbackExceptionMapper.class);
```

---

## 9. Dependências novas (`pom.xml`)

```xml
<properties>
  <h2.version>2.3.232</h2.version>
  <junit.version>5.11.4</junit.version>
  <mockito.version>5.14.2</mockito.version>
  <eclipselink.version>2.7.15</eclipselink.version>   <!-- javax; só para os testes -->
  <maven-surefire-plugin.version>3.5.2</maven-surefire-plugin.version>
</properties>
```

| Dependência | Escopo | Motivo |
|---|---|---|
| `com.h2database:h2:${h2.version}` | `compile` | driver JDBC — **vai no WAR** |
| `org.eclipse.persistence:eclipselink:${eclipselink.version}` | `test` | provedor JPA para rodar os testes fora do WebLogic |
| `org.junit.jupiter:junit-jupiter:${junit.version}` | `test` | testes |
| `org.mockito:mockito-junit-jupiter:${mockito.version}` | `test` | mock do repositório no `BookServiceTest` |
| `org.assertj:assertj-core` (opcional) | `test` | asserts legíveis |

- **Não** adicionar provedor JPA em `compile`/`runtime` (WebLogic fornece).
- `javax.persistence` / `javax.validation` já vêm do `javaee-api` (provided → presente também no classpath de teste).
- Adicionar `maven-surefire-plugin` fixado para execução estável do JUnit 5.

---

## 10. Configuração

### `src/main/resources/META-INF/persistence.xml`

```xml
<persistence xmlns="http://xmlns.jcp.org/xml/ns/persistence"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/persistence
               http://xmlns.jcp.org/xml/ns/persistence/persistence_2_2.xsd"
             version="2.2">
  <persistence-unit name="sampledbPU" transaction-type="RESOURCE_LOCAL">
    <provider>org.eclipse.persistence.jpa.PersistenceProvider</provider>
    <class>org.timsoft.api.book.Book</class>
    <exclude-unlisted-classes>true</exclude-unlisted-classes>
    <properties>
      <property name="javax.persistence.jdbc.driver"   value="org.h2.Driver"/>
      <property name="javax.persistence.jdbc.url"      value="jdbc:h2:mem:sampledb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"/>
      <property name="javax.persistence.jdbc.user"     value="sa"/>
      <property name="javax.persistence.jdbc.password" value=""/>
      <property name="javax.persistence.schema-generation.database.action" value="drop-and-create"/>
      <property name="javax.persistence.sql-load-script-source" value="META-INF/sql/books-seed.sql"/>
      <property name="eclipselink.weaving"       value="false"/>
      <property name="eclipselink.logging.level" value="INFO"/>
    </properties>
  </persistence-unit>
</persistence>
```

### `src/main/resources/META-INF/sql/books-seed.sql`

3–5 `INSERT INTO BOOK (...) VALUES (...)` de exemplo (Clean Code, The Pragmatic Programmer, etc.).
Sem `id` se a coluna for IDENTITY, ou com `id` explícito + ajuste de sequência.

### `weblogic.xml`

Sem mudança obrigatória. H2 não conflita com libs do WebLogic. (Se aparecer choque de
EclipseLink, avaliar `prefer-application-packages`, mas o plano é **não** empacotar EclipseLink.)

---

## 11. Testes

| Teste | Tipo | Como |
|---|---|---|
| `BookRepositoryTest` | integração leve | `Persistence.createEntityManagerFactory("sampledbPU")` com H2 `mem`; cada teste em transação com rollback ou schema recriado; cobre `save`, `findById`, filtros+paginação, `existsByIsbn`, `deleteById`, `@Version` |
| `BookServiceTest` | unitário | Mockito no `BookRepository`; cobre regras: 404, 409 ISBN, patch parcial, update total |
| `BookMapperTest` | unitário | ida e volta entidade ⇄ DTO |
| `BookResourceIT` (opcional, Fase 8) | e2e | REST Assured contra deploy real, ou Arquillian |

- PU de teste pode ser o mesmo `sampledbPU` (o `persistence.xml` de `src/test/resources` tem
  precedência no classpath de teste) — útil para logging mais verboso e `create-or-extend`.
- `mvn test` deve rodar tudo sem WebLogic.

---

## 12. OpenAPI / Swagger

- `BookResource`: `@Tag(name = "books")`; cada método com `@Operation(summary, description)` e
  `@ApiResponse` para os códigos relevantes.
- DTOs: `@Schema(description = ..., example = ...)` nos campos.
- `ApiError`: anotar como schema reutilizável.
- Verificar após deploy: `GET /sample-jee8/api/openapi.json` lista `/books*`; `api-docs.html`
  renderiza a seção "books".

---

## 13. Impacto em Spotless / Lombok

- Todo arquivo novo passa por `spotless:apply` antes do commit; `spotless:check`
  (`process-sources`) quebra o build se algo ficar desformatado — **sem exceção para o CRUD**.
- Lombok já está em `annotationProcessorPaths`; `@Getter/@Setter/@Data/@Builder` funcionam.
- `mvn-clean-install.ps1` (que roda `spotless:apply clean install`) formata e builda de uma vez.

---

## 14. Plano de execução em fases

> Cada fase termina com **`mvn clean install` verde**. Sugestão: branch `feature/book-crud`.

### Fase 0 — Preparação  · S
- [ ] Branch a partir de `master`
- [ ] `mvn clean install` baseline verde

### Fase 1 — Infra de persistência  · M
- [ ] `pom.xml`: h2 (`compile`), junit-jupiter, mockito, eclipselink 2.7.15 (`test`), surefire
- [ ] `persistence.xml` + `books-seed.sql`
- [ ] `EntityManagerProducer` (EMF `@ApplicationScoped`, EM `@RequestScoped` + `@Disposes`)
- [ ] `Tx` + `TransactionalInterceptor` (`@Priority(APPLICATION)`)
- [ ] Build verde

### Fase 2 — Domínio  · S
- [ ] `BookGenre`
- [ ] `Book` (timestamps via callbacks, `@Version`, Lombok `@Getter/@Setter/@NoArgsConstructor`)

### Fase 3 — Repositório  · M
- [ ] `BookRepository`: `save`, `findById → Optional`, `findAll(filtros, paginação)` (Criteria ou JPQL dinâmico), `existsByIsbn`, `deleteById`, `count`
- [ ] `BookRepositoryTest` contra H2

### Fase 4 — DTOs + Mapper + Validação  · M
- [ ] `BookRequest`, `BookPatchRequest`, `BookResponse`, `PageResponse`
- [ ] Bean Validation nos requests (+ validador de ano, se optar por anotação)
- [ ] `BookMapper` + `BookMapperTest`

### Fase 5 — Serviço  · M
- [ ] `BookService` (create/get/list/update/patch/delete; `DuplicateIsbnException`, `BookNotFoundException`; writes com `@Tx`)
- [ ] `BookServiceTest` (Mockito)

### Fase 6 — Camada REST  · M
- [ ] `BookResource` (6 endpoints, `@Valid`, `201`+`Location`, `204`, paginação)
- [ ] `ApiError` + 5 `ExceptionMapper`
- [ ] Registrar tudo em `ApplicationConfig.getClasses()`
- [ ] Anotações OpenAPI (`@Operation`, `@Schema`, `@ApiResponse`)

### Fase 7 — Verificação e docs  · M
- [ ] `mvn clean install` verde (Spotless + testes)
- [ ] Deploy via `autodeploy.ps1`
- [ ] Matriz `curl`: criar → listar → get → put → patch → delete → `404` → `400` → `409`
- [ ] `/api/openapi.json` e `api-docs.html` com `books`
- [ ] Atualizar `CLAUDE.md` (nova arquitetura, camadas, PU, endpoints) e `README.md` (URLs)

### Fase 8 — Extras (opcional)  · —
- [ ] MapStruct no lugar do mapper manual (+ `lombok-mapstruct-binding`, ordem de processadores)
- [ ] `BookResourceIT` real (REST Assured / Arquillian)
- [ ] `ETag`/`If-Match` usando `@Version`; HATEOAS; filtros avançados
- [ ] Perfil alternativo com DataSource JTA no WebLogic (documentar passos WLST)

---

## 15. Critérios de aceite (checklist final)

- [ ] Os 6 endpoints respondem os códigos da tabela da seção 5 no WebLogic.
- [ ] `POST` devolve `Location` e o recurso é recuperável por `GET {id}`.
- [ ] `POST`/`PUT` com ISBN já existente → `409` + `ApiError`.
- [ ] Payload inválido → `400` + `ApiError.details` com os campos.
- [ ] `GET /books` pagina e filtra por `title`/`author`/`genre`.
- [ ] Seed carrega na subida; `drop-and-create` recria a cada redeploy.
- [ ] `mvn test` roda repo + serviço + mapper sem container, verde.
- [ ] `mvn clean install` verde com o gate Spotless ativo.
- [ ] `openapi.json` inclui `books` e schemas dos DTOs.
- [ ] `CLAUDE.md` e `README.md` atualizados.

---

## 16. Riscos e mitigações

| Risco | Mitigação |
|---|---|
| Schema H2 some entre conexões | `DB_CLOSE_DELAY=-1` no URL (obrigatório) |
| EclipseLink do WAR conflita com o do WebLogic | Não empacotar provedor; só `test` scope |
| `ConstraintViolationException` cair no mapper default do Jersey (corpo não-padronizado) | Mapper custom + garantir precedência (registro explícito) |
| Weaving dinâmico do EclipseLink no WAR | `eclipselink.weaving=false` |
| Corrida criando o mesmo ISBN | `existsByIsbn` no serviço **+** `PersistenceExceptionMapper` como rede de segurança (`409`) |
| `@Data` do Lombok na entidade (equals/hashCode com lazy) | Proibido na entidade; só `@Getter/@Setter` |
| Novos arquivos quebrando o build por formatação | `mvn spotless:apply` antes de cada commit (já no script) |
| Driver H2 não visível ao EclipseLink | `com.h2database:h2` em `compile` → `WEB-INF/lib`; auto-registro JDBC 4 |

---

## 17. Fora de escopo

Autenticação/autorização · migrações versionadas (Flyway/Liquibase) · cache de 2º nível ·
banco persistente/externo · múltiplos ambientes · paginação *cursor-based* · CI.
