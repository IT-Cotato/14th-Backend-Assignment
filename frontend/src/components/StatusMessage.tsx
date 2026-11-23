// 로딩 중, 에러 메시지 표시용 공통 컴포넌트

export function Loading({ text = '불러오는 중...' }: { text?: string }) {
  return (
    <p role="status" className="py-8 text-center text-sm text-gray-500">
      {text}
    </p>
  )
}

export function ErrorMessage({ message }: { message: string }) {
  return (
    <p role="alert" className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
      {message}
    </p>
  )
}
