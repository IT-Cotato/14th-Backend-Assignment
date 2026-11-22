import type { Profile } from '../api/auth'

export default function ProfilePage({ result, onRetry }: {
  result: { profile: Profile } | { error: string } | null
  onRetry: () => void
}) {
  return (
    <section className="panel" aria-labelledby="profile-heading">
      <h1 id="profile-heading">내 정보</h1>
      {!result && <p role="status">내 정보를 불러오는 중입니다.</p>}
      {result && 'error' in result && <>
        <p className="error" role="alert">{result.error}</p>
        <button type="button" onClick={onRetry}>다시 시도</button>
      </>}
      {result && 'profile' in result && (
        <dl className="profile">
          <div><dt>이름</dt><dd>{result.profile.name}</dd></div>
          <div><dt>이메일</dt><dd>{result.profile.email}</dd></div>
          <div><dt>닉네임</dt><dd>{result.profile.nickname}</dd></div>
          <div><dt>기수</dt><dd>{result.profile.generation}</dd></div>
          <div><dt>파트</dt><dd>{result.profile.part}</dd></div>
        </dl>
      )}
    </section>
  )
}
