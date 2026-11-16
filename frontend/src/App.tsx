import type { ReactNode } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { Header } from './components/Header'
import { RequireAuth } from './components/RequireAuth'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { MyPage } from './pages/MyPage'
import { SignupPage } from './pages/SignupPage'
import { StudyDetailPage } from './pages/StudyDetailPage'
import { StudyFormPage } from './pages/StudyFormPage'
import { StudyListPage } from './pages/StudyListPage'

// 회원 화면은 좁은 폭을 유지한다
function Narrow({ children }: { children: ReactNode }) {
  return <div className="mx-auto w-full max-w-md">{children}</div>
}

function App() {
  return (
    <div className="min-h-screen bg-gray-50">
      <Header />
      <main className="mx-auto w-full max-w-3xl px-4 py-8">
        <Routes>
          <Route path="/" element={<Narrow><HomePage /></Narrow>} />
          <Route path="/signup" element={<Narrow><SignupPage /></Narrow>} />
          <Route path="/login" element={<Narrow><LoginPage /></Narrow>} />
          <Route
            path="/mypage"
            element={
              <RequireAuth>
                <Narrow>
                  <MyPage />
                </Narrow>
              </RequireAuth>
            }
          />
          <Route path="/studies" element={<StudyListPage />} />
          <Route
            path="/studies/new"
            element={
              <RequireAuth>
                <StudyFormPage mode="create" />
              </RequireAuth>
            }
          />
          <Route path="/studies/:id" element={<StudyDetailPage />} />
          <Route
            path="/studies/:id/edit"
            element={
              <RequireAuth>
                <StudyFormPage mode="edit" />
              </RequireAuth>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
