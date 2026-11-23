import { STUDY_STATUS_LABEL, type StudyStatus } from '../api/types'

export function StatusBadge({ status }: { status: StudyStatus }) {
  const color =
    status === 'RECRUITING'
      ? 'bg-emerald-100 text-emerald-800 ring-emerald-200'
      : 'bg-stone-200 text-stone-600 ring-stone-300'
  return (
    <span className={`inline-flex shrink-0 items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ${color}`}>
      {STUDY_STATUS_LABEL[status]}
    </span>
  )
}
