import { PART_LABELS, type Author } from '../api/types'

// 서버의 ISO 날짜(2026-11-08T12:00:00)를 화면용 문자열로 바꿉니다.
export function formatDateTime(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) {
    return iso
  }
  return date.toLocaleString('ko-KR', { dateStyle: 'medium', timeStyle: 'short' })
}

// 예: 길동이 · 14기 · BE(백엔드)
export function formatAuthor(author: Author): string {
  return `${author.nickname} · ${author.generation}기 · ${author.part}(${PART_LABELS[author.part]})`
}
