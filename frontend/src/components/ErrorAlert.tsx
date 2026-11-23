// 서버가 내려준 에러 메시지를 그대로 보여줍니다.
export function ErrorAlert({ message }: { message: string | null }) {
  if (!message) {
    return null
  }
  return (
    <p role="alert" className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
      {message}
    </p>
  )
}
