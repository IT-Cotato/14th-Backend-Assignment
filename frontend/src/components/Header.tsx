import { useState } from 'react'
import { Link } from 'react-router'
import { useAuth } from '../auth/AuthContext'

// 로그인 상태에 따라 메뉴가 달라집니다.
export function Header() {
  const { isLoggedIn, me, logout } = useAuth()
  const [isLoggingOut, setIsLoggingOut] = useState(false)

  const handleLogout = async () => {
    setIsLoggingOut(true)
    try {
      await logout()
    } finally {
      setIsLoggingOut(false)
    }
  }

  const linkClassName = 'rounded-md px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-100'

  return (
    <header className="border-b border-gray-200 bg-white">
      <div className="mx-auto flex max-w-3xl flex-wrap items-center justify-between gap-2 px-4 py-3">
        <Link to="/studies" className="text-lg font-bold text-gray-900">
          코테이토 스터디
        </Link>
        <nav aria-label="사용자 메뉴" className="flex flex-wrap items-center gap-1">
          {isLoggedIn ? (
            <>
              <span className="px-2 text-sm font-medium text-gray-900">
                {me ? `${me.nickname}님` : '불러오는 중…'}
              </span>
              <Link to="/mypage" className={linkClassName}>
                마이페이지
              </Link>
              <button
                type="button"
                onClick={handleLogout}
                disabled={isLoggingOut}
                className="rounded-md px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-100 disabled:opacity-50"
              >
                {isLoggingOut ? '로그아웃 중…' : '로그아웃'}
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className={linkClassName}>
                로그인
              </Link>
              <Link to="/signup" className="rounded-md bg-blue-600 px-3 py-1.5 text-sm text-white hover:bg-blue-700">
                회원가입
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  )
}
