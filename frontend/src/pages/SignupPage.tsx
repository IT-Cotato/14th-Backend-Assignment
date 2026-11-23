import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { signup } from '../api/auth'
import { getErrorMessage } from '../api/client'
import { ErrorMessage } from '../components/ErrorMessage'
import { cardClass, inputClass, labelClass, primaryButtonClass } from '../components/styles'
import { PART_LABELS, PARTS, type Part } from '../types'

interface SignupForm {
  email: string
  password: string
  name: string
  nickname: string
  generation: string
  part: Part | ''
}

const INITIAL_FORM: SignupForm = {
  email: '',
  password: '',
  name: '',
  nickname: '',
  generation: '',
  part: '',
}

/**
 * 보내기 전에 화면에서 먼저 확인하는 검증이다.
 * 기수의 범위(1 이상)는 서버가 검증하고, 그 메시지를 그대로 보여준다.
 */
function validate(form: SignupForm): string | null {
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    return '이메일 형식이 올바르지 않습니다.'
  }
  if (form.password.length < 8) {
    return '비밀번호는 8자 이상이어야 합니다.'
  }
  if (form.name.trim() === '') {
    return '이름을 입력해야 합니다.'
  }
  if (form.nickname.length < 2 || form.nickname.length > 10) {
    return '닉네임은 2자 이상 10자 이하여야 합니다.'
  }
  if (form.generation.trim() === '') {
    return '기수를 입력해야 합니다.'
  }
  if (form.part === '') {
    return '파트를 선택해야 합니다.'
  }
  return null
}

export function SignupPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState<SignupForm>(INITIAL_FORM)
  const [validationMessage, setValidationMessage] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: signup,
    onSuccess: () => navigate('/login'),
  })

  const update = (field: keyof SignupForm, value: string) => {
    setForm((previous) => ({ ...previous, [field]: value }))
  }

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    const message = validate(form)
    setValidationMessage(message)
    if (message || form.part === '') {
      mutation.reset()
      return
    }
    mutation.mutate({
      email: form.email,
      password: form.password,
      name: form.name,
      nickname: form.nickname,
      generation: Number(form.generation),
      part: form.part,
    })
  }

  const errorMessage = validationMessage ?? (mutation.isError ? getErrorMessage(mutation.error) : null)

  return (
    <section className={`mx-auto max-w-md ${cardClass}`}>
      <h1 className="mb-4 text-xl font-bold">회원가입</h1>
      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        <div>
          <label htmlFor="signup-email" className={labelClass}>
            이메일
          </label>
          <input
            id="signup-email"
            type="email"
            autoComplete="email"
            value={form.email}
            onChange={(event) => update('email', event.target.value)}
            className={inputClass}
          />
        </div>
        <div>
          <label htmlFor="signup-password" className={labelClass}>
            비밀번호
          </label>
          <input
            id="signup-password"
            type="password"
            autoComplete="new-password"
            value={form.password}
            onChange={(event) => update('password', event.target.value)}
            className={inputClass}
          />
          <p className="mt-1 text-xs text-slate-500">8자 이상 입력해 주세요.</p>
        </div>
        <div>
          <label htmlFor="signup-name" className={labelClass}>
            이름
          </label>
          <input
            id="signup-name"
            type="text"
            autoComplete="name"
            value={form.name}
            onChange={(event) => update('name', event.target.value)}
            className={inputClass}
          />
        </div>
        <div>
          <label htmlFor="signup-nickname" className={labelClass}>
            닉네임
          </label>
          <input
            id="signup-nickname"
            type="text"
            value={form.nickname}
            onChange={(event) => update('nickname', event.target.value)}
            className={inputClass}
          />
          <p className="mt-1 text-xs text-slate-500">2자 이상 10자 이하로 입력해 주세요.</p>
        </div>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <label htmlFor="signup-generation" className={labelClass}>
              기수
            </label>
            <input
              id="signup-generation"
              type="number"
              inputMode="numeric"
              value={form.generation}
              onChange={(event) => update('generation', event.target.value)}
              className={inputClass}
            />
          </div>
          <div>
            <label htmlFor="signup-part" className={labelClass}>
              파트
            </label>
            <select
              id="signup-part"
              value={form.part}
              onChange={(event) => update('part', event.target.value)}
              className={inputClass}
            >
              <option value="">선택해 주세요</option>
              {PARTS.map((part) => (
                <option key={part} value={part}>
                  {part} ({PART_LABELS[part]})
                </option>
              ))}
            </select>
          </div>
        </div>
        <ErrorMessage message={errorMessage} />
        <button type="submit" disabled={mutation.isPending} className={`w-full ${primaryButtonClass}`}>
          {mutation.isPending ? '가입하는 중...' : '회원가입'}
        </button>
      </form>
      <p className="mt-4 text-center text-sm text-slate-600">
        이미 계정이 있나요?{' '}
        <Link to="/login" className="font-semibold text-indigo-600 hover:underline">
          로그인하러 가기
        </Link>
      </p>
    </section>
  )
}
