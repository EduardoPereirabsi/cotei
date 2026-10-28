# Cria o cluster EKS do Cotei no AWS Academy Learner Lab.
#
#   .\aws\criar-cluster.ps1 -DryRun   so valida a configuracao (sem custo)
#   .\aws\criar-cluster.ps1           cria de verdade (~20 min, consome o orcamento)
#
# Pre-requisitos: Learner Lab iniciado e o bloco "AWS CLI" do AWS Details
# colado em ~/.aws/credentials.
param([switch]$DryRun)

$ErrorActionPreference = "Stop"
$template = Join-Path $PSScriptRoot "eks-cluster.template.yaml"
$gerado   = Join-Path $PSScriptRoot "eks-cluster.generated.yaml"

Write-Host "1/3 Conferindo as credenciais do Learner Lab..."
$identidade = aws sts get-caller-identity --output json 2>$null | ConvertFrom-Json
if ($LASTEXITCODE -ne 0 -or -not $identidade.Arn) {
    throw "Credenciais invalidas ou expiradas. Inicie o Learner Lab e cole o bloco novo do AWS Details em ~/.aws/credentials."
}
Write-Host ("    conta {0} ({1})" -f $identidade.Account, $identidade.Arn.Split("/")[1])

Write-Host "2/3 Localizando as roles do AWS Academy..."
$roleCluster = aws iam list-roles --query "Roles[?contains(RoleName, 'LabEksClusterRole')].Arn | [0]" --output text
$roleNo      = aws iam list-roles --query "Roles[?contains(RoleName, 'LabEksNodeRole')].Arn | [0]" --output text
if (-not $roleCluster -or $roleCluster -eq "None" -or -not $roleNo -or $roleNo -eq "None") {
    throw "As roles LabEksClusterRole e LabEksNodeRole nao existem nesta conta."
}
Write-Host "    cluster: $roleCluster"
Write-Host "    nos:     $roleNo"

$config = (Get-Content $template -Raw).Replace("__CLUSTER_ROLE_ARN__", $roleCluster).Replace("__NODE_ROLE_ARN__", $roleNo)
[System.IO.File]::WriteAllText($gerado, $config, (New-Object System.Text.UTF8Encoding($false)))

if ($DryRun) {
    Write-Host "3/3 Validando a configuracao (dry-run, nada sera criado)..."
    eksctl create cluster -f $gerado --dry-run
} else {
    Write-Host "3/3 Criando o cluster. Leva cerca de 20 minutos e consome o orcamento do lab."
    Write-Host "    Lembre de apagar ao terminar: .\aws\apagar-cluster.ps1"
    eksctl create cluster -f $gerado
}
if ($LASTEXITCODE -ne 0) { throw "O eksctl terminou com erro (codigo $LASTEXITCODE)." }
