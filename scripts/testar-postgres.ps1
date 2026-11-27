$ErrorActionPreference = 'Stop'
$projeto = Split-Path $PSScriptRoot -Parent
$props = Get-Content (Join-Path $projeto 'database.properties') -Raw | ConvertFrom-StringData
$anteriorUrl = $env:TEST_DB_URL
$anteriorUsuario = $env:TEST_DB_USER
$anteriorSenha = $env:TEST_DB_PASSWORD
try {
    # Usa exclusivamente o banco de testes, com um schema isolado por teste.
    $env:TEST_DB_URL = $props.'db.url' -replace '/stuxnet$', '/stuxnet_test'
    if ($env:TEST_DB_URL -eq $props.'db.url') { throw 'Configure uma URL que termine em /stuxnet para derivar o banco de testes.' }
    $env:TEST_DB_USER = $props.'db.user'
    $env:TEST_DB_PASSWORD = $props.'db.password'
    Push-Location $projeto
    try {
        & mvn test
        if ($LASTEXITCODE -ne 0) { throw 'Testes com PostgreSQL falharam.' }
    } finally { Pop-Location }
} finally {
    $env:TEST_DB_URL = $anteriorUrl
    $env:TEST_DB_USER = $anteriorUsuario
    $env:TEST_DB_PASSWORD = $anteriorSenha
}
