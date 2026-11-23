import { Link, Navigate, Route, Routes } from 'react-router'
import { ProtectedRoute } from './auth/ProtectedRoute'
import { Layout } from './components/Layout'
import { LoginPage } from './pages/LoginPage'
import { MyPage } from './pages/MyPage'
import { SignupPage } from './pages/SignupPage'
import { StudiesPage } from './pages/StudiesPage'
import { StudyDetailPage } from './pages/StudyDetailPage'
import { StudyFormPage } from './pages/StudyFormPage'

function NotFoundPage() {
  return (
    <div className="text-center">
      <h1 className="mb-2 text-2xl font-bold">페이지를 찾을 수 없어요</h1>
      <Link to="/studies" className="text-indigo-600 hover:underline">
        스터디 목록으로 돌아가기
      </Link>
    </div>
  )
}

function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Navigate to="/studies" replace />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/studies" element={<StudiesPage />} />
        <Route path="/studies/:id" element={<StudyDetailPage />} />

        {/* 로그인이 필요한 화면. /studies/new 는 /studies/:id 보다 먼저 매칭된다(고정 경로가 우선) */}
        <Route element={<ProtectedRoute />}>
          <Route path="/studies/new" element={<StudyFormPage />} />
          <Route path="/studies/:id/edit" element={<StudyFormPage />} />
          <Route path="/mypage" element={<MyPage />} />
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}

export default App
