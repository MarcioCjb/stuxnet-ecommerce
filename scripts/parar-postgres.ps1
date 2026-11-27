$ErrorActionPreference = 'Stop'
$nome = 'postgresql-x64-18'
$servico = Get-Service -Name $nome -ErrorAction SilentlyContinue
if (!$servico) { throw 'Serviço PostgreSQL não encontrado. Execute scripts/instalar-postgres.ps1.' }
if ($servico.Status -eq 'Stopped') { Write-Host 'PostgreSQL: Stopped.'; exit 0 }
try {
    Stop-Service -Name $nome
    (Get-Service $nome).WaitForStatus('Stopped', [TimeSpan]::FromSeconds(30))
    Write-Host 'PostgreSQL: Stopped.'
} catch {
    throw 'Para iniciar/parar o serviço do Windows, execute este script em um PowerShell como administrador.'
}
