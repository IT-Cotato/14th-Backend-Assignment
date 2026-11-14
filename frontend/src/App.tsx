import { Navigate, Route, Routes } from 'react-router'
import { RequireAuth } from './auth/RequireAuth'
import { Layout } from './components/Layout'
import { LoginPage } from './pages/LoginPage'
import { MyPage } from './pages/MyPage'
import { SignupPage } from './pages/SignupPage'
import { StudiesPage } from './pages/StudiesPage'
import { StudyDetailPage } from './pages/StudyDetailPage'
import { StudyFormPage } from './pages/StudyFormPage'

function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Navigate to="/studies" replace />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/studies" element={<StudiesPage />} />
        <Route
          path="/studies/new"
          element={
            <RequireAuth>
              <StudyFormPage />
            </RequireAuth>
          }
        />
        <Route path="/studies/:id" element={<StudyDetailPage />} />
        <Route
          path="/studies/:id/edit"
          element={
            <RequireAuth>
              <StudyFormPage />
            </RequireAuth>
          }
        />
        <Route
          path="/mypage"
          element={
            <RequireAuth>
              <MyPage />
            </RequireAuth>
          }
        />
        <Route path="*" element={<Navigate to="/studies" replace />} />
      </Route>
    </Routes>
  )
}

export default App
