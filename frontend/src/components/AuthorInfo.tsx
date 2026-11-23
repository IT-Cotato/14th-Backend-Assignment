import { PART_LABELS, type Author } from '../api/types'

// 예: 길동이 · 14기 · BE (백엔드)
export function AuthorInfo({ author }: { author: Author }) {
  return (
    <span className="text-sm text-slate-600">
      <span className="font-medium text-slate-800">{author.nickname}</span>
      {' · '}
      {author.generation}기{' · '}
      {author.part} ({PART_LABELS[author.part]})
    </span>
  )
}
