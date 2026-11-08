import { Routes, Route, Link, NavLink, Navigate, useNavigate } from 'react-router'
import { useAuth } from './auth/AuthContext'
import RequireAuth from './auth/RequireAuth'
import { useLogout } from './auth/useLogout'
import LoginPage from './pages/LoginPage'
import SignupPage from './pages/SignupPage'
import StudyListPage from './pages/StudyListPage'
import StudyDetailPage from './pages/StudyDetailPage'
import StudyCreatePage from './pages/StudyCreatePage'
import StudyEditPage from './pages/StudyEditPage'
import MyPage from './pages/MyPage'
import MessageState from './components/MessageState'

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
    `text-sm font-medium transition ${isActive ? 'text-blue-600' : 'text-slate-600 hover:text-slate-900'}`

function Header() {
    const { user, isLoggedIn } = useAuth()
    const navigate = useNavigate()
    // 서버에서 로그아웃을 확인하지 못하면 마이페이지에서 다시 시도하거나 이 기기에서만 로그아웃할 수 있어요.
    const { loggingOut, run } = useLogout(() =>
        navigate('/mypage', { state: { logoutUnconfirmed: true } }),
    )

    return (
        <header className="bg-white shadow-sm">
            <div className="mx-auto flex max-w-5xl flex-col gap-3 px-4 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-6">
                <Link to="/studies">
                    <h1 className="text-xl font-bold text-blue-600 sm:text-2xl">코테이토 스터디 매칭</h1>
                </Link>

                <nav className="flex flex-wrap items-center gap-x-5 gap-y-2">
                    <NavLink to="/studies" end className={navLinkClass}>
                        스터디 목록
                    </NavLink>
                    {isLoggedIn ? (
                        <>
                            <NavLink to="/mypage" className={navLinkClass}>
                                마이페이지
                            </NavLink>
                            <span className="text-sm text-slate-500">
                                <span className="font-semibold text-slate-800">{user?.nickname}</span>님
                            </span>
                            <button
                                type="button"
                                onClick={() => void run()}
                                disabled={loggingOut}
                                className="text-sm font-medium text-slate-600 hover:text-slate-900 disabled:opacity-50"
                            >
                                {loggingOut ? '로그아웃 중...' : '로그아웃'}
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

function App() {
    return (
        <div className="min-h-screen bg-slate-100">
            <Header />

            <main className="mx-auto max-w-5xl px-4 py-6 sm:px-6 sm:py-10">
                <Routes>
                    <Route path="/" element={<Navigate to="/studies" replace />} />

                    <Route path="/login" element={<LoginPage />} />

                    <Route path="/signup" element={<SignupPage />} />

                    <Route path="/studies" element={<StudyListPage />} />

                    <Route
                        path="/studies/new"
                        element={
                            <RequireAuth>
                                <StudyCreatePage />
                            </RequireAuth>
                        }
                    />

                    <Route path="/studies/:id" element={<StudyDetailPage />} />

                    <Route
                        path="/studies/:id/edit"
                        element={
                            <RequireAuth>
                                <StudyEditPage />
                            </RequireAuth>
                        }
                    />

                    <Route
                        path="/mypage"
                        element={
                            <RequireAuth>
                                <MyPage />
                            </RequireAuth>
                        }
                    />

                    <Route
                        path="*"
                        element={
                            <MessageState
                                title="페이지를 찾을 수 없어요."
                                action={
                                    <Link to="/studies" className="text-sm font-semibold text-blue-600 hover:underline">
                                        스터디 목록으로
                                    </Link>
                                }
                            />
                        }
                    />
                </Routes>
            </main>
        </div>
    )
}

export default App
