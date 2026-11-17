import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api from '../api'

export default function SignupPage() {
    const navigate = useNavigate()
    const [form, setForm] = useState({
        email: '',
        password: '',
        name: '',
        nickname: '',
        generation: 14,
        part: 'BE',
    })
    const [error, setError] = useState('')
    const [loading, setLoading] = useState(false)

    function set<K extends keyof typeof form>(key: K, value: (typeof form)[K]) {
        setForm((prev) => ({ ...prev, [key]: value }))
    }

    async function onSubmit(e: React.FormEvent) {
        e.preventDefault()
        setError('')
        setLoading(true)
        try {
            await api.post('/api/auth/signup', {
                ...form,
                generation: Number(form.generation),
            })
            navigate('/login')
        } catch (err: unknown) {
            const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
            setError(message ?? '회원가입에 실패했어요.')
        } finally {
            setLoading(false)
        }
    }

    return (
        <form onSubmit={onSubmit} className="mx-auto max-w-sm space-y-4 p-6">
            <h1 className="text-xl font-bold">회원가입</h1>
            <div>
                <label htmlFor="email">이메일</label>
                <input id="email" type="email" value={form.email} onChange={(e) => set('email', e.target.value)} className="mt-1 w-full border p-2" required />
            </div>
            <div>
                <label htmlFor="password">비밀번호</label>
                <input id="password" type="password" value={form.password} onChange={(e) => set('password', e.target.value)} className="mt-1 w-full border p-2" required minLength={8} />
            </div>
            <div>
                <label htmlFor="name">이름</label>
                <input id="name" value={form.name} onChange={(e) => set('name', e.target.value)} className="mt-1 w-full border p-2" required />
            </div>
            <div>
                <label htmlFor="nickname">닉네임</label>
                <input id="nickname" value={form.nickname} onChange={(e) => set('nickname', e.target.value)} className="mt-1 w-full border p-2" required minLength={2} maxLength={10} />
            </div>
            <div>
                <label htmlFor="generation">기수</label>
                <input id="generation" type="number" min={1} value={form.generation} onChange={(e) => set('generation', Number(e.target.value))} className="mt-1 w-full border p-2" required />
            </div>
            <div>
                <label htmlFor="part">파트</label>
                <select id="part" value={form.part} onChange={(e) => set('part', e.target.value)} className="mt-1 w-full border p-2">
                    <option value="PM">PM</option>
                    <option value="DE">DE</option>
                    <option value="BE">BE</option>
                    <option value="FE">FE</option>
                </select>
            </div>
            {error && <p role="alert">{error}</p>}
            <button type="submit" disabled={loading} className="w-full border bg-black p-2 text-white">
                회원가입
            </button>
            <Link to="/login">로그인</Link>
        </form>
    )
}