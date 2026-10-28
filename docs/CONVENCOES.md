# Convenções do Cotei

Regras que **todos os serviços** seguem. Elas existem para que a integração funcione
sem retrabalho: se cada um inventar o próprio padrão, a semana de integração vira
semana de correção.

## 1. Serviços

| Serviço | Responsável | Linguagem (sugestão) | Banco | Prefixo no Ingress | Service Kubernetes |
|---|---|---|---|---|---|
| `ms-produto`   | Eduardo     | Java (Spring Boot) | MySQL      | `/produto`   | `ms-produto:8080` |
| `ms-loja`      | Patrick     | Java (Spring Boot) | PostgreSQL | `/loja`      | `ms-loja:8080` |
| `ms-coletor`   | Roberto     | Python (FastAPI)   | —          | `/coletor`   | `ms-coletor:8080` |
| `ms-avaliacao` | Bruno       | TypeScript (Express) | MongoDB  | `/avaliacao` | `ms-avaliacao:8080` |
| `ms-alerta`    | mathfraiz   | Java ou Python     | PostgreSQL | `/alerta`    | `ms-alerta:8080` |

A N2 exige **ao menos duas linguagens e dois SGBDs**. Esta distribuição cumpre com folga;
se mudarem, confiram que a regra continua atendida.

## 2. Regras de implantação

**Porta 8080 dentro do contêiner, em todos os serviços.** Dentro do cluster não há
conflito: os Services se diferenciam pelo nome, não pela porta. Para rodar localmente
fora do Kubernetes, cada um escolhe a porta que quiser.

**Cada serviço responde sob o próprio prefixo.** O Ingress é único e não reescreve o
caminho, então `/produto/...` chega ao `ms-produto` exatamente assim. Sem isso, os cinco
Swaggers disputariam `/swagger-ui`.

| Linguagem | Como configurar o prefixo |
|---|---|
| Spring Boot | variável `SERVER_SERVLET_CONTEXT_PATH=/loja` no manifesto |
| FastAPI | `APIRouter(prefix="/coletor")` e `FastAPI(docs_url="/coletor/docs", openapi_url="/coletor/openapi.json")` |
| Express | `app.use("/avaliacao", router)` e a documentação em `/avaliacao/docs` |

**Banco próprio, sempre.** Deployment e Service com o nome `<sgbd>-<serviço>`
(ex.: `postgres-loja`). Nenhum serviço acessa o banco de outro — se precisa de um dado
alheio, pede por REST ou recebe por evento.

**Readiness probe obrigatória.** Sem ela, o Kubernetes manda tráfego para o pod assim
que o contêiner inicia, com a aplicação ainda subindo. Vimos isso derrubar requisições
na N1 e no Lab 07. Use um GET barato do próprio serviço.

**Imagem com versão, nunca `latest`.** As imagens são publicadas e públicas no Docker Hub,
em `eduardo1112/<serviço>:<versão>`, e o manifesto referencia a tag exata. Publicar uma
mudança é gerar uma versão nova e atualizar a tag no manifesto.

**Service Discovery: só nomes.** Chamadas internas usam o nome do Service —
`http://ms-produto:8080/produto/produtos/1` — nunca IP de pod.

## 3. Contratos entre serviços

### REST — consulta de produto

O `ms-produto` é a fonte da verdade do catálogo.

```
GET http://ms-produto:8080/produto/produtos/{id}
200 → {"id":1,"nome":"iPhone 15 128GB","marca":"Apple","categoria":"SMARTPHONE","ean":"7891234567890","descricao":"...","ativo":true}
404 → produto inexistente
```

Consumidores: `ms-loja` (valida o produto antes de registrar uma oferta), `ms-avaliacao`
(valida antes de registrar uma avaliação) e `ms-alerta` (busca o nome do produto para montar o aviso).

### RabbitMQ — evento `oferta.atualizada`

| Item | Valor |
|---|---|
| Exchange | `cotei.ofertas`, tipo **fanout**, durável |
| Publicador | `ms-coletor` |
| Fila do `ms-loja` | `loja.oferta-atualizada` (durável, ligada ao exchange) |
| Fila do `ms-alerta` | `alerta.oferta-atualizada` (durável, ligada ao exchange) |

```json
{
  "produtoId": 1,
  "loja": "Loja Exemplo",
  "preco": 4299.90,
  "frete": 0.0,
  "disponivel": true,
  "url": "https://loja-exemplo.local/iphone-15",
  "coletadoEm": "2026-10-27T10:00:00Z"
}
```

**Por que fanout, e não uma fila só.** No Lab 07, três réplicas do mesmo consumidor
*disputavam* uma fila: cada mensagem ia para **um** consumidor. Aqui é o contrário —
`ms-loja` e `ms-alerta` precisam receber **todos** os eventos. Com uma fila
compartilhada, cada serviço veria só metade deles. Por isso: um exchange fanout e
**uma fila por serviço consumidor**. Réplicas do mesmo serviço continuam disputando a
fila *daquele* serviço, o que está certo.

## 4. Armadilhas que já custaram tempo

- **Maven wrapper ausente.** Os projetos do professor vêm sem `.mvn/` e `mvnw`. Copie
  do `ms-produto`.
- **Versão do Java.** A imagem base do Dockerfile precisa ser compatível com a versão
  do `pom.xml`: um JAR de Java 21 não roda num `eclipse-temurin:17-jre`.
- **springdoc e Spring Boot.** Boot 3.x usa springdoc **2.x**; Boot 4.x usa springdoc **3.x**.
- **Swagger com prefixo.** Não use `@OpenAPIDefinition(servers = @Server(url = "/"))`
  junto com context path: o Try it out passaria a chamar a URL sem o prefixo. O padrão
  do springdoc já inclui o prefixo na URL do servidor.
- **Porta do navegador e Try it out.** O Swagger monta a URL a partir da requisição. Se
  o navegador entra numa porta e o Ingress escuta em outra, o Execute falha com
  "Failed to fetch". No EKS o LoadBalancer já responde na 80; localmente use
  `kubectl port-forward -n ingress-nginx service/ingress-nginx-controller 80:80`.
- **Painel do RabbitMQ atrasa.** As métricas das filas atualizam a cada ~5 s. Espere
  antes de concluir que uma mensagem sumiu.
- **Consumidor "pronto" não é consumidor conectado.** O rollout termina quando o
  contêiner sobe, não quando o listener se registra no broker. Confira os consumidores
  no painel antes de testar distribuição.

## 5. Git

Cada integrante **commita do próprio GitHub**, dentro da pasta do seu serviço.
Mudanças em `k8s/` e `docs/` (partes comuns) devem ser combinadas com o grupo antes.
