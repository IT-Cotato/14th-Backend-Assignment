import { Link } from 'react-router'

export function NotFoundPage() {
  return (
    <section className="py-16 text-center">
      <h1 className="mb-2 text-2xl font-bold">페이지를 찾을 수 없어요</h1>
      <p className="mb-6 text-stone-600">주소가 바뀌었거나 삭제된 페이지예요.</p>
      <Link to="/" className="font-medium text-amber-700 underline">
        처음으로 가기
      </Link>
    </section>
  )
}
