import { getStudyStatusLabel, type StudyStatus } from '../api/types'

const COLORS: Record<StudyStatus, string> = {
  RECRUITING: 'bg-emerald-100 text-emerald-700',
  CLOSED: 'bg-slate-200 text-slate-600',
}

export function StatusBadge({ status }: { status: StudyStatus }) {
  return (
    <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${COLORS[status]}`}>
      {getStudyStatusLabel(status)}
    </span>
  )
}
