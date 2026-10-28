# Apaga o cluster EKS do Cotei e tudo o que ele criou na AWS.
#
#   .\aws\apagar-cluster.ps1
#
# Remove o Load Balancer ANTES do cluster: se o cluster sumir primeiro, o
# balanceador criado pelo Ingress Controller pode ficar orfao na conta,
# consumindo o orcamento do Learner Lab sem ninguem perceber.
param([switch]$SemConfirmar, [string]$Kubectl = "")

$ErrorActionPreference = "Stop"

if (-not $Kubectl) {
    $k137 = Join-Path $env:USERPROFILE ".minikube\cache\windows\amd64\v1.37.0\kubectl.exe"
    $Kubectl = if (Test-Path $k137) { $k137 } else { "kubectl" }
}

if (-not $SemConfirmar) {
    $resposta = Read-Host "Isto apaga o cluster 'cotei' e todos os dados dele. Digite APAGAR para confirmar"
    if ($resposta -ne "APAGAR") { Write-Host "Cancelado."; exit 0 }
}

Write-Host "1/3 Removendo o Ingress Controller e o Load Balancer..."
$contexto = & $Kubectl config current-context 2>$null
if ($contexto -match "cotei") {
    & $Kubectl delete -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.15.1/deploy/static/provider/aws/deploy.yaml --ignore-not-found --wait=true
    Write-Host "    aguardando a AWS liberar o Load Balancer..."
    Start-Sleep -Seconds 60
} else {
    Write-Host "    contexto atual nao e o cotei; pulando (o eksctl ainda tenta limpar)."
}

Write-Host "2/3 Apagando o cluster (cerca de 10 a 15 minutos)..."
eksctl delete cluster --name cotei --region us-east-1 --wait
if ($LASTEXITCODE -ne 0) { throw "O eksctl terminou com erro. Confira o cluster no Console da AWS." }

Write-Host "3/3 Conferindo se nada ficou para tras..."
$clusters = aws eks list-clusters --region us-east-1 --query "clusters" --output text
$lbs = aws elbv2 describe-load-balancers --region us-east-1 --query "LoadBalancers[].LoadBalancerName" --output text
Write-Host ("    clusters EKS restantes: " + $(if ($clusters) { $clusters } else { "nenhum" }))
Write-Host ("    load balancers restantes: " + $(if ($lbs) { $lbs } else { "nenhum" }))
if ($clusters -or $lbs) { Write-Warning "Ainda ha recursos cobrando. Remova-os pelo Console da AWS." }
else { Write-Host "Tudo removido. O orcamento parou de ser consumido." }
