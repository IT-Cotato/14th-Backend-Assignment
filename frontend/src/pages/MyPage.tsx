import { PART_LABELS } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { ErrorAlert } from '../components/ErrorAlert'

// RequireAuth 안에서만 렌더링됩니다.
export function MyPage() {
  const { me, isLoadingMe, meError } = useAuth()

  return (
    <section className="mx-auto w-full max-w-md">
      <h1 className="mb-6 text-2xl font-bold text-gray-900">마이페이지</h1>

      {isLoadingMe && (
        <p role="status" className="text-gray-600">
          불러오는 중…
        </p>
      )}

      <ErrorAlert message={meError} />

      {me && (
        <dl className="grid grid-cols-[auto_1fr] gap-x-6 gap-y-3 rounded-lg bg-white p-6 text-base shadow-sm">
          <dt className="font-medium text-gray-600">이메일</dt>
          <dd className="break-all text-gray-900">{me.email}</dd>
          <dt className="font-medium text-gray-600">이름</dt>
          <dd className="text-gray-900">{me.name}</dd>
          <dt className="font-medium text-gray-600">닉네임</dt>
          <dd className="text-gray-900">{me.nickname}</dd>
          <dt className="font-medium text-gray-600">기수</dt>
          <dd className="text-gray-900">{me.generation}기</dd>
          <dt className="font-medium text-gray-600">파트</dt>
          <dd className="text-gray-900">
            {me.part} ({PART_LABELS[me.part]})
          </dd>
        </dl>
      )}
    </section>
  )
}
