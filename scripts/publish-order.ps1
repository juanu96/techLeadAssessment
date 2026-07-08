param(
    [string]$File = (Join-Path $PSScriptRoot '..\examples\order.json'),
    [string]$Topic = 'orders-topic'
)

if (-not (Test-Path -LiteralPath $File)) {
    throw "Order file not found: $File"
}

Get-Content -LiteralPath $File -Raw |
    docker compose exec -T kafka kafka-console-producer `
        --bootstrap-server kafka:29092 `
        --topic $Topic

if ($LASTEXITCODE -ne 0) {
    throw "Could not publish the order to $Topic"
}

Write-Host "Order published to $Topic"
