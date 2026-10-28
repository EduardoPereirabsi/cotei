# ms-avaliacao

Avaliações de produtos e nota agregada. Responsável: **Bruno**.

| | |
|---|---|
| Linguagem | TypeScript / Node.js 24 / Express 5 |
| Banco | MongoDB (`mongo-avaliacao`, banco `avaliacaodb`) |
| Prefixo no Ingress | `/avaliacao` |
| Swagger | `/avaliacao/docs` |

## Endpoints

| Método | Caminho | Descrição |
|---|---|---|
| GET | `/avaliacao/avaliacoes?produtoId=` | lista avaliações, opcionalmente de um produto |
| GET | `/avaliacao/avaliacoes/{id}` | uma avaliação |
| POST | `/avaliacao/avaliacoes` | registra uma avaliação |
| DELETE | `/avaliacao/avaliacoes/{id}` | remove uma avaliação |
| GET | `/avaliacao/produtos/{produtoId}/resumo` | nota média e total de avaliações |
| GET | `/avaliacao/health` | readiness: 200 só com o MongoDB conectado |
| GET | `/avaliacao/instance` | nome do pod que respondeu |

## Integração REST

Antes de gravar, consulta `GET http://ms-produto:8080/produto/produtos/{id}`:

| Resposta do ms-produto | Resultado no ms-avaliacao |
|---|---|
| 200 | grava a avaliação (201) |
| 404 | recusa com **422** (produto inexistente) |
| timeout ou erro | recusa com **503** (não foi possível validar) |

## Decisões de projeto

- **MongoDB para avaliações.** Uma avaliação é um documento autocontido (autor, nota,
  comentário livre), sem relacionamentos — o encaixe natural de um banco de documentos.
- **Resumo calculado pelo banco.** A nota média sai de uma *aggregation pipeline*
  (`$match` → `$group` com `$avg`): o MongoDB processa e devolve só o resultado,
  em vez de a aplicação carregar todas as avaliações para fazer a conta.
- **Validação devolve todos os erros de uma vez.** Um POST inválido recebe a lista
  completa, não só o primeiro problema.
- **Sobe sem depender do banco.** O servidor HTTP inicia imediatamente e conecta ao
  MongoDB com retentativas a cada 3 s. A readiness probe (`/avaliacao/health`) só
  libera tráfego depois da conexão — o pod não entra em ciclo de reinícios enquanto
  o banco ainda sobe.
- **Swagger com servidor relativo** (`/avaliacao`). O "Try it out" chama a API no
  mesmo endereço em que o Swagger foi aberto, no Minikube ou no EKS.
- **Imagem multi-stage.** O estágio de build compila o TypeScript; a imagem final leva
  só o JavaScript compilado e as dependências de produção, e roda como usuário sem
  privilégios (`node`).

## Build e implantação

```bash
npm install
npm run build
docker build -t eduardo1112/ms-avaliacao:0.1.0 .
docker push eduardo1112/ms-avaliacao:0.1.0
kubectl apply -f k8s/
```
