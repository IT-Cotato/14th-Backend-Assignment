import { useState, type ChangeEvent, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router'
import { signup } from '../api/auth'
import { PARTS } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { SelectField, TextField } from '../components/FormFields'
import { cardClass, primaryButtonClass } from '../components/styles'
import { getErrorMessage } from '../utils/messages'

interface SignupForm {
  email: string
  password: string
  name: string
  nickname: string
  generation: string
  part: string
}

type FormErrors = Partial<Record<keyof SignupForm, string>>

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

// 서버 검증 규칙(docs/api-spec.md)과 같은 기준으로 먼저 확인해서 빠르게 안내한다.
function validate(form: SignupForm): FormErrors {
  const errors: FormErrors = {}

  if (!EMAIL_PATTERN.test(form.email)) {
    errors.email = '올바른 이메일 형식으로 입력해 주세요.'
  }
  if (form.password.length < 8 || form.password.length > 72) {
    errors.password = '비밀번호는 8자 이상 72자 이하로 입력해 주세요.'
  }
  if (form.name.trim().length < 1 || form.name.length > 50) {
    errors.name = '이름을 50자 이내로 입력해 주세요.'
  }
  if (form.nickname.trim().length < 2 || form.nickname.trim().length > 10) {
    errors.nickname = '닉네임은 2자 이상 10자 이하로 입력해 주세요.'
  }
  if (!/^\d+$/.test(form.generation) || Number(form.generation) < 1) {
    errors.generation = '기수는 1 이상의 정수로 입력해 주세요.'
  }
  if (!PARTS.some((part) => part.value === form.part)) {
    errors.part = '파트를 선택해 주세요.'
  }
  return errors
}

export function SignupPage() {
  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()

  const [form, setForm] = useState<SignupForm>({
    email: '',
    password: '',
    name: '',
    nickname: '',
    generation: '',
    part: '',
  })
  const [errors, setErrors] = useState<FormErrors>({})
  const [serverError, setServerError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (isAuthenticated) {
    return <Navigate to="/studies" replace />
  }

  function handleChange(event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) {
    const { name, value } = event.target
    setForm((prev) => ({ ...prev, [name]: value }))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setServerError(null)

    const found = validate(form)
    setErrors(found)
    const part = PARTS.find((p) => p.value === form.part)?.value
    if (Object.keys(found).length > 0 || !part) {
      return
    }

    setSubmitting(true)
    try {
      await signup({
        email: form.email,
        password: form.password,
        name: form.name.trim(),
        nickname: form.nickname.trim(),
        generation: Number(form.generation),
        part,
      })
      navigate('/login', { replace: true, state: { message: '가입이 완료되었어요. 로그인해 주세요.' } })
    } catch (err) {
      setServerError(getErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-md">
      <h1 className="mb-6 text-2xl font-bold">회원가입</h1>

      {/* noValidate: 브라우저 기본 검증 대신 위의 validate가 만든 안내 문구를 보여준다 */}
      <form onSubmit={handleSubmit} noValidate className={`${cardClass} space-y-4`}>
        <TextField
          label="이메일"
          type="email"
          name="email"
          autoComplete="email"
          value={form.email}
          onChange={handleChange}
          error={errors.email}
        />
        <TextField
          label="비밀번호"
          type="password"
          name="password"
          autoComplete="new-password"
          value={form.password}
          onChange={handleChange}
          error={errors.password}
        />
        <TextField
          label="이름"
          type="text"
          name="name"
          autoComplete="name"
          value={form.name}
          onChange={handleChange}
          error={errors.name}
        />
        <TextField
          label="닉네임"
          type="text"
          name="nickname"
          value={form.nickname}
          onChange={handleChange}
          error={errors.nickname}
        />
        <TextField
          label="기수"
          type="number"
          name="generation"
          inputMode="numeric"
          min={1}
          step={1}
          value={form.generation}
          onChange={handleChange}
          error={errors.generation}
        />
        <SelectField
          label="파트"
          name="part"
          value={form.part}
          onChange={handleChange}
          error={errors.part}
        >
          <option value="">선택해 주세요</option>
          {PARTS.map((part) => (
            <option key={part.value} value={part.value}>
              {part.label}
            </option>
          ))}
        </SelectField>

        {serverError && (
          <p role="alert" className="text-sm text-red-600">
            {serverError}
          </p>
        )}

        <button type="submit" disabled={submitting} className={primaryButtonClass}>
          {submitting ? '가입 중...' : '회원가입'}
        </button>
      </form>
    </div>
  )
}
