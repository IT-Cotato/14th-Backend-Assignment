# PowerShell이 stderr로 내보내는 글자를 UTF-8로 맞춘다.
# 이 설정이 없으면 Claude Code가 메시지를 읽을 때 글자가 깨질 수 있다.
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# Claude Code가 Bash 도구를 쓰려고 할 때마다 이 스크립트가 먼저 실행된다.
# 실행하려는 명령어가 git이면 차단한다.
# 차단 메시지는 AI가 읽는 내용이라, 인코딩 문제를 피하기 위해 영어로 쓴다.

# 표준 입력으로 들어오는 JSON을 전부 읽는다.
$input_json = [Console]::In.ReadToEnd()

# 읽은 문자열을 PowerShell 객체로 변환한다.
# 변환에 실패하면 명령어를 확인할 수 없으므로 안전하게 차단한다.
try {
    $data = $input_json | ConvertFrom-Json -ErrorAction Stop
} catch {
    [Console]::Error.WriteLine("Hook: failed to parse JSON input.")
    exit 2
}

# 실행하려는 명령어를 꺼낸다.
# 맨 위의 command가 아니라 tool_input 안에 들어 있다.
$command = [string]$data.tool_input.command

# 명령어가 비어 있으면 검사할 게 없으므로 통과시킨다.
if ([string]::IsNullOrWhiteSpace($command)) {
    exit 0
}

# 앞뒤 공백을 제거한다.
$trimmed = $command.Trim()

# 명령어가 git으로 시작하는지 확인한다.
# -match는 정규식 비교이고, ^는 문자열의 시작을 뜻한다.
# \s는 공백 문자이므로 "git status"는 걸리고 "gitignore"는 걸리지 않는다.
# $는 문자열의 끝이므로 "git" 한 단어만 쳐도 걸린다.
if ($trimmed -match '^git(\s|$)') {
    # stderr로 메시지를 보내면 그 내용이 AI에게 전달된다.
    [Console]::Error.WriteLine("Blocked: git commands must be run by the user. Suggest the command instead of running it.")
    # 종료 코드 2는 "차단"을 뜻한다.
    exit 2
}

# 여기까지 왔으면 git 명령이 아니므로 통과시킨다.
exit 0