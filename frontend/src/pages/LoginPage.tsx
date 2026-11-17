import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { login as loginApi } from '../api/auth.ts'
import { getErrorMessage } from '../api/client.ts'
import { useAuth } from '../auth/useAuth.ts'
import FormField from '../components/FormField.tsx'

export default function LoginPage() {
  const navigate = useNavigate()
  const { login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    if (!email || !password) {
      setError('이메일과 비밀번호를 입력해 주세요.')
      return
    }
    setSubmitting(true)
    try {
      const { accessToken } = await loginApi({ email, password })
      await login(accessToken)
      navigate('/studies')
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-md rounded-lg bg-white p-6 shadow-sm">
      <h1 className="mb-4 text-xl font-bold">로그인</h1>
      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        <FormField id="email" label="이메일" type="email" value={email}
          onChange={(e) => setEmail(e.target.value)} autoComplete="email" />
        <FormField id="password" label="비밀번호" type="password" value={password}
          onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" />
        {error ? <p role="alert" className="text-sm text-red-600">{error}</p> : null}
        <button type="submit" disabled={submitting}
          className="w-full rounded bg-indigo-600 py-2 font-medium text-white hover:bg-indigo-700 disabled:opacity-50">
          {submitting ? '로그인 중...' : '로그인'}
        </button>
      </form>
      <p className="mt-4 text-center text-sm text-gray-600">
        계정이 없나요? <Link to="/signup" className="text-indigo-600 hover:underline">회원가입</Link>
      </p>
    </div>
  )
}
