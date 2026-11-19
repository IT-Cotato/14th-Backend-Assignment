import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { signup } from '../api/auth'
import { getErrorMessage } from '../api/client'
import { PART_LABELS, PARTS, type Part } from '../api/types'
import { Card, ErrorMessage, Field, inputClass, primaryButton } from '../components/ui'

type Form = { email: string; password: string; name: string; nickname: string; generation: string; part: Part | '' }
type Errors = Partial<Record<keyof Form, string>>

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function validate(form: Form): Errors {
  const errors: Errors = {}
  if (!EMAIL_PATTERN.test(form.email)) errors.email = '이메일 형식이 올바르지 않아요.'
  if (form.password.length < 8) errors.password = '비밀번호는 8자 이상이어야 해요.'
  if (!form.name.trim()) errors.name = '이름을 입력해 주세요.'
  const nick = form.nickname.trim()
  if (nick.length < 2 || nick.length > 10) errors.nickname = '닉네임은 2~10자여야 해요.'
  if (form.generation === '' || !Number.isInteger(Number(form.generation))) errors.generation = '기수를 숫자로 입력해 주세요.'
  if (!form.part) errors.part = '파트를 선택해 주세요.'
  return errors
}

export default function SignupPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState<Form>({ email: '', password: '', name: '', nickname: '', generation: '', part: '' })
  const [errors, setErrors] = useState<Errors>({})
  const [serverError, setServerError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const update = (key: keyof Form) => (e: { target: { value: string } }) =>
    setForm((prev) => ({ ...prev, [key]: e.target.value }))

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setServerError(null)
    const nextErrors = validate(form)
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) return

    setSubmitting(true)
    try {
      // 기수 1 이상 검증은 서버가 해요 (서버 메시지를 그대로 보여줘요)
      await signup({ ...form, nickname: form.nickname.trim(), generation: Number(form.generation), part: form.part as Part })
      navigate('/login', { state: { signedUp: true } })
    } catch (err) {
      setServerError(getErrorMessage(err, '회원가입에 실패했어요.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-md">
      <Card>
        <h1 className="mb-6 text-xl font-bold">회원가입</h1>
        <form onSubmit={handleSubmit} noValidate className="space-y-4">
          <Field id="email" label="이메일" error={errors.email}>
            <input id="email" type="email" autoComplete="email" className={inputClass} value={form.email} onChange={update('email')} />
          </Field>
          <Field id="password" label="비밀번호" error={errors.password}>
            <input id="password" type="password" autoComplete="new-password" className={inputClass} value={form.password} onChange={update('password')} />
          </Field>
          <Field id="name" label="이름" error={errors.name}>
            <input id="name" className={inputClass} value={form.name} onChange={update('name')} />
          </Field>
          <Field id="nickname" label="닉네임" error={errors.nickname}>
            <input id="nickname" className={inputClass} value={form.nickname} onChange={update('nickname')} />
          </Field>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field id="generation" label="기수" error={errors.generation}>
              <input id="generation" type="number" inputMode="numeric" className={inputClass} value={form.generation} onChange={update('generation')} />
            </Field>
            <Field id="part" label="파트" error={errors.part}>
              <select id="part" className={inputClass} value={form.part} onChange={update('part')}>
                <option value="">선택해 주세요</option>
                {PARTS.map((p) => (
                  <option key={p} value={p}>
                    {PART_LABELS[p]} ({p})
                  </option>
                ))}
              </select>
            </Field>
          </div>
          <ErrorMessage message={serverError} />
          <button type="submit" className={`${primaryButton} w-full`} disabled={submitting}>
            {submitting ? '가입 중...' : '회원가입'}
          </button>
        </form>
        <p className="mt-4 text-center text-sm text-gray-600">
          이미 계정이 있나요?{' '}
          <Link to="/login" className="text-indigo-600 hover:underline">
            로그인하러 가기
          </Link>
        </p>
      </Card>
    </div>
  )
}
