#Requires -Version 5.1
<#
.SYNOPSIS
  Доставляет образ бота на компьютер, где он запущен.

.DESCRIPTION
  Запускать на компьютере, где ведётся разработка.
  Нужен deploy.env (см. deploy.env.example) и SSH-доступ к компьютеру с ботом.
  На том компьютере заранее лежит .env (см. .env.example). Исходники туда не копируются.

.EXAMPLE
  .\deploy.ps1 -Push
  Пуш master, ожидание сборки в GitHub Actions, выкладка образа с тегом короткого SHA.

.EXAMPLE
  .\deploy.ps1 -Tag latest
  Выкладка уже собранного тега, без пуша.

.EXAMPLE
  .\deploy.ps1 -Tag a1b2c3d
  Откат или выкладка конкретного коммита.

.EXAMPLE
  .\deploy.ps1 -Push -Bootstrap
  Первый запуск: поднять бота вместе с Postgres и Redis.
#>
[CmdletBinding()]
param(
    [string]$Tag,
    [switch]$Push,
    [switch]$Bootstrap
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root

function Read-DeployEnv {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Создайте deploy.env из deploy.env.example и заполните хост, пользователя, путь и DOCKER_REPO."
    }
    $map = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        $trim = $line.Trim()
        if ($trim.Length -eq 0 -or $trim.StartsWith("#")) { continue }
        $eq = $trim.IndexOf("=")
        if ($eq -lt 1) { throw "Строка без ключа в deploy.env: $trim" }
        $key = $trim.Substring(0, $eq).Trim()
        $value = $trim.Substring($eq + 1).Trim().Trim('"').Trim("'")
        $map[$key] = $value
    }
    return $map
}

function Assert-Exit {
    param([string]$Action)
    if ($LASTEXITCODE -ne 0) {
        throw "$Action завершился с кодом $LASTEXITCODE"
    }
}

function Invoke-Remote {
    param(
        [string[]]$SshArgs,
        [string]$Target,
        [string]$ShellName,
        [string]$ScriptText
    )
    if ($ShellName -eq "bash") {
        $ScriptText | & ssh @SshArgs $Target "bash -s"
    } else {
        $ScriptText | & ssh @SshArgs $Target "powershell -NoProfile -Command -"
    }
    Assert-Exit "ssh"
}

$cfg = Read-DeployEnv (Join-Path $Root "deploy.env")
foreach ($key in @("DEPLOY_HOST", "DEPLOY_USER", "DEPLOY_PATH", "DOCKER_REPO")) {
    if (-not $cfg.ContainsKey($key) -or [string]::IsNullOrWhiteSpace($cfg[$key])) {
        throw "В deploy.env не задано $key."
    }
}

$shellName = "bash"
if ($cfg.ContainsKey("DEPLOY_SHELL") -and -not [string]::IsNullOrWhiteSpace($cfg["DEPLOY_SHELL"])) {
    $shellName = $cfg["DEPLOY_SHELL"].ToLowerInvariant()
}
if ($shellName -notin @("bash", "powershell")) {
    throw "DEPLOY_SHELL должен быть bash или powershell."
}

$remotePath = $cfg["DEPLOY_PATH"]
if ($remotePath -match "\s") {
    throw "DEPLOY_PATH укажите без пробелов."
}

$repo = $cfg["DOCKER_REPO"].Trim()
if ($repo -match "[:\s]" -or $repo -notmatch "/") {
    throw "DOCKER_REPO укажите без тега, например username/english."
}

$sshArgs = @("-o", "BatchMode=yes")
if ($cfg.ContainsKey("DEPLOY_SSH_KEY") -and -not [string]::IsNullOrWhiteSpace($cfg["DEPLOY_SSH_KEY"])) {
    $sshArgs = @("-i", $cfg["DEPLOY_SSH_KEY"]) + $sshArgs
}
$target = "{0}@{1}" -f $cfg["DEPLOY_USER"], $cfg["DEPLOY_HOST"]

$pushedSha = $null
if ($Push) {
    $branch = (git rev-parse --abbrev-ref HEAD).Trim()
    Assert-Exit "git rev-parse"
    if ($branch -ne "master") {
        throw "Образ публикуется при пуше в master. Сейчас ветка $branch."
    }
    git push origin master
    Assert-Exit "git push"
    if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
        throw "Пуш выполнен. Чтобы дождаться сборки и выкатить образ, установите GitHub CLI (gh) и снова запустите deploy.ps1."
    }
    $pushedSha = (git rev-parse HEAD).Trim()
    Assert-Exit "git rev-parse"
    $runId = $null
    for ($i = 0; $i -lt 30; $i++) {
        $runId = (gh run list --workflow deploy.yml --commit $pushedSha --json databaseId --jq ".[0].databaseId" | Out-String).Trim()
        if ($runId -and $runId -ne "null") { break }
        Start-Sleep -Seconds 2
    }
    if (-not $runId -or $runId -eq "null") {
        throw "GitHub Actions не показал сборку для коммита $pushedSha."
    }
    gh run watch $runId --exit-status
    Assert-Exit "gh run watch"
    if (-not $PSBoundParameters.ContainsKey("Tag") -or [string]::IsNullOrWhiteSpace($Tag)) {
        $Tag = $pushedSha.Substring(0, 7)
    }
}

if ([string]::IsNullOrWhiteSpace($Tag)) {
    $Tag = "latest"
}
if ($Tag -notmatch "^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$") {
    throw "Недопустимый тег образа: $Tag"
}

$image = "${repo}:$Tag"
Write-Host "Образ: $image"
Write-Host "Куда:  $target`:$remotePath"

if ($shellName -eq "bash") {
    $mkdir = "mkdir -p '$remotePath'"
} else {
    $mkdir = "New-Item -ItemType Directory -Force -Path '$remotePath' | Out-Null"
}
Invoke-Remote -SshArgs $sshArgs -Target $target -ShellName $shellName -ScriptText $mkdir

$composeFile = Join-Path $Root "docker-compose.yml"
$scpDest = "{0}:{1}/docker-compose.yml" -f $target, $remotePath
& scp @sshArgs $composeFile $scpDest
Assert-Exit "scp docker-compose.yml"

$upArgs = "up -d --no-deps english-back"
if ($Bootstrap) {
    $upArgs = "up -d"
}

if ($shellName -eq "bash") {
    $remoteTemplate = @'
set -euo pipefail
cd '__PATH__'
if [ ! -f .env ]; then
  echo '.env не найден в __PATH__. Создайте его на компьютере с ботом по образцу .env.example.' >&2
  exit 1
fi
tmp=$(mktemp)
grep -v '^DOCKER_IMAGE=' .env > "$tmp" || true
printf 'DOCKER_IMAGE=%s\n' '__IMAGE__' >> "$tmp"
mv "$tmp" .env
export DOCKER_IMAGE='__IMAGE__'
docker compose pull english-back
docker compose __UP__
docker compose ps english-back
'@
} else {
    $remoteTemplate = @'
$ErrorActionPreference = 'Stop'
Set-Location '__PATH__'
if (-not (Test-Path -LiteralPath '.env')) {
  throw '.env не найден в __PATH__. Создайте его на компьютере с ботом по образцу .env.example.'
}
$lines = @(Get-Content -LiteralPath '.env' | Where-Object { $_ -notmatch '^DOCKER_IMAGE=' })
$lines += 'DOCKER_IMAGE=__IMAGE__'
[System.IO.File]::WriteAllLines((Join-Path (Get-Location) '.env'), $lines)
$env:DOCKER_IMAGE = '__IMAGE__'
$dockerBin = Join-Path $env:TEMP 'english-docker-bin'
$dockerCfg = Join-Path $env:TEMP 'english-docker-cfg'
New-Item -ItemType Directory -Force -Path $dockerBin, $dockerCfg | Out-Null
@(
  '@echo off'
  'echo credentials not found in native keychain'
  'exit /b 1'
) | Set-Content -Path (Join-Path $dockerBin 'docker-credential-none.cmd') -Encoding ascii
Set-Content -Path (Join-Path $dockerCfg 'config.json') -Encoding ascii -Value '{"auths":{},"credsStore":"none"}'
$env:PATH = "$dockerBin;$env:PATH"
$env:DOCKER_CONFIG = $dockerCfg
docker compose pull english-back
if ($LASTEXITCODE -ne 0) { throw "docker compose pull завершился с кодом $LASTEXITCODE" }
docker compose __UP__
if ($LASTEXITCODE -ne 0) { throw "docker compose up завершился с кодом $LASTEXITCODE" }
docker compose ps english-back
'@
}
$remote = $remoteTemplate.Replace("__PATH__", $remotePath).Replace("__IMAGE__", $image).Replace("__UP__", $upArgs)

Invoke-Remote -SshArgs $sshArgs -Target $target -ShellName $shellName -ScriptText $remote
Write-Host "Готово. Откат: .\deploy.ps1 -Tag <короткий SHA предыдущего коммита>"
