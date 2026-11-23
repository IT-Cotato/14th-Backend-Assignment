import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, Navigate, Outlet, Route, Routes, useNavigate } from 'react-router'
import { getProfile, logout, type Profile } from './api/auth'
import { ApiError, errorMessage } from './api/client'
import AuthForm from './pages/AuthForm'
import ProfilePage from './pages/ProfilePage'
import StudyPostDetailPage from './pages/StudyPostDetailPage'
import StudyPostListPage from './pages/StudyPostListPage'
import StudyPostWritePage from './pages/StudyPostWritePage'
import './App.css'

const tokenKey = 'cotato.accessToken'

type ProfileResult = { profile: Profile } | { error: string }

function Layout({ signedIn, nickname, pending, error, onLogout }: {
  signedIn: boolean
  nickname?: string
  pending: boolean
  error: string
  onLogout: () => void
}) {
  return (
    <>
      <header className="header">
        <div className="header-inner">
          <Link className="brand" to="/">코테이토 스터디</Link>
          <nav aria-label="주 메뉴">
            <Link to="/study-posts">모집글</Link>
            {signedIn ? (
              <>
                {nickname && <span>{nickname}</span>}
                <Link to="/me">내 정보</Link>
                <button type="button" disabled={pending} onClick={onLogout}>
                  {pending ? '로그아웃 중…' : '로그아웃'}
                </button>
              </>
            ) : (
              <><Link to="/login">로그인</Link><Link to="/signup">회원가입</Link></>
            )}
          </nav>
        </div>
      </header>
      <main className="main">
        {error && <p className="error" role="alert">{error}</p>}
        <Outlet />
      </main>
    </>
  )
}

export default function App() {
  const navigate = useNavigate()
  const [token, setToken] = useState(() => sessionStorage.getItem(tokenKey))
  const activeToken = useRef(token)
  const [notice, setNotice] = useState('')
  const [logoutPending, setLogoutPending] = useState(false)
  const [logoutError, setLogoutError] = useState('')
  const [profileRequest, setProfileRequest] = useState<{ token: string; result: ProfileResult } | null>(null)
  const [attempt, setAttempt] = useState(0)
  const profileResult = profileRequest?.token === token ? profileRequest?.result : null

  function startSession(value: string) {
    sessionStorage.setItem(tokenKey, value)
    activeToken.current = value
    setToken(value)
    setNotice('')
    setLogoutError('')
  }

  const endSession = useCallback((expectedToken: string, message = '') => {
    if (activeToken.current !== expectedToken) return
    sessionStorage.removeItem(tokenKey)
    activeToken.current = null
    setToken(null)
    setNotice(message)
    setLogoutError('')
  }, [])

  const handleUnauthorized = useCallback((value: string) => {
    endSession(value, '로그인 세션이 만료되었습니다. 다시 로그인해 주세요.')
  }, [endSession])

  useEffect(() => {
    if (!token) return
    const controller = new AbortController()
    getProfile(token, controller.signal).then((profile) => {
      if (!controller.signal.aborted) setProfileRequest({ token, result: { profile } })
    }).catch((error: unknown) => {
      if (controller.signal.aborted) return
      if (error instanceof ApiError && error.status === 401) handleUnauthorized(token)
      else setProfileRequest({ token, result: { error: errorMessage(error) } })
    })
    return () => controller.abort()
  }, [token, attempt, handleUnauthorized])

  function retryProfile() {
    setProfileRequest(null)
    setAttempt((value) => value + 1)
  }

  function handleWriteUnauthorized(value: string) {
    if (activeToken.current !== value) return
    handleUnauthorized(value)
    navigate('/login', { replace: true })
  }

  async function handleLogout() {
    if (!token || logoutPending) return
    setLogoutPending(true)
    setLogoutError('')
    try {
      await logout(token)
      if (activeToken.current === token) {
        endSession(token, '로그아웃되었습니다.')
        navigate('/login', { replace: true })
      }
    } catch (error) {
      if (error instanceof ApiError && error.status === 401 && activeToken.current === token) {
        handleUnauthorized(token)
        navigate('/login', { replace: true })
      } else if (activeToken.current === token) setLogoutError(errorMessage(error))
    } finally {
      setLogoutPending(false)
    }
  }

  return (
    <Routes>
      <Route element={<Layout signedIn={Boolean(token)} nickname={profileResult && 'profile' in profileResult ? profileResult.profile.nickname : undefined} pending={logoutPending} error={logoutError} onLogout={handleLogout} />}>
        <Route index element={<Navigate replace to="/study-posts" />} />
        <Route path="study-posts" element={<StudyPostListPage signedIn={Boolean(token)} />} />
        <Route path="study-posts/:postId" element={<StudyPostDetailPage token={token} onUnauthorized={handleWriteUnauthorized} />} />
        <Route path="study-posts/new" element={token
          ? <StudyPostWritePage key={token} mode="create" token={token} onUnauthorized={handleWriteUnauthorized} />
          : <Navigate replace to="/login" />} />
        <Route path="study-posts/:postId/edit" element={token
          ? <StudyPostWritePage key={token} mode="edit" token={token} onUnauthorized={handleWriteUnauthorized} />
          : <Navigate replace to="/login" />} />
        <Route path="login" element={token ? <Navigate replace to="/me" /> :
          <AuthForm key="login" mode="login" onLogin={startSession} notice={notice} />} />
        <Route path="signup" element={token ? <Navigate replace to="/me" /> :
          <AuthForm key="signup" mode="signup" onLogin={startSession} />} />
        <Route path="me" element={token ? <ProfilePage result={profileResult ?? null} onRetry={retryProfile} /> :
          <Navigate replace to="/login" />} />
        <Route path="*" element={<section className="panel"><h1>페이지를 찾을 수 없습니다</h1>
          <Link to="/">첫 화면으로 이동</Link></section>} />
      </Route>
    </Routes>
  )
}
