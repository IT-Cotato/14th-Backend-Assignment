import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { cardClass } from '../components/styles'

export function HomePage() {
  const { user } = useAuth()

  return (
    <section className={cardClass}>
      <h1 className="mb-2 text-2xl font-bold text-gray-900">코테이토 스터디 매칭</h1>
      <p className="mb-6 text-gray-600">함께 공부할 스터디를 찾고, 모집해 보세요.</p>
      {user ? (
        <p className="text-gray-700">
          <span className="font-medium">{user.nickname}</span>님, 반가워요!
        </p>
      ) : (
        <div className="flex flex-col gap-2 sm:flex-row">
          <Link to="/login"
            className="rounded-md bg-indigo-600 px-4 py-2 text-center font-medium text-white hover:bg-indigo-700">
            로그인
          </Link>
          <Link to="/signup"
            className="rounded-md border border-gray-300 px-4 py-2 text-center font-medium text-gray-700 hover:bg-gray-50">
            회원가입
          </Link>
        </div>
      )}
    </section>
  )
}
