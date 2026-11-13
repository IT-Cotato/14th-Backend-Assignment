import { createContext, useContext, useEffect, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { api, errorMessage, json, parts } from './api'
import type { Profile } from './api'

const Auth = createContext<{
  token: string | null
  profile: Profile | null
  setToken: (token: string | null) => void
} | null>(null)

export function useAuth() {
  const value = useContext(Auth)
  if (!value) throw new Error('AuthProvider required')
  return value
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate()
  const [token, saveToken] = useState(() => sessionStorage.getItem('accessToken'))
  const [profile, setProfile] = useState<Profile | null>(null)

  function setToken(value: string | null) {
    if (value) sessionStorage.setItem('accessToken', value)
    else sessionStorage.removeItem('accessToken')
    setProfile(null)
    saveToken(value)
  }

  useEffect(() => {
    function unauthorized() {
      setToken(null)
      navigate('/login', { replace: true })
    }
    window.addEventListener('unauthorized', unauthorized)
    return () => window.removeEventListener('unauthorized', unauthorized)
  }, [navigate])

  useEffect(() => {
    if (!token) return
    const controller = new AbortController()
    api<Profile>('/users/me', { signal: controller.signal })
      .then(value => { if (!controller.signal.aborted) setProfile(value) })
      .catch(() => { /* Protected screens show their own errors. */ })
    return () => controller.abort()
  }, [token])

  return <Auth.Provider value={{ token, profile, setToken }}>{children}</Auth.Provider>
}

export function RequireAuth({ children }: { children: ReactNode }) {
  return useAuth().token ? children : <Navigate to="/login" replace />
}

export function Header() {
  const { token, profile, setToken } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function logout() {
    setBusy(true)
    setError('')
    try {
      await api('/auth/logout', { method: 'POST' })
      setToken(null)
      navigate('/login', { replace: true })
    } catch (failure) {
      setError(errorMessage(failure))
    } finally {
      setBusy(false)
    }
  }

  return <header className="border-b border-slate-200 bg-white">
    <nav className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-3 px-4 py-4" aria-label="주 메뉴">
      <Link className="font-bold text-indigo-700" to="/studies">COTATO 스터디</Link>
      <div className="flex flex-wrap items-center gap-4 text-sm">
        {token ? <>
          {profile && <span>{profile.nickname}</span>}
          <Link to="/mypage">마이페이지</Link>
          <button type="button" disabled={busy} onClick={logout}>로그아웃</button>
        </> : <><Link to="/login">로그인</Link><Link to="/signup">회원가입</Link></>}
      </div>
      {error && <p role="alert">{error}</p>}
    </nav>
  </header>
}

export function AuthForm({ signup = false }: { signup?: boolean }) {
  const navigate = useNavigate()
  const { setToken } = useAuth()
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const fields = Object.fromEntries(new FormData(event.currentTarget))
    setError('')
    if (signup && (!Number.isInteger(Number(fields.generation)) || Number(fields.generation) < 1)) {
      setError('기수는 1 이상이어야 해요.')
      return
    }
    setBusy(true)
    try {
      if (signup) {
        await api('/auth/signup', json('POST', { ...fields, generation: Number(fields.generation) }))
        navigate('/login')
      } else {
        const result = await api<{ accessToken: string }>('/auth/login', json('POST', fields))
        setToken(result.accessToken)
        navigate('/studies', { replace: true })
      }
    } catch (failure) {
      setError(errorMessage(failure))
    } finally {
      setBusy(false)
    }
  }

  return <section className="panel mx-auto max-w-md">
    <h1>{signup ? '회원가입' : '로그인'}</h1>
    <form onSubmit={submit} className="grid gap-4">
      <label>이메일<input name="email" type="email" required maxLength={254} /></label>
      <label>비밀번호<input name="password" type="password" required minLength={signup ? 8 : undefined}
        maxLength={72} autoComplete={signup ? 'new-password' : 'current-password'} /></label>
      {signup && <>
        <label>이름<input name="name" required maxLength={100} /></label>
        <label>닉네임<input name="nickname" required minLength={2} maxLength={10} /></label>
        <label>기수<input name="generation" type="number" required /></label>
        <label>파트<select name="part" required defaultValue=""><option value="" disabled>파트 선택</option>
          {parts.map(part => <option key={part}>{part}</option>)}</select></label>
      </>}
      {error && <p role="alert">{error}</p>}
      <button className="primary" disabled={busy}>{busy ? '처리 중…' : signup ? '회원가입' : '로그인'}</button>
    </form>
  </section>
}

export function ProfilePage() {
  const [profile, setProfile] = useState<Profile>()
  const [error, setError] = useState('')
  useEffect(() => {
    const controller = new AbortController()
    api<Profile>('/users/me', { signal: controller.signal })
      .then(setProfile).catch(failure => { if (!controller.signal.aborted) setError(errorMessage(failure)) })
    return () => controller.abort()
  }, [])
  return <section className="panel max-w-lg">
    <h1>마이페이지</h1>
    {error ? <p role="alert">{error}</p> : profile ? <dl className="grid grid-cols-[auto_1fr] gap-3">
      <dt>이메일</dt><dd>{profile.email}</dd>
      <dt>이름</dt><dd>{profile.name}</dd>
      <dt>닉네임</dt><dd>{profile.nickname}</dd>
      <dt>기수</dt><dd>{profile.generation}기</dd>
      <dt>파트</dt><dd>{profile.part}</dd>
    </dl> : <p role="status">불러오는 중…</p>}
  </section>
}
