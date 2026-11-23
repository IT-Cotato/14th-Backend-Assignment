import { useState, type SubmitEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { login, parts, signup, type Part } from '../api/auth'
import { ApiError, errorMessage } from '../api/client'

type Props = {
  mode: 'login' | 'signup'
  onLogin: (token: string) => void
  notice?: string
}

export default function AuthForm({ mode, onLogin, notice }: Props) {
  const [pending, setPending] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()
  const location = useLocation()
  const isSignup = mode === 'signup'
  const label = isSignup ? '회원가입' : '로그인'

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    if (pending) return

    const form = new FormData(event.currentTarget)
    const email = String(form.get('email')).trim()
    const password = String(form.get('password'))
    setError('')
    setPending(true)
    try {
      if (isSignup) {
        const generation = Number(form.get('generation'))
        const part = String(form.get('part')) as Part
        if (!Number.isInteger(generation) || generation < 1 || !parts.includes(part)) {
          throw new ApiError('기수와 파트를 확인해 주세요.', 400)
        }
        await signup({
          email, password, generation, part,
          name: String(form.get('name')).trim(),
          nickname: String(form.get('nickname')).trim(),
        })
        navigate('/login', { replace: true, state: { signedUp: true, email } })
      } else {
        const { accessToken } = await login({ email, password })
        onLogin(accessToken)
        navigate('/me', { replace: true })
      }
    } catch (error) {
      setError(errorMessage(error))
    } finally {
      setPending(false)
    }
  }

  return (
    <section className="panel" aria-labelledby="auth-heading">
      <h1 id="auth-heading">{label}</h1>
      {!isSignup && location.state?.signedUp && <p className="notice" role="status">가입이 완료되었습니다. 로그인해 주세요.</p>}
      {!isSignup && notice && <p className="notice" role="status">{notice}</p>}
      {error && <p className="error" role="alert">{error}</p>}
      <form onSubmit={handleSubmit} aria-busy={pending}>
        <fieldset disabled={pending}>
          <label>이메일
            <input name="email" type="email" autoComplete="username" maxLength={255} required
              defaultValue={!isSignup && typeof location.state?.email === 'string' ? location.state.email : ''} />
          </label>
          <label>비밀번호
            <input name="password" type="password" autoComplete={isSignup ? 'new-password' : 'current-password'}
              minLength={isSignup ? 8 : undefined} required />
          </label>
          {isSignup && <>
            <label>이름<input name="name" autoComplete="name" maxLength={100} required /></label>
            <label>닉네임<input name="nickname" autoComplete="nickname" minLength={2} maxLength={10} required /></label>
            <div className="form-row">
              <label>기수<input name="generation" type="number" inputMode="numeric" min={1} step={1} required /></label>
              <label>파트
                <select name="part" defaultValue="" required>
                  <option value="" disabled>선택</option>
                  {parts.map((part) => <option key={part}>{part}</option>)}
                </select>
              </label>
            </div>
          </>}
          <button className="primary" type="submit">{pending ? `${label} 중…` : label}</button>
        </fieldset>
      </form>
      <p className="switch">{isSignup ? '이미 계정이 있나요?' : '계정이 없나요?'}{' '}
        <Link to={isSignup ? '/login' : '/signup'}>{isSignup ? '로그인' : '회원가입'}</Link>
      </p>
    </section>
  )
}
