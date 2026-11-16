import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import api from '../api'

type Detail = {
    postId: number
    title: string
    content: string
    studyStatus: string
    author: { nickname: string; generation: number; part: string }
    isAuthor: boolean
}

type Comment = {
    commentId: number
    content: string
    isAuthor: boolean
    author: { nickname: string }
}

export default function StudyDetailPage() {
    const { id } = useParams()
    const navigate = useNavigate()
    const [post, setPost] = useState<Detail | null>(null)
    const [comments, setComments] = useState<Comment[]>([])
    const [content, setContent] = useState('')
    const [error, setError] = useState('')
    const loggedIn = Boolean(localStorage.getItem('accessToken'))

    async function load() {
        const postRes = await api.get(`/api/study-posts/${id}`)
        const commentRes = await api.get(`/api/study-posts/${id}/comments`)
        setPost(postRes.data.data)
        setComments(commentRes.data.data.content)
    }

    useEffect(() => {
        load().catch(() => setError('글을 불러오지 못했어요.'))
    }, [id])

    async function removePost() {
        await api.delete(`/api/study-posts/${id}`)
        navigate('/studies')
    }

    async function closePost() {
        await api.patch(`/api/study-posts/${id}`, { studyStatus: 'CLOSED' })
        await load()
    }

    async function addComment(e: React.FormEvent) {
        e.preventDefault()
        try {
            await api.post(`/api/study-posts/${id}/comments`, { content })
            setContent('')
            await load()
            setError('')
        } catch (err: unknown) {
            const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
            setError(message ?? '댓글을 작성하지 못했어요.')
        }
    }

    async function removeComment(commentId: number) {
        await api.delete(`/api/comments/${commentId}`)
        await load()
    }

    if (!post) return <p className="p-6">로딩 중</p>

    return (
        <section className="space-y-4 p-6">
            <h1 className="text-xl font-bold">{post.title}</h1>
            <p>{post.author.nickname} · {post.author.generation}기 · {post.author.part} · {post.studyStatus}</p>
            <p className="whitespace-pre-wrap">{post.content}</p>
            {post.isAuthor && (
                <div className="flex gap-2">
                    <Link to={`/studies/${id}/edit`}>수정</Link>
                    <button type="button" onClick={closePost}>모집 마감</button>
                    <button type="button" onClick={removePost}>삭제</button>
                </div>
            )}
            <h2 className="font-bold">댓글</h2>
            {error && <p role="alert">{error}</p>}
            {loggedIn && post.studyStatus === 'RECRUITING' && (
                <form onSubmit={addComment} className="space-y-2">
                    <label htmlFor="comment">댓글</label>
                    <input id="comment" value={content} onChange={(e) => setContent(e.target.value)} className="w-full border p-2" required />
                    <button type="submit">댓글 작성</button>
                </form>
            )}
            <ul className="space-y-2">
                {comments.map((c) => (
                    <li key={c.commentId} className="border p-2">
                        <p>{c.author.nickname}: {c.content}</p>
                        {c.isAuthor && <button type="button" onClick={() => removeComment(c.commentId)}>댓글 삭제</button>}
                    </li>
                ))}
            </ul>
        </section>
    )
}