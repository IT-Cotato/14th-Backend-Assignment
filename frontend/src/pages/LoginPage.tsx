import { useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router'
import { login as loginRequest } from '../api/auth'
import { useAuth } from '../auth/useAuth'
import { TextField } from '../components/FormFields'
import { cardClass, primaryButtonClass } from '../components/styles'
import { getErrorMessage, getFlashMessage } from '../utils/messages'

export function LoginPage() {
  const { isAuthenticated, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  // 회원가입 완료, 로그인 만료 등 다른 화면에서 넘겨준 안내 문구
  const flashMessage = getFlashMessage(location.state)

  if (isAuthenticated) {
    return <Navigate to="/studies" replace />
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const { accessToken } = await loginRequest({ email, password })
      await login(accessToken)
      navigate('/studies', { replace: true })
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-md">
      <h1 className="mb-6 text-2xl font-bold">로그인</h1>

      {flashMessage && (
        <p role="status" className="mb-4 rounded-md bg-indigo-50 px-3 py-2 text-sm text-indigo-700">
          {flashMessage}
        </p>
      )}

      <form onSubmit={handleSubmit} className={`${cardClass} space-y-4`}>
        <TextField
          label="이메일"
          type="email"
          name="email"
          autoComplete="email"
          required
          value={email}
          onChange={(event) => setEmail(event.target.value)}
        />
        <TextField
          label="비밀번호"
          type="password"
          name="password"
          autoComplete="current-password"
          required
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />

        {error && (
          <p role="alert" className="text-sm text-red-600">
            {error}
          </p>
        )}

        <button type="submit" disabled={submitting} className={primaryButtonClass}>
          {submitting ? '로그인 중...' : '로그인'}
        </button>
      </form>
    </div>
  )
}
