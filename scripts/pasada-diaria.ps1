# ============================================================
#  Una pasada del radar, pensada para el Programador de tareas
#  de Windows.
#
#  Levanta Postgres si hace falta, ejecuta la recoleccion y el
#  resumen, y termina. Deja registro en logs\pasada-diaria.log.
#
#  Es la opcion de cero registros: no necesita ni servidor ni base
#  de datos gestionada. A cambio, el PC tiene que estar encendido
#  a la hora programada.
#
#  Para programarlo (PowerShell como administrador, una sola vez):
#
#    $accion  = New-ScheduledTaskAction -Execute 'powershell.exe' `
#      -Argument '-NoProfile -ExecutionPolicy Bypass -File "C:\Users\migue\OneDrive\Escritorio\empleo\Proyecto RedTrack\scripts\pasada-diaria.ps1"'
#    $cuando  = New-ScheduledTaskTrigger -Daily -At 8:00am
#    $ajustes = New-ScheduledTaskSettingsSet -StartWhenAvailable `
#      -DontStopIfGoingOnBatteries -AllowStartIfOnBatteries
#    Register-ScheduledTask -TaskName 'RedTrack' -Action $accion `
#      -Trigger $cuando -Settings $ajustes
#
#  -StartWhenAvailable importa: si el PC estaba apagado a las 8:00,
#  la tarea se ejecuta en cuanto arranque.
# ============================================================

$ErrorActionPreference = 'Stop'

$raiz = Split-Path -Parent $PSScriptRoot
Set-Location $raiz

$logs = Join-Path $raiz 'logs'
if (-not (Test-Path $logs)) { New-Item -ItemType Directory $logs | Out-Null }
$log = Join-Path $logs 'pasada-diaria.log'

function Registrar($mensaje) {
    $linea = "{0}  {1}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $mensaje
    Add-Content -Path $log -Value $linea -Encoding utf8
    Write-Output $linea
}

Registrar "=== arranca la pasada diaria ==="

try {
    # --- Variables del .env ---------------------------------
    $rutaEnv = Join-Path $raiz '.env'
    if (-not (Test-Path $rutaEnv)) { throw "No existe $rutaEnv" }

    foreach ($linea in Get-Content $rutaEnv) {
        if ($linea -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$') {
            $valor = $matches[2].Trim().Trim('"').Trim("'")
            if ($valor) { Set-Item -Path "env:$($matches[1])" -Value $valor }
        }
    }
    $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'

    # --- Postgres -------------------------------------------
    $puerto = if ($env:POSTGRES_PUERTO_HOST) { [int]$env:POSTGRES_PUERTO_HOST } else { 5433 }
    $vivo = Test-NetConnection -ComputerName 127.0.0.1 -Port $puerto -WarningAction SilentlyContinue
    if (-not $vivo.TcpTestSucceeded) {
        Registrar "Postgres no responde en $puerto, levantandolo"
        docker compose up -d postgres | Out-Null
        Start-Sleep -Seconds 12
    }

    # --- El jar ---------------------------------------------
    $jar = Get-ChildItem (Join-Path $raiz 'target') -Filter 'redtrack-*.jar' -ErrorAction SilentlyContinue |
           Where-Object { $_.Name -notlike '*.original' } | Select-Object -First 1
    if (-not $jar) {
        Registrar "No hay jar compilado, compilando"
        mvn -B -q package -DskipTests
        $jar = Get-ChildItem (Join-Path $raiz 'target') -Filter 'redtrack-*.jar' |
               Where-Object { $_.Name -notlike '*.original' } | Select-Object -First 1
    }

    Registrar "Ejecutando $($jar.Name)"
    & java -jar $jar.FullName --spring.profiles.active=una-pasada 2>&1 |
        ForEach-Object { Add-Content -Path $log -Value $_ -Encoding utf8 }

    $codigo = $LASTEXITCODE
    if ($codigo -eq 0) { Registrar "=== terminada correctamente ===" }
    else { Registrar "=== TERMINADA CON ERROR (codigo $codigo) ===" }
    exit $codigo
}
catch {
    Registrar "=== FALLO: $($_.Exception.Message) ==="
    exit 1
}
