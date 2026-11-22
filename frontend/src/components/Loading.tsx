export function Loading({ message = '불러오는 중...' }: { message?: string }) {
  return (
    <p role="status" className="py-12 text-center text-slate-500">
      {message}
    </p>
  )
}
