import { Navigate, Route, Routes } from 'react-router-dom'
import { GuestOnly } from './auth/GuestOnly'
import { RequireAuth } from './auth/RequireAuth'
import { Layout } from './components/Layout'
import { LoginPage } from './pages/LoginPage'
import { MyPage } from './pages/MyPage'
import { SignupPage } from './pages/SignupPage'
import { StudiesPage } from './pages/StudiesPage'
import { StudyCreatePage } from './pages/StudyCreatePage'
import { StudyDetailPage } from './pages/StudyDetailPage'
import { StudyEditPage } from './pages/StudyEditPage'

function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Navigate to="/studies" replace />} />
        <Route path="/studies" element={<StudiesPage />} />
        <Route path="/studies/:id" element={<StudyDetailPage />} />

        <Route element={<GuestOnly />}>
          <Route path="/signup" element={<SignupPage />} />
          <Route path="/login" element={<LoginPage />} />
        </Route>

        <Route element={<RequireAuth />}>
          <Route path="/mypage" element={<MyPage />} />
          {/* 고정 경로 new가 :id보다 먼저 맞춰져서 /studies/new가 상세로 가지 않아요 */}
          <Route path="/studies/new" element={<StudyCreatePage />} />
          <Route path="/studies/:id/edit" element={<StudyEditPage />} />
        </Route>

        <Route path="*" element={<Navigate to="/studies" replace />} />
      </Route>
    </Routes>
  )
}

export default App
