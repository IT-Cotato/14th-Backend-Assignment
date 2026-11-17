import { useId, type InputHTMLAttributes, type SelectHTMLAttributes } from 'react'
import { inputClass } from './styles'

interface FieldErrorProps {
  id: string
  message?: string | undefined
}

function FieldError({ id, message }: FieldErrorProps) {
  if (!message) {
    return null
  }
  return (
    <p id={id} role="alert" className="mt-1 text-sm text-red-600">
      {message}
    </p>
  )
}

type TextFieldProps = {
  label: string
  error?: string | undefined
} & InputHTMLAttributes<HTMLInputElement>

// label과 입력창을 htmlFor로 연결한다. 접근성과 E2E의 getByLabel이 이 연결에 의존한다.
export function TextField({ label, error, id, ...inputProps }: TextFieldProps) {
  const autoId = useId()
  const inputId = id ?? autoId
  const errorId = `${inputId}-error`

  return (
    <div>
      <label htmlFor={inputId} className="mb-1 block text-sm font-medium text-slate-700">
        {label}
      </label>
      <input
        {...inputProps}
        id={inputId}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        className={inputClass}
      />
      <FieldError id={errorId} message={error} />
    </div>
  )
}

type SelectFieldProps = {
  label: string
  error?: string | undefined
} & SelectHTMLAttributes<HTMLSelectElement>

export function SelectField({ label, error, id, children, ...selectProps }: SelectFieldProps) {
  const autoId = useId()
  const selectId = id ?? autoId
  const errorId = `${selectId}-error`

  return (
    <div>
      <label htmlFor={selectId} className="mb-1 block text-sm font-medium text-slate-700">
        {label}
      </label>
      <select
        {...selectProps}
        id={selectId}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        className={inputClass}
      >
        {children}
      </select>
      <FieldError id={errorId} message={error} />
    </div>
  )
}
