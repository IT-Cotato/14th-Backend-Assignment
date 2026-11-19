import type { ReactNode } from 'react'
import type { Author } from '../api/types'
import { PART_LABELS } from '../api/types'

export const inputClass =
  'w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500'
export const primaryButton =
  'rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50'
export const secondaryButton =
  'rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50 disabled:opacity-50'
export const dangerButton =
  'rounded-md border border-red-300 bg-white px-3 py-1.5 text-sm text-red-600 hover:bg-red-50 disabled:opacity-50'

export function ErrorMessage({ message }: { message: string | null }) {
  if (!message) return null
  return (
    <p role="alert" className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
      {message}
    </p>
  )
}

export function Loading() {
  return <p className="py-8 text-center text-sm text-gray-500">불러오는 중...</p>
}

export function Field({ id, label, error, children }: { id: string; label: string; error?: string; children: ReactNode }) {
  return (
    <div className="space-y-1">
      <label htmlFor={id} className="block text-sm font-medium text-gray-700">
        {label}
      </label>
      {children}
      {error && <p className="text-xs text-red-600">{error}</p>}
    </div>
  )
}

export function AuthorBadge({ author }: { author: Author }) {
  return (
    <span className="text-sm text-gray-600">
      <span className="font-medium text-gray-900">{author.nickname}</span> · {author.generation}기 ·{' '}
      {PART_LABELS[author.part]}({author.part})
    </span>
  )
}

export function formatDate(value: string) {
  return value.replace('T', ' ').slice(0, 16)
}

export function Card({ children }: { children: ReactNode }) {
  return <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm sm:p-6">{children}</div>
}
