import { Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout.tsx'
import ProtectedRoute from './components/ProtectedRoute.tsx'
import LoginPage from './pages/LoginPage.tsx'
import MyPage from './pages/MyPage.tsx'
import SignupPage from './pages/SignupPage.tsx'
import StudiesPage from './pages/StudiesPage.tsx'
import StudyDetailPage from './pages/StudyDetailPage.tsx'
import StudyFormPage from './pages/StudyFormPage.tsx'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Navigate to="/studies" replace />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/studies" element={<StudiesPage />} />
        <Route path="/studies/:postId" element={<StudyDetailPage />} />
        {/* 로그인이 필요한 화면 */}
        <Route element={<ProtectedRoute />}>
          <Route path="/mypage" element={<MyPage />} />
          <Route path="/studies/new" element={<StudyFormPage />} />
          <Route path="/studies/:postId/edit" element={<StudyFormPage />} />
        </Route>
      </Route>
    </Routes>
  )
}
