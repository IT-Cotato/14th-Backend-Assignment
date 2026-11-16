import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const menuClass = 'rounded-md px-2 py-1 text-sm text-gray-700 hover:bg-gray-100'

export function Header() {
  const { user, initializing, logout } = useAuth()
  const [loggingOut, setLoggingOut] = useState(false)

  const handleLogout = async () => {
    setLoggingOut(true)
    try {
      await logout()
    } finally {
      setLoggingOut(false)
    }
  }

  return (
    <header className="border-b border-gray-200 bg-white">
      <div className="mx-auto flex w-full max-w-3xl flex-wrap items-center justify-between gap-2 px-4 py-3">
        <Link to="/" className="text-lg font-bold text-indigo-600">
          COTATO 스터디
        </Link>
        {!initializing && (
          <nav aria-label="사용자 메뉴" className="flex flex-wrap items-center gap-1">
            {user ? (
              <>
                <span className="max-w-[8rem] truncate px-2 text-sm font-medium text-gray-900">
                  {user.nickname}
                </span>
                <Link to="/mypage" className={menuClass}>
                  마이페이지
                </Link>
                <button
                  type="button"
                  onClick={handleLogout}
                  disabled={loggingOut}
                  className={`${menuClass} disabled:opacity-50`}
                >
                  {loggingOut ? '로그아웃 중...' : '로그아웃'}
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className={menuClass}>
                  로그인
                </Link>
                <Link to="/signup" className={menuClass}>
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
