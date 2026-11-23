import { getPartLabel, type Author } from '../api/types'

// "2026-11-17T15:13:41" → "2026-11-17 15:13"
export function formatDateTime(iso: string): string {
  return iso.replace('T', ' ').slice(0, 16)
}

// "길동이 · 14기 · 백엔드 (BE)"
export function formatAuthor(author: Author): string {
  return `${author.nickname} · ${author.generation}기 · ${getPartLabel(author.part)}`
}
