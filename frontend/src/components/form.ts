// 입력 폼에서 함께 쓰는 값입니다. (FormField.tsx는 컴포넌트만 내보내도록 분리했습니다)

export function errorId(id: string) {
  return `${id}-error`
}

export const inputClassName =
  'w-full rounded-md border border-gray-300 px-3 py-2 text-base focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200 aria-[invalid=true]:border-red-500'

export const primaryButtonClassName =
  'w-full rounded-md bg-blue-600 px-4 py-2.5 text-base font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60'
