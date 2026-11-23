import type { ReactNode } from 'react'

interface FormFieldProps {
  id: string
  label: string
  error?: string
  hint?: string
  children: ReactNode
}

/** label 과 입력창, 필드 에러를 묶는다. 입력창에는 id, aria-invalid, aria-describedby 를 직접 넘긴다. */
export function FormField({ id, label, error, hint, children }: FormFieldProps) {
  return (
    <div className="space-y-1.5">
      <label htmlFor={id} className="block text-sm font-medium text-stone-700">
        {label}
      </label>
      {children}
      {hint && !error && <p className="text-xs text-stone-500">{hint}</p>}
      {error && (
        <p id={`${id}-error`} className="text-xs text-red-600">
          {error}
        </p>
      )}
    </div>
  )
}
