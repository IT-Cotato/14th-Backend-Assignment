import { STUDY_STATUS_LABELS, type StudyStatus } from '../api/types'

export function StatusBadge({ status }: { status: StudyStatus }) {
  const colorClass =
    status === 'RECRUITING'
      ? 'bg-green-50 text-green-700 ring-green-200'
      : 'bg-slate-100 text-slate-600 ring-slate-200'
  return (
    <span className={`inline-flex shrink-0 items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ring-inset ${colorClass}`}>
      {STUDY_STATUS_LABELS[status]}
    </span>
  )
}
