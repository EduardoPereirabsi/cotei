# ms-coletor

Coleta ofertas das lojas parceiras e publica o evento `oferta.atualizada`.
Responsável: **Roberto**.

| | |
|---|---|
| Linguagem | Python 3.12 / FastAPI |
| Banco | nenhum — o serviço não guarda estado: coleta e publica |
| Mensageria | publica no exchange fanout `cotei.ofertas` |
| Prefixo no Ingress | `/coletor` |
| Swagger | `/coletor/docs` |

## Endpoints

| Método | Caminho | Descrição |
|---|---|---|
| GET | `/coletor/fontes` | lojas parceiras configuradas |
| POST | `/coletor/coletas` | coleta nas lojas e publica um evento por oferta |
| POST | `/coletor/ofertas` | publica uma oferta informada manualmente |
| GET | `/coletor/health` | readiness: 200 só com o RabbitMQ conectado |
| GET | `/coletor/instance` | nome do pod que respondeu |

`POST /coletor/coletas` sem corpo coleta **todos os produtos ativos do catálogo**.
Com `{"produtoIds": [1, 2]}`, só esses — os inexistentes voltam em `produtosIgnorados`.

## O fluxo que este serviço inicia

```
POST /coletor/coletas
  └─ ms-coletor ─REST─► ms-produto          quais produtos existem?
       └─ uma oferta por loja ─► RabbitMQ (exchange fanout cotei.ofertas)
            ├─► ms-loja    (fila loja.oferta-atualizada)   grava/atualiza a oferta
            └─► ms-alerta  (fila alerta.oferta-atualizada) confere preços-alvo
```

A resposta é **202 Accepted**: o coletor confirma que publicou, e o processamento
segue de forma assíncrona nos consumidores.

## Decisões de projeto

- **Adaptadores de fontes.** Cada loja implementa a interface `FonteDeOfertas`
  (`app/fontes.py`). As três atuais **simulam** a vitrine de uma loja — sem scraping
  real, decisão da equipe. Trocar uma simulação por uma fonte real é escrever um
  adaptador novo, sem tocar no resto do serviço.
- **Cada loja tem um comportamento.** Alfa: preço médio, frete grátis. Beta: vitrine
  mais barata, frete fixo de R$ 149,90. Gama: frete grátis acima de R$ 3.000 e às vezes
  sem estoque. As diferenças fazem a "melhor oferta" variar de verdade.
- **Preço simulado coerente com a categoria.** O catálogo não guarda preço, então cada
  produto ganha uma referência estável dentro da faixa da categoria (um smartphone fica
  entre R$ 2.500 e R$ 7.000), com variação a cada coleta.
- **Publisher confirms + mensagens persistentes.** Cada publicação só é considerada feita
  quando o broker confirma o recebimento; a mensagem sobrevive a um reinício do RabbitMQ.
- **Exchange fanout**, não uma fila única: `ms-loja` e `ms-alerta` recebem **todos** os
  eventos, cada um na sua fila. Ver `docs/CONVENCOES.md`.
- **A validação mora em quem grava.** A oferta manual publica sem consultar o catálogo; é
  o consumidor que valida antes de persistir.
- **Sobe sem depender do broker.** O HTTP inicia na hora e a conexão com o RabbitMQ é feita
  em segundo plano, com retentativas; a readiness probe só libera tráfego depois dela.

## Build e implantação

```bash
docker build -t eduardo1112/ms-coletor:0.1.1 .
docker push eduardo1112/ms-coletor:0.1.1
kubectl apply -f k8s/
```
