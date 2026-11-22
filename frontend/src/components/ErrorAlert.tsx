// 서버가 내려준 에러 메시지를 보여줘요. role="alert"라 스크린리더가 바로 읽어요.
export function ErrorAlert({ message }: { message: string | null }) {
  if (!message) return null
  return (
    <p role="alert" className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
      {message}
    </p>
  )
}
