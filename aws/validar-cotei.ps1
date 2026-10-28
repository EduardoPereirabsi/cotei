# Confere o Cotei implantado no EKS e imprime a evidencia de cada rubrica.
#
#   .\aws\validar-cotei.ps1             ensaio completo: grava dados de demonstracao
#   .\aws\validar-cotei.ps1 -SemDados   so leitura: confere a infraestrutura
#
# O ensaio completo cria um produto novo, uma avaliacao, dois alertas e cinco
# ofertas, sempre ligados ao produto recem-criado: pode rodar mais de uma vez.
# No dia da apresentacao use -SemDados, para a demonstracao comecar com os
# bancos vazios.
param([switch]$SemDados, [string]$Kubectl = "")

if (-not $Kubectl) {
    $k137 = Join-Path $env:USERPROFILE ".minikube\cache\windows\amd64\v1.37.0\kubectl.exe"
    $Kubectl = if (Test-Path $k137) { $k137 } else { "kubectl" }
}

$contexto = & $Kubectl config current-context
if ($contexto -notmatch "cotei") { throw "O contexto atual nao e o cluster cotei." }

$lb = & $Kubectl get service ingress-nginx-controller -n ingress-nginx -o jsonpath="{.status.loadBalancer.ingress[0].hostname}"
if (-not $lb) { throw "O Load Balancer ainda nao tem endereco. Rode antes o implantar-cotei.ps1." }
$base = "http://$lb"
Write-Host "Load Balancer: $base"

function J($o) { $o | ConvertTo-Json -Depth 5 -Compress }
function Post($caminho, $corpo) {
    $bytes = [Text.Encoding]::UTF8.GetBytes((J $corpo))
    Invoke-RestMethod -Method Post -Uri "$base$caminho" -Body $bytes -ContentType "application/json; charset=utf-8"
}

Write-Host "`n=== R1/R2: deployments e distribuicao dos pods entre os nos ==="
& $Kubectl get deployments
& $Kubectl get pods -o wide

Write-Host "`n=== R1: um banco por servico ==="
& $Kubectl exec deploy/mysql-produto -- mysql -uroot -proot -e "SHOW DATABASES;" 2>$null
& $Kubectl exec deploy/postgres-loja -- psql -U postgres -lqt 2>$null | Select-String "lojadb"
& $Kubectl exec deploy/postgres-alerta -- psql -U postgres -lqt 2>$null | Select-String "alertadb"
& $Kubectl exec deploy/mongo-avaliacao -- mongosh --quiet --eval "db.adminCommand({listDatabases:1}).databases.map(d=>d.name)"

$alvo = "produtos"
if (-not $SemDados) {
    Write-Host "`n=== R2: Ingress pelo Load Balancer (cadastro de produto) ==="
    $produto = Post "/produto/produtos" @{ nome = "Smartphone Galaxy S26"; marca = "Samsung"; categoria = "SMARTPHONE"; ean = "7891234567890"; descricao = "256 GB"; ativo = $true }
    J $produto
    $id = [int]$produto.id
    $alvo = "produtos/$id"
}

Write-Host "`n=== R2: balanceamento entre as replicas do ms-produto ==="
1..15 | ForEach-Object { (Invoke-RestMethod "$base/produto/instance").pod } |
    Group-Object | ForEach-Object { "{0,3}x {1}" -f $_.Count, $_.Name }

Write-Host "`n=== R2: Service Discovery (curl pelo nome, de dentro do cluster) ==="
& $Kubectl delete pod teste --ignore-not-found --now | Out-Null
& $Kubectl run teste --image=curlimages/curl --restart=Never -- sleep 600 | Out-Null
& $Kubectl wait --for=condition=Ready pod/teste --timeout=120s | Out-Null
& $Kubectl exec teste -- curl -s "http://ms-produto:8080/produto/$alvo"
& $Kubectl delete pod teste --now | Out-Null

if (-not $SemDados) {
    Write-Host "`n`n=== R3 REST: o ms-avaliacao valida o produto no ms-produto ==="
    J (Post "/avaliacao/avaliacoes" @{ produtoId = $id; autor = "Maria"; nota = 5; comentario = "Excelente" })
    try { Post "/avaliacao/avaliacoes" @{ produtoId = 999999; autor = "X"; nota = 3 } | Out-Null }
    catch { "produto inexistente -> HTTP $([int]$_.Exception.Response.StatusCode)" }
    J (Invoke-RestMethod "$base/avaliacao/produtos/$id/resumo")

    Write-Host "`n=== R3 RabbitMQ: alertas e ofertas publicadas pelo ms-coletor ==="
    J (Post "/alerta/alertas" @{ produtoId = $id; email = "maria@exemplo.com"; precoAlvo = 2500 })
    J (Post "/alerta/alertas" @{ produtoId = $id; email = "joao@exemplo.com"; precoAlvo = 3000 })
    $ofertas = @(
        @{ produtoId = $id; loja = "Loja Alfa"; preco = 3199.90; frete = 0 },
        @{ produtoId = $id; loja = "Loja Gama"; preco = 1999; frete = 0; disponivel = $false },
        @{ produtoId = $id; loja = "Loja Beta"; preco = 2749; frete = 149.90 },
        @{ produtoId = $id; loja = "Loja Alfa"; preco = 2399.90; frete = 0 },
        @{ produtoId = $id; loja = "Loja Gama"; preco = 2199; frete = 0 }
    )
    foreach ($o in $ofertas) { J (Post "/coletor/ofertas" $o); Start-Sleep -Seconds 1 }
    Start-Sleep -Seconds 4

    Write-Host "`n--- avisos disparados (esperado: Joao pela Beta e Maria pela Alfa) ---"
    J (Invoke-RestMethod "$base/alerta/notificacoes")
    Write-Host "`n--- melhor oferta no ms-loja (esperado: Gama, R$ 2199) ---"
    J (Invoke-RestMethod "$base/loja/ofertas/melhor?produtoId=$id")
    Write-Host "`n--- coleta pelas fontes simuladas (o ms-coletor le o catalogo por REST) ---"
    J (Post "/coletor/coletas" @{ produtoIds = @($id) })
    Start-Sleep -Seconds 4
    Write-Host "`n--- os mesmos eventos nos dois consumidores do fanout ---"
    & $Kubectl logs deployment/ms-loja --tail=8
    & $Kubectl logs deployment/ms-alerta --tail=8
}

Write-Host "`n=== Documentacao pelo Load Balancer ==="
foreach ($u in "/produto/swagger-ui/index.html", "/loja/swagger-ui/index.html", "/avaliacao/docs", "/coletor/docs", "/alerta/swagger-ui/index.html") {
    try { $r = Invoke-WebRequest "$base$u" -UseBasicParsing; "{0}  {1}{2}" -f $r.StatusCode, $base, $u }
    catch { "ERRO  $base$u : $($_.Exception.Message)" }
}
