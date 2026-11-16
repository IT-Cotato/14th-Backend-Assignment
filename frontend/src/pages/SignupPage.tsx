import { useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { PARTS, signup } from '../api/auth'
import type { Part } from '../api/auth'
import { getErrorMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { ErrorMessage } from '../components/ErrorMessage'
import { FormField } from '../components/FormField'
import { cardClass, inputClass, submitClass } from '../components/styles'

const initialForm = {
  email: '',
  password: '',
  name: '',
  nickname: '',
  generation: '',
  part: '',
}

export function SignupPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState(initialForm)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (user) return <Navigate to="/mypage" replace />

  const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      await signup({
        email: form.email,
        password: form.password,
        name: form.name,
        nickname: form.nickname,
        generation: form.generation === '' ? null : Number(form.generation),
        part: form.part === '' ? null : (form.part as Part),
      })
      navigate('/login', {
        state: { message: '회원가입이 완료되었습니다. 로그인해주세요.' },
      })
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className={cardClass}>
      <h1 className="mb-6 text-2xl font-bold text-gray-900">회원가입</h1>
      <form noValidate onSubmit={handleSubmit} className="flex flex-col gap-4">
        <FormField id="signup-email" label="이메일">
          <input id="signup-email" name="email" type="email" autoComplete="email"
            value={form.email} onChange={handleChange} className={inputClass} />
        </FormField>
        <FormField id="signup-password" label="비밀번호">
          <input id="signup-password" name="password" type="password" autoComplete="new-password"
            value={form.password} onChange={handleChange} className={inputClass} />
        </FormField>
        <FormField id="signup-name" label="이름">
          <input id="signup-name" name="name" type="text" autoComplete="name"
            value={form.name} onChange={handleChange} className={inputClass} />
        </FormField>
        <FormField id="signup-nickname" label="닉네임">
          <input id="signup-nickname" name="nickname" type="text" autoComplete="nickname"
            value={form.nickname} onChange={handleChange} className={inputClass} />
        </FormField>
        <FormField id="signup-generation" label="기수">
          <input id="signup-generation" name="generation" type="number" inputMode="numeric"
            value={form.generation} onChange={handleChange} className={inputClass} />
        </FormField>
        <FormField id="signup-part" label="파트">
          <select id="signup-part" name="part" value={form.part} onChange={handleChange}
            className={inputClass}>
            <option value="">선택하세요</option>
            {PARTS.map((part) => (
              <option key={part} value={part}>
                {part}
              </option>
            ))}
          </select>
        </FormField>

        <ErrorMessage message={error} />

        <button type="submit" disabled={submitting} className={submitClass}>
          {submitting ? '가입 중...' : '회원가입'}
        </button>
      </form>
      <p className="mt-4 text-center text-sm text-gray-600">
        이미 계정이 있나요?{' '}
        <Link to="/login" className="font-medium text-indigo-600 hover:underline">
          로그인하기
        </Link>
      </p>
    </section>
  )
}
