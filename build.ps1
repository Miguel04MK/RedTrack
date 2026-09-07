# ============================================================
#  Atajo de build para Windows.
#
#  El JAVA_HOME del sistema apunta a un Adoptium 17 que ya no existe,
#  asi que mvn falla en seco. Este script fija el JDK 21 solo para
#  esta sesion, sin tocar la variable de entorno global.
#
#  Uso:
#     .\build.ps1            compila y pasa los tests
#     .\build.ps1 test       solo los tests
#     .\build.ps1 run        arranca la aplicacion
#     .\build.ps1 package    genera el jar sin tests
# ============================================================

param(
    [ValidateSet('verify', 'test', 'run', 'package', 'clean')]
    [string]$Tarea = 'verify'
)

$jdk = 'C:\Program Files\Java\jdk-21'

if (-not (Test-Path $jdk)) {
    Write-Error "No se encuentra el JDK 21 en '$jdk'. Ajusta la ruta en build.ps1."
    exit 1
}

$env:JAVA_HOME = $jdk
Write-Host "JAVA_HOME = $env:JAVA_HOME" -ForegroundColor DarkGray

switch ($Tarea) {
    'verify'  { mvn clean verify }
    'test'    { mvn test }
    'run'     { mvn spring-boot:run }
    'package' { mvn clean package -DskipTests }
    'clean'   { mvn clean }
}

exit $LASTEXITCODE
