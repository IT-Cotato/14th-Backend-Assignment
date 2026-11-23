import { useState, type FormEvent } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { getErrorMessage } from '../api/error'
import { useAuth } from '../auth/useAuth'
import { AuthCard } from '../components/AuthCard'
import { ErrorAlert } from '../components/ErrorAlert'
import { SubmitButton, TextField } from '../components/FormField'

interface LoginErrors {
  email?: string
  password?: string
}

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const signedUp = (location.state as { signedUp?: boolean } | null)?.signedUp === true

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<LoginErrors>({})
  const [serverError, setServerError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (loading) return
    setServerError(null)

    const nextErrors: LoginErrors = {}
    if (!email.trim()) nextErrors.email = '이메일을 입력해 주세요.'
    if (!password) nextErrors.password = '비밀번호를 입력해 주세요.'
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) return

    setLoading(true)
    try {
      await login(email.trim(), password)
      navigate('/studies', { replace: true })
    } catch (error) {
      setServerError(getErrorMessage(error))
      setLoading(false)
    }
  }

  return (
    <AuthCard title="로그인">
      {signedUp && (
        <p role="status" className="mb-4 rounded-md border border-green-200 bg-green-50 px-3 py-2 text-sm text-green-700">
          회원가입이 완료됐어요. 로그인해 주세요.
        </p>
      )}
      <form noValidate onSubmit={handleSubmit} className="space-y-4">
        <ErrorAlert message={serverError} />
        <TextField label="이메일" name="email" type="email" autoComplete="email" placeholder="user@gmail.com"
          value={email} error={errors.email}
          onChange={(e) => { setEmail(e.target.value); setErrors((prev) => ({ ...prev, email: undefined })) }} />
        <TextField label="비밀번호" name="password" type="password" autoComplete="current-password"
          value={password} error={errors.password}
          onChange={(e) => { setPassword(e.target.value); setErrors((prev) => ({ ...prev, password: undefined })) }} />
        <SubmitButton loading={loading} loadingText="로그인 중...">
          로그인
        </SubmitButton>
      </form>
    </AuthCard>
  )
}
