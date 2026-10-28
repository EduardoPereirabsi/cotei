# ms-loja

Lojas parceiras e suas ofertas. Responsável: **Patrick**.

| | |
|---|---|
| Linguagem | Java 21 / Spring Boot 4.1 |
| Banco | PostgreSQL (`postgres-loja`, banco `lojadb`) |
| Prefixo no Ingress | `/loja` |
| Swagger | `/loja/swagger-ui/index.html` |

## Endpoints

| Método | Caminho | Descrição |
|---|---|---|
| GET | `/loja/ofertas?produtoId=` | lista ofertas, opcionalmente de um produto |
| GET | `/loja/ofertas/{id}` | uma oferta |
| GET | `/loja/ofertas/melhor?produtoId=` | menor **preço + frete** entre as disponíveis |
| POST | `/loja/ofertas` | registra ou atualiza a oferta de uma loja |
| DELETE | `/loja/ofertas/{id}` | remove uma oferta |

## Integrações

**REST (síncrono).** Antes de gravar qualquer oferta, consulta
`GET http://ms-produto:8080/produto/produtos/{id}`:

| Resposta do ms-produto | Resultado no ms-loja |
|---|---|
| 200 | grava a oferta |
| 404 | recusa com **422** (produto inexistente) |
| timeout ou erro | recusa com **503** (não foi possível validar) |

**RabbitMQ (assíncrono).** Consome `oferta.atualizada` pela fila própria
`loja.oferta-atualizada`, ligada ao exchange fanout `cotei.ofertas`. Cada evento
passa pela mesma validação REST antes de ser gravado — um único fluxo exercita as
duas formas de comunicação.

## Decisões de projeto

- **Uma oferta por (produto, loja).** Uma nova coleta da mesma loja atualiza a oferta
  existente em vez de duplicá-la.
- **Dinheiro em `BigDecimal`.** `double` acumula erro de arredondamento.
- **Melhor oferta considera o frete.** O menor preço de vitrine nem sempre é o menor
  valor pago.
- **Timeout curto no REST** (2 s conexão, 3 s leitura): se o `ms-produto` travar, o
  `ms-loja` falha rápido em vez de prender a requisição.
- **Mensagem com problema é descartada com log**, não reenfileirada
  (`default-requeue-rejected=false`). O padrão do Spring AMQP reentregaria uma
  mensagem corrompida para sempre, travando a fila.

## Limitação conhecida

Se o `ms-produto` estiver fora do ar, eventos recebidos nesse intervalo são
descartados. Em produção, eles iriam para uma *dead letter queue* para
reprocessamento.

## Build e implantação

O teste de contexto gerado pelo Initializr exige PostgreSQL e RabbitMQ de pé, por
isso o build da imagem pula os testes:

```bash
./mvnw clean package -DskipTests
minikube image build -t ms-loja:0.1.0 .
kubectl apply -f k8s/
```
