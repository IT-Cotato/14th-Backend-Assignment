import { Link } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

const navLinkClass =
  'rounded-md px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 focus-visible:outline-2 focus-visible:outline-indigo-500'

export function Header() {
  const { user, status, logout } = useAuth()

  return (
    <header className="border-b border-slate-200 bg-white">
      <div className="mx-auto flex max-w-4xl flex-wrap items-center justify-between gap-2 px-4 py-3">
        <Link to="/studies" className="text-lg font-bold text-indigo-600">
          코테이토 스터디
        </Link>

        <nav aria-label="사용자 메뉴" className="flex flex-wrap items-center gap-1">
          {status === 'authenticated' && user ? (
            <>
              <span className="px-2 text-sm text-slate-600">
                <strong className="font-semibold text-slate-900">{user.nickname}</strong>님
              </span>
              <Link to="/mypage" className={navLinkClass}>
                마이페이지
              </Link>
              <button type="button" onClick={logout} className={navLinkClass}>
                로그아웃
              </button>
            </>
          ) : status === 'anonymous' ? (
            <>
              <Link to="/login" className={navLinkClass}>
                로그인
              </Link>
              <Link
                to="/signup"
                className="rounded-md bg-indigo-600 px-3 py-2 text-sm font-medium text-white hover:bg-indigo-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-500"
              >
                회원가입
              </Link>
            </>
          ) : null}
        </nav>
      </div>
    </header>
  )
}
