# ============================================================
#  Arranca la aplicacion en local cargando el .env.
#
#  Spring no lee .env por su cuenta: docker-compose si lo hace, pero
#  al arrancar con Maven o desde el IDE hay que meter las variables
#  en el entorno del proceso a mano. Eso es lo unico que hace esto.
#
#  Requiere Postgres levantado:  docker compose up -d postgres
#
#  Uso:
#     .\scripts\arrancar-local.ps1
#     .\scripts\arrancar-local.ps1 -Perfil local
# ============================================================

param(
    [string]$Perfil = 'local'
)

$ErrorActionPreference = 'Stop'

$raiz = Split-Path -Parent $PSScriptRoot
$rutaEnv = Join-Path $raiz '.env'

if (-not (Test-Path $rutaEnv)) {
    Write-Host "No existe $rutaEnv. Copia .env.example a .env y rellenalo." -ForegroundColor Red
    exit 1
}

$cargadas = @()
foreach ($linea in Get-Content $rutaEnv) {
    if ($linea -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$') {
        $nombre = $matches[1]
        $valor = $matches[2].Trim().Trim('"').Trim("'")
        if (-not [string]::IsNullOrWhiteSpace($valor)) {
            Set-Item -Path "env:$nombre" -Value $valor
            $cargadas += $nombre
        }
    }
}

# Se listan los NOMBRES, nunca los valores.
Write-Host "Variables cargadas del .env: $($cargadas -join ', ')" -ForegroundColor DarkGray

$env:SPRING_PROFILES_ACTIVE = $Perfil
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'

Write-Host "Perfil de Spring: $Perfil" -ForegroundColor DarkGray
Write-Host "Comprobando que Postgres responde..." -ForegroundColor DarkGray

$puerto = if ($env:POSTGRES_PUERTO_HOST) { [int]$env:POSTGRES_PUERTO_HOST } else { 5433 }
$prueba = Test-NetConnection -ComputerName 127.0.0.1 -Port $puerto -WarningAction SilentlyContinue
if (-not $prueba.TcpTestSucceeded) {
    Write-Host "Nadie escucha en 127.0.0.1:$puerto. Levanta la base de datos:" -ForegroundColor Red
    Write-Host "    docker compose up -d postgres" -ForegroundColor Yellow
    exit 1
}

Write-Host "Postgres OK en el puerto $puerto. Arrancando...`n" -ForegroundColor Green

Set-Location $raiz
mvn spring-boot:run
