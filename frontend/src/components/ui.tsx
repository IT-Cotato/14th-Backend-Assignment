import type { ReactNode } from 'react'
import { PART_LABEL, type Author } from '../api/types'

export function ErrorMessage({ children }: { children: ReactNode }) {
  return (
    <p role="alert" className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
      {children}
    </p>
  )
}

export function InfoMessage({ children }: { children: ReactNode }) {
  return (
    <p role="status" className="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
      {children}
    </p>
  )
}

export function Loading({ label = '불러오는 중이에요...' }: { label?: string }) {
  return (
    <p role="status" className="py-10 text-center text-sm text-stone-500">
      {label}
    </p>
  )
}

export function AuthorInfo({ author }: { author: Author }) {
  return (
    <span className="text-sm text-stone-600">
      <span className="font-medium text-stone-800">{author.nickname}</span>
      <span className="mx-1 text-stone-300">·</span>
      {author.generation}기
      <span className="mx-1 text-stone-300">·</span>
      {PART_LABEL[author.part]}({author.part})
    </span>
  )
}

export const inputClass =
  'w-full rounded-lg border border-stone-300 bg-white px-3 py-2 text-sm outline-none focus:border-amber-500 focus:ring-2 focus:ring-amber-200 aria-[invalid=true]:border-red-400'

export const primaryButtonClass =
  'inline-flex items-center justify-center rounded-lg bg-amber-500 px-4 py-2 text-sm font-semibold text-white hover:bg-amber-600 disabled:cursor-not-allowed disabled:opacity-60'

export const secondaryButtonClass =
  'inline-flex items-center justify-center rounded-lg border border-stone-300 bg-white px-4 py-2 text-sm font-medium text-stone-700 hover:bg-stone-100 disabled:cursor-not-allowed disabled:opacity-60'

export const dangerButtonClass =
  'inline-flex items-center justify-center rounded-lg border border-red-300 bg-white px-4 py-2 text-sm font-medium text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-60'
