import { useState, type ChangeEvent, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { signup } from '../api/auth'
import { getErrorMessage } from '../api/error'
import { PARTS, PART_LABELS, type Part } from '../api/types'
import { AuthCard } from '../components/AuthCard'
import { ErrorAlert } from '../components/ErrorAlert'
import { SelectField, SubmitButton, TextField } from '../components/FormField'

interface SignupForm {
  email: string
  password: string
  name: string
  nickname: string
  generation: string
  part: Part | ''
}

type SignupErrors = Partial<Record<keyof SignupForm, string>>

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

// 서버(docs/api-spec.md 1번)와 같은 규칙으로 먼저 확인해서, 틀린 칸 바로 아래에 메시지를 보여줘요
function validate(form: SignupForm): SignupErrors {
  const errors: SignupErrors = {}
  if (!form.email.trim()) errors.email = '이메일을 입력해 주세요.'
  else if (!EMAIL_PATTERN.test(form.email.trim())) errors.email = '이메일 형식이 올바르지 않아요.'

  if (!form.password) errors.password = '비밀번호를 입력해 주세요.'
  else if (form.password.length < 8) errors.password = '비밀번호는 8자 이상이어야 해요.'

  if (!form.name.trim()) errors.name = '이름을 입력해 주세요.'

  const nickname = form.nickname.trim()
  if (!nickname) errors.nickname = '닉네임을 입력해 주세요.'
  else if (nickname.length < 2 || nickname.length > 10) errors.nickname = '닉네임은 2~10자로 입력해 주세요.'

  if (!form.generation.trim()) errors.generation = '기수를 입력해 주세요.'
  else if (!/^\d+$/.test(form.generation.trim()) || Number(form.generation) < 1)
    errors.generation = '기수는 1 이상의 정수로 입력해 주세요.'

  if (!form.part) errors.part = '파트를 선택해 주세요.'
  return errors
}

const initialForm: SignupForm = {
  email: '',
  password: '',
  name: '',
  nickname: '',
  generation: '',
  part: '',
}

export function SignupPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState<SignupForm>(initialForm)
  const [errors, setErrors] = useState<SignupErrors>({})
  const [serverError, setServerError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target
    setForm((prev) => ({ ...prev, [name]: value }))
    // 고치기 시작한 칸의 에러는 지워요
    setErrors((prev) => ({ ...prev, [name]: undefined }))
  }

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (loading) return
    setServerError(null)

    const nextErrors = validate(form)
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) return

    setLoading(true)
    try {
      await signup({
        email: form.email.trim(),
        password: form.password,
        name: form.name.trim(),
        nickname: form.nickname.trim(),
        generation: Number(form.generation),
        part: form.part as Part,
      })
      navigate('/login', { state: { signedUp: true } })
    } catch (error) {
      setServerError(getErrorMessage(error))
      setLoading(false)
    }
  }

  return (
    <AuthCard title="회원가입">
      <form noValidate onSubmit={handleSubmit} className="space-y-4">
        <ErrorAlert message={serverError} />
        <TextField label="이메일" name="email" type="email" autoComplete="email" placeholder="user@gmail.com"
          value={form.email} onChange={handleChange} error={errors.email} />
        <TextField label="비밀번호" name="password" type="password" autoComplete="new-password" placeholder="8자 이상"
          value={form.password} onChange={handleChange} error={errors.password} />
        <TextField label="이름" name="name" autoComplete="name" placeholder="홍길동"
          value={form.name} onChange={handleChange} error={errors.name} />
        <TextField label="닉네임" name="nickname" autoComplete="nickname" placeholder="2~10자"
          value={form.nickname} onChange={handleChange} error={errors.nickname} />
        <TextField label="기수" name="generation" type="number" inputMode="numeric" min={1} step={1} placeholder="14"
          value={form.generation} onChange={handleChange} error={errors.generation} />
        <SelectField label="파트" name="part" value={form.part} onChange={handleChange} error={errors.part}>
          <option value="">파트를 선택해 주세요</option>
          {PARTS.map((part) => (
            <option key={part} value={part}>
              {part} ({PART_LABELS[part]})
            </option>
          ))}
        </SelectField>
        <SubmitButton loading={loading} loadingText="가입 중...">
          회원가입
        </SubmitButton>
      </form>
    </AuthCard>
  )
}
