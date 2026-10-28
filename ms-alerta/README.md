# ms-alerta

Alertas de preço: o usuário acompanha um produto e é avisado quando ele atinge o valor
desejado. Responsável: **mathfraiz**.

| | |
|---|---|
| Linguagem | Java 21 / Spring Boot 4.1 |
| Banco | PostgreSQL (`postgres-alerta`, banco `alertadb`) |
| Mensageria | consome `oferta.atualizada` pela fila própria `alerta.oferta-atualizada` |
| Prefixo no Ingress | `/alerta` |
| Swagger | `/alerta/swagger-ui/index.html` |

## Endpoints

| Método | Caminho | Descrição |
|---|---|---|
| POST | `/alerta/alertas` | cria um alerta (`produtoId`, `email`, `precoAlvo`) |
| GET | `/alerta/alertas?email=&produtoId=` | lista alertas |
| GET | `/alerta/alertas/{id}` | um alerta |
| DELETE | `/alerta/alertas/{id}` | remove um alerta |
| GET | `/alerta/notificacoes?email=` | avisos já disparados — a "caixa de entrada" |

## Regras de negócio

- **Disparo único.** Ao atingir o alvo, o alerta é desativado. Sem isso, o usuário
  receberia um aviso a cada coleta enquanto o preço continuasse baixo.
- **Compara o valor total** (preço + frete), a mesma regra da melhor oferta do `ms-loja`.
- **Oferta sem estoque não dispara**, mesmo abaixo do alvo: avisar de algo que não dá
  para comprar seria inútil.
- **O aviso não se perde se o `ms-produto` cair.** O nome do produto vem do catálogo por
  REST; se a consulta falhar no momento do disparo, o alerta dispara com um nome genérico
  ("Produto 1"). Perder a notificação seria pior.
- **O envio é simulado.** O aviso aparece no log (`[ALERTA] DISPARADO -> email: ...`) e
  fica gravado em `notificacoes`.

## Integrações

**RabbitMQ.** A fila `alerta.oferta-atualizada` é ligada ao exchange fanout
`cotei.ofertas`. O mesmo evento que o `ms-loja` usa para gravar a oferta chega aqui, na
fila deste serviço — os dois consumidores recebem **todos** os eventos, sem disputar.

**REST.** Consulta `GET http://ms-produto:8080/produto/produtos/{id}` para validar o
produto ao criar o alerta (404 vira **422**) e para montar a mensagem do aviso.

## Exemplo

Com dois alertas para o produto 1 — Maria com alvo **R$ 2.500** e João com **R$ 3.000** —,
esta sequência de ofertas publicadas pelo `ms-coletor` (`POST /coletor/ofertas`) produz:

| Oferta publicada | Resultado | Regra |
|---|---|---|
| Alfa R$ 3.199,90 | nada | acima dos dois alvos |
| Gama R$ 1.999, `"disponivel": false` | nada | sem estoque não dispara |
| Beta R$ 2.749 + frete R$ 149,90 | dispara **João** | compara o total, com frete |
| Alfa R$ 2.399,90 | dispara **Maria** | alvo atingido |
| Gama R$ 2.199 | nada | disparo único |

Depois, `GET /alerta/notificacoes` traz os dois avisos. O `ms-loja` recebe os mesmos
cinco eventos pela própria fila.

## Build e implantação

O teste de contexto gerado pelo Initializr exige PostgreSQL e RabbitMQ de pé, por isso o
build pula os testes. Aplique o banco antes do serviço:

```bash
./mvnw clean package -DskipTests
docker build -t eduardo1112/ms-alerta:0.1.1 .
docker push eduardo1112/ms-alerta:0.1.1
kubectl apply -f k8s/postgres-alerta.yaml
kubectl apply -f k8s/ms-alerta.yaml
```
