import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'
import { useMutation } from '@tanstack/react-query'
import { login } from '../api/auth'
import { getErrorMessage } from '../api/errors'
import { useAuth } from '../auth/AuthContext'
import { FormField } from '../components/FormField'
import { ErrorMessage, InfoMessage, inputClass, primaryButtonClass } from '../components/ui'

interface LoginLocationState {
  from?: string
  message?: string
}

const AFTER_LOGIN_PATH = '/studies'

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const state = (location.state ?? {}) as LoginLocationState
  const { isLoggedIn, loginWithToken } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [formError, setFormError] = useState('')

  const mutation = useMutation({
    mutationFn: login,
    onSuccess: ({ accessToken }) => {
      loginWithToken(accessToken)
      navigate(state.from ?? AFTER_LOGIN_PATH, { replace: true })
    },
  })

  if (isLoggedIn && !mutation.isSuccess) {
    return <Navigate to={AFTER_LOGIN_PATH} replace />
  }

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    if (!email.trim() || !password) {
      setFormError('이메일과 비밀번호를 입력해 주세요.')
      return
    }
    setFormError('')
    mutation.mutate({ email: email.trim(), password })
  }

  const errorText = formError || (mutation.isError ? getErrorMessage(mutation.error) : '')

  return (
    <section className="mx-auto max-w-md">
      <h1 className="mb-6 text-2xl font-bold">로그인</h1>
      <form onSubmit={handleSubmit} noValidate className="space-y-4 rounded-xl bg-white p-5 shadow-sm sm:p-6">
        {state.message && !errorText && <InfoMessage>{state.message}</InfoMessage>}
        {errorText && <ErrorMessage>{errorText}</ErrorMessage>}

        <FormField id="email" label="이메일">
          <input
            id="email"
            type="email"
            autoComplete="email"
            className={inputClass}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </FormField>
        <FormField id="password" label="비밀번호">
          <input
            id="password"
            type="password"
            autoComplete="current-password"
            className={inputClass}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </FormField>

        <button type="submit" disabled={mutation.isPending} className={`${primaryButtonClass} w-full`}>
          {mutation.isPending ? '로그인하는 중...' : '로그인'}
        </button>
        <p className="text-center text-sm text-stone-600">
          아직 회원이 아닌가요?{' '}
          <Link to="/signup" className="font-medium text-amber-700 underline">
            회원가입하러 가기
          </Link>
        </p>
      </form>
    </section>
  )
}
