# Cotei

Comparador de preços construído como arquitetura Cloud Native de microsserviços.
O Cotei mantém um catálogo de produtos, coleta ofertas de lojas parceiras, registra
avaliações de usuários e avisa quando um produto atinge o preço desejado.

Trabalho da avaliação N2 de **Back-End: Cloud Computing** (PUCPR), com implantação
no **Amazon EKS**.

## Arquitetura

```
                        Cliente / Browser
                               |
                        NGINX Ingress Controller  (Amazon EKS)
                               |
     +-----------+-------------+-------------+--------------+
     |           |             |             |              |
 /produto      /loja       /coletor     /avaliacao       /alerta
     |           |             |             |              |
 ms-produto   ms-loja     ms-coletor   ms-avaliacao     ms-alerta
 (3 réplicas)    |             |             |              |
     |           |             |             |              |
   MySQL     PostgreSQL        |          MongoDB       PostgreSQL
                 ^             |                            ^
                 |   publica oferta.atualizada              |
                 |             v                            |
                 +------ RabbitMQ (exchange fanout) ---------+

 REST síncrono:  ms-loja ──► ms-produto      ms-alerta ──► ms-produto
```

## Serviços

| Serviço | Responsabilidade | Responsável |
|---|---|---|
| `ms-produto`   | Catálogo canônico: nome, marca, categoria, EAN | Eduardo |
| `ms-loja`      | Lojas parceiras e suas ofertas (preço, frete, disponibilidade) | Patrick |
| `ms-coletor`   | Coleta ofertas de fontes simuladas e publica `oferta.atualizada` | a definir |
| `ms-avaliacao` | Avaliações de usuários e nota agregada por produto | Bruno |
| `ms-alerta`    | Usuário acompanha um produto com preço-alvo e é avisado na queda | a definir |

Linguagens, bancos, portas e contratos estão em [docs/CONVENCOES.md](docs/CONVENCOES.md).
**Leia antes de começar o seu serviço.**

## Fluxos integrados

**Síncrono (REST).** Ao registrar uma oferta, o `ms-loja` consulta
`GET ms-produto:8080/produto/produtos/{id}` para validar o produto.

**Assíncrono (RabbitMQ).** O `ms-coletor` publica `oferta.atualizada` no exchange
fanout `cotei.ofertas`. O `ms-loja` grava a oferta; o `ms-alerta` confere se o preço
atingiu o alvo de algum usuário. Os dois recebem **todos** os eventos, cada um na sua fila.

## Estrutura do repositório

```
cotei/
├── docs/CONVENCOES.md    convenções e contratos entre serviços
├── k8s/                  infraestrutura comum
│   ├── rabbitmq.yaml
│   └── ingress.yaml
└── ms-<serviço>/         código, Dockerfile e k8s/ de cada serviço
```

## Como subir (Minikube)

```bash
minikube start --driver=docker
minikube addons enable ingress

kubectl apply -f k8s/rabbitmq.yaml
kubectl apply -f ms-produto/k8s/
kubectl apply -f k8s/ingress.yaml

kubectl port-forward -n ingress-nginx service/ingress-nginx-controller 80:80
```

Swagger do `ms-produto`: http://localhost/produto/swagger-ui/index.html

O roteiro do Amazon EKS entra aqui após o Lab 06.

## Requisitos da N2

| Requisito | Onde é atendido | Situação |
|---|---|---|
| Um microsserviço por integrante | tabela de serviços acima | 1 de 5 |
| Duas linguagens | Java + Python + TypeScript | só Java |
| Dois SGBDs | MySQL + PostgreSQL + MongoDB | só MySQL |
| Conteinerização | Dockerfile em cada `ms-*` | `ms-produto` ✓ |
| Database per Service | um banco por serviço, acessado pelo seu Service | `ms-produto` ✓ |
| Comunicação síncrona (REST) | `ms-loja` e `ms-alerta` → `ms-produto` | pendente |
| Comunicação assíncrona (RabbitMQ) | `oferta.atualizada` | broker pronto, fluxo pendente |
| Service Discovery | URLs internas por nome de Service | ✓ |
| Escalabilidade horizontal | `ms-produto` com 3 réplicas + `/produto/instance` | ✓ |
| Ingress | `k8s/ingress.yaml` | `ms-produto` ✓ |
| Amazon EKS | — | pendente (Lab 06) |
