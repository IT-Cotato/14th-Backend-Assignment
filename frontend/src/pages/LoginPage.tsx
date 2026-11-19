import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'

import { getErrorMessage } from '../api/error'
import { useAuth } from '../auth/authContext'
import { ErrorMessage } from '../components/StatusMessage'

interface LoginLocationState {
  from?: string
  signedUp?: boolean
}

const inputClass =
  'mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-gray-900 focus:outline-none'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const state = (location.state ?? {}) as LoginLocationState

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)

    if (!email.trim() || !password) {
      setError('이메일과 비밀번호를 입력해 주세요.')
      return
    }

    setIsSubmitting(true)
    try {
      await login(email.trim(), password)
      // 로그인이 필요한 화면에서 넘어왔으면 그 화면으로, 아니면 목록으로 가요
      navigate(state.from ?? '/studies', { replace: true })
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className="mx-auto max-w-md">
      <h1 className="mb-6 text-2xl font-bold">로그인</h1>

      {state.signedUp && (
        <p role="status" className="mb-4 rounded-md bg-green-50 px-3 py-2 text-sm text-green-700">
          회원가입이 완료됐어요. 로그인해 주세요.
        </p>
      )}

      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        {error && <ErrorMessage message={error} />}

        <div>
          <label htmlFor="login-email" className="text-sm font-medium">
            이메일
          </label>
          <input
            id="login-email"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className={inputClass}
          />
        </div>

        <div>
          <label htmlFor="login-password" className="text-sm font-medium">
            비밀번호
          </label>
          <input
            id="login-password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className={inputClass}
          />
        </div>

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full rounded-md bg-gray-900 py-2 text-sm font-medium text-white hover:bg-gray-700 disabled:opacity-50"
        >
          {isSubmitting ? '로그인하는 중...' : '로그인'}
        </button>
      </form>

      <p className="mt-4 text-center text-sm text-gray-600">
        아직 계정이 없나요?{' '}
        <Link to="/signup" className="font-medium text-gray-900 underline">
          회원가입하러 가기
        </Link>
      </p>
    </section>
  )
}
