import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { signup } from '../api/auth.ts'
import { getErrorMessage } from '../api/client.ts'
import { PART_LABELS, type Part } from '../api/types.ts'
import FormField from '../components/FormField.tsx'

type Errors = Partial<Record<'email' | 'password' | 'name' | 'nickname' | 'generation' | 'part', string>>

export default function SignupPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [name, setName] = useState('')
  const [nickname, setNickname] = useState('')
  const [generation, setGeneration] = useState('')
  const [part, setPart] = useState<Part | ''>('')
  const [errors, setErrors] = useState<Errors>({})
  const [serverError, setServerError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  // 서버와 같은 규칙(docs/api-spec.md)으로 먼저 검사한다
  function validate(): Errors {
    const next: Errors = {}
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) next.email = '이메일 형식이 올바르지 않아요.'
    if (password.length < 8) next.password = '비밀번호는 8자 이상이에요.'
    if (name.trim().length === 0) next.name = '이름을 입력해 주세요.'
    if (nickname.length < 2 || nickname.length > 10) next.nickname = '닉네임은 2자 이상 10자 이하예요.'
    const gen = Number(generation)
    if (!Number.isInteger(gen) || gen < 1) next.generation = '기수는 1 이상의 정수예요.'
    if (!part) next.part = '파트를 선택해 주세요.'
    return next
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setServerError('')
    const found = validate()
    setErrors(found)
    if (Object.keys(found).length > 0 || !part) return

    setSubmitting(true)
    try {
      await signup({ email, password, name, nickname, generation: Number(generation), part })
      navigate('/login')
    } catch (error) {
      setServerError(getErrorMessage(error))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-md rounded-lg bg-white p-6 shadow-sm">
      <h1 className="mb-4 text-xl font-bold">회원가입</h1>
      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        <FormField id="email" label="이메일" type="email" value={email} error={errors.email}
          onChange={(e) => setEmail(e.target.value)} autoComplete="email" />
        <FormField id="password" label="비밀번호" type="password" value={password} error={errors.password}
          onChange={(e) => setPassword(e.target.value)} autoComplete="new-password" />
        <FormField id="name" label="이름" value={name} error={errors.name}
          onChange={(e) => setName(e.target.value)} autoComplete="name" />
        <FormField id="nickname" label="닉네임" value={nickname} error={errors.nickname}
          onChange={(e) => setNickname(e.target.value)} />
        <FormField id="generation" label="기수" type="number" value={generation} error={errors.generation}
          onChange={(e) => setGeneration(e.target.value)} />
        <div>
          <label htmlFor="part" className="mb-1 block text-sm font-medium text-gray-700">파트</label>
          <select id="part" value={part} onChange={(e) => setPart(e.target.value as Part | '')}
            className="w-full rounded border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none">
            <option value="">선택해 주세요</option>
            {(Object.keys(PART_LABELS) as Part[]).map((p) => (
              <option key={p} value={p}>{PART_LABELS[p]} ({p})</option>
            ))}
          </select>
          {errors.part ? <p className="mt-1 text-sm text-red-600">{errors.part}</p> : null}
        </div>

        {serverError ? <p role="alert" className="text-sm text-red-600">{serverError}</p> : null}

        <button type="submit" disabled={submitting}
          className="w-full rounded bg-indigo-600 py-2 font-medium text-white hover:bg-indigo-700 disabled:opacity-50">
          {submitting ? '가입 중...' : '가입하기'}
        </button>
      </form>
      <p className="mt-4 text-center text-sm text-gray-600">
        이미 계정이 있나요? <Link to="/login" className="text-indigo-600 hover:underline">로그인</Link>
      </p>
    </div>
  )
}
