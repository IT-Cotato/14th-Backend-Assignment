import { Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout.tsx'
import ProtectedRoute from './components/ProtectedRoute.tsx'
import LoginPage from './pages/LoginPage.tsx'
import MyPage from './pages/MyPage.tsx'
import SignupPage from './pages/SignupPage.tsx'
import StudiesPage from './pages/StudiesPage.tsx'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Navigate to="/studies" replace />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/studies" element={<StudiesPage />} />
        {/* 로그인이 필요한 화면 */}
        <Route element={<ProtectedRoute />}>
          <Route path="/mypage" element={<MyPage />} />
        </Route>
      </Route>
    </Routes>
  )
}
