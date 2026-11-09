<#
.SYNOPSIS
  Respaldo de EpCore en Windows: base principal, base de cada cliente y documentos.

.DESCRIPTION
  Usa las mismas credenciales que EpCore: DB_URL, DB_USERNAME y DB_PASSWORD (variables
  de entorno o archivo .env) y config\clientes.properties para los clientes.
  Las claves pasan a mysqldump por variable de entorno, nunca en la línea de comandos.
  Se puede programar con el Programador de tareas de Windows.

.EXAMPLE
  .\scripts\respaldo.ps1 -Destino D:\Respaldos\EpCore -RetencionDias 14
#>
param(
    [string]$Destino = (Join-Path $env:USERPROFILE "Respaldos\EpCore"),
    [int]$RetencionDias = 14,
    [string]$Mysqldump = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe"
)
$ErrorActionPreference = "Stop"
$raiz = Split-Path $PSScriptRoot -Parent

function Leer-Propiedades([string]$ruta) {
    $m = @{}
    if (Test-Path $ruta) {
        Get-Content $ruta | Where-Object { $_ -match '^\s*[^#].*?=' } | ForEach-Object {
            $i = $_.IndexOf('='); $m[$_.Substring(0, $i).Trim()] = $_.Substring($i + 1).Trim()
        }
    }
    $m
}

$envArchivo = Leer-Propiedades (Join-Path $raiz ".env")
$url = if ($env:DB_URL) { $env:DB_URL } else { $envArchivo["DB_URL"] }
$usr = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { $envArchivo["DB_USERNAME"] }
$clv = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { $envArchivo["DB_PASSWORD"] }
if (-not $url -or -not $usr -or -not $clv) { throw "Defina DB_URL, DB_USERNAME y DB_PASSWORD (variables de entorno o .env)." }

$carpeta = Join-Path $Destino (Get-Date -Format "yyyyMMdd-HHmm")
New-Item -ItemType Directory -Force -Path $carpeta | Out-Null
Write-Host "Respaldo EpCore -> $carpeta"

function Respaldar([string]$nombre, [string]$jdbc, [string]$usuario, [string]$clave) {
    if ($jdbc -notmatch '^jdbc:mysql://([^:/?]+)(?::(\d+))?/([^?]+)') { throw "URL no válida para $nombre" }
    $h = $Matches[1]; $p = if ($Matches[2]) { $Matches[2] } else { "3306" }; $b = $Matches[3]
    $sal = Join-Path $carpeta "$nombre.sql"
    $env:MYSQL_PWD = $clave
    try {
        & $Mysqldump -h $h -P $p -u $usuario --single-transaction --quick --routines --triggers --no-tablespaces "--result-file=$sal" $b
        if ($LASTEXITCODE -ne 0) { throw "mysqldump falló para $nombre" }
    } finally { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
    Compress-Archive -Path $sal -DestinationPath "$sal.zip" -Force; Remove-Item $sal
    Write-Host ("  base {0} ({1}): {2:N0} KB" -f $nombre, $b, ((Get-Item "$sal.zip").Length / 1KB))
}

Respaldar "principal" $url $usr $clv

$cli = Leer-Propiedades (Join-Path $raiz "config\clientes.properties")
$ids = $cli.Keys | Where-Object { $_ -match '^epcore\.multicliente\.clientes\.([a-z0-9-]+)\.url$' } | ForEach-Object { $_.Split('.')[3] } | Sort-Object -Unique
foreach ($id in $ids) {
    $p = "epcore.multicliente.clientes.$id"
    Respaldar "cliente-$id" $cli["$p.url"] $cli["$p.usuario"] $cli["$p.clave"]
}

$docs = Join-Path $raiz "uploads"
if (Test-Path $docs) {
    Compress-Archive -Path $docs -DestinationPath (Join-Path $carpeta "documentos.zip") -Force
    Write-Host "  documentos: copiados"
}

Get-ChildItem $carpeta -File | Get-FileHash -Algorithm SHA256 |
    ForEach-Object { "{0}  {1}" -f $_.Hash.ToLower(), (Split-Path $_.Path -Leaf) } |
    Set-Content (Join-Path $carpeta "SHA256SUMS") -Encoding ASCII

Get-ChildItem $Destino -Directory | Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-$RetencionDias) } |
    Remove-Item -Recurse -Force
Write-Host "Listo. Se conservan los respaldos de los últimos $RetencionDias días."
