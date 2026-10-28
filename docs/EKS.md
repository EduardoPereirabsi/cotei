# Implantação no Amazon EKS

Roteiro para subir o Cotei no EKS do AWS Academy, validar as rubricas e apagar tudo.
São quatro scripts em `aws/`; os tempos foram medidos no ensaio de 27/10/2026:

| Script | O que faz | Tempo |
|---|---|---|
| `criar-cluster.ps1` | descobre as roles do Academy e cria o cluster com o `eksctl` | ~16 min |
| `implantar-cotei.ps1` | Ingress Controller, RabbitMQ, bancos, serviços e Ingress, nessa ordem | ~5 min |
| `validar-cotei.ps1` | imprime a evidência de cada rubrica, passando pelo Load Balancer | ~1 min |
| `apagar-cluster.ps1` | remove o Load Balancer e depois o cluster | ~15 min |

## Custo

O cluster consome o orçamento do Learner Lab (US$ 50 por aluno) **enquanto existir** —
algo na faixa de US$ 0,25 a 0,30 por hora, entre plano de controle, dois nós e o
balanceador. Duas regras:

- **Apague sempre ao terminar.** Encerrar a sessão do lab **não** apaga o cluster.
- **Cada integrante tem o próprio Learner Lab.** O `criar-cluster.ps1` descobre as roles
  na conta logada, então o cluster sobe na conta de qualquer um sem alterar nada.

## Pré-requisitos (uma vez por máquina)

```bash
winget install --id Amazon.AWSCLI -e
winget install --id eksctl.eksctl -e
```

Abra um PowerShell novo depois de instalar.

## A cada sessão do Learner Lab

1. AWS Academy → Learner Lab → **Start Lab** → espere a bolinha do **AWS** ficar verde.
2. **AWS Details** → **AWS CLI: Show** → copie o bloco `[default]` inteiro.
3. Cole em `~/.aws/credentials` (`notepad $env:USERPROFILE\.aws\credentials`).
   O Bloco de Notas pode salvar como `credentials.txt` — o nome precisa ser exatamente
   `credentials`.
4. Confira: `aws sts get-caller-identity` deve mostrar a role `voclabs`.

As credenciais expiram com a sessão (4 horas). Nunca as coloque no Git.

## Subir

Na raiz do repositório:

```bash
.\aws\criar-cluster.ps1 -DryRun
.\aws\criar-cluster.ps1
.\aws\implantar-cotei.ps1
```

O `-DryRun` só valida a configuração, sem custo. O último script imprime os endereços
públicos — o DNS do Load Balancer pode levar de 1 a 3 minutos para responder.

## Validar (as evidências de cada rubrica)

```bash
.\aws\validar-cotei.ps1
```

O script cadastra um produto novo e repete, pelo endereço público, a demonstração inteira:
bancos, distribuição dos pods, balanceamento, Service Discovery, REST e o fanout do
RabbitMQ. No ensaio, tudo passou de primeira: 15 chamadas a `/produto/instance` se
dividiram em 5/5/5 entre as três réplicas, e os avisos de João e Maria dispararam como
na tabela do [ms-alerta/README.md](../ms-alerta/README.md).

Os comandos abaixo são as mesmas evidências, uma a uma, para mostrar ao vivo.

**R1 — serviços, linguagens e bancos.**

```bash
kubectl get deployments
kubectl exec deploy/mysql-produto -- mysql -uroot -proot -e "SHOW DATABASES;"
kubectl exec deploy/postgres-loja -- psql -U postgres -l
kubectl exec deploy/mongo-avaliacao -- mongosh --quiet --eval "db.adminCommand({listDatabases:1}).databases.map(d=>d.name)"
```

**R2 — EKS, Service Discovery, Ingress e balanceamento.**

```bash
kubectl get nodes -o wide
kubectl get pods -o wide
```

A coluna `NODE` mostra os pods distribuídos entre as duas instâncias EC2.

```bash
kubectl run teste --image=curlimages/curl --restart=Never -- sleep 3600
kubectl exec teste -- curl -s http://ms-produto:8080/produto/produtos
```

No navegador, `http://<load-balancer>/produto/instance` com recargas sucessivas mostra
réplicas diferentes respondendo.

**R3 — REST e RabbitMQ num fluxo funcional.** Siga a demonstração do
[ms-alerta/README.md](../ms-alerta/README.md): cadastre alertas, publique ofertas pelo
`POST /coletor/ofertas` e mostre os logs:

```bash
kubectl logs deployment/ms-loja --tail=20
kubectl logs deployment/ms-alerta --tail=20
```

## Apagar

```bash
.\aws\apagar-cluster.ps1
```

Ele pede para digitar `APAGAR`, remove o Load Balancer antes do cluster e, no fim,
confere se sobrou algum cluster ou balanceador cobrando.

## No dia da apresentação

| Quando | O quê |
|---|---|
| ~1 h antes | iniciar o Learner Lab, colar as credenciais, `criar-cluster.ps1` |
| ~40 min antes | `implantar-cotei.ps1` e `validar-cotei.ps1` |
| durante | apresentação |
| logo depois | `apagar-cluster.ps1` |

## Problemas comuns

| Sintoma | Causa provável |
|---|---|
| `Unable to locate credentials` ou `ExpiredToken` | sessão do lab encerrada; cole o bloco novo |
| `O contexto atual nao e o cluster cotei` | rode `aws eks update-kubeconfig --region us-east-1 --name cotei` |
| endereço do Load Balancer não abre | DNS ainda propagando; aguarde 1 a 3 minutos. Se você tentou antes da hora, o Windows guarda a falha em cache: `ipconfig /flushdns` |
| pod em `Pending` | falta de capacidade nos nós: `kubectl describe pod <nome>`. No ensaio, os 21 pods ocuparam 71% e 48% da memória dos dois nós: dá para subir réplicas extras do `ms-produto` ao vivo, mas não muitas |

## Diferenças em relação ao Minikube

- O **RabbitMQ usa armazenamento efêmero** (`emptyDir`), como os bancos: um volume
  persistente no EKS exigiria o driver EBS CSI, que o Academy não permite configurar.
- **Não há port-forward**: o Ingress Controller ganha um Network Load Balancer público
  da AWS, na porta 80. Por isso o "Try it out" dos Swaggers funciona direto pelo endereço
  do balanceador.
