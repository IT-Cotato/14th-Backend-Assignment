import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api'

type Me = {
    userId: number
    email: string
    name: string
    nickname: string
    generation: number
    part: string
}

export default function MyPage() {
    const navigate = useNavigate()
    const [me, setMe] = useState<Me | null>(null)
    const [error, setError] = useState('')

    useEffect(() => {
        if (!localStorage.getItem('accessToken')) {
            navigate('/login')
            return
        }
        api.get('/api/users/me')
            .then((res) => setMe(res.data.data))
            .catch((err) => setError(err.response?.data?.message ?? '내 정보를 불러오지 못했어요.'))
    }, [navigate])

    if (error) return <p role="alert" className="p-6">{error}</p>
    if (!me) return <p className="p-6">로딩 중</p>

    return (
        <section className="mx-auto max-w-sm space-y-2 p-6">
            <h1 className="text-xl font-bold">마이페이지</h1>
            <p>이메일: {me.email}</p>
            <p>이름: {me.name}</p>
            <p>닉네임: {me.nickname}</p>
            <p>기수: {me.generation}</p>
            <p>파트: {me.part}</p>
        </section>
    )
}