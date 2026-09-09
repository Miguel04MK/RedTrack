# ============================================================
#  Envia un mensaje de prueba con el formato del resumen diario,
#  para comprobar de punta a punta que token y chat_id funcionan.
#
#  Lee TELEGRAM_BOT_TOKEN y TELEGRAM_CHAT_ID del .env y no los
#  imprime nunca.
#
#  Uso:
#     .\scripts\probar-telegram.ps1
# ============================================================

$ErrorActionPreference = 'Stop'

$raiz = Split-Path -Parent $PSScriptRoot
$rutaEnv = Join-Path $raiz '.env'

if (-not (Test-Path $rutaEnv)) {
    Write-Host "No existe $rutaEnv." -ForegroundColor Red
    exit 1
}

$vars = @{}
foreach ($linea in Get-Content $rutaEnv) {
    if ($linea -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$') {
        $vars[$matches[1]] = $matches[2].Trim().Trim('"').Trim("'")
    }
}

$token = $vars['TELEGRAM_BOT_TOKEN']
$chatId = $vars['TELEGRAM_CHAT_ID']

if ([string]::IsNullOrWhiteSpace($token) -or [string]::IsNullOrWhiteSpace($chatId)) {
    Write-Host "Faltan TELEGRAM_BOT_TOKEN o TELEGRAM_CHAT_ID en el .env." -ForegroundColor Red
    exit 1
}

# Mismo formato que produce FormateadorTelegram, con datos de ejemplo.
#
# Los nombres de empresa son inventados A PROPOSITO. Esta captura acaba en
# un README publico, y poner una banda salarial concreta junto al nombre de
# una empresa real se lee como si fuera su oferta de verdad.
$texto = @"
*RedTrack* - 2 ofertas nuevas

[87%] Desarrollador/a Java Junior - Empresa Ejemplo S.L.
Santiago de Compostela · Hibrido · 21.000-25.000 EUR
Pide: Java, Spring Boot, PostgreSQL, Docker
Te falta: nada
https://www.adzuna.es/land/ad/ejemplo

[62%] Backend Developer - Otra Empresa S.A.
Madrid · Hibrido · sin salario
Pide: Java, Kafka
Te falta: Kafka
AVISO: pide 2 anos
AVISO: tecnologia troncal que no tienes: Kafka
https://www.adzuna.es/land/ad/ejemplo2

_Mensaje de prueba. Datos inventados._
"@

Write-Host "Enviando mensaje de prueba al chat $chatId ..." -ForegroundColor Cyan

$cuerpo = @{
    chat_id                  = $chatId
    text                     = $texto
    parse_mode               = 'Markdown'
    disable_web_page_preview = $true
} | ConvertTo-Json -Depth 3

try {
    $r = Invoke-RestMethod -Uri "https://api.telegram.org/bot$token/sendMessage" `
        -Method Post -ContentType 'application/json; charset=utf-8' `
        -Body ([System.Text.Encoding]::UTF8.GetBytes($cuerpo)) -TimeoutSec 20
}
catch {
    $codigo = $null
    if ($_.Exception.Response) { $codigo = [int]$_.Exception.Response.StatusCode }
    switch ($codigo) {
        400 { Write-Host "400: chat_id incorrecto, o el formato Markdown no es valido." -ForegroundColor Red }
        401 { Write-Host "401: el token no es valido." -ForegroundColor Red }
        403 { Write-Host "403: el bot no puede escribirte. Escribele /start primero." -ForegroundColor Red }
        default { Write-Host "El envio ha fallado (codigo: $codigo)." -ForegroundColor Red }
    }
    exit 1
}

if ($r.ok) {
    Write-Host "Enviado. Mira el movil." -ForegroundColor Green
} else {
    Write-Host "Telegram ha respondido con error." -ForegroundColor Red
    exit 1
}
