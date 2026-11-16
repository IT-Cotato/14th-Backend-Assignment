import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import api from '../api'

export default function StudyFormPage() {
    const { id } = useParams()
    const isEdit = Boolean(id)
    const navigate = useNavigate()
    const [title, setTitle] = useState('')
    const [content, setContent] = useState('')
    const [studyStatus, setStudyStatus] = useState('RECRUITING')
    const [error, setError] = useState('')

    useEffect(() => {
        if (!localStorage.getItem('accessToken')) {
            navigate('/login')
            return
        }
        if (!isEdit) return
        api.get(`/api/study-posts/${id}`).then((res) => {
            const data = res.data.data
            setTitle(data.title)
            setContent(data.content)
            setStudyStatus(data.studyStatus)
        }).catch(() => setError('글을 불러오지 못했어요.'))
    }, [id, isEdit, navigate])

    async function onSubmit(e: React.FormEvent) {
        e.preventDefault()
        try {
            if (isEdit) {
                await api.patch(`/api/study-posts/${id}`, { title, content, studyStatus })
                navigate(`/studies/${id}`)
            } else {
                const res = await api.post('/api/study-posts', { title, content })
                navigate(`/studies/${res.data.data.postId}`)
            }
        } catch (err: unknown) {
            const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
            setError(message ?? '저장에 실패했어요.')
        }
    }

    return (
        <form onSubmit={onSubmit} className="mx-auto max-w-lg space-y-4 p-6">
            <h1 className="text-xl font-bold">{isEdit ? '모집글 수정' : '모집글 작성'}</h1>
            <div>
                <label htmlFor="title">제목</label>
                <input id="title" value={title} onChange={(e) => setTitle(e.target.value)} className="mt-1 w-full border p-2" required maxLength={50} />
            </div>
            <div>
                <label htmlFor="content">내용</label>
                <textarea id="content" value={content} onChange={(e) => setContent(e.target.value)} className="mt-1 w-full border p-2" required maxLength={2000} />
            </div>
            {isEdit && (
                <div>
                    <label htmlFor="studyStatus">모집 상태</label>
                    <select id="studyStatus" value={studyStatus} onChange={(e) => setStudyStatus(e.target.value)} className="mt-1 w-full border p-2">
                        <option value="RECRUITING">RECRUITING</option>
                        <option value="CLOSED">CLOSED</option>
                    </select>
                </div>
            )}
            {error && <p role="alert">{error}</p>}
            <button type="submit">{isEdit ? '수정' : '작성'}</button>
        </form>
    )
}