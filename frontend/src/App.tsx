import { Link, NavLink, Route, Routes, useNavigate } from 'react-router-dom'
import { useEffect, useState } from 'react'
import api from './api'
import LoginPage from './pages/LoginPage'
import SignupPage from './pages/SignupPage'
import MyPage from './pages/MyPage'
import StudyListPage from './pages/StudyListPage'

function Header() {
  const navigate = useNavigate()
  const [nickname, setNickname] = useState<string | null>(null)
  const loggedIn = Boolean(localStorage.getItem('accessToken'))

  useEffect(() => {
    if (!loggedIn) {
      setNickname(null)
      return
    }
    api.get('/api/users/me')
        .then((res) => setNickname(res.data.data.nickname))
        .catch(() => setNickname(null))
  }, [loggedIn])

  async function logout() {
    try {
      await api.post('/api/auth/logout')
    } catch {
      // 토큰이 이미 만료돼도 화면은 로그아웃
    }
    localStorage.removeItem('accessToken')
    navigate('/login')
  }

  return (
      <header className="flex flex-wrap items-center justify-between gap-3 border-b p-4">
        <Link to="/studies">코테이토 스터디</Link>
        <nav className="flex flex-wrap items-center gap-3">
          {loggedIn ? (
              <>
                <span>{nickname}</span>
                <Link to="/mypage">마이페이지</Link>
                <button type="button" onClick={logout}>로그아웃</button>
              </>
          ) : (
              <>
                <Link to="/login">로그인</Link>
                <Link to="/signup">회원가입</Link>
              </>
          )}
        </nav>
      </header>
  )
}

export default function App() {
  return (
      <>
        <Header />
        <Routes>
          <Route path="/" element={<StudyListPage />} />
          <Route path="/studies" element={<StudyListPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />
          <Route path="/mypage" element={<MyPage />} />
        </Routes>
      </>
  )
}