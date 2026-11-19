import { Navigate, Route, Routes } from 'react-router'
import { RequireAuth } from './auth/RequireAuth'
import { Layout } from './components/Layout'
import { LoginPage } from './pages/LoginPage'
import { MyPage } from './pages/MyPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { SignupPage } from './pages/SignupPage'
import { StudyDetailPage } from './pages/StudyDetailPage'
import { StudyFormPage } from './pages/StudyFormPage'
import { StudyListPage } from './pages/StudyListPage'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Navigate to="/studies" replace />} />
        <Route path="signup" element={<SignupPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="studies" element={<StudyListPage />} />
        <Route
          path="studies/new"
          element={
            <RequireAuth>
              <StudyFormPage />
            </RequireAuth>
          }
        />
        <Route path="studies/:postId" element={<StudyDetailPage />} />
        <Route
          path="studies/:postId/edit"
          element={
            <RequireAuth>
              <StudyFormPage />
            </RequireAuth>
          }
        />
        <Route
          path="mypage"
          element={
            <RequireAuth>
              <MyPage />
            </RequireAuth>
          }
        />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
