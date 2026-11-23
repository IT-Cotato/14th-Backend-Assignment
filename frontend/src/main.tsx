import { createRoot } from 'react-dom/client'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthForm, AuthProvider, Header, ProfilePage, RequireAuth } from './auth'
import { StudyDetail, StudyEditor, StudyList } from './studies'
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
          <Route path="/studies" element={<StudyList />} />
          <Route path="/studies/new" element={<RequireAuth><StudyEditor /></RequireAuth>} />
          <Route path="/studies/:id" element={<StudyDetail />} />
          <Route path="/studies/:id/edit" element={<RequireAuth><StudyEditor edit /></RequireAuth>} />
          <Route path="*" element={<Navigate to="/studies" replace />} />
        </Routes>
      </main>
    </AuthProvider>
  </BrowserRouter>,
)
