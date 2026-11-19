import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { useMutation } from '@tanstack/react-query'
import { signup } from '../api/auth'
import { getErrorMessage } from '../api/errors'
import { PART_LABEL, PARTS, type Part } from '../api/types'
import { FormField } from '../components/FormField'
import { ErrorMessage, inputClass, primaryButtonClass } from '../components/ui'

interface SignupForm {
  email: string
  password: string
  name: string
  nickname: string
  generation: string
  part: Part | ''
}

type FieldErrors = Partial<Record<keyof SignupForm, string>>

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function validate(form: SignupForm): FieldErrors {
  const errors: FieldErrors = {}
  if (!EMAIL_PATTERN.test(form.email.trim())) errors.email = '이메일 형식이 올바르지 않아요.'
  if (form.password.length < 8) errors.password = '비밀번호는 8자 이상으로 입력해 주세요.'
  if (!form.name.trim()) errors.name = '이름을 입력해 주세요.'
  const nicknameLength = form.nickname.trim().length
  if (nicknameLength < 2 || nicknameLength > 10) errors.nickname = '닉네임은 2자 이상 10자 이하로 입력해 주세요.'
  const generation = Number(form.generation)
  if (!/^\d+$/.test(form.generation) || !Number.isInteger(generation) || generation < 1) {
    errors.generation = '기수는 1 이상의 정수로 입력해 주세요.'
  }
  if (!form.part) errors.part = '파트를 선택해 주세요.'
  return errors
}

export function SignupPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState<SignupForm>({
    email: '',
    password: '',
    name: '',
    nickname: '',
    generation: '',
    part: '',
  })
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})

  const mutation = useMutation({
    mutationFn: signup,
    onSuccess: () => {
      navigate('/login', { state: { message: '회원가입이 완료됐어요. 로그인해 주세요.' } })
    },
  })

  const update = (key: keyof SignupForm, value: string) => {
    setForm((prev) => ({ ...prev, [key]: value }))
    setFieldErrors((prev) => ({ ...prev, [key]: undefined }))
  }

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    const errors = validate(form)
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0 || !form.part) return
    mutation.mutate({
      email: form.email.trim(),
      password: form.password,
      name: form.name.trim(),
      nickname: form.nickname.trim(),
      generation: Number(form.generation),
      part: form.part,
    })
  }

  const fieldProps = (key: keyof SignupForm) => ({
    id: key,
    'aria-invalid': fieldErrors[key] ? true : undefined,
    'aria-describedby': fieldErrors[key] ? `${key}-error` : undefined,
    className: inputClass,
  })

  return (
    <section className="mx-auto max-w-md">
      <h1 className="mb-6 text-2xl font-bold">회원가입</h1>
      <form onSubmit={handleSubmit} noValidate className="space-y-4 rounded-xl bg-white p-5 shadow-sm sm:p-6">
        {mutation.isError && <ErrorMessage>{getErrorMessage(mutation.error)}</ErrorMessage>}

        <FormField id="email" label="이메일" error={fieldErrors.email}>
          <input
            {...fieldProps('email')}
            type="email"
            autoComplete="email"
            value={form.email}
            onChange={(e) => update('email', e.target.value)}
          />
        </FormField>
        <FormField id="password" label="비밀번호" error={fieldErrors.password} hint="8자 이상">
          <input
            {...fieldProps('password')}
            type="password"
            autoComplete="new-password"
            value={form.password}
            onChange={(e) => update('password', e.target.value)}
          />
        </FormField>
        <FormField id="name" label="이름" error={fieldErrors.name}>
          <input
            {...fieldProps('name')}
            autoComplete="name"
            value={form.name}
            onChange={(e) => update('name', e.target.value)}
          />
        </FormField>
        <FormField id="nickname" label="닉네임" error={fieldErrors.nickname} hint="2~10자">
          <input
            {...fieldProps('nickname')}
            value={form.nickname}
            onChange={(e) => update('nickname', e.target.value)}
          />
        </FormField>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <FormField id="generation" label="기수" error={fieldErrors.generation}>
            <input
              {...fieldProps('generation')}
              type="number"
              inputMode="numeric"
              min={1}
              value={form.generation}
              onChange={(e) => update('generation', e.target.value)}
            />
          </FormField>
          <FormField id="part" label="파트" error={fieldErrors.part}>
            <select {...fieldProps('part')} value={form.part} onChange={(e) => update('part', e.target.value)}>
              <option value="">파트를 선택해 주세요</option>
              {PARTS.map((part) => (
                <option key={part} value={part}>
                  {PART_LABEL[part]} ({part})
                </option>
              ))}
            </select>
          </FormField>
        </div>

        <button type="submit" disabled={mutation.isPending} className={`${primaryButtonClass} w-full`}>
          {mutation.isPending ? '가입하는 중...' : '가입하기'}
        </button>
        <p className="text-center text-sm text-stone-600">
          이미 계정이 있나요?{' '}
          <Link to="/login" className="font-medium text-amber-700 underline">
            로그인하러 가기
          </Link>
        </p>
      </form>
    </section>
  )
}
