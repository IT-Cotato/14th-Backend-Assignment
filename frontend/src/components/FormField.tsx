import type { ReactNode } from 'react'
import { errorId } from './form'

interface FormFieldProps {
  id: string
  label: string
  error?: string
  children: ReactNode
}

// label과 입력창, 에러 메시지를 묶습니다.
// 입력창에는 같은 id와 aria-describedby={errorId(id)}, aria-invalid를 붙여 주세요.
export function FormField({ id, label, error, children }: FormFieldProps) {
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-sm font-medium text-gray-800">
        {label}
      </label>
      {children}
      {error && (
        <p id={errorId(id)} className="text-sm text-red-600">
          {error}
        </p>
      )}
    </div>
  )
}
