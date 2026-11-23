export function Loading({ message = '불러오는 중입니다...' }: { message?: string }) {
  return (
    <p role="status" className="py-8 text-center text-sm text-slate-500">
      {message}
    </p>
  )
}
