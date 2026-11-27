$ErrorActionPreference = 'Stop'
$nome = 'postgresql-x64-18'
$servico = Get-Service -Name $nome -ErrorAction SilentlyContinue
if (!$servico) { throw 'Serviço PostgreSQL não encontrado. Execute scripts/instalar-postgres.ps1.' }
if ($servico.Status -eq 'Running') { Write-Host 'PostgreSQL: Running.'; exit 0 }
try {
    Start-Service -Name $nome
    (Get-Service $nome).WaitForStatus('Running', [TimeSpan]::FromSeconds(30))
    Write-Host 'PostgreSQL: Running.'
} catch {
    throw 'Para iniciar/parar o serviço do Windows, execute este script em um PowerShell como administrador.'
}
