<#
.SYNOPSIS
  Da de alta un cliente nuevo de EpCore con su propia base de datos.

.DESCRIPTION
  1. Crea la base epcore_<id> y un usuario de MySQL con permisos SOLO sobre ella
     (pide la contraseña de root de MySQL; el script no la guarda).
  2. Genera claves aleatorias para ese usuario y para el primer administrador.
  3. Agrega el cliente a config\clientes.properties (archivo local, fuera de Git).
  Al reiniciar EpCore, las migraciones crean las tablas y se crean los roles,
  los catálogos y el administrador inicial del cliente.

.EXAMPLE
  .\scripts\alta-cliente.ps1 -Id bancocafetero -Nombre "Banco Cafetero" -AdminCorreo accesos@bancocafetero.co
#>
param(
    [Parameter(Mandatory)][string]$Id,
    [Parameter(Mandatory)][string]$Nombre,
    [Parameter(Mandatory)][string]$AdminCorreo,
    [string]$MysqlHost = "localhost",
    [int]$MysqlPuerto = 3306,
    # Host desde el que EpCore se conecta a MySQL ('%' si EpCore corre en otro contenedor o servidor)
    [string]$HostUsuario = "localhost",
    [string]$Mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
    [string]$Archivo = (Join-Path (Split-Path $PSScriptRoot -Parent) "config\clientes.properties")
)
$ErrorActionPreference = "Stop"

if ($Id -notmatch '^[a-z0-9](?:[a-z0-9-]{0,38}[a-z0-9])?$' -or $Id -eq "principal") {
    throw "Identificador no válido: use minúsculas, números y guion (será el subdominio, p. ej. bancocafetero)."
}
if ($AdminCorreo -notmatch '^[^@\s]+@[^@\s]+\.[^@\s]+$') { throw "Correo del administrador no válido." }
if (-not (Test-Path $Mysql)) { throw "No se encontró mysql.exe en $Mysql (use -Mysql para indicar la ruta)." }
if ((Test-Path $Archivo) -and (Select-String -Path $Archivo -Pattern "^epcore\.multicliente\.clientes\.$([regex]::Escape($Id))\." -Quiet)) {
    throw "El cliente '$Id' ya está en $Archivo."
}

function Nueva-Clave([int]$largo) {
    $chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
    $bytes = New-Object byte[] $largo
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    -join ($bytes | ForEach-Object { $chars[$_ % $chars.Length] })
}
# Nombre en .properties: caracteres no ASCII como \uXXXX
function Escapar([string]$t) { -join ($t.ToCharArray() | ForEach-Object { if ([int]$_ -gt 126) { '\u{0:x4}' -f [int]$_ } else { $_ } }) }

$base = "epcore_" + $Id.Replace("-", "_")
$usuario = ("epc_" + $Id.Replace("-", "_"))
if ($usuario.Length -gt 32) { $usuario = $usuario.Substring(0, 32) }
$claveBd = Nueva-Clave 24
$claveAdmin = Nueva-Clave 16

$sql = @"
CREATE DATABASE $base CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER '$usuario'@'$HostUsuario' IDENTIFIED BY '$claveBd';
GRANT ALL PRIVILEGES ON $base.* TO '$usuario'@'$HostUsuario';
"@

Write-Host "Se creará la base '$base' y el usuario '$usuario'@'$HostUsuario' en ${MysqlHost}:$MysqlPuerto."
if (-not $env:MYSQL_PWD) {
    $seg = Read-Host "Contraseña de root de MySQL (no se muestra ni se guarda)" -AsSecureString
    $env:MYSQL_PWD = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($seg))
}
try { & $Mysql -h $MysqlHost -P $MysqlPuerto -u root -e $sql } finally { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
if ($LASTEXITCODE -ne 0) { throw "MySQL no pudo crear la base (¿ya existe o la contraseña de root no es correcta?). No se cambió la configuración." }

$dir = Split-Path $Archivo -Parent
if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir | Out-Null }
$lineas = @()
if (-not (Test-Path $Archivo) -or -not (Select-String -Path $Archivo -Pattern "^epcore\.multicliente\.habilitado=" -Quiet)) {
    $lineas += "# Clientes de EpCore. ARCHIVO LOCAL CON CLAVES: no se sube a Git."
    $lineas += "epcore.multicliente.habilitado=true"
}
$p = "epcore.multicliente.clientes.$Id"
$lineas += ""
$lineas += "# $Id - alta $(Get-Date -Format 'yyyy-MM-dd')"
$lineas += "$p.nombre=$(Escapar $Nombre)"
$lineas += "$p.url=jdbc:mysql://${MysqlHost}:$MysqlPuerto/${base}?sslMode=PREFERRED&serverTimezone=America/Bogota"
$lineas += "$p.usuario=$usuario"
$lineas += "$p.clave=$claveBd"
$lineas += "$p.admin-correo=$AdminCorreo"
$lineas += "$p.admin-clave=$claveAdmin"
Add-Content -Path $Archivo -Value $lineas -Encoding ASCII

Write-Host ""
Write-Host "Cliente '$Id' dado de alta."
Write-Host "  Configuración: $Archivo"
Write-Host "  Primer administrador: $AdminCorreo"
Write-Host "  Clave inicial (entréguela por un canal seguro y pida cambiarla): $claveAdmin"
Write-Host ""
Write-Host "Siguiente paso: reinicie EpCore. Las tablas, roles, catálogos y el administrador se crean solos."
Write-Host "El cliente entra por https://$Id.<dominio-base> (configure ese subdominio en el DNS)."
