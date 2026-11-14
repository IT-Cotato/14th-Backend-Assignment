import { useState, type ChangeEvent, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { signup } from '../api/auth'
import { getErrorMessage } from '../api/errors'
import { PART_LABELS, PARTS } from '../api/types'
import { ErrorAlert } from '../components/ErrorAlert'
import { FormField } from '../components/FormField'
import { errorId, inputClassName, primaryButtonClassName } from '../components/form'
import { toSignupRequest, validateSignup, type SignupErrors, type SignupForm } from '../validation/signup'

const INITIAL_FORM: SignupForm = {
  email: '',
  password: '',
  name: '',
  nickname: '',
  generation: '',
  part: '',
}

export function SignupPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState<SignupForm>(INITIAL_FORM)
  const [errors, setErrors] = useState<SignupErrors>({})
  const [serverError, setServerError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleChange = (event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = event.target
    setForm((prev) => ({ ...prev, [name]: value }))
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setServerError(null)

    const nextErrors = validateSignup(form)
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) {
      return
    }

    setIsSubmitting(true)
    try {
      await signup(toSignupRequest(form))
      navigate('/login')
    } catch (error) {
      setServerError(getErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  // 입력창마다 같은 접근성 속성을 붙입니다.
  const fieldProps = (field: keyof SignupForm) => ({
    id: `signup-${field}`,
    name: field,
    value: form[field],
    onChange: handleChange,
    'aria-invalid': errors[field] ? true : undefined,
    'aria-describedby': errors[field] ? errorId(`signup-${field}`) : undefined,
    className: inputClassName,
  })

  return (
    <section className="mx-auto w-full max-w-md">
      <h1 className="mb-6 text-2xl font-bold text-gray-900">회원가입</h1>
      <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4 rounded-lg bg-white p-6 shadow-sm">
        <FormField id="signup-email" label="이메일" error={errors.email}>
          <input type="email" autoComplete="email" {...fieldProps('email')} />
        </FormField>

        <FormField id="signup-password" label="비밀번호" error={errors.password}>
          <input type="password" autoComplete="new-password" {...fieldProps('password')} />
        </FormField>

        <FormField id="signup-name" label="이름" error={errors.name}>
          <input type="text" autoComplete="name" {...fieldProps('name')} />
        </FormField>

        <FormField id="signup-nickname" label="닉네임" error={errors.nickname}>
          <input type="text" autoComplete="nickname" {...fieldProps('nickname')} />
        </FormField>

        <FormField id="signup-generation" label="기수" error={errors.generation}>
          <input type="number" inputMode="numeric" min={1} step={1} {...fieldProps('generation')} />
        </FormField>

        <FormField id="signup-part" label="파트" error={errors.part}>
          <select {...fieldProps('part')}>
            <option value="">파트를 선택해 주세요</option>
            {PARTS.map((part) => (
              <option key={part} value={part}>
                {part} ({PART_LABELS[part]})
              </option>
            ))}
          </select>
        </FormField>

        <ErrorAlert message={serverError} />

        <button type="submit" disabled={isSubmitting} className={primaryButtonClassName}>
          {isSubmitting ? '가입 중…' : '회원가입'}
        </button>

        <p className="text-center text-sm text-gray-600">
          이미 계정이 있나요?{' '}
          <Link to="/login" className="font-medium text-blue-600 hover:underline">
            로그인
          </Link>
        </p>
      </form>
    </section>
  )
}
