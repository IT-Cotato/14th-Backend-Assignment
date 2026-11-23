import {
  useId,
  type InputHTMLAttributes,
  type ReactNode,
  type SelectHTMLAttributes,
  type TextareaHTMLAttributes,
} from 'react'

const controlClass =
  'block w-full rounded-md border bg-white px-3 py-2 text-base text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2'

function controlStateClass(hasError: boolean) {
  return hasError
    ? 'border-red-400 focus:border-red-500 focus:ring-red-200'
    : 'border-slate-300 focus:border-indigo-500 focus:ring-indigo-200'
}

interface FieldShellProps {
  label: string
  error?: string
  // 글자 수 표시 (예: 12 / 50). 넘으면 빨갛게 보여줘요
  count?: { current: number; max: number }
  children: (ids: { inputId: string; errorId: string }) => ReactNode
}

// label과 입력창을 htmlFor/id로 연결하고, 에러 메시지는 aria-describedby로 연결해요 (getByLabel로 찾을 수 있게)
function FieldShell({ label, error, count, children }: FieldShellProps) {
  const inputId = useId()
  const errorId = useId()
  return (
    <div className="space-y-1">
      <label htmlFor={inputId} className="block text-sm font-medium text-slate-700">
        {label}
      </label>
      {children({ inputId, errorId })}
      {(error || count) && (
        <div className="flex items-start justify-between gap-2">
          {error ? (
            <p id={errorId} className="text-sm text-red-600">
              {error}
            </p>
          ) : (
            <span />
          )}
          {count && (
            <span
              className={`shrink-0 text-xs ${count.current > count.max ? 'text-red-600' : 'text-slate-400'}`}
            >
              {count.current} / {count.max}
            </span>
          )}
        </div>
      )}
    </div>
  )
}

interface TextFieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id'> {
  label: string
  error?: string
  maxCount?: number
}

export function TextField({ label, error, maxCount, className, ...inputProps }: TextFieldProps) {
  const count = maxCount ? { current: String(inputProps.value ?? '').length, max: maxCount } : undefined
  return (
    <FieldShell label={label} error={error} count={count}>
      {({ inputId, errorId }) => (
        <input
          id={inputId}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? errorId : undefined}
          className={`${controlClass} ${controlStateClass(Boolean(error))} ${className ?? ''}`}
          {...inputProps}
        />
      )}
    </FieldShell>
  )
}

interface SelectFieldProps extends Omit<SelectHTMLAttributes<HTMLSelectElement>, 'id'> {
  label: string
  error?: string
  children: ReactNode
}

export function SelectField({ label, error, className, children, ...selectProps }: SelectFieldProps) {
  return (
    <FieldShell label={label} error={error}>
      {({ inputId, errorId }) => (
        <select
          id={inputId}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? errorId : undefined}
          className={`${controlClass} ${controlStateClass(Boolean(error))} ${className ?? ''}`}
          {...selectProps}
        >
          {children}
        </select>
      )}
    </FieldShell>
  )
}

interface TextAreaFieldProps extends Omit<TextareaHTMLAttributes<HTMLTextAreaElement>, 'id'> {
  label: string
  error?: string
  maxCount?: number
  // className으로 min-h를 덧붙이면 기본값과 충돌해서(어느 쪽이 이길지 CSS 순서에 달림) 따로 받아요
  minHeightClass?: string
}

export function TextAreaField({
  label,
  error,
  maxCount,
  minHeightClass = 'min-h-48',
  className,
  ...textareaProps
}: TextAreaFieldProps) {
  const count = maxCount ? { current: String(textareaProps.value ?? '').length, max: maxCount } : undefined
  return (
    <FieldShell label={label} error={error} count={count}>
      {({ inputId, errorId }) => (
        <textarea
          id={inputId}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? errorId : undefined}
          className={`${controlClass} ${controlStateClass(Boolean(error))} ${minHeightClass} resize-y ${className ?? ''}`}
          {...textareaProps}
        />
      )}
    </FieldShell>
  )
}

export function SubmitButton({ loading, children, loadingText }: { loading: boolean; children: ReactNode; loadingText: string }) {
  return (
    <button
      type="submit"
      disabled={loading}
      aria-busy={loading}
      className="w-full rounded-md bg-indigo-600 px-4 py-2.5 text-base font-semibold text-white hover:bg-indigo-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-500 disabled:cursor-not-allowed disabled:bg-indigo-300"
    >
      {loading ? loadingText : children}
    </button>
  )
}
