import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { getErrorMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { Card, ErrorMessage, Field, inputClass, primaryButton } from '../components/ui'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const state = location.state as { signedUp?: boolean; from?: string } | null
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    if (!email || !password) {
      setError('이메일과 비밀번호를 입력해 주세요.')
      return
    }
    setSubmitting(true)
    try {
      await login(email, password)
      navigate(state?.from ?? '/studies', { replace: true })
    } catch (err) {
      setError(getErrorMessage(err, '로그인에 실패했어요.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-md">
      <Card>
        <h1 className="mb-6 text-xl font-bold">로그인</h1>
        {state?.signedUp && (
          <p className="mb-4 rounded-md bg-green-50 px-3 py-2 text-sm text-green-700">회원가입이 완료됐어요. 로그인해 주세요.</p>
        )}
        <form onSubmit={handleSubmit} noValidate className="space-y-4">
          <Field id="email" label="이메일">
            <input id="email" type="email" autoComplete="email" className={inputClass} value={email} onChange={(e) => setEmail(e.target.value)} />
          </Field>
          <Field id="password" label="비밀번호">
            <input id="password" type="password" autoComplete="current-password" className={inputClass} value={password} onChange={(e) => setPassword(e.target.value)} />
          </Field>
          <ErrorMessage message={error} />
          <button type="submit" className={`${primaryButton} w-full`} disabled={submitting}>
            {submitting ? '로그인 중...' : '로그인'}
          </button>
        </form>
        <p className="mt-4 text-center text-sm text-gray-600">
          계정이 없나요?{' '}
          <Link to="/signup" className="text-indigo-600 hover:underline">
            회원가입하러 가기
          </Link>
        </p>
      </Card>
    </div>
  )
}
