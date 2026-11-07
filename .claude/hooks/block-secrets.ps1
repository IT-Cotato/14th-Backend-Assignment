# PreToolUse 훅: Edit, Write로 민감한 설정 파일을 수정하지 못하게 차단한다.
# 차단할 때는 exit 2로 끝내고, stderr 메시지를 Claude에게 전달한다.
[Console]::InputEncoding = [Text.Encoding]::UTF8
[Console]::OutputEncoding = [Text.Encoding]::UTF8

function Stop-SecretEdit([string]$reason) {
    [Console]::Error.WriteLine("차단: $reason (.claude/rules/secrets.md 참고)")
    exit 2
}

try {
    $payload = [Console]::In.ReadToEnd() | ConvertFrom-Json
} catch {
    Stop-SecretEdit '훅 입력을 해석할 수 없어 수정을 허용하지 않습니다.'
}

$filePath = [string]$payload.tool_input.file_path
if (-not $filePath) { exit 0 }

$normalized = $filePath -replace '\\', '/'
$leaf = ($normalized -split '/')[-1]

if ($leaf -match '^\.env(\..+)?$' -and $leaf -ne '.env.example') {
    Stop-SecretEdit "$leaf 파일은 민감한 정보가 들어 있어 수정할 수 없습니다."
}

if ($leaf -match '^application-.+\.ya?ml$' -and $normalized -notmatch '(^|/)src/test/') {
    Stop-SecretEdit "$leaf 파일은 민감한 정보가 들어 있을 수 있어 수정할 수 없습니다."
}

exit 0
