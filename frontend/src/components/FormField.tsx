import type { InputHTMLAttributes } from 'react'

interface Props extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  id: string
  error?: string
}

// label과 입력창을 짝지어 둔다. (E2E 테스트가 라벨로 입력창을 찾는다)
export default function FormField({ label, id, error, ...inputProps }: Props) {
  return (
    <div>
      <label htmlFor={id} className="mb-1 block text-sm font-medium text-gray-700">
        {label}
      </label>
      <input
        id={id}
        aria-invalid={error ? true : undefined}
        className="w-full rounded border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none"
        {...inputProps}
      />
      {error ? <p className="mt-1 text-sm text-red-600">{error}</p> : null}
    </div>
  )
}
