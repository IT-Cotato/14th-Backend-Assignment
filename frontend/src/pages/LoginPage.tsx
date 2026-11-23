import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { getErrorMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { ErrorMessage } from '../components/ErrorMessage'
import { FormField } from '../components/FormField'
import { cardClass, inputClass, submitClass } from '../components/styles'

export function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const notice = (location.state as { message?: string } | null)?.message ?? null

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (user) return <Navigate to="/mypage" replace />

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      await login(email, password)
      navigate('/mypage', { replace: true })
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className={cardClass}>
      <h1 className="mb-6 text-2xl font-bold text-gray-900">로그인</h1>
      {notice && !error && (
        <p role="status"
          className="mb-4 rounded-md border border-green-200 bg-green-50 px-3 py-2 text-sm text-green-700">
          {notice}
        </p>
      )}
      <form noValidate onSubmit={handleSubmit} className="flex flex-col gap-4">
        <FormField id="login-email" label="이메일">
          <input id="login-email" name="email" type="email" autoComplete="email"
            value={email} onChange={(e) => setEmail(e.target.value)} className={inputClass} />
        </FormField>
        <FormField id="login-password" label="비밀번호">
          <input id="login-password" name="password" type="password" autoComplete="current-password"
            value={password} onChange={(e) => setPassword(e.target.value)} className={inputClass} />
        </FormField>

        <ErrorMessage message={error} />

        <button type="submit" disabled={submitting} className={submitClass}>
          {submitting ? '로그인 중...' : '로그인'}
        </button>
      </form>
      <p className="mt-4 text-center text-sm text-gray-600">
        아직 계정이 없나요?{' '}
        <Link to="/signup" className="font-medium text-indigo-600 hover:underline">
          회원가입하기
        </Link>
      </p>
    </section>
  )
}
