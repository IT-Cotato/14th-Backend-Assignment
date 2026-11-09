import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import { signup } from '../api/auth'
import { toApiError } from '../api/errors'
import FormField, { FormError, inputClass, primaryButtonClass } from '../components/FormField'
import { PARTS, PART_LABELS, type Part } from '../types/api'

interface SignupForm {
    email: string
    password: string
    passwordConfirm: string
    name: string
    nickname: string
    generation: string
    part: Part | ''
}

type SignupErrors = Partial<Record<keyof SignupForm, string>>

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const INITIAL_FORM: SignupForm = {
    email: '',
    password: '',
    passwordConfirm: '',
    name: '',
    nickname: '',
    generation: '14',
    part: '',
}

// docs/api-spec.md 4-1의 검증 규칙과 같아요.
function validate(form: SignupForm): SignupErrors {
    const errors: SignupErrors = {}
    if (!EMAIL_PATTERN.test(form.email.trim())) errors.email = '올바른 이메일 형식으로 입력해 주세요.'
    if (form.password.length < 8) errors.password = '비밀번호는 8자 이상이어야 해요.'
    if (form.passwordConfirm !== form.password) errors.passwordConfirm = '비밀번호가 일치하지 않아요.'
    if (!form.name.trim()) errors.name = '이름을 입력해 주세요.'
    const nickname = form.nickname.trim()
    if (nickname.length < 2 || nickname.length > 10) errors.nickname = '닉네임은 2~10자로 입력해 주세요.'
    const generation = Number(form.generation)
    if (!Number.isInteger(generation) || generation < 1) errors.generation = '기수는 1 이상의 정수여야 해요.'
    if (!form.part) errors.part = '파트를 선택해 주세요.'
    return errors
}

export default function SignupPage() {
    const { isLoggedIn } = useAuth()
    const navigate = useNavigate()
    const [form, setForm] = useState<SignupForm>(INITIAL_FORM)
    const [errors, setErrors] = useState<SignupErrors>({})
    const [formError, setFormError] = useState<string | null>(null)
    const [submitting, setSubmitting] = useState(false)

    if (isLoggedIn) {
        return <Navigate to="/studies" replace />
    }

    const update = <K extends keyof SignupForm>(key: K, value: SignupForm[K]) => {
        setForm((prev) => ({ ...prev, [key]: value }))
    }

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault()
        const nextErrors = validate(form)
        setErrors(nextErrors)
        if (Object.keys(nextErrors).length > 0 || !form.part) return

        setSubmitting(true)
        setFormError(null)
        try {
            await signup({
                email: form.email.trim(),
                password: form.password,
                name: form.name.trim(),
                nickname: form.nickname.trim(),
                generation: Number(form.generation),
                part: form.part,
            })
            navigate('/login', {
                replace: true,
                state: { message: '회원가입이 완료됐어요. 로그인해 주세요.' },
            })
        } catch (error) {
            setFormError(toApiError(error).message)
            setSubmitting(false)
        }
    }

    return (
        <div className="mx-auto w-full max-w-md rounded-xl bg-white p-6 shadow-sm sm:p-8">
            <h2 className="text-2xl font-bold text-slate-900">회원가입</h2>
            <p className="mt-1 text-sm text-slate-500">코테이토 회원 정보를 입력해 주세요.</p>

            <form className="mt-6 flex flex-col gap-4" onSubmit={handleSubmit} noValidate>
                <FormField label="이메일" htmlFor="email" error={errors.email}>
                    <input
                        id="email"
                        type="email"
                        autoComplete="email"
                        className={inputClass}
                        value={form.email}
                        onChange={(e) => update('email', e.target.value)}
                        aria-invalid={!!errors.email}
                        placeholder="user@gmail.com"
                    />
                </FormField>
                <FormField label="비밀번호" htmlFor="password" error={errors.password} hint="8자 이상">
                    <input
                        id="password"
                        type="password"
                        autoComplete="new-password"
                        className={inputClass}
                        value={form.password}
                        onChange={(e) => update('password', e.target.value)}
                        aria-invalid={!!errors.password}
                    />
                </FormField>
                <FormField label="비밀번호 확인" htmlFor="passwordConfirm" error={errors.passwordConfirm}>
                    <input
                        id="passwordConfirm"
                        type="password"
                        autoComplete="new-password"
                        className={inputClass}
                        value={form.passwordConfirm}
                        onChange={(e) => update('passwordConfirm', e.target.value)}
                        aria-invalid={!!errors.passwordConfirm}
                    />
                </FormField>
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <FormField label="이름" htmlFor="name" error={errors.name}>
                        <input
                            id="name"
                            autoComplete="name"
                            className={inputClass}
                            value={form.name}
                            onChange={(e) => update('name', e.target.value)}
                            aria-invalid={!!errors.name}
                        />
                    </FormField>
                    <FormField label="닉네임" htmlFor="nickname" error={errors.nickname} hint="2~10자">
                        <input
                            id="nickname"
                            className={inputClass}
                            value={form.nickname}
                            onChange={(e) => update('nickname', e.target.value)}
                            aria-invalid={!!errors.nickname}
                            maxLength={10}
                        />
                    </FormField>
                    <FormField label="기수" htmlFor="generation" error={errors.generation}>
                        <input
                            id="generation"
                            type="number"
                            min={1}
                            className={inputClass}
                            value={form.generation}
                            onChange={(e) => update('generation', e.target.value)}
                            aria-invalid={!!errors.generation}
                        />
                    </FormField>
                    <FormField label="파트" htmlFor="part" error={errors.part}>
                        <select
                            id="part"
                            className={inputClass}
                            value={form.part}
                            onChange={(e) => update('part', e.target.value as Part | '')}
                            aria-invalid={!!errors.part}
                        >
                            <option value="">선택해 주세요</option>
                            {PARTS.map((part) => (
                                <option key={part} value={part}>
                                    {PART_LABELS[part]}
                                </option>
                            ))}
                        </select>
                    </FormField>
                </div>

                <FormError message={formError} />

                <button type="submit" className={primaryButtonClass} disabled={submitting}>
                    {submitting ? '가입 중...' : '회원가입'}
                </button>
            </form>

            <p className="mt-6 text-center text-sm text-slate-500">
                이미 회원이신가요?{' '}
                <Link to="/login" className="font-semibold text-blue-600 hover:underline">
                    로그인
                </Link>
            </p>
        </div>
    )
}
