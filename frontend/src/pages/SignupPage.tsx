import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { signup } from '../api/auth'
import { getErrorMessage } from '../api/error'
import { PART_OPTIONS, type Part } from '../api/types'
import { ErrorMessage } from '../components/StatusMessage'

interface SignupForm {
  email: string
  password: string
  name: string
  nickname: string
  generation: string
  part: Part | ''
}

type FieldErrors = Partial<Record<keyof SignupForm, string>>

const INITIAL_FORM: SignupForm = {
  email: '',
  password: '',
  name: '',
  nickname: '',
  generation: '',
  part: '',
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

// 서버 검증(docs/api-spec.md 1번)과 같은 기준으로 미리 검사해서 필드별 메시지를 보여줘요
function validate(form: SignupForm): FieldErrors {
  const errors: FieldErrors = {}
  if (!EMAIL_PATTERN.test(form.email.trim())) {
    errors.email = '올바른 이메일 형식이 아니에요.'
  }
  if (form.password.length < 8) {
    errors.password = '비밀번호는 8자 이상이어야 해요.'
  }
  if (!form.name.trim()) {
    errors.name = '이름을 입력해 주세요.'
  }
  const nicknameLength = form.nickname.trim().length
  if (nicknameLength < 2 || nicknameLength > 10) {
    errors.nickname = '닉네임은 2~10자여야 해요.'
  }
  const generation = Number(form.generation)
  if (!form.generation.trim() || !Number.isInteger(generation) || generation < 1) {
    errors.generation = '기수는 1 이상의 정수여야 해요.'
  }
  if (!form.part) {
    errors.part = '파트를 선택해 주세요.'
  }
  return errors
}

const inputClass =
  'mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-gray-900 focus:outline-none'

export function SignupPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState<SignupForm>(INITIAL_FORM)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [serverError, setServerError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const updateField = (field: keyof SignupForm, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setServerError(null)

    const errors = validate(form)
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0 || !form.part) {
      return
    }

    setIsSubmitting(true)
    try {
      await signup({
        email: form.email.trim(),
        password: form.password,
        name: form.name.trim(),
        nickname: form.nickname.trim(),
        generation: Number(form.generation),
        part: form.part,
      })
      navigate('/login', { state: { signedUp: true } })
    } catch (error) {
      setServerError(getErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  // 입력창 아래 에러 문구. 스크린리더가 입력창과 함께 읽도록 aria-describedby로 연결해요.
  const renderFieldError = (field: keyof SignupForm) =>
    fieldErrors[field] && (
      <p id={`${field}-error`} className="mt-1 text-xs text-red-600">
        {fieldErrors[field]}
      </p>
    )

  const describedBy = (field: keyof SignupForm) =>
    fieldErrors[field] ? `${field}-error` : undefined

  return (
    <section className="mx-auto max-w-md">
      <h1 className="mb-6 text-2xl font-bold">회원가입</h1>

      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        {serverError && <ErrorMessage message={serverError} />}

        <div>
          <label htmlFor="email" className="text-sm font-medium">
            이메일
          </label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            value={form.email}
            onChange={(e) => updateField('email', e.target.value)}
            aria-invalid={Boolean(fieldErrors.email)}
            aria-describedby={describedBy('email')}
            className={inputClass}
          />
          {renderFieldError('email')}
        </div>

        <div>
          <label htmlFor="password" className="text-sm font-medium">
            비밀번호
          </label>
          <input
            id="password"
            type="password"
            autoComplete="new-password"
            value={form.password}
            onChange={(e) => updateField('password', e.target.value)}
            aria-invalid={Boolean(fieldErrors.password)}
            aria-describedby={describedBy('password')}
            className={inputClass}
          />
          {renderFieldError('password')}
        </div>

        <div>
          <label htmlFor="name" className="text-sm font-medium">
            이름
          </label>
          <input
            id="name"
            type="text"
            autoComplete="name"
            value={form.name}
            onChange={(e) => updateField('name', e.target.value)}
            aria-invalid={Boolean(fieldErrors.name)}
            aria-describedby={describedBy('name')}
            className={inputClass}
          />
          {renderFieldError('name')}
        </div>

        <div>
          <label htmlFor="nickname" className="text-sm font-medium">
            닉네임
          </label>
          <input
            id="nickname"
            type="text"
            value={form.nickname}
            onChange={(e) => updateField('nickname', e.target.value)}
            aria-invalid={Boolean(fieldErrors.nickname)}
            aria-describedby={describedBy('nickname')}
            className={inputClass}
          />
          {renderFieldError('nickname')}
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <label htmlFor="generation" className="text-sm font-medium">
              기수
            </label>
            <input
              id="generation"
              type="number"
              inputMode="numeric"
              min={1}
              value={form.generation}
              onChange={(e) => updateField('generation', e.target.value)}
              aria-invalid={Boolean(fieldErrors.generation)}
              aria-describedby={describedBy('generation')}
              className={inputClass}
            />
            {renderFieldError('generation')}
          </div>

          <div>
            <label htmlFor="part" className="text-sm font-medium">
              파트
            </label>
            <select
              id="part"
              value={form.part}
              onChange={(e) => updateField('part', e.target.value)}
              aria-invalid={Boolean(fieldErrors.part)}
              aria-describedby={describedBy('part')}
              className={inputClass}
            >
              <option value="">선택해 주세요</option>
              {PART_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            {renderFieldError('part')}
          </div>
        </div>

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full rounded-md bg-gray-900 py-2 text-sm font-medium text-white hover:bg-gray-700 disabled:opacity-50"
        >
          {isSubmitting ? '가입하는 중...' : '회원가입'}
        </button>
      </form>

      <p className="mt-4 text-center text-sm text-gray-600">
        이미 계정이 있나요?{' '}
        <Link to="/login" className="font-medium text-gray-900 underline">
          로그인하러 가기
        </Link>
      </p>
    </section>
  )
}
