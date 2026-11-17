import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth.ts'

export default function Header() {
  const { user, loading, logout } = useAuth()
  const navigate = useNavigate()

  async function handleLogout() {
    await logout()
    navigate('/login')
  }

  return (
    <header className="border-b border-gray-200 bg-white">
      <div className="mx-auto flex max-w-3xl flex-wrap items-center justify-between gap-2 px-4 py-3">
        <Link to="/studies" className="text-lg font-bold text-indigo-600">
          코테이토 스터디
        </Link>
        <nav className="flex items-center gap-3 text-sm">
          {loading ? null : user ? (
            <>
              <span className="text-gray-700">{user.nickname}</span>
              <Link to="/mypage" className="text-gray-600 hover:text-indigo-600">
                마이페이지
              </Link>
              <button
                type="button"
                onClick={handleLogout}
                className="rounded border border-gray-300 px-3 py-1 text-gray-700 hover:bg-gray-100"
              >
                로그아웃
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="text-gray-600 hover:text-indigo-600">
                로그인
              </Link>
              <Link to="/signup" className="rounded bg-indigo-600 px-3 py-1 text-white hover:bg-indigo-700">
                회원가입
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  )
}
