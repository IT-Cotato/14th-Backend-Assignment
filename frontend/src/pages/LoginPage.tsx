import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { getErrorMessage } from '../api/errors'
import { useAuth } from '../auth/AuthContext'
import { ErrorAlert } from '../components/ErrorAlert'
import { FormField } from '../components/FormField'
import { inputClassName, primaryButtonClassName } from '../components/form'

export function LoginPage() {
  const navigate = useNavigate()
  const { login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [serverError, setServerError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setServerError(null)
    setIsSubmitting(true)
    try {
      await login(email.trim(), password)
      navigate('/studies')
    } catch (error) {
      // 예: 401 "이메일 또는 비밀번호가 틀려요." 를 그대로 보여줍니다
      setServerError(getErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className="mx-auto w-full max-w-md">
      <h1 className="mb-6 text-2xl font-bold text-gray-900">로그인</h1>
      <form onSubmit={handleSubmit} className="flex flex-col gap-4 rounded-lg bg-white p-6 shadow-sm">
        <FormField id="login-email" label="이메일">
          <input
            id="login-email"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            className={inputClassName}
          />
        </FormField>

        <FormField id="login-password" label="비밀번호">
          <input
            id="login-password"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            className={inputClassName}
          />
        </FormField>

        <ErrorAlert message={serverError} />

        <button type="submit" disabled={isSubmitting} className={primaryButtonClassName}>
          {isSubmitting ? '로그인 중…' : '로그인'}
        </button>

        <p className="text-center text-sm text-gray-600">
          아직 계정이 없나요?{' '}
          <Link to="/signup" className="font-medium text-blue-600 hover:underline">
            회원가입
          </Link>
        </p>
      </form>
    </section>
  )
}
