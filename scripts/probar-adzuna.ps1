# ============================================================
#  Comprueba que las claves de Adzuna funcionan, lanzando una
#  busqueda real contra su API.
#
#  Lee ADZUNA_APP_ID y ADZUNA_APP_KEY del .env y NO las imprime
#  nunca: ni en la salida, ni en los mensajes de error.
#
#  Uso:
#     .\scripts\probar-adzuna.ps1
#     .\scripts\probar-adzuna.ps1 -Que "java spring" -Donde "vigo"
#     .\scripts\probar-adzuna.ps1 -Cuantas 10
# ============================================================

param(
    [string]$Que = 'java spring',
    [string]$Donde = 'galicia',
    [int]$Cuantas = 5,
    [int]$DiasMax = 7
)

$ErrorActionPreference = 'Stop'

# --- Cargar el .env ----------------------------------------
$raiz = Split-Path -Parent $PSScriptRoot
$rutaEnv = Join-Path $raiz '.env'

if (-not (Test-Path $rutaEnv)) {
    Write-Host "No existe $rutaEnv. Copia .env.example a .env y rellena las claves." -ForegroundColor Red
    exit 1
}

$vars = @{}
foreach ($linea in Get-Content $rutaEnv) {
    if ($linea -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$') {
        $vars[$matches[1]] = $matches[2].Trim().Trim('"').Trim("'")
    }
}

$appId = $vars['ADZUNA_APP_ID']
$appKey = $vars['ADZUNA_APP_KEY']

if ([string]::IsNullOrWhiteSpace($appId) -or [string]::IsNullOrWhiteSpace($appKey)) {
    Write-Host "Faltan ADZUNA_APP_ID o ADZUNA_APP_KEY en el .env." -ForegroundColor Red
    exit 1
}

Write-Host "app_id  : $($appId.Length) caracteres" -ForegroundColor DarkGray
Write-Host "app_key : $($appKey.Length) caracteres" -ForegroundColor DarkGray
Write-Host "Buscando '$Que' en '$Donde' (ultimos $DiasMax dias)...`n" -ForegroundColor Cyan

# --- Llamada ------------------------------------------------
$uri = 'https://api.adzuna.com/v1/api/jobs/es/search/1' +
       "?app_id=$([uri]::EscapeDataString($appId))" +
       "&app_key=$([uri]::EscapeDataString($appKey))" +
       "&results_per_page=$Cuantas" +
       "&what=$([uri]::EscapeDataString($Que))" +
       "&where=$([uri]::EscapeDataString($Donde))" +
       "&max_days_old=$DiasMax" +
       '&content-type=application/json'

try {
    $respuesta = Invoke-RestMethod -Uri $uri -Method Get -TimeoutSec 20
}
catch {
    # Cuidado: la excepcion original arrastra la URL con la clave dentro.
    # Solo se muestra el codigo de estado.
    $codigo = $null
    if ($_.Exception.Response) { $codigo = [int]$_.Exception.Response.StatusCode }

    switch ($codigo) {
        401 { Write-Host "401: las claves no son validas. Revisa el .env." -ForegroundColor Red }
        403 { Write-Host "403: claves rechazadas o cuenta sin activar." -ForegroundColor Red }
        429 { Write-Host "429: has superado el limite de peticiones. Espera un rato." -ForegroundColor Yellow }
        default { Write-Host "La llamada ha fallado (codigo: $codigo)." -ForegroundColor Red }
    }
    exit 1
}

# --- Resultado ----------------------------------------------
Write-Host "OK. Adzuna dice que hay $($respuesta.count) ofertas para esa busqueda.`n" -ForegroundColor Green

if (-not $respuesta.results -or $respuesta.results.Count -eq 0) {
    Write-Host "Sin resultados. Prueba con otro termino o amplia -DiasMax." -ForegroundColor Yellow
    exit 0
}

$conSalario = 0
foreach ($o in $respuesta.results) {
    $salario = 'sin salario'
    if ($null -ne $o.salary_min) {
        $conSalario++
        $salario = if ($o.salary_min -eq $o.salary_max) {
            '{0:N0} EUR' -f $o.salary_min
        } else {
            '{0:N0}-{1:N0} EUR' -f $o.salary_min, $o.salary_max
        }
        if ($o.salary_is_predicted -eq '1') { $salario += ' (estimado)' }
    }

    Write-Host ('  ' + $o.title) -ForegroundColor White
    Write-Host ('    ' + $o.company.display_name + '  |  ' + $o.location.display_name)
    Write-Host ('    ' + $salario + '  |  ' + $o.created) -ForegroundColor DarkGray
    Write-Host ''
}

Write-Host "$conSalario de $($respuesta.results.Count) publican salario." -ForegroundColor DarkGray
