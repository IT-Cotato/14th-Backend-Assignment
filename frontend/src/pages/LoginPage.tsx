import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api from '../api'

export default function LoginPage() {
    const navigate = useNavigate()
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState('')
    const [loading, setLoading] = useState(false)

    async function onSubmit(e: React.FormEvent) {
        e.preventDefault()
        setError('')
        setLoading(true)
        try {
            const res = await api.post('/api/auth/login', { email, password })
            const token = res.data.data.accessToken
            localStorage.setItem('accessToken', token)
            navigate('/studies')
        } catch (err: unknown) {
            const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
            setError(message ?? '로그인에 실패했어요.')
        } finally {
            setLoading(false)
        }
    }

    return (
        <form onSubmit={onSubmit} className="mx-auto max-w-sm space-y-4 p-6">
            <h1 className="text-xl font-bold">로그인</h1>
            <div>
                <label htmlFor="email">이메일</label>
                <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} className="mt-1 w-full border p-2" required />
            </div>
            <div>
                <label htmlFor="password">비밀번호</label>
                <input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} className="mt-1 w-full border p-2" required />
            </div>
            {error && <p role="alert">{error}</p>}
            <button type="submit" disabled={loading} className="w-full border bg-black p-2 text-white">
                로그인
            </button>
            <Link to="/signup">회원가입</Link>
        </form>
    )
}