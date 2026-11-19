import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import RequireAuth from './auth/RequireAuth'
import Header from './components/Header'
import LoginPage from './pages/LoginPage'
import MyPage from './pages/MyPage'
import SignupPage from './pages/SignupPage'
import StudyDetailPage from './pages/StudyDetailPage'
import StudyFormPage from './pages/StudyFormPage'
import StudyListPage from './pages/StudyListPage'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <div className="min-h-screen bg-gray-50 text-gray-900">
          <Header />
          <main className="mx-auto max-w-4xl px-4 py-6 sm:py-8">
            <Routes>
              <Route path="/" element={<Navigate to="/studies" replace />} />
              <Route path="/signup" element={<SignupPage />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/studies" element={<StudyListPage />} />
              <Route path="/studies/new" element={<RequireAuth><StudyFormPage /></RequireAuth>} />
              <Route path="/studies/:id" element={<StudyDetailPage />} />
              <Route path="/studies/:id/edit" element={<RequireAuth><StudyFormPage /></RequireAuth>} />
              <Route path="/mypage" element={<RequireAuth><MyPage /></RequireAuth>} />
              <Route path="*" element={<Navigate to="/studies" replace />} />
            </Routes>
          </main>
        </div>
      </AuthProvider>
    </BrowserRouter>
  )
}
