/** 서버나 입력 검증의 에러 메시지를 보여준다. 메시지가 없으면 아무것도 그리지 않는다. */
export function ErrorMessage({ message }: { message: string | null }) {
  if (!message) {
    return null
  }
  return (
    <p role="alert" className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
      {message}
    </p>
  )
}
