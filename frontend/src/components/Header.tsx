import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function Header() {
  const { isLoggedIn, me, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  return (
    <header className="border-b border-gray-200 bg-white">
      <div className="mx-auto flex max-w-4xl flex-wrap items-center justify-between gap-2 px-4 py-3">
        <Link to="/studies" className="text-lg font-bold text-indigo-600">
          Cotato Study
        </Link>
        <nav className="flex items-center gap-3 text-sm">
          {isLoggedIn ? (
            <>
              {me && <span className="font-medium text-gray-900">{me.nickname}님</span>}
              <Link to="/mypage" className="text-gray-700 hover:text-indigo-600">
                마이페이지
              </Link>
              <button type="button" onClick={handleLogout} className="text-gray-700 hover:text-indigo-600">
                로그아웃
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="text-gray-700 hover:text-indigo-600">
                로그인
              </Link>
              <Link to="/signup" className="rounded-md bg-indigo-600 px-3 py-1.5 text-white hover:bg-indigo-700">
                회원가입
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  )
}
