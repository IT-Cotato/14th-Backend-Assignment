import { createRoot } from 'react-dom/client'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthForm, AuthProvider, Header, ProfilePage, RequireAuth } from './auth'
import './styles.css'

createRoot(document.getElementById('root')!).render(
  <BrowserRouter>
    <AuthProvider>
      <Header />
      <main className="mx-auto max-w-5xl px-4 py-8">
        <Routes>
          <Route path="/signup" element={<AuthForm key="signup" signup />} />
          <Route path="/login" element={<AuthForm key="login" />} />
          <Route path="/mypage" element={<RequireAuth><ProfilePage /></RequireAuth>} />
          <Route path="/studies" element={<h1>스터디 모집</h1>} />
          <Route path="*" element={<Navigate to="/studies" replace />} />
        </Routes>
      </main>
    </AuthProvider>
  </BrowserRouter>,
)
