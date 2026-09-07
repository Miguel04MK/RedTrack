# ============================================================
#  Averigua tu chat_id de Telegram y lo escribe en el .env.
#
#  Requisito previo: haber creado el bot con @BotFather, tener el
#  token en TELEGRAM_BOT_TOKEN y HABERLE ESCRITO ALGO AL BOT.
#  Sin ese primer mensaje getUpdates viene vacio: un bot no puede
#  escribir a alguien que no le ha hablado antes.
#
#  Lee el token del .env y NO lo imprime nunca.
#
#  Uso:
#     .\scripts\telegram-chat-id.ps1
#     .\scripts\telegram-chat-id.ps1 -NoGuardar   (solo mostrar)
# ============================================================

param(
    [switch]$NoGuardar
)

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

if ([string]::IsNullOrWhiteSpace($token)) {
    Write-Host "Falta TELEGRAM_BOT_TOKEN en el .env." -ForegroundColor Red
    exit 1
}

# Formato de token de BotFather: <numeros>:<35 caracteres>
if ($token -notmatch '^\d+:[A-Za-z0-9_-]{30,}$') {
    Write-Host "El token no tiene la forma que da BotFather (numeros, dos puntos, cadena larga)." -ForegroundColor Red
    Write-Host "Longitud actual: $($token.Length) caracteres." -ForegroundColor DarkGray
    exit 1
}

Write-Host "token: $($token.Length) caracteres, formato correcto" -ForegroundColor DarkGray
Write-Host "Consultando getUpdates...`n" -ForegroundColor Cyan

try {
    $r = Invoke-RestMethod -Uri "https://api.telegram.org/bot$token/getUpdates" -TimeoutSec 20
}
catch {
    $codigo = $null
    if ($_.Exception.Response) { $codigo = [int]$_.Exception.Response.StatusCode }
    if ($codigo -eq 401) {
        Write-Host "401: el token no es valido. Revisalo con @BotFather (/mybots)." -ForegroundColor Red
    } else {
        Write-Host "La llamada ha fallado (codigo: $codigo)." -ForegroundColor Red
    }
    exit 1
}

if (-not $r.ok) {
    Write-Host "Telegram ha respondido con error." -ForegroundColor Red
    exit 1
}

$chats = @()
foreach ($u in $r.result) {
    $m = if ($u.message) { $u.message } elseif ($u.edited_message) { $u.edited_message } else { $null }
    if ($m -and $m.chat) {
        $chats += [pscustomobject]@{
            Id     = $m.chat.id
            Tipo   = $m.chat.type
            Quien  = (@($m.chat.first_name, $m.chat.last_name, $m.chat.username, $m.chat.title) |
                      Where-Object { $_ } | Select-Object -First 1)
        }
    }
}
$chats = $chats | Sort-Object Id -Unique

if ($chats.Count -eq 0) {
    Write-Host "getUpdates no devuelve nada." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Abre Telegram, busca tu bot y escribele cualquier cosa (un /start vale)." -ForegroundColor Yellow
    Write-Host "Un bot no puede iniciar la conversacion: tienes que hablarle tu primero." -ForegroundColor Yellow
    exit 1
}

Write-Host "Conversaciones encontradas:" -ForegroundColor Green
foreach ($c in $chats) {
    Write-Host ("  chat_id {0}   {1}   ({2})" -f $c.Id, $c.Quien, $c.Tipo)
}
Write-Host ""

if ($NoGuardar) { exit 0 }

if ($chats.Count -gt 1) {
    Write-Host "Hay mas de una conversacion. Elige el chat_id que quieras y ponlo a mano" -ForegroundColor Yellow
    Write-Host "en TELEGRAM_CHAT_ID dentro del .env." -ForegroundColor Yellow
    exit 0
}

$chatId = $chats[0].Id
$contenido = Get-Content $rutaEnv | ForEach-Object {
    if ($_ -match '^\s*TELEGRAM_CHAT_ID\s*=') { "TELEGRAM_CHAT_ID=$chatId" } else { $_ }
}
Set-Content $rutaEnv $contenido -Encoding utf8

Write-Host "TELEGRAM_CHAT_ID=$chatId guardado en el .env." -ForegroundColor Green
