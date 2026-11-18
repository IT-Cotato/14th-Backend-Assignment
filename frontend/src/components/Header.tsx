import { Link, useNavigate } from 'react-router'
import { useAuth } from '../auth/useAuth'

export function Header() {
  const { user, isAuthenticated, loading, logout } = useAuth()
  const navigate = useNavigate()

  async function handleLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <header className="border-b border-slate-200 bg-white">
      <div className="mx-auto flex max-w-3xl flex-wrap items-center justify-between gap-2 px-4 py-3">
        <Link to="/studies" className="text-lg font-bold text-indigo-600">
          코테이토 스터디
        </Link>

        {/* 내 정보를 불러오는 동안에는 로그인/비로그인 메뉴가 깜빡이지 않도록 비워 둔다 */}
        {!loading && (
          <nav className="flex items-center gap-3 text-sm">
            {isAuthenticated && user ? (
              <>
                <span className="font-medium text-slate-700">{user.nickname}</span>
                <Link to="/mypage" className="text-slate-600 hover:text-indigo-600">
                  마이페이지
                </Link>
                <button
                  type="button"
                  onClick={handleLogout}
                  className="text-slate-600 hover:text-indigo-600"
                >
                  로그아웃
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="text-slate-600 hover:text-indigo-600">
                  로그인
                </Link>
                <Link to="/signup" className="text-slate-600 hover:text-indigo-600">
                  회원가입
                </Link>
              </>
            )}
          </nav>
        )}
      </div>
    </header>
  )
}
