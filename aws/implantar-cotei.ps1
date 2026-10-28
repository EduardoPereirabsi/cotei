# Implanta o Cotei inteiro no cluster EKS (contexto atual do kubectl).
#
#   .\aws\implantar-cotei.ps1
#
# Ordem: Ingress Controller -> RabbitMQ e bancos -> microsservicos -> Ingress.
# Subir os bancos antes evita o ciclo de reinicios dos servicos que dependem deles.
param([string]$Kubectl = "")

$ErrorActionPreference = "Stop"
$raiz = Split-Path $PSScriptRoot -Parent

# kubectl 1.37 (mesma versao do cluster), se disponivel; senao o do PATH.
if (-not $Kubectl) {
    $k137 = Join-Path $env:USERPROFILE ".minikube\cache\windows\amd64\v1.37.0\kubectl.exe"
    $Kubectl = if (Test-Path $k137) { $k137 } else { "kubectl" }
}

function K { & $Kubectl @args; if ($LASTEXITCODE -ne 0) { throw "kubectl $($args -join ' ') falhou" } }

$contexto = & $Kubectl config current-context
Write-Host "Contexto do kubectl: $contexto"
if ($contexto -notmatch "cotei") {
    throw "O contexto atual nao e o cluster cotei. Rode: aws eks update-kubeconfig --region us-east-1 --name cotei"
}

Write-Host "`n1/5 NGINX Ingress Controller (cria o Network Load Balancer da AWS)..."
K apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.15.1/deploy/static/provider/aws/deploy.yaml
K wait --namespace ingress-nginx --for=condition=available deployment/ingress-nginx-controller --timeout=300s

Write-Host "`n2/5 RabbitMQ e bancos de dados..."
K apply -f "$raiz\k8s\rabbitmq.yaml"
K apply -f "$raiz\ms-produto\k8s\mysql-produto.yaml"
K apply -f "$raiz\ms-loja\k8s\postgres-loja.yaml"
K apply -f "$raiz\ms-avaliacao\k8s\mongo-avaliacao.yaml"
K apply -f "$raiz\ms-alerta\k8s\postgres-alerta.yaml"
foreach ($d in "rabbitmq", "mysql-produto", "postgres-loja", "mongo-avaliacao", "postgres-alerta") {
    K rollout status deployment/$d --timeout=600s
}

Write-Host "`n3/5 Microsservicos..."
foreach ($s in "ms-produto", "ms-loja", "ms-avaliacao", "ms-coletor", "ms-alerta") {
    K apply -f "$raiz\$s\k8s\$s.yaml"
}
foreach ($s in "ms-produto", "ms-loja", "ms-avaliacao", "ms-coletor", "ms-alerta") {
    K rollout status deployment/$s --timeout=600s
}

Write-Host "`n4/5 Regras de Ingress..."
K apply -f "$raiz\k8s\ingress.yaml"

Write-Host "`n5/5 Aguardando o endereco publico do Load Balancer..."
$host_ = ""
for ($i = 0; $i -lt 60 -and -not $host_; $i++) {
    $host_ = & $Kubectl get service ingress-nginx-controller -n ingress-nginx -o jsonpath="{.status.loadBalancer.ingress[0].hostname}"
    if (-not $host_) { Start-Sleep -Seconds 5 }
}
if (-not $host_) { throw "O Load Balancer ainda nao recebeu endereco. Confira: kubectl get svc -n ingress-nginx" }

Write-Host "`nCotei implantado. O DNS do Load Balancer pode levar de 1 a 3 minutos para responder."
Write-Host "  http://$host_/produto/swagger-ui/index.html"
Write-Host "  http://$host_/loja/swagger-ui/index.html"
Write-Host "  http://$host_/avaliacao/docs"
Write-Host "  http://$host_/coletor/docs"
Write-Host "  http://$host_/alerta/swagger-ui/index.html"
Write-Host "  http://$host_/produto/instance   (balanceamento entre as replicas)"
