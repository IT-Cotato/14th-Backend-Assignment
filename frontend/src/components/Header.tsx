import { useState } from 'react'
import { Link, NavLink } from 'react-router'
import { useAuth } from '../auth/AuthContext'

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `rounded-md px-2 py-1 text-sm ${isActive ? 'font-semibold text-amber-700' : 'text-stone-600 hover:text-stone-900'}`

export function Header() {
  const { isLoggedIn, me, logout } = useAuth()
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
    <header className="border-b border-stone-200 bg-white">
      <div className="mx-auto flex max-w-4xl flex-wrap items-center justify-between gap-2 px-4 py-3">
        <Link to="/" className="text-lg font-bold text-stone-900">
          🥔 코테이토 스터디
        </Link>
        <nav aria-label="주요 메뉴" className="flex flex-wrap items-center gap-1 sm:gap-2">
          {isLoggedIn ? (
            <>
              <span className="px-2 text-sm text-stone-700">
                {me ? `${me.nickname}님` : '...'}
              </span>
              <NavLink to="/mypage" className={navLinkClass}>
                마이페이지
              </NavLink>
              <button
                type="button"
                onClick={handleLogout}
                disabled={loggingOut}
                className="rounded-md px-2 py-1 text-sm text-stone-600 hover:text-stone-900 disabled:opacity-60"
              >
                로그아웃
              </button>
            </>
          ) : (
            <>
              <NavLink to="/login" className={navLinkClass}>
                로그인
              </NavLink>
              <NavLink to="/signup" className={navLinkClass}>
                회원가입
              </NavLink>
            </>
          )}
        </nav>
      </div>
    </header>
  )
}
