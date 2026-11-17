import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../api'

type Post = {
    postId: number
    title: string
    studyStatus: string
    author: { nickname: string; generation: number; part: string }
    commentCount: number
}

export default function StudyListPage() {
    const [posts, setPosts] = useState<Post[]>([])
    const [keyword, setKeyword] = useState('')
    const [status, setStatus] = useState('')
    const [part, setPart] = useState('')
    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(0)
    const [error, setError] = useState('')

    async function load(nextPage = 0) {
        try {
            const res = await api.get('/api/study-posts', {
                params: {
                    keyword: keyword || undefined,
                    status: status || undefined,
                    part: part || undefined,
                    page: nextPage,
                    size: 10,
                },
            })
            const data = res.data.data
            setPosts(data.content)
            setPage(data.page)
            setTotalPages(data.totalPages)
            setError('')
        } catch (err: unknown) {
            const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
            setError(message ?? '목록을 불러오지 못했어요.')
        }
    }

    useEffect(() => {
        load(0)
    }, [])

    return (
        <section className="space-y-4 p-6">
            <h1 className="text-xl font-bold">스터디 목록</h1>
            <form
                className="flex flex-wrap gap-2"
                onSubmit={(e) => {
                    e.preventDefault()
                    load(0)
                }}
            >
                <div>
                    <label htmlFor="keyword">검색</label>
                    <input id="keyword" value={keyword} onChange={(e) => setKeyword(e.target.value)} className="ml-2 border p-1" />
                </div>
                <div>
                    <label htmlFor="status">모집 상태</label>
                    <select id="status" value={status} onChange={(e) => setStatus(e.target.value)} className="ml-2 border p-1">
                        <option value="">전체</option>
                        <option value="RECRUITING">RECRUITING</option>
                        <option value="CLOSED">CLOSED</option>
                    </select>
                </div>
                <div>
                    <label htmlFor="part">파트</label>
                    <select id="part" value={part} onChange={(e) => setPart(e.target.value)} className="ml-2 border p-1">
                        <option value="">전체</option>
                        <option value="PM">PM</option>
                        <option value="DE">DE</option>
                        <option value="BE">BE</option>
                        <option value="FE">FE</option>
                    </select>
                </div>
                <button type="submit">검색</button>
            </form>
            {error && <p role="alert">{error}</p>}
            <ul className="space-y-3">
                {posts.map((post) => (
                    <li key={post.postId} className="border p-3">
                        <Link to={`/studies/${post.postId}`}>{post.title}</Link>
                        <p>
                            {post.author.nickname} · {post.author.generation}기 · {post.author.part} · {post.studyStatus}
                        </p>
                    </li>
                ))}
            </ul>
            <div className="flex gap-2">
                <button type="button" disabled={page <= 0} onClick={() => load(page - 1)}>
                    이전
                </button>
                <button type="button" disabled={page + 1 >= totalPages} onClick={() => load(page + 1)}>
                    다음
                </button>
            </div>
        </section>
    )
}