# block-force-push.ps1
# PreToolUse Hook이에요. Claude Code가 셸 명령(Bash / PowerShell)을 실행하기 전에
# stdin으로 받은 JSON의 tool_input.command를 검사해서 강제 push를 막아요.
#
# 차단 대상 (git push 뒤 어느 위치에 있어도 차단해요)
#   --force, --force-with-lease[=...], 그 밖에 --forc 로 시작하는 축약형
#   -f, 그리고 -uf, -fu 처럼 f가 들어간 짧은 옵션 묶음
#   +main 처럼 '+'로 시작하는 refspec (강제 push와 같은 효과예요)
#
# 종료 코드
#   0 : 안전한 명령이라 실행을 허용해요.
#   2 : 강제 push로 판단해서 차단해요. 이유는 stderr로 출력해요.
# 스크립트 안에서 예외가 나도 안전하게 차단(2)하도록 만들었어요. (fail-closed)

$ErrorActionPreference = 'Stop'

# 한글이 깨지지 않도록 입출력 인코딩을 UTF-8로 맞춰요.
try {
    [Console]::InputEncoding  = [System.Text.Encoding]::UTF8
    [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
} catch { }

$SEP = [string][char]0   # 명령 구분자(; | & 줄바꿈)를 나타내는 표식이에요.

function Write-Block([string]$reason) {
    [Console]::Error.WriteLine("[block-force-push] $reason")
    exit 2
}

# 명령 문자열을 토큰으로 나눠요.
# 따옴표 안의 내용은 하나의 토큰으로 묶고, 따옴표 안 문자열은 따로 모아서
# 'sh -c "git push -f"' 같은 중첩 실행도 다시 검사할 수 있게 해요.
function Split-Command([string]$s) {
    $tokens = New-Object System.Collections.Generic.List[string]
    $quoted = New-Object System.Collections.Generic.List[string]
    $sb = New-Object System.Text.StringBuilder
    $hasToken = $false
    $i = 0
    while ($i -lt $s.Length) {
        $c = $s[$i]
        if ($c -eq '"' -or $c -eq "'") {
            $j = $i + 1
            while ($j -lt $s.Length -and $s[$j] -ne $c) {
                if ($c -eq '"' -and ($s[$j] -eq '\' -or $s[$j] -eq '`') -and ($j + 1) -lt $s.Length) { $j++ }
                $j++
            }
            $end = [Math]::Min($j, $s.Length)
            $inner = $s.Substring($i + 1, $end - $i - 1)
            [void]$sb.Append($inner)
            $quoted.Add($inner)
            $hasToken = $true
            $i = $end + 1
            continue
        }
        if ([char]::IsWhiteSpace($c) -and $c -ne "`n" -and $c -ne "`r") {
            if ($hasToken) { $tokens.Add($sb.ToString()); [void]$sb.Clear(); $hasToken = $false }
        }
        elseif ($c -eq ';' -or $c -eq '|' -or $c -eq '&' -or $c -eq "`n" -or $c -eq "`r" -or $c -eq '(' -or $c -eq ')' -or $c -eq '{' -or $c -eq '}') {
            if ($hasToken) { $tokens.Add($sb.ToString()); [void]$sb.Clear(); $hasToken = $false }
            $tokens.Add($SEP)
        }
        else {
            [void]$sb.Append($c)
            $hasToken = $true
        }
        $i++
    }
    if ($hasToken) { $tokens.Add($sb.ToString()) }
    return @{ Tokens = $tokens; Quoted = $quoted }
}

# 토큰 목록에서 'git ... push ...' 를 찾아 강제 push 옵션이 있는지 확인해요.
# 찾으면 차단 이유를, 없으면 $null을 돌려줘요.
function Find-ForcePush($tokens) {
    for ($i = 0; $i -lt $tokens.Count; $i++) {
        $t = $tokens[$i]
        if ($t -notmatch '^(.*[\\/])?git(\.exe)?$') { continue }

        # git 전역 옵션(-C <경로>, -c <키=값>, --git-dir=... 등)을 건너뛰고 하위 명령을 찾아요.
        $j = $i + 1
        while ($j -lt $tokens.Count -and $tokens[$j] -ne $SEP -and $tokens[$j].StartsWith('-')) {
            if ($tokens[$j] -in @('-C', '-c', '--git-dir', '--work-tree', '--namespace', '--exec-path')) { $j++ }
            $j++
        }
        if ($j -ge $tokens.Count -or $tokens[$j] -ne 'push') { continue }

        # push 뒤의 인자를 구분자 전까지 모두 검사해요.
        $endOfOptions = $false
        for ($k = $j + 1; $k -lt $tokens.Count -and $tokens[$k] -ne $SEP; $k++) {
            $a = $tokens[$k]
            if (-not $endOfOptions) {
                if ($a -eq '--') { $endOfOptions = $true; continue }
                if ($a -match '^--force-with-lease') { return "git push --force-with-lease 는 원격 이력을 덮어쓸 수 있어 차단했어요. (인자: $a)" }
                if ($a -match '^--forc')             { return "git push --force 는 원격 이력을 덮어쓸 수 있어 차단했어요. (인자: $a)" }
                if ($a -match '^-[A-Za-z0-9]*f[A-Za-z0-9]*$') { return "git push -f 는 원격 이력을 덮어쓸 수 있어 차단했어요. (인자: $a)" }
            }
            if ($a.StartsWith('+')) { return "git push 의 '+' refspec 은 강제 push와 같아서 차단했어요. (인자: $a)" }
        }
    }
    return $null
}

function Test-Command([string]$cmd, [int]$depth) {
    if ([string]::IsNullOrWhiteSpace($cmd) -or $depth -gt 5) { return $null }
    $parsed = Split-Command $cmd
    $reason = Find-ForcePush $parsed.Tokens
    if ($reason) { return $reason }
    # 따옴표 안 문자열도 별도의 명령일 수 있어서 다시 검사해요.
    foreach ($q in $parsed.Quoted) {
        $reason = Test-Command $q ($depth + 1)
        if ($reason) { return $reason }
    }
    return $null
}

try {
    # stdin을 바이트로 읽어 UTF-8로 해석하고, 앞에 BOM이 붙어 있으면 떼어내요.
    $ms = New-Object System.IO.MemoryStream
    [Console]::OpenStandardInput().CopyTo($ms)
    $raw = (New-Object System.Text.UTF8Encoding($false)).GetString($ms.ToArray()).TrimStart([char]0xFEFF)
    if ([string]::IsNullOrWhiteSpace($raw)) { exit 0 }

    $data = $raw | ConvertFrom-Json
    $command = $null
    if ($data.tool_input -and $data.tool_input.command) { $command = [string]$data.tool_input.command }
    if ([string]::IsNullOrWhiteSpace($command)) { exit 0 }

    $reason = Test-Command $command 0
    if ($reason) {
        Write-Block "$reason 강제 push가 꼭 필요하다면 사용자가 직접 실행해 주세요. (.claude/rules/git-workflow.md 5번 참고)"
    }
    exit 0
}
catch {
    # 입력을 해석하지 못하면 안전하게 차단해요.
    Write-Block "Hook 입력을 검사하는 중 오류가 나서 안전을 위해 차단했어요: $($_.Exception.Message)"
}
