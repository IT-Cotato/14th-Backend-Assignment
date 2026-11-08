import type { Author } from '../types/api'

// 작성자의 닉네임, 기수, 파트를 보여줘요.
export default function AuthorInfo({ author }: { author: Author }) {
    return (
        <span className="inline-flex flex-wrap items-center gap-1.5 text-sm text-slate-600">
            <span className="font-medium text-slate-800">{author.nickname}</span>
            <span className="text-slate-400">·</span>
            <span>{author.generation}기</span>
            <span className="rounded bg-slate-100 px-1.5 py-0.5 text-xs font-semibold text-slate-600">
                {author.part}
            </span>
        </span>
    )
}
