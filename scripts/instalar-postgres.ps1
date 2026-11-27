param([int]$Porta = 5432)
$ErrorActionPreference = 'Stop'
$projeto = Split-Path $PSScriptRoot -Parent
$localConfig = Join-Path $projeto '.local'
$pgInstall = Join-Path $env:ProgramFiles 'PostgreSQL\18'
$pgData = Join-Path $env:ProgramData 'PostgreSQL\18\data'
$pgBin = Join-Path $pgInstall 'bin'
$servicoNome = 'postgresql-x64-18'
$servico = Get-Service -Name $servicoNome -ErrorAction SilentlyContinue
if ($servico) {
    Write-Host "PostgreSQL já instalado como serviço: $servicoNome ($($servico.Status))."
    exit 0
}
if (Test-Path $pgInstall) { throw "Já existe $pgInstall sem o serviço esperado. Confira a instalação existente." }
New-Item -ItemType Directory -Force -Path $localConfig | Out-Null
$adminFile = Join-Path $localConfig 'postgres-admin.properties'
$configFile = Join-Path $projeto 'database.properties'
$optionsFile = Join-Path $localConfig 'postgres-installer.options'
$origemRoot = Join-Path $env:LOCALAPPDATA 'StuxNet\postgresql'
$origemBin = Join-Path $origemRoot 'pgsql\bin'
$origemData = Join-Path $origemRoot 'data'
$migrar = Test-Path (Join-Path $origemData 'PG_VERSION')
if (!$migrar -and (Test-Path $configFile)) { throw 'database.properties já existe. Configure a instalação existente sem sobrescrever as credenciais.' }
if ($migrar) {
    if (!(Test-Path $adminFile)) { throw 'Cluster antigo encontrado sem credenciais administrativas para criar o backup.' }
    $props = Get-Content $adminFile -Raw | ConvertFrom-StringData
    $adminSenha = $props.password
    $Porta = [int]$props.port
} else {
    $adminSenha = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
    $appSenha = [guid]::NewGuid().ToString('N')
}
$backup = Join-Path $localConfig 'backup-antes-instalacao.sql'
$restaurar = Join-Path $localConfig 'restaurar-na-maquina.sql'
$senhaAnterior = $env:PGPASSWORD
$origemParada = $false
try {
    $env:PGPASSWORD = $adminSenha
    if ($migrar) {
        & "$origemBin\pg_dumpall.exe" -h 127.0.0.1 -p $Porta -U postgres -f $backup
        if ($LASTEXITCODE -ne 0) { throw 'Falha ao criar o backup. A instalação anterior foi preservada.' }
        $sql = [IO.File]::ReadAllText($backup, [Text.Encoding]::UTF8).Replace('CREATE ROLE postgres;', '-- Superusuário já criado pelo instalador oficial.')
        [IO.File]::WriteAllText($restaurar, $sql, (New-Object Text.UTF8Encoding($false)))
        & "$origemBin\pg_ctl.exe" -D $origemData -m fast -w stop
        if ($LASTEXITCODE -ne 0) { throw 'Não foi possível parar o cluster antigo com segurança.' }
        $origemParada = $true
    }
    $opcoes = @(
        'mode=unattended', 'unattendedmodeui=none', "prefix=$pgInstall", "datadir=$pgData",
        "serverport=$Porta", "servicename=$servicoNome", 'superaccount=postgres',
        "superpassword=$adminSenha", 'disable-components=stackbuilder', 'enable_acledit=1'
    )
    [IO.File]::WriteAllLines($optionsFile, $opcoes, (New-Object Text.UTF8Encoding($false)))
    Write-Host 'Instalando com o instalador oficial. Confirme a solicitação de administrador do Windows.'
    $linha = 'install --id PostgreSQL.PostgreSQL.18 --exact --scope machine --silent --accept-package-agreements --accept-source-agreements --override "--optionfile \"{0}\""' -f $optionsFile
    $processo = Start-Process winget.exe -ArgumentList $linha -WindowStyle Hidden -PassThru
    $processo.WaitForExit()
    if ($processo.ExitCode -ne 0) { throw 'O instalador oficial não concluiu. Verifique a solicitação administrativa e o histórico do winget.' }
    if (!(Get-Service $servicoNome -ErrorAction SilentlyContinue)) { throw 'O serviço PostgreSQL não foi registrado pelo instalador.' }
    & "$pgBin\pg_isready.exe" -h 127.0.0.1 -p $Porta
    if ($LASTEXITCODE -ne 0) { throw 'O PostgreSQL instalado ainda não aceita conexões.' }
    if ($migrar) {
        & "$pgBin\psql.exe" -h 127.0.0.1 -p $Porta -U postgres -d postgres -v ON_ERROR_STOP=1 -f $restaurar
        if ($LASTEXITCODE -ne 0) { throw 'Instalação concluída, mas a restauração precisa ser revisada. O backup e o cluster antigo foram preservados.' }
    } else {
        $sqlFile = Join-Path $localConfig 'setup.tmp.sql'
        try {
            Set-Content -LiteralPath $sqlFile -Encoding ascii -Value "CREATE ROLE stuxnet LOGIN PASSWORD '$appSenha';`nCREATE DATABASE stuxnet OWNER stuxnet;`nCREATE DATABASE stuxnet_test OWNER stuxnet;"
            & "$pgBin\psql.exe" -h 127.0.0.1 -p $Porta -U postgres -d postgres -v ON_ERROR_STOP=1 -f $sqlFile
            if ($LASTEXITCODE -ne 0) { throw 'Falha ao criar usuário/bancos. As credenciais serão preservadas para concluir a configuração.' }
        } finally { Remove-Item -LiteralPath $sqlFile -ErrorAction SilentlyContinue }
        Set-Content -LiteralPath $configFile -Encoding ascii -Value "db.url=jdbc:postgresql://127.0.0.1:$Porta/stuxnet`ndb.user=stuxnet`ndb.password=$appSenha"
    }
    Write-Host "PostgreSQL instalado em $pgInstall. Dados em $pgData. Serviço $servicoNome com início automático."
    if ($migrar) { Write-Host 'Bancos restaurados. A origem antiga foi preservada para conferência; remova-a somente após validar os dados.' }
} catch {
    if ($origemParada -and !(Get-Service $servicoNome -ErrorAction SilentlyContinue)) {
        $argsPg = '-D "{0}" -l "{1}" -w start' -f $origemData, (Join-Path $origemRoot 'postgresql.log')
        $recuperacao = Start-Process "$origemBin\pg_ctl.exe" -ArgumentList $argsPg -WindowStyle Hidden -PassThru
        $recuperacao.WaitForExit()
    }
    throw
} finally {
    $env:PGPASSWORD = $senhaAnterior
    # Preserva as credenciais administrativas mesmo em caso de falha do instalador.
    Set-Content -LiteralPath $adminFile -Encoding ascii -Value "user=postgres`npassword=$adminSenha`nport=$Porta"
    Remove-Item -LiteralPath $optionsFile -ErrorAction SilentlyContinue
}
