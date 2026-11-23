import type { Author } from '../types'

/** "2026-11-08T12:00:00" -> "2026-11-08 12:00" */
export function formatDateTime(value: string): string {
  return value.replace('T', ' ').slice(0, 16)
}

/** 작성자 표시: "길동이 (14기 BE)" */
export function formatAuthor(author: Author): string {
  return `${author.nickname} (${author.generation}기 ${author.part})`
}
