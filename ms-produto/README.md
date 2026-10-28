# ms-produto

Catálogo canônico de produtos do Cotei. Responsável: **Eduardo**.

| | |
|---|---|
| Linguagem | Java 21 / Spring Boot 4.1 |
| Banco | MySQL (`mysql-produto`, banco `produtodb`) |
| Prefixo no Ingress | `/produto` |
| Swagger | `/produto/swagger-ui/index.html` |
| Réplicas | **3** |

## Papel no sistema

O `ms-produto` é a **fonte da verdade do catálogo**: o "produto de verdade", independente
de loja. "iPhone 15 128GB" é um produto; "R$ 4.299 na Loja X" é uma oferta — e ofertas
são responsabilidade do `ms-loja`.

Por isso ele é o serviço mais consultado da arquitetura. Os outros quatro dependem dele
por REST, sempre pelo nome do Service (`http://ms-produto:8080/produto`), nunca por IP:

| Quem consulta | Para quê |
|---|---|
| `ms-loja` | validar o produto antes de gravar uma oferta |
| `ms-avaliacao` | validar o produto antes de gravar uma avaliação |
| `ms-alerta` | validar o alerta e obter o nome do produto para o aviso |
| `ms-coletor` | listar o catálogo para saber o que coletar |

## Endpoints

| Método | Caminho | Descrição |
|---|---|---|
| GET | `/produto/produtos` | lista o catálogo |
| GET | `/produto/produtos/{id}` | um produto — **200** ou **404** |
| POST | `/produto/produtos` | cadastra um produto |
| PUT | `/produto/produtos/{id}` | atualiza um produto |
| DELETE | `/produto/produtos/{id}` | remove um produto |
| GET | `/produto/instance` | nome do pod que respondeu |

Campos: `id`, `nome`, `marca`, `categoria`, `ean`, `descricao`, `ativo`.

## Contrato REST com os demais serviços

O `GET /produto/produtos/{id}` é um contrato, e a semântica dos códigos importa:

- **200** → o produto existe
- **404** → o produto **não existe**; os consumidores transformam isso em 422
- **qualquer outra falha** → o `ms-produto` está indisponível; os consumidores respondem
  503, porque *não existe* e *não consegui perguntar* são situações diferentes

## Escalabilidade horizontal

É o serviço que cumpre o requisito de **múltiplas réplicas com balanceamento**. Faz sentido
que seja ele: é o mais requisitado, porque todos os outros dependem dele.

O endpoint `/produto/instance` devolve o nome do pod que atendeu. Chamadas repetidas pelo
Ingress mostram a distribuição — no teste de validação, 12 chamadas resultaram em
**4 / 4 / 4** entre as três réplicas.

```bash
for i in 1 2 3 4 5 6; do curl -s http://localhost/produto/instance; echo; done
```

## Decisões de projeto

- **Configuração por variáveis de ambiente**, com valores padrão para rodar localmente
  (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`). A mesma imagem roda na
  máquina, no Docker e no Kubernetes — muda só o ambiente.
- **Prefixo pelo ambiente, não pelo código.** O `/produto` vem de
  `SERVER_SERVLET_CONTEXT_PATH` no manifesto. Localmente o serviço responde na raiz.
- **readinessProbe em `/produto/produtos`.** Sem ela, o Kubernetes manda tráfego para o pod
  assim que o contêiner inicia, com a JVM ainda subindo. Isso foi observado na prática no
  Lab 05: 12 de 30 requisições voltaram vazias enquanto as réplicas novas aqueciam.
- **Swagger sem servidor fixo no código.** O springdoc gera a URL do servidor já com o
  prefixo (`http://<host>/produto`), então o "Try it out" funciona pelo Ingress. Fixar
  `@Server(url = "/")` quebraria isso, porque o prefixo se perderia.

## Histórico

O serviço foi construído ao longo dos laboratórios da disciplina, no repositório
[`Back-EndCloudComputing`](https://github.com/EduardoPereirabsi/Back-EndCloudComputing),
e trazido para o Cotei na estrutura inicial:

| Etapa | O que entrou |
|---|---|
| Lab 01 | API REST com CRUD em memória |
| Lab 03 | JPA + MySQL, Swagger, Dockerfile e imagem no Docker Hub |
| Lab 05 | Kubernetes: Deployment, Service, réplicas, Ingress e `/instance` |
| N1 | readinessProbe e convivência com outro microsserviço no mesmo Ingress |

## Limitações conhecidas

- **MySQL sem volume persistente.** Os dados vivem no pod: se o banco for recriado, o
  catálogo se perde. Em produção seria um `PersistentVolumeClaim` ou um banco gerenciado.
- **Credenciais em texto no manifesto**, apenas por ser um ambiente didático. Em produção
  iriam para um `Secret`.

## Build e implantação

O teste de contexto precisa de um MySQL acessível. Para gerar a imagem sem ele:

```bash
./mvnw clean package -DskipTests
docker build -t eduardo1112/ms-produto:1.1.0 .
kubectl apply -f k8s/
```

A imagem `eduardo1112/ms-produto:1.1.0` está publicada no Docker Hub, pronta para ser
puxada pelo EKS.
