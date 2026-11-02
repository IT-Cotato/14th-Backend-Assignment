# PreToolUse 훅: develop 직접 커밋/머지/푸시와 base가 minho/basic이 아닌 PR을 차단한다.
# 차단할 때는 exit 2로 끝내고, stderr 메시지를 Claude에게 전달한다.
[Console]::InputEncoding = [Text.Encoding]::UTF8
[Console]::OutputEncoding = [Text.Encoding]::UTF8

$payload = [Console]::In.ReadToEnd() | ConvertFrom-Json
$command = [string]$payload.tool_input.command
if (-not $command) { exit 0 }

function Stop-GitTarget([string]$reason) {
    [Console]::Error.WriteLine("차단: $reason (docs/git-workflow.md 참고)")
    exit 2
}

# 테스트에서 현재 브랜치를 지정할 수 있게 환경변수를 먼저 확인한다.
$branch = $env:GUARD_TEST_BRANCH
if (-not $branch) { $branch = (git branch --show-current 2>$null) }

$developRef = '(^|[\s:])(refs/heads/)?develop($|\s)'

if ($command -match '\bgit\s+(commit|merge)\b' -and $branch -eq 'develop') {
    Stop-GitTarget 'develop 브랜치에는 직접 커밋하거나 머지할 수 없습니다.'
}

if ($command -match '\bgit\s+push\b') {
    $pushText = $command.Substring($Matches[0].Length + $command.IndexOf($Matches[0]))
    if ($pushText -match $developRef) {
        Stop-GitTarget 'develop으로 푸시할 수 없습니다.'
    }
    if ($branch -eq 'develop') {
        Stop-GitTarget 'develop 브랜치에서는 푸시할 수 없습니다.'
    }
}

if ($command -match '\bgh\s+pr\s+(create|edit)\b') {
    $subcommand = $Matches[1]
    $hasBase = $command -match '(--base[=\s]+|-B\s+)["'']?([^\s"'']+)'
    if ($hasBase -and $Matches[2] -ne 'minho/basic') {
        Stop-GitTarget 'PR의 base는 minho/basic이어야 합니다.'
    }
    if (-not $hasBase -and $subcommand -eq 'create') {
        Stop-GitTarget 'PR을 만들 때는 --base minho/basic을 지정해야 합니다.'
    }
}

exit 0
