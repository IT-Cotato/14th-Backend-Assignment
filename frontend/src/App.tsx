import { Navigate, Route, Routes } from 'react-router-dom'

import { RequireAuth } from './auth/RequireAuth'
import { Header } from './components/Header'
import { LoginPage } from './pages/LoginPage'
import { MyPage } from './pages/MyPage'
import { SignupPage } from './pages/SignupPage'
import { StudyDetailPage } from './pages/StudyDetailPage'
import { StudyFormPage } from './pages/StudyFormPage'
import { StudyListPage } from './pages/StudyListPage'

function App() {
  return (
    <div className="min-h-screen bg-gray-50 text-gray-900">
      <Header />
      <main className="mx-auto max-w-3xl px-4 py-6">
        <Routes>
          <Route path="/" element={<Navigate to="/studies" replace />} />
          <Route path="/signup" element={<SignupPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/studies" element={<StudyListPage />} />
          <Route path="/studies/:id" element={<StudyDetailPage />} />

          {/* 로그인이 필요한 화면 */}
          <Route element={<RequireAuth />}>
            <Route path="/mypage" element={<MyPage />} />
            <Route path="/studies/new" element={<StudyFormPage />} />
            <Route path="/studies/:id/edit" element={<StudyFormPage />} />
          </Route>

          <Route path="*" element={<Navigate to="/studies" replace />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
