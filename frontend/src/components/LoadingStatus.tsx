// 불러오는 중 상태를 보여줍니다.
export function LoadingStatus({ message = '불러오는 중…' }: { message?: string }) {
  return (
    <p role="status" className="py-4 text-gray-600">
      {message}
    </p>
  )
}
