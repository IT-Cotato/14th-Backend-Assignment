import { Navigate, Route, Routes } from 'react-router-dom'
import { Header } from './components/Header'
import { RequireAuth } from './components/RequireAuth'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { MyPage } from './pages/MyPage'
import { SignupPage } from './pages/SignupPage'

function App() {
  return (
    <div className="min-h-screen bg-gray-50">
      <Header />
      <main className="mx-auto w-full max-w-md px-4 py-8">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/signup" element={<SignupPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/mypage"
            element={
              <RequireAuth>
                <MyPage />
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
