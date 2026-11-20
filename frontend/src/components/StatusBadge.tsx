import { STUDY_STATUS_LABELS, type StudyStatus } from '../types'

export function StatusBadge({ status }: { status: StudyStatus }) {
  const colorClass =
    status === 'RECRUITING' ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-200 text-slate-600'

  return (
    <span className={`shrink-0 rounded-full px-2.5 py-0.5 text-xs font-semibold ${colorClass}`}>
      {STUDY_STATUS_LABELS[status]}
    </span>
  )
}
