import { Link } from 'react-router'
import { useAuth } from '../auth/AuthContext'

const navLinkClass = 'rounded-md px-2 py-1 text-sm font-medium text-slate-700 hover:bg-slate-100'

export function Header() {
  const { isLoggedIn, user, logout } = useAuth()

  return (
    <header className="border-b border-slate-200 bg-white">
      <div className="mx-auto flex max-w-3xl flex-wrap items-center justify-between gap-2 px-4 py-3">
        <Link to="/studies" className="text-lg font-bold text-indigo-600">
          코테이토 스터디
        </Link>
        <nav aria-label="주요 메뉴" className="flex flex-wrap items-center gap-1 sm:gap-2">
          {isLoggedIn ? (
            <>
              {user && <span className="px-2 text-sm text-slate-600">{user.nickname}님</span>}
              <Link to="/mypage" className={navLinkClass}>
                마이페이지
              </Link>
              <button type="button" onClick={() => void logout()} className={navLinkClass}>
                로그아웃
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className={navLinkClass}>
                로그인
              </Link>
              <Link to="/signup" className={navLinkClass}>
                회원가입
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  )
}
