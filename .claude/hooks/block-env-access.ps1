# PreToolUse 훅: .env 파일의 읽기, 수정, 출력을 차단한다.
# 차단할 때는 exit 2로 끝내고, stderr 메시지를 Claude에게 전달한다.
[Console]::InputEncoding = [Text.Encoding]::UTF8
[Console]::OutputEncoding = [Text.Encoding]::UTF8

$payload = [Console]::In.ReadToEnd() | ConvertFrom-Json
$toolInput = $payload.tool_input

function Stop-EnvAccess {
    [Console]::Error.WriteLine('차단: .env 파일은 열거나 수정하거나 출력할 수 없습니다. (.claude/rules/secrets.md 참고)')
    exit 2
}

function Test-EnvFileName([string]$value) {
    if ([string]::IsNullOrWhiteSpace($value)) { return $false }
    $leaf = ($value -split '[\\/]')[-1]
    return ($leaf -match '^\.env(\..+)?$') -and ($leaf -ne '.env.example')
}

# 파일 도구: 대상 경로만 검사한다. (문서 본문에 .env라는 글자가 있어도 막지 않는다)
foreach ($value in @($toolInput.file_path, $toolInput.notebook_path, $toolInput.path, $toolInput.glob)) {
    if (Test-EnvFileName $value) { Stop-EnvAccess }
}

# 셸 도구: 명령어에 .env 파일 토큰이 있으면 막는다.
$command = [string]$toolInput.command
if ($command) {
    # 내용을 읽지 않는 단일 git 명령은 허용한다. 체이닝이 있으면 예외를 적용하지 않는다.
    $isSafeGit = ($command -match '^\s*git\s+(check-ignore|ls-files|commit)\b') -and ($command -notmatch '[;&|]')
    if (-not $isSafeGit) {
        $envToken = '(^|[\s"''=<>|;&(,\\/])\.env(?!\.example)(\.[\w.-]+)?($|[\s"'';|&)<>,\\/*])'
        if ($command -match $envToken) { Stop-EnvAccess }
    }
}

exit 0
