import type { ReactNode } from 'react'

interface FormFieldProps {
    label: string
    htmlFor: string
    error?: string
    hint?: ReactNode
    children: ReactNode
}

export const inputClass =
    'w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-100 aria-[invalid=true]:border-red-400'

export const primaryButtonClass =
    'inline-flex items-center justify-center rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60'

export const secondaryButtonClass =
    'inline-flex items-center justify-center rounded-lg bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 ring-1 ring-slate-300 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60'

export const dangerButtonClass =
    'inline-flex items-center justify-center rounded-lg bg-white px-4 py-2.5 text-sm font-semibold text-red-600 ring-1 ring-red-200 transition hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-60'

export default function FormField({ label, htmlFor, error, hint, children }: FormFieldProps) {
    return (
        <div className="flex flex-col gap-1.5">
            <div className="flex items-baseline justify-between gap-2">
                <label htmlFor={htmlFor} className="text-sm font-medium text-slate-700">
                    {label}
                </label>
                {hint && <span className="text-xs text-slate-400">{hint}</span>}
            </div>
            {children}
            {error && <p className="text-xs text-red-600">{error}</p>}
        </div>
    )
}

export function FormError({ message }: { message: string | null }) {
    if (!message) return null
    return (
        <p className="rounded-lg bg-red-50 px-3 py-2.5 text-sm text-red-700" role="alert">
            {message}
        </p>
    )
}
