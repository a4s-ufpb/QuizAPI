# Roda um cenário do teste de carga com amostragem de CPU/memória do container.
param(
    [int]$Rooms = 1,
    [int]$Players = 12,
    [int]$Questions = 6,
    [int]$Timeout = 240000
)
$ErrorActionPreference = "Continue"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path

$samples = New-Object System.Collections.ArrayList
$job = Start-Job -ScriptBlock {
    while ($true) {
        $line = docker stats container-quiz-api --no-stream --format "{{.CPUPerc}};{{.MemUsage}}"
        if ($line) { $line }
        Start-Sleep -Milliseconds 1500
    }
}

node "$here\loadtest.js" --rooms $Rooms --players $Players --questions $Questions --timeout $Timeout

Stop-Job $job | Out-Null
$raw = Receive-Job $job
Remove-Job $job | Out-Null

$cpuMax = 0.0; $memMax = 0.0
foreach ($s in $raw) {
    $parts = $s -split ";"
    if ($parts.Count -lt 2) { continue }
    $cpu = [double]($parts[0].TrimEnd("%"))
    if ($cpu -gt $cpuMax) { $cpuMax = $cpu }
    $memStr = ($parts[1] -split "/")[0].Trim()
    $mem = 0.0
    if ($memStr -match "([\d\.]+)GiB") { $mem = [double]$Matches[1] * 1024 }
    elseif ($memStr -match "([\d\.]+)MiB") { $mem = [double]$Matches[1] }
    if ($mem -gt $memMax) { $memMax = $mem }
}
Write-Host ("container: CPU max {0:N1}% (de 200% = 2 cores) | RAM max {1:N0} MiB (limite 2560)" -f $cpuMax, $memMax)
