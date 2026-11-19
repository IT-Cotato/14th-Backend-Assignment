import { Link, useNavigate } from 'react-router-dom'

import { useAuth } from '../auth/authContext'

// 로그인 상태에 따라 메뉴가 달라져요.
// - 로그인 전: 로그인, 회원가입
// - 로그인 후: 닉네임, 마이페이지, 로그아웃
export function Header() {
  const { user, isLoggedIn, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = async () => {
    try {
      await logout()
    } finally {
      navigate('/login')
    }
  }

  return (
    <header className="border-b border-gray-200 bg-white">
      <div className="mx-auto flex max-w-3xl flex-wrap items-center justify-between gap-2 px-4 py-3">
        <Link to="/studies" className="text-lg font-bold text-gray-900">
          코테이토 스터디
        </Link>

        <nav aria-label="사용자 메뉴" className="flex items-center gap-3 text-sm">
          {isLoggedIn ? (
            <>
              {user && <span className="font-medium text-gray-900">{user.nickname}</span>}
              <Link to="/mypage" className="text-gray-700 hover:underline">
                마이페이지
              </Link>
              <button
                type="button"
                onClick={handleLogout}
                className="rounded-md border border-gray-300 px-3 py-1 text-gray-700 hover:bg-gray-50"
              >
                로그아웃
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="text-gray-700 hover:underline">
                로그인
              </Link>
              <Link
                to="/signup"
                className="rounded-md bg-gray-900 px-3 py-1 text-white hover:bg-gray-700"
              >
                회원가입
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  )
}
