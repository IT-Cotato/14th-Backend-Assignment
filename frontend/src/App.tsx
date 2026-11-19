import { Navigate, Route, Routes } from 'react-router-dom'

import { RequireAuth } from './auth/RequireAuth'
import { Header } from './components/Header'
import { LoginPage } from './pages/LoginPage'
import { MyPage } from './pages/MyPage'
import { SignupPage } from './pages/SignupPage'

function App() {
  return (
    <div className="min-h-screen bg-gray-50 text-gray-900">
      <Header />
      <main className="mx-auto max-w-3xl px-4 py-6">
        <Routes>
          <Route path="/" element={<Navigate to="/studies" replace />} />
          <Route path="/signup" element={<SignupPage />} />
          <Route path="/login" element={<LoginPage />} />
          {/* 모집글 화면은 다음 묶음에서 추가해요 */}
          <Route path="/studies" element={<p className="text-sm text-gray-500">스터디 목록 준비 중</p>} />

          {/* 로그인이 필요한 화면 */}
          <Route element={<RequireAuth />}>
            <Route path="/mypage" element={<MyPage />} />
          </Route>

          <Route path="*" element={<Navigate to="/studies" replace />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
