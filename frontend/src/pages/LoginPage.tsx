import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import { toApiError } from '../api/errors'
import FormField, { FormError, inputClass, primaryButtonClass } from '../components/FormField'

interface LoginLocationState {
    from?: string
    message?: string
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export default function LoginPage() {
    const { login, isLoggedIn } = useAuth()
    const navigate = useNavigate()
    const location = useLocation()
    const state = (location.state ?? {}) as LoginLocationState
    const redirectTo = state.from && state.from !== '/login' ? state.from : '/studies'

    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [fieldErrors, setFieldErrors] = useState<{ email?: string; password?: string }>({})
    const [formError, setFormError] = useState<string | null>(null)
    const [submitting, setSubmitting] = useState(false)

    if (isLoggedIn && !submitting) {
        return <Navigate to={redirectTo} replace />
    }

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault()
        const errors: typeof fieldErrors = {}
        if (!EMAIL_PATTERN.test(email.trim())) errors.email = '올바른 이메일 형식으로 입력해 주세요.'
        if (!password) errors.password = '비밀번호를 입력해 주세요.'
        setFieldErrors(errors)
        if (Object.keys(errors).length > 0) return

        setSubmitting(true)
        setFormError(null)
        try {
            await login({ email: email.trim(), password })
            navigate(redirectTo, { replace: true })
        } catch (error) {
            setFormError(toApiError(error).message)
            setSubmitting(false)
        }
    }

    return (
        <div className="mx-auto w-full max-w-md rounded-xl bg-white p-6 shadow-sm sm:p-8">
            <h2 className="text-2xl font-bold text-slate-900">로그인</h2>
            <p className="mt-1 text-sm text-slate-500">코테이토 스터디 매칭에 오신 걸 환영해요.</p>

            {state.message && (
                <p className="mt-4 rounded-lg bg-blue-50 px-3 py-2.5 text-sm text-blue-700">{state.message}</p>
            )}

            <form className="mt-6 flex flex-col gap-4" onSubmit={handleSubmit} noValidate>
                <FormField label="이메일" htmlFor="email" error={fieldErrors.email}>
                    <input
                        id="email"
                        type="email"
                        autoComplete="email"
                        className={inputClass}
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        aria-invalid={!!fieldErrors.email}
                        placeholder="user@gmail.com"
                    />
                </FormField>
                <FormField label="비밀번호" htmlFor="password" error={fieldErrors.password}>
                    <input
                        id="password"
                        type="password"
                        autoComplete="current-password"
                        className={inputClass}
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        aria-invalid={!!fieldErrors.password}
                    />
                </FormField>

                <FormError message={formError} />

                <button type="submit" className={primaryButtonClass} disabled={submitting}>
                    {submitting ? '로그인 중...' : '로그인'}
                </button>
            </form>

            <p className="mt-6 text-center text-sm text-slate-500">
                아직 회원이 아니신가요?{' '}
                <Link to="/signup" className="font-semibold text-blue-600 hover:underline">
                    회원가입
                </Link>
            </p>
        </div>
    )
}
