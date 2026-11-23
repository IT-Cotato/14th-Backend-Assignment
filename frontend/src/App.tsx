import { Navigate, Route, Routes } from 'react-router'
import { ProtectedRoute } from './auth/ProtectedRoute'
import { Layout } from './components/Layout'
import { LoginPage } from './pages/LoginPage'
import { MyPage } from './pages/MyPage'
import { SignupPage } from './pages/SignupPage'
import { StudyDetailPage } from './pages/StudyDetailPage'
import { StudyCreatePage, StudyEditPage } from './pages/StudyFormPage'
import { StudyListPage } from './pages/StudyListPage'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Navigate to="/studies" replace />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/studies" element={<StudyListPage />} />
        <Route path="/studies/:id" element={<StudyDetailPage />} />
        <Route element={<ProtectedRoute />}>
          <Route path="/studies/new" element={<StudyCreatePage />} />
          <Route path="/studies/:id/edit" element={<StudyEditPage />} />
          <Route path="/mypage" element={<MyPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/studies" replace />} />
      </Route>
    </Routes>
  )
}
