import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router'
import { getErrorMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { ErrorMessage } from '../components/ErrorMessage'
import { cardClass, inputClass, labelClass, primaryButtonClass } from '../components/styles'

export function LoginPage() {
  const { isLoggedIn, login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  const mutation = useMutation({
    mutationFn: login,
    onSuccess: () => navigate('/studies'),
  })

  if (isLoggedIn && !mutation.isPending) {
    return <Navigate to="/studies" replace />
  }

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    mutation.mutate({ email, password })
  }

  return (
    <section className={`mx-auto max-w-md ${cardClass}`}>
      <h1 className="mb-4 text-xl font-bold">로그인</h1>
      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        <div>
          <label htmlFor="login-email" className={labelClass}>
            이메일
          </label>
          <input
            id="login-email"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            className={inputClass}
          />
        </div>
        <div>
          <label htmlFor="login-password" className={labelClass}>
            비밀번호
          </label>
          <input
            id="login-password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            className={inputClass}
          />
        </div>
        <ErrorMessage message={mutation.isError ? getErrorMessage(mutation.error) : null} />
        <button type="submit" disabled={mutation.isPending} className={`w-full ${primaryButtonClass}`}>
          {mutation.isPending ? '로그인하는 중...' : '로그인'}
        </button>
      </form>
      <p className="mt-4 text-center text-sm text-slate-600">
        아직 계정이 없나요?{' '}
        <Link to="/signup" className="font-semibold text-indigo-600 hover:underline">
          회원가입하러 가기
        </Link>
      </p>
    </section>
  )
}
